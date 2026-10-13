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
val spots = listOf(
    Spot("공북문 · 닫힌 편지",35.1929,128.0827,"성문 아래, 이름 없는 기록관의 편지가 남아 있다. ‘북쪽 문에서 이야기를 시작하라.’", "공북문의 이름에서 방향을 나타내는 한 글자는?", "북", "동서남북 중 하나입니다.","봉인된 편지"),
    Spot("진주성 우물 · 기억의 물",35.1918,128.0816,"편지의 먹빛이 물에 비친다. 물은 성 안 사람들의 기억을 간직하고 있다.","‘기억은 흐르지만 약속은 남는다’에서 기억 다음 두 글자는?", "은흐", "공백을 지우고 ‘기억’ 바로 뒤를 읽으세요.","물빛 조각"),
    Spot("촉석루 · 세 개의 빛",35.1903,128.0847,"누각에 세 단어가 새겨졌다. 용기, 기억, 약속. 마지막 가락지의 암호는 각 단어의 첫 글자다.","용기 · 기억 · 약속의 첫 글자를 순서대로 적으세요.", "용기약", "용 / 기 / 약", "누각의 암호"),
    Spot("의암 · 마지막 가락지",35.1898,128.0851,"강바람 속에서 마지막 기록이 열린다. ‘우리의 이야기를 잊지 않겠다는 약속이 가락지를 완성한다.’", "이 이야기를 잊지 않겠다는 두 글자의 다짐은?", "약속", "촉석루의 세 번째 단어입니다.","마지막 가락지")
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
    private val completedKey = intPreferencesKey("completed")
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
