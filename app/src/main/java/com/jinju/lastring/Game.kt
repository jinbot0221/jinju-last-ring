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
        "현재의 성문 위로 1593년의 풍경이 겹쳐집니다. 논개가 부탁합니다. ‘맡겨 둔 옥가락지를 찾아 촉석루까지 가져다주세요. 길이 막혔으니 다른 길을 찾아야 해요.’ 나침반 지도에서 성문의 방향을 찾아 첫 길을 엽니다.",
        "공북문의 방향을 화면 나침반으로 맞추세요.","북","공북문은 북쪽 문입니다. 화면 나침반을 북으로 돌리세요.","논개의 부탁과 지도"),
    Spot("성벽 안쪽 · 전달자의 흔적",35.1921,128.0811,
        "가상의 전달자가 성벽 안내판을 가리킵니다. ‘이 성이 사람들을 어떻게 지켰는지 알아내면 다음 길이 열린다.’ 안내판을 촬영하고 글자를 인식해 단서를 찾아보세요.",
        "안내판을 촬영하고 성벽의 기능을 선택하세요.","방어","진주성·성벽·성곽 글자가 보이는 안내판을 촬영하세요. 성벽은 침입을 막는 방어 시설입니다.","성벽의 방어 단서"),
    Spot("촉석루 주변 · 막힌 길",35.1903,128.0847,
        "화면 속 경비가 시간의 조각을 내밉니다. ‘진주성의 두 전투와 후대의 추모를 순서대로 이어라.’ 현재의 관람 동선은 그대로 이용하고 휴대폰 안에서만 사건을 배열합니다.",
        "사건 카드를 시간 순서대로 배열하세요.","0|1|2","1592년 제1차 전투 → 1593년 제2차 전투 → 후대의 의기사 추모.","경비 통과 암호"),
    Spot("의기사 인근 · 옥가락지 보관함",35.1907,128.0840,
        "논개를 기리는 후대의 추모 공간에서 과거의 기억이 열립니다. 가상의 보관인이 세 고리의 상자를 남겼습니다. ‘이 장소가 언제, 누구를, 무엇을 위해 기리는지 기억하라.’ 보관함은 휴대폰 속에만 있습니다.",
        "시기·인물·목적 잠금 고리를 맞추세요.","후대|논개|추모","의기사는 후대에 논개를 추모하기 위해 세워진 사당입니다.","옥가락지"),
    Spot("남강 관람 지점 · 마지막 전달",35.1907,128.0851,
        "화면 속 논개가 당신을 기다립니다. ‘가락지를 가져오셨군요.’ 화면 속 가락지를 그녀에게 건네세요. 의암 바위나 강가로 내려가지 않습니다. 이 좌표는 답사 전 후보입니다.",
        "화면 속 가락지를 오른쪽 끝으로 옮겨 전달하세요.","전달","슬라이더를 오른쪽 끝으로 옮긴 뒤 가락지 건네기를 누르세요.","논개에게 가락지 전달 완료")
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
