package com.jinju.lastring

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(state: GameState, start: () -> Unit, map: () -> Unit, settings: () -> Unit) {
    var showBackground by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        Text("JINJU  ·  1593",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
        Text("진주성\n마지막 가락지",style=MaterialTheme.typography.headlineLarge)
        Text("그녀가 부탁한 것은 가락지였다.\n그 이유는 마지막에야 알게 되었다.",style=MaterialTheme.typography.bodyLarge)
        Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(if(state.completed==spots.size) "옥가락지 전달 완료" else if(state.completed==0) "논개의 부탁이 기다립니다" else "다음 목적지",style=MaterialTheme.typography.titleLarge)
                spots.getOrNull(state.completed)?.let { Text(it.title) }
                Text("임무 ${state.completed} / ${spots.size} 완료",style=MaterialTheme.typography.labelLarge)
                LinearProgressIndicator(progress={state.completed.toFloat()/spots.size},modifier=Modifier.fillMaxWidth())
                Button(onClick=start,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) { Text(if(state.completed==spots.size) "결말 다시 보기" else "여정 시작 / 이어하기") }
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick=map,modifier=Modifier.weight(1f).heightIn(min=52.dp)) { Text("지도 / 연습") }
            OutlinedButton(onClick=settings,modifier=Modifier.weight(1f).heightIn(min=52.dp)) { Text("설정 / 안내") }
        }
        Text("${if(state.developer) "개발 모드 · 집에서도 플레이 가능" else "현장 모드 · 실제 위치로 진행"}\n지도 이동 → 스팟 도착 → 임무 → 다음 이야기",style=MaterialTheme.typography.bodyMedium)
        if(state.completed>0) {
            Text("지금까지 찾은 단서",style=MaterialTheme.typography.titleLarge)
            spots.take(state.completed).forEach { Text("✦  ${it.relic}") }
        }
        HorizontalDivider()
        TextButton(onClick={showBackground=!showBackground},contentPadding=PaddingValues(0.dp)) { Text(if(showBackground) "전승과 창작 안내 접기" else "전승과 창작 안내 보기") }
        if(showBackground) Text(STORY_NOTICE,style=MaterialTheme.typography.bodySmall)
        Text("평소 관람 동선에서 플레이하세요. 실제 잠입이나 의암·위험한 강변 접근은 필요하지 않습니다.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
