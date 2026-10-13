package com.jinju.lastring

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.google.android.gms.location.*

class MainActivity: ComponentActivity() {
    private val client by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private var location by mutableStateOf<Location?>(null)
    private var precise by mutableStateOf(false)
    private var locationMessage by mutableStateOf("위치를 확인하려면 권한을 허용해주세요.")
    private val callback = object: LocationCallback() {
        override fun onLocationResult(result: LocationResult) { location=result.lastLocation; locationMessage="위치를 갱신했습니다." }
    }
    private val permissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { startLocation() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFFE5C780), background=Color(0xFF101C1B), surface=Color(0xFF1C2C29))) {
                val vm: GameViewModel = viewModel()
                val state by vm.state.collectAsStateWithLifecycle()
                val nav = rememberNavController()
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp)) {
                        Text("JINJU · 1593",color=MaterialTheme.colorScheme.primary)
                        Text("진주성: 마지막 가락지",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        if (!state.loaded) { CircularProgressIndicator(); return@Column }
                        state.error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
                        NavHost(navController=nav,startDestination="home",modifier=Modifier.weight(1f)) {
                            composable("home") {
                                Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                                    Text("강물은 기억하고, 가락지는 약속한다.",style=MaterialTheme.typography.headlineMedium)
                                    Text("진주성의 네 장소를 따라 잊힌 편지를 완성하세요. 역사에서 영감을 받은 창작 이야기입니다.")
                                    Text("진행 ${state.completed} / ${spots.size}")
                                    LinearProgressIndicator(progress={ state.completed.toFloat()/spots.size },modifier=Modifier.fillMaxWidth())
                                    Button(onClick={nav.navigate(if(state.completed==spots.size) "ending" else "spots")}) { Text(if(state.completed==spots.size) "결말 다시 보기" else "여정 시작 / 이어하기") }
                                    OutlinedButton(onClick={nav.navigate("map")}) { Text("현장 지도 / 걷기 테스트") }
                                    OutlinedButton(onClick={nav.navigate("settings")}) { Text("설정 및 플레이 안내") }
                                    Text("수집한 기록",style=MaterialTheme.typography.titleLarge)
                                    spots.take(state.completed).forEach { Text("✦ ${it.relic}") }
                                    Text("현장에서는 통행과 관람 규칙을 지켜주세요. 의암과 강변의 위험 구역에는 접근하지 마세요. 안전한 관람 동선에서 플레이하세요.")
                                }
                            }
                            composable("spots") {
                                Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                                    Text(if(state.developer) "개발 모드 · 위치 없이 도착 처리 가능" else "현장 모드 · 도착 범위 80m")
                                    if(!state.developer) {
                                        Text(locationMessage)
                                        if(!precise) Text("대략적 위치로 거리는 확인할 수 있지만, 도착 판정에는 정확한 위치가 필요합니다.")
                                        OutlinedButton(onClick={permissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION))}) { Text("위치 권한 요청 / 갱신") }
                                    }
                                    OutlinedButton(onClick={nav.navigate("map")}) { Text("지도에서 위치와 도착 확인") }
                                    spots.forEachIndexed { index, spot ->
                                        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                            Text("0${index+1}  ${spot.title}",style=MaterialTheme.typography.titleMedium)
                                            val distance = location?.let { distanceMeters(it.latitude,it.longitude,spot.latitude,spot.longitude).toInt() }
                                            Text(if(state.developer) "모의 도착 지원" else distance?.let { "약 ${it}m${if(!precise) " · 대략적 위치" else ""}" } ?: "거리 확인 대기 중")
                                            Text(if(index<state.completed) "기록 수집 완료" else if(index>state.completed) "이전 기록을 먼저 찾아주세요" else "다음 목적지")
                                            Button(enabled=index<=state.completed,onClick={nav.navigate("puzzle/$index")}) { Text(if(index<state.completed) "기록 읽기" else "스팟 열기") }
                                        } }
                                    }
                                    TextButton(onClick={nav.popBackStack()}) { Text("홈으로") }
                                }
                            }
                            composable("map") {
                                FieldMapScreen(
                                    location = location, precise = precise, locationMessage = locationMessage,
                                    completed = state.completed, developer = state.developer,
                                    requestPermission = { permissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION)) },
                                    openPuzzle = { nav.navigate("puzzle/$it") },
                                    back = { nav.popBackStack() }
                                )
                            }
                            composable("puzzle/{index}") { entry ->
                                val index=entry.arguments?.getString("index")?.toIntOrNull()
                                if(index==null || index !in spots.indices || index>state.completed) { Text("아직 열리지 않은 기록입니다."); return@composable }
                                val spot=spots[index]
                                var answer by rememberSaveable(index) { mutableStateOf("") }
                                var feedback by rememberSaveable(index) { mutableStateOf("") }
                                var hint by rememberSaveable(index) { mutableStateOf(false) }
                                var simulated by rememberSaveable(index,state.developer) { mutableStateOf(false) }
                                var now by remember { mutableLongStateOf(SystemClock.elapsedRealtimeNanos()) }
                                LaunchedEffect(Unit) { while(true) { now=SystemClock.elapsedRealtimeNanos(); kotlinx.coroutines.delay(1000) } }
                                val arrived=if(state.developer) simulated else precise && location?.let {
                                    canArrive(precise, distanceMeters(it.latitude,it.longitude,spot.latitude,spot.longitude), if(it.hasAccuracy()) it.accuracy else null, now-it.elapsedRealtimeNanos)
                                }==true
                                Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                                    Text(spot.title,style=MaterialTheme.typography.headlineSmall)
                                    Text(spot.story)
                                    if(index<state.completed) { Text("수집 완료 · ${spot.relic}") }
                                    else {
                                        Text(if(arrived) "도착 확인 완료" else "도착 확인 후 퍼즐을 풀 수 있습니다.")
                                        if(state.developer) Button(onClick={simulated=true}) { Text("개발 모드: 이곳에 도착") }
                                        else { Text(locationMessage)
                                            if(!precise) Text("대략적 위치만으로는 도착을 확인할 수 없습니다. 정확한 위치를 허용해주세요.")
                                            Text("정확한 위치, 50m 이하의 오차, 1분 이내의 위치 정보가 필요합니다. GPS를 켜고 안전한 야외 동선에서 기다려주세요.")
                                            OutlinedButton(onClick={permissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION))}) { Text("위치 권한 요청 / 갱신") }
                                        }
                                        Text(spot.question)
                                        OutlinedTextField(value=answer,onValueChange={answer=it;feedback=""},label={Text("정답")},enabled=arrived, singleLine=true,modifier=Modifier.fillMaxWidth())
                                        Button(enabled=arrived,onClick={ if(answerMatches(answer,spot.answer)) { vm.complete(index); feedback="정답입니다! 기록을 저장하고 있습니다." } else feedback="다시 생각해보세요. 힌트를 확인할 수 있습니다." }) { Text("기록 해독") }
                                        TextButton(onClick={hint=true}) { Text("힌트 보기") }
                                        if(hint) Text(spot.hint)
                                        Text(feedback)
                                    }
                                    if(state.completed==spots.size) Button(onClick={nav.navigate("ending")}) { Text("마지막 편지 읽기") }
                                    TextButton(onClick={nav.popBackStack()}) { Text("스팟 목록으로") }
                                }
                            }
                            composable("ending") {
                                Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(20.dp)) {
                                    if(state.completed==spots.size) {
                                        Text("약속은 남는다",style=MaterialTheme.typography.headlineLarge)
                                        Text("네 조각의 기록이 하나의 가락지가 되었습니다. 잊힌 목소리를 기억하겠다는 당신의 약속으로, 진주성의 마지막 편지가 완성됩니다.")
                                        Text("✦ 마지막 가락지 획득 · 모든 기록 수집 완료")
                                    } else Text("네 기록을 모두 찾아야 마지막 편지를 읽을 수 있습니다.")
                                    Button(onClick={nav.navigate("home") { popUpTo("home") { inclusive=true } }}) { Text("홈으로") }
                                }
                            }
                            composable("settings") {
                                var confirm by remember { mutableStateOf(false) }
                                Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                                    Text("플레이 설정",style=MaterialTheme.typography.headlineSmall)
                                    Row { Text("개발 모드",Modifier.weight(1f)); Switch(checked=state.developer,onCheckedChange=vm::developer) }
                                    Text("개발 모드에서는 위치 권한 없이 모든 스팟에 모의 도착할 수 있습니다. 기본값은 개발 모드입니다. 현장 모드로 전환하면 실제 위치를 확인합니다.")
                                    Text("로그인·게임 서버·결제·지도 API 키가 없습니다. 지도 타일은 인터넷으로 OpenStreetMap에서 불러옵니다. 진행 상황과 설정은 이 기기에 자동 저장됩니다. 앱을 삭제하면 기록도 삭제됩니다.")
                                    Text("좌표는 초기 시제품용 근사값입니다. 현장 답사로 보정해야 합니다. 퍼즐은 창작이며 실제 시설물에 답을 부착할 필요가 없습니다.")
                                    OutlinedButton(onClick={confirm=true}) { Text("진행 상황 초기화") }
                                    TextButton(onClick={nav.popBackStack()}) { Text("돌아가기") }
                                }
                                if(confirm) AlertDialog(onDismissRequest={confirm=false},title={Text("기록을 지울까요?")},text={Text("수집 기록과 퍼즐 진행이 초기화됩니다.")},confirmButton={TextButton(onClick={vm.reset();confirm=false}) {Text("초기화")}},dismissButton={TextButton(onClick={confirm=false}) {Text("취소")}})
                            }
                        }
                    }
                }
            }
        }
    }
    override fun onStart() { super.onStart(); startLocation() }
    override fun onStop() { client.removeLocationUpdates(callback); super.onStop() }
    private fun startLocation() {
        precise=ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
        val coarse=ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
        client.removeLocationUpdates(callback)
        location=null
        if(!precise && !coarse) { locationMessage="위치 권한이 없습니다. 개발 모드로도 플레이할 수 있습니다."; return }
        try {
            val request=LocationRequest.Builder(if(precise) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY,5000).setMinUpdateIntervalMillis(2000).build()
            client.requestLocationUpdates(request,callback,mainLooper).addOnFailureListener { locationMessage="위치를 가져오지 못했습니다. GPS와 Google Play 서비스를 확인해주세요." }
            locationMessage=if(precise) "정확한 GPS 위치를 기다리고 있습니다." else "대략적 위치 허용됨 · 도착 판정에는 정확한 위치가 필요합니다."
        } catch (_: SecurityException) { precise=false; location=null; locationMessage="위치 권한이 변경되었습니다. 다시 허용해주세요." }
    }
}
