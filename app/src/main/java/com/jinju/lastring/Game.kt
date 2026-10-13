package com.jinju.lastring

import android.app.Application
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.math.*

val android.content.Context.gameStore by preferencesDataStore("last_ring")
data class Spot(val title: String, val latitude: Double, val longitude: Double, val story: String, val question: String, val answer: String, val hint: String, val relic: String)
const val STORY_NOTICE = "옥가락지 전승을 바탕으로 재구성한 창작 게임입니다. 옥이라는 재질과 가락지를 찾아 전달하는 부탁·잠입·퍼즐은 확정된 역사적 사실이 아닙니다. 의기사는 후대의 추모 공간으로, 현재에서 과거의 기억이 열리는 지점으로 사용합니다."
val spots = listOf(
    Spot("입구 인근 · 논개의 부탁",35.1929,128.0827,
        "현재의 성문 위로 1593년의 풍경이 겹쳐집니다. 논개의 목소리가 들립니다. ‘부탁이 있어요. 맡겨 둔 옥가락지를 찾아 촉석루까지 가져다주세요. 길이 막혔으니 다른 길을 찾아야 해요.’ 찢어진 지도 조각에 이동 순서가 남아 있습니다.",
        "지도 조각: ③ 누각 / ① 성문 / ④ 보관함 / ② 성벽. 성문 → 성벽 → 누각 → 보관함 순서로 숫자를 연결하세요.",
        "1234","장소 앞의 숫자를 이동 순서대로 읽으세요.","논개의 부탁과 지도"),
    Spot("성벽 안쪽 · 전달자의 흔적",35.1921,128.0811,
        "성벽 안쪽의 평범한 산책길에서 가상의 전달자가 남긴 두 그림이 열립니다. ‘바뀐 물건의 이름이 경비를 지날 암호다.’ 실제 시설물에 손대지 않고 휴대폰 속 그림을 비교하세요.",
        "그림 A: 성벽 · 나무 · 빈 고리\n그림 B: 성벽 · 나무 · 등불\n빈 고리 대신 등장한 물건은 무엇인가요?",
        "등불","두 그림의 세 번째 물건을 비교하세요.","등불의 단서"),
    Spot("촉석루 주변 · 막힌 길",35.1903,128.0847,
        "화면 속 경비가 누각으로 가는 길을 막습니다. ‘전달자라면 앞서 찾은 물건과 함께 불을 밝히는 방법도 알겠지.’ 현실의 관람 동선은 그대로 이용하고, 앱 안에서만 통과 암호를 풉니다.",
        "성벽에서 찾은 물건 ‘등불’에 불을 밝히는 한 글자 ‘빛’을 붙여 통과 암호를 적으세요.",
        "등불빛","등불 + 빛. 실제 경비에게 말을 걸 필요는 없습니다.","경비 통과 암호"),
    Spot("의기사 인근 · 옥가락지 보관함",35.1907,128.0840,
        "논개를 기리는 후대의 사당 인근에서 과거의 기억이 열립니다. 가상의 보관인이 남긴 편지에는 ‘문양을 세면 보관함이 열린다’고 쓰여 있습니다. 보관함은 휴대폰 안에만 있습니다.",
        "앱 속 보관함 문양: 달 ☾☾ / 꽃 ✿✿✿ / 물결 ≋\n달 → 꽃 → 물결의 개수를 순서대로 입력하세요.",
        "231","달 2개, 꽃 3개, 물결 1개입니다.","옥가락지"),
    Spot("남강 관람 지점 · 마지막 전달",35.1907,128.0851,
        "의암을 바라볼 수 있는 관람 지점에서 화면 속 논개가 당신을 기다립니다. ‘가락지를 가져오셨군요.’ 마지막 봉인을 풀면 화면 안에서 가락지를 전달할 수 있습니다. 의암 바위나 강가로 내려갈 필요는 없습니다. 이 좌표는 답사 전 후보입니다.",
        "전달 봉인에는 ‘옥 / 가 / 락 / 지’ 네 글자가 흩어져 있습니다. 논개가 부탁한 물건의 이름을 이어 적으세요.",
        "옥가락지","옥 + 가 + 락 + 지", "논개에게 가락지 전달 완료")
)

fun distanceMeters(lat: Double, lon: Double, targetLat: Double, targetLon: Double): Double {
    val dLat = Math.toRadians(targetLat-lat)
    val dLon = Math.toRadians(targetLon-lon)
    val a = sin(dLat/2).pow(2)+cos(Math.toRadians(lat))*cos(Math.toRadians(targetLat))*sin(dLon/2).pow(2)
    return 6371000 * 2 * atan2(sqrt(a.coerceIn(0.0,1.0)),sqrt((1-a).coerceIn(0.0,1.0)))
}
fun answerMatches(input: String, expected: String) = input.filterNot(Char::isWhitespace) == expected

data class GameState(val completed: Int = 0, val developer: Boolean = true, val loaded: Boolean = false, val error: String? = null)
class GameViewModel(app: Application): AndroidViewModel(app) {
    private val completedKey = intPreferencesKey("completed_jade_delivery_v1")
    private val developerKey = booleanPreferencesKey("developer")
    private val errors = MutableStateFlow<String?>(null)
    val state = combine(app.gameStore.data.catch { e ->
        if (e is IOException) { errors.value = "저장된 진행 상황을 읽지 못했습니다. 앱을 다시 실행해주세요."; emit(emptyPreferences()) } else throw e
    }, errors) { p, error -> GameState((p[completedKey] ?: 0).coerceIn(0,spots.size), p[developerKey] ?: true,true,error) }
        .stateIn(viewModelScope,SharingStarted.Eagerly,GameState())
    private fun write(block: (MutablePreferences)->Unit) { viewModelScope.launch {
        try { getApplication<Application>().gameStore.edit(block); errors.value=null }
        catch (_: IOException) { errors.value="저장에 실패했습니다. 저장 공간을 확인하고 다시 시도해주세요." }
    } }
    fun developer(enabled: Boolean) = write { it[developerKey]=enabled }
    fun complete(index: Int) = write { if ((it[completedKey] ?: 0)==index) it[completedKey]=index+1 }
    fun reset() = write { it[completedKey]=0 }
}
