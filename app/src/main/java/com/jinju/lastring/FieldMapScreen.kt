package com.jinju.lastring

import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.io.File

/** OSM tile requests stay on-device; no position upload or game backend. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldMapScreen(
    location: Location?, precise: Boolean, locationMessage: String,
    completed: Int, developer: Boolean, requestPermission: () -> Unit,
    simulation: Boolean, simulationEnabled: Boolean, setSimulation: (Boolean) -> Unit,
    movePlayer: (Double, Double) -> Unit, resetPlayer: (Spot) -> Unit,
    openPuzzle: (Int) -> Unit, back: () -> Unit
) {
    // Store the chapter identity: saved state can be restored after progress changes.
    var lastArrivalChapter by key("arrival-chapter-v2") { rememberSaveable { mutableIntStateOf(-1) } }
    var pendingArrivalChapter by key("pending-arrival-chapter-v2") { rememberSaveable { mutableIntStateOf(-1) } }
    var showDetails by rememberSaveable { mutableStateOf(false) }
    val showArrivalEvent = pendingArrivalChapter==completed
    var step by rememberSaveable { mutableDoubleStateOf(10.0) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var selected by rememberSaveable { mutableIntStateOf(completed.coerceAtMost(spots.lastIndex)) }
    var follow by rememberSaveable { mutableStateOf(true) }
    var testLatitude by rememberSaveable(simulation) { mutableStateOf<Double?>(null) }
    var testLongitude by rememberSaveable(simulation) { mutableStateOf<Double?>(null) }
    var testError by remember { mutableStateOf("") }
    var wasOutside by rememberSaveable(simulation) { mutableStateOf(false) }
    var returned by rememberSaveable(simulation) { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtimeNanos()) }
    LaunchedEffect(lifecycleOwner) {
        while (true) { now = SystemClock.elapsedRealtimeNanos(); delay(1000) }
    }
    LaunchedEffect(completed) { selected=completed.coerceAtMost(spots.lastIndex) }
    val target = spots[selected]
    val age = location?.let { if(simulation) 0L else now - it.elapsedRealtimeNanos }
    val accuracy = location?.takeIf { it.hasAccuracy() }?.accuracy
    val distance = location?.let { distanceMeters(it.latitude,it.longitude,target.latitude,target.longitude) }
    val arrived = canArrive(precise,distance,accuracy,age)
    val fresh = precise && accuracy != null && accuracy <= MAX_LOCATION_ACCURACY_METERS &&
        age != null && age in 0..MAX_LOCATION_AGE_NANOS
    val nextSpot = spots.getOrNull(completed)
    val nextDistance = nextSpot?.let { spot -> location?.let { fix ->
        distanceMeters(fix.latitude,fix.longitude,spot.latitude,spot.longitude)
    } }
    val nextArrived = canArrive(precise,nextDistance,accuracy,age)
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
    LaunchedEffect(completed,nextArrived,lifecycleState) {
        if(lifecycleState.isAtLeast(Lifecycle.State.RESUMED)) {
            spotEventToShow(completed,spots.size,lastArrivalChapter,nextArrived)?.let { index ->
                selected=index
                lastArrivalChapter=index
                pendingArrivalChapter=index
                showDetails=false
            }
        }
    }
    val testDistance = if (testLatitude != null && testLongitude != null) location?.let {
        distanceMeters(it.latitude,it.longitude,testLatitude!!,testLongitude!!)
    } else null
    val testArrived = canArrive(precise,testDistance,accuracy,age)
    // Require the uncertainty circle to be fully outside before accepting departure.
    LaunchedEffect(testDistance, fresh, testArrived) {
        if (fresh && testDistance != null && testDistance - (accuracy ?: 0f) > ARRIVAL_RADIUS_METERS) wasOutside=true
        if (wasOutside && testArrived) returned=true
    }
    val map = remember(context) {
        Configuration.getInstance().apply {
            userAgentValue = "com.jinju.lastring/1.0 (+https://github.com/jinbot0221/jinju-last-ring)"
            osmdroidBasePath = File(context.cacheDir,"osm")
            osmdroidTileCache = File(context.cacheDir,"osm/tiles")
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(17.0)
            controller.setCenter(GeoPoint(target.latitude,target.longitude))
            minZoomLevel = 3.0
            maxZoomLevel = 19.0
        }
    }
    DisposableEffect(map,lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when(event) {
                Lifecycle.Event.ON_RESUME -> map.onResume()
                Lifecycle.Event.ON_PAUSE -> map.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) map.onResume()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            map.onPause()
            map.onDetach()
        }
    }
    Box(Modifier.fillMaxSize()) {
            AndroidView(factory={map},modifier=Modifier.fillMaxSize(),update={ view ->
                view.overlays.clear()
                spots.forEachIndexed { index, spot ->
                    view.overlays.add(Marker(view).apply {
                        position=GeoPoint(spot.latitude,spot.longitude)
                        title="${index+1}. ${spot.title}"
                        snippet=if(index<completed) "수집 완료" else if(index>completed) "이전 기록을 먼저 수집하세요" else "다음 목적지"
                        setAnchor(Marker.ANCHOR_CENTER,Marker.ANCHOR_BOTTOM)
                        setOnMarkerClickListener { marker, _ ->
                            selected=index; follow=false; marker.showInfoWindow(); true
                        }
                    })
                }
                view.overlays.add(Polygon(view).apply {
                    points=Polygon.pointsAsCircle(GeoPoint(target.latitude,target.longitude),ARRIVAL_RADIUS_METERS)
                    fillPaint.color=0x224CAF50
                    outlinePaint.color=0xFF4CAF50.toInt()
                    outlinePaint.strokeWidth=3f
                })
                location?.let { fix ->
                    val position=GeoPoint(fix.latitude,fix.longitude)
                    if(fix.hasAccuracy()) view.overlays.add(Polygon(view).apply {
                        points=Polygon.pointsAsCircle(position,fix.accuracy.toDouble().coerceAtLeast(1.0))
                        fillPaint.color=0x223498DB
                        outlinePaint.color=0xFF3498DB.toInt()
                        outlinePaint.strokeWidth=2f
                    })
                    view.overlays.add(Marker(view).apply {
                        this.position=position
                        title=if(simulation) "모의 플레이어 · 버튼 이동" else if(precise) "내 위치" else "내 위치 · 대략적"
                        snippet="파란 원은 위치 오차입니다."
                        setAnchor(Marker.ANCHOR_CENTER,Marker.ANCHOR_BOTTOM)
                        icon=context.getDrawable(android.R.drawable.ic_menu_mylocation)
                    })
                    if(follow) view.controller.setCenter(position)
                }
                if(testLatitude!=null && testLongitude!=null) {
                    val testPoint=GeoPoint(testLatitude!!,testLongitude!!)
                    view.overlays.add(Polygon(view).apply {
                        points=Polygon.pointsAsCircle(testPoint,ARRIVAL_RADIUS_METERS)
                        fillPaint.color=0x22FF9800
                        outlinePaint.color=0xFFFF9800.toInt()
                        outlinePaint.strokeWidth=3f
                    })
                    view.overlays.add(Marker(view).apply { position=testPoint; title="임시 걷기 테스트 스팟" })
                }
                view.invalidate()
            })
        Card(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(12.dp).fillMaxWidth(),
            colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface.copy(alpha=0.94f))) {
            Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(nextSpot?.let { "${completed+1} / ${spots.size} · ${it.title}" } ?: "모든 기록 수집 완료",style=MaterialTheme.typography.titleMedium)
                Text(nextDistance?.let { "목적지까지 ${it.toInt()}m · 80m 안에서 이벤트 자동 발생" } ?: "위치 수신 대기 · 정확한 위치를 허용해주세요.")
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick=back) { Text("돌아가기") }
                    TextButton(onClick={follow=true}) { Text("내 위치") }
                    TextButton(onClick={showDetails=true}) { Text("스팟 / 테스트 설정") }
                }
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(8.dp).fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            if(simulation) Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface.copy(alpha=0.94f))) {
                Column(Modifier.padding(8.dp)) {
                if(simulation) {
                    Text("모의 위치 · GPS/위치 권한 불필요 · 한 번에 ${step.toInt()}m 이동")
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected=step==10.0,onClick={step=10.0},label={Text("10m")})
                        FilterChip(selected=step==25.0,onClick={step=25.0},label={Text("25m")})
                        TextButton(onClick={resetPlayer(target);follow=true}) { Text("선택 스팟 출발점") }
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        listOf("북 ↑" to 0.0,"남 ↓" to 180.0,"서 ←" to 270.0,"동 →" to 90.0).forEach { (label,bearing) ->
                            OutlinedButton(modifier=Modifier.weight(1f),contentPadding=PaddingValues(horizontal=4.dp),onClick={movePlayer(bearing,step);follow=true}) { Text(label) }
                        }
                    }
                }
                }
            }
            Surface(color=MaterialTheme.colorScheme.surface.copy(alpha=0.94f),shape=MaterialTheme.shapes.small) {
                TextButton(onClick={context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.openstreetmap.org/copyright")))},contentPadding=PaddingValues(horizontal=8.dp,vertical=0.dp)) {
                    Text("© OpenStreetMap contributors",style=MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
    if(showDetails) ModalBottomSheet(onDismissRequest={showDetails=false},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
        Column(Modifier.fillMaxWidth().heightIn(max=500.dp).verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            if(developer) Row { Text("지도 버튼 이동",Modifier.weight(1f)); Switch(checked=simulationEnabled,onCheckedChange=setSimulation) }
                Text("${selected+1}. ${target.title}",style=MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick={selected=(selected+spots.size-1)%spots.size;follow=false;val s=spots[selected];map.controller.setCenter(GeoPoint(s.latitude,s.longitude))}) { Text("이전 스팟") }
                    OutlinedButton(onClick={selected=(selected+1)%spots.size;follow=false;val s=spots[selected];map.controller.setCenter(GeoPoint(s.latitude,s.longitude))}) { Text("다음 스팟") }
                }
                Text(distance?.let { "거리 ${it.toInt()}m · GPS 오차 ${accuracy?.toInt()?.let { a -> "${a}m" } ?: "미상"} · ${age?.let { a -> (a.coerceAtLeast(0)/1_000_000_000).toString()+"초 전" } ?: "대기"}" } ?: "현재 위치 수신 대기 중")
                Text(when {
                    !precise -> "정확한 위치 권한이 필요합니다. 대략적 위치로는 도착을 판정하지 않습니다."
                    location == null -> locationMessage
                    !fresh -> "GPS 위치가 오래되었거나 오차가 큽니다. 야외에서 위치 갱신을 기다려주세요."
                    arrived -> "도착 확인 완료 · 80m 범위 안입니다."
                    else -> "스팟까지 이동하세요 · 80m 이내에서 도착을 확인합니다."
                })
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick={follow=true}) { Text(if(follow) "내 위치 추적 중" else "내 위치 따라가기") }
                    TextButton(onClick={follow=false;map.controller.setCenter(GeoPoint(target.latitude,target.longitude))}) { Text("선택 스팟 보기") }
                }
                if(!simulation) OutlinedButton(onClick=requestPermission) { Text("위치 권한 요청 / 갱신") }
                if(developer) Text("개발 모드입니다. 실제 GPS 테스트는 설정에서 개발 모드를 끄세요. 버튼 이동은 실제 기기 위치를 변경하지 않습니다.")
                Button(enabled=selected<completed || (selected==completed && (arrived || (developer && !simulation))),onClick={if(selected==completed && arrived) pendingArrivalChapter=completed else openPuzzle(selected)}) {
                    Text(if(selected<completed) "수집한 기록 보기" else if(arrived) "도착 이벤트 다시 보기" else "퍼즐 열기")
                }
                HorizontalDivider()
                Text("지금 있는 곳에서 걷기 테스트",style=MaterialTheme.typography.titleMedium)
                Text("현장 답사 전에 사용할 임시 스팟입니다. 게임 기록은 변경하지 않습니다. 주황 원 밖으로 나갔다가 돌아오세요.")
                OutlinedButton(enabled=fresh,onClick={
                    // Recheck when tapped: a previously rendered enabled button can become stale.
                    val fix=location
                    val tapAge=fix?.let { if(simulation) 0L else SystemClock.elapsedRealtimeNanos()-it.elapsedRealtimeNanos }
                    if(fix!=null && canArrive(precise,0.0,if(fix.hasAccuracy()) fix.accuracy else null,tapAge)) {
                        testLatitude=fix.latitude;testLongitude=fix.longitude;wasOutside=false;returned=false;testError=""
                    } else testError="정확하고 최신인 위치를 받은 뒤 다시 시도해주세요."
                }) { Text("현재 위치를 테스트 스팟으로 지정") }
                if(testError.isNotEmpty()) Text(testError)
                if(testLatitude!=null) {
                    Text("임시 스팟 거리: ${testDistance?.toInt()?.let { "${it}m" } ?: "대기"}")
                    Text(when {
                        returned -> "걷기 테스트 완료 · 범위 밖 이동 후 재도착을 확인했습니다."
                        !fresh -> "정확하고 최신인 GPS 위치를 기다립니다."
                        wasOutside -> "범위 밖 이동 확인 · 80m 안으로 돌아오세요."
                        else -> "출발 위치 지정 완료 · 오차 원까지 80m 범위 밖으로 이동하세요."
                    })
                    TextButton(onClick={testLatitude=null;testLongitude=null;wasOutside=false;returned=false}) { Text("임시 스팟 지우기") }
                }
                Text("지도에는 인터넷이 필요합니다. 연결이 없으면 캐시된 타일만 보일 수 있으며 GPS 도착 판정은 계속 동작합니다. 표시한 원은 직선 거리 범위이며 보행 경로가 아닙니다.")
                TextButton(onClick=back) { Text("돌아가기") }
        }
    }
    if(showArrivalEvent && nextSpot!=null) {
        AlertDialog(
            onDismissRequest={pendingArrivalChapter=-1},
            title={Text("기록 발견 · ${nextSpot.title}")},
            text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(if(simulation) "버튼 이동으로 스팟에 도착했습니다." else "스팟에 도착했습니다.")
                Text(nextSpot.story)
                if(!nextArrived) Text("현재 도착 조건을 충족하지 않습니다. 지도로 돌아가 위치와 거리를 확인해주세요.")
            }},
            confirmButton={TextButton(enabled=nextArrived,onClick={
                pendingArrivalChapter=-1
                openPuzzle(completed)
            }) { Text("편지 해독하기") }},
            dismissButton={TextButton(onClick={pendingArrivalChapter=-1}) { Text("지도에 머무르기") }}
        )
    }
}
