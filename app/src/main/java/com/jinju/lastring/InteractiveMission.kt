package com.jinju.lastring

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import java.io.File
import java.io.IOException

@Composable
fun InteractiveMission(index: Int, enabled: Boolean, developer: Boolean, onCheck: (String) -> Unit) {
    val context=LocalContext.current
    val haptics=LocalHapticFeedback.current
    fun check(value: String) {
        if(enabled) { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onCheck(value) }
    }
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("역사 단서",style=MaterialTheme.typography.titleMedium)
        Text(when(index) {
            0 -> "공북문은 진주성의 북쪽 문입니다. 나침반으로 북쪽을 찾아 길을 여세요."
            1 -> "성벽은 적의 침입을 막는 방어 시설입니다. 성벽 밖으로 나가거나 시설물을 만질 필요가 없습니다."
            2 -> "1592년의 제1차 진주성 전투와 1593년의 제2차 전투는 다른 사건입니다. 의기사는 논개를 기리는 후대의 사당입니다."
            3 -> "의기사는 논개를 기리는 추모 공간으로 후대에 세워졌습니다. 앱 속 보관함과 가락지 보관인의 이야기는 창작입니다."
            else -> "열 손가락의 가락지는 전승에 바탕을 두었습니다. 재질이 옥이라는 점과 당신이 가락지를 전달하는 임무는 창작 설정입니다."
        })
        TextButton(onClick={context.startActivity(Intent(Intent.ACTION_VIEW, (if(index==3) "https://encykorea.aks.ac.kr/Article/E0013009" else "https://jinju.museum.go.kr/").toUri()))}) { Text("역사 자료 찾아보기") }
    } }
    when(index) {
        0 -> {
            var direction by rememberSaveable { mutableIntStateOf(2) }
            val labels=listOf("북","동","남","서")
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("지도 나침반",style=MaterialTheme.typography.titleLarge)
                Canvas(Modifier.fillMaxWidth().height(150.dp)) {
                    val center=Offset(size.width/2,size.height/2)
                    drawCircle(Color(0xFFE5C780),60.dp.toPx(),center,style=androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()))
                    val radians=Math.toRadians(direction*90.0-90.0)
                    val tip=center+Offset(kotlin.math.cos(radians).toFloat()*50.dp.toPx(),kotlin.math.sin(radians).toFloat()*50.dp.toPx())
                    drawLine(Color(0xFFE5C780),center,tip,8.dp.toPx())
                }
                Text("현재 방향: ${labels[direction]}")
                OutlinedButton(enabled=enabled,onClick={direction=(direction+1)%4}) { Text("나침반 90° 돌리기") }
                Button(enabled=enabled,onClick={check(labels[direction])}) { Text("이 방향으로 지도 펼치기") }
                Text("화면의 나침반을 조작하는 퍼즐입니다. 기기의 실제 방향 센서를 사용하지 않습니다.",style=MaterialTheme.typography.bodySmall)
            } }
        }
        1 -> {
            val photo=remember(context) { File(context.cacheDir,"camera/mission-sign.jpg").apply { parentFile?.mkdirs() } }
            val uri=remember(photo) { FileProvider.getUriForFile(context,context.packageName+".files",photo) }
            var photoVersion by rememberSaveable { mutableIntStateOf(0) }
            var practice by rememberSaveable { mutableStateOf(false) }
            var recognized by rememberSaveable { mutableStateOf("") }
            var cameraMessage by remember { mutableStateOf("") }
            var recognizing by remember { mutableStateOf(false) }
            var showFullText by rememberSaveable { mutableStateOf(false) }
            val recognizer=remember { TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build()) }
            DisposableEffect(recognizer) { onDispose { recognizer.close() } }
            val bitmap=remember(photoVersion) {
                if(photoVersion>0 && photo.exists()) {
                    val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
                    BitmapFactory.decodeFile(photo.absolutePath,bounds)
                    var sample=1
                    while(maxOf(bounds.outWidth,bounds.outHeight)/sample>1600) sample*=2
                    val decoded=BitmapFactory.decodeFile(photo.absolutePath,BitmapFactory.Options().apply { inSampleSize=sample })
                    if(decoded==null) null else {
                        val orientation=try { ExifInterface(photo.absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL) } catch(_: IOException) { ExifInterface.ORIENTATION_NORMAL }
                        val matrix=Matrix().apply { when(orientation) {
                            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f,1f)
                            ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f,-1f)
                            ExifInterface.ORIENTATION_TRANSPOSE -> { postRotate(90f); postScale(-1f,1f) }
                            ExifInterface.ORIENTATION_TRANSVERSE -> { postRotate(270f); postScale(-1f,1f) }
                        } }
                        android.graphics.Bitmap.createBitmap(decoded,0,0,decoded.width,decoded.height,matrix,true)
                    }
                } else null
            }
            fun recognizePhoto() {
                recognizing=true; recognized=""; practice=false; showFullText=false
                try {
                    val input=InputImage.fromFilePath(context,uri)
                    recognizer.process(input).addOnSuccessListener { result ->
                        recognizing=false; recognized=result.text.take(8000)
                        cameraMessage=if(isMissionSignText(recognized)) "안내판 관련 글자를 인식했습니다. 아래 관찰 퀴즈를 풀어주세요." else "진주성·성벽·성곽 글자를 찾지 못했습니다. 글자가 선명하게 보이도록 다시 촬영해주세요."
                    }.addOnFailureListener {
                        recognizing=false; cameraMessage="글자 인식에 실패했습니다. 밝은 곳에서 흔들림 없이 다시 촬영해주세요."
                    }
                } catch(_: IOException) { recognizing=false; cameraMessage="사진을 읽지 못했습니다. 다시 촬영해주세요." }
            }
            val camera=rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                if(!success || !photo.exists() || photo.length()==0L) { recognized=""; photoVersion=0; cameraMessage="촬영을 취소했거나 사진이 저장되지 않았습니다. 다시 촬영해주세요." }
                else { photoVersion++; recognizePhoto() }
            }
            Text("성벽 안내판 · 카메라 OCR 미션",style=MaterialTheme.typography.titleLarge)
            Text("공개 관람 동선의 진주성·성벽 안내판을 글자가 크게 보이도록 촬영하세요. 다른 관람객의 얼굴은 피해주세요.")
            OutlinedButton(enabled=enabled && !recognizing,onClick={
                recognized=""; practice=false
                try { camera.launch(uri) }
                catch(_: ActivityNotFoundException) { cameraMessage="카메라 앱이 없습니다. 실제 기기에서 촬영하거나 개발 모드의 연습 글을 사용하세요." }
                catch(_: SecurityException) { cameraMessage="카메라 앱 접근이 차단되었습니다. 기기 설정을 확인해주세요." }
            }) { Text(if(bitmap==null) "카메라로 안내판 촬영" else "안내판 다시 찍기") }
            bitmap?.let { Image(it.asImageBitmap(),contentDescription="직접 촬영한 안내판 사진",modifier=Modifier.fillMaxWidth().height(180.dp)) }
            if(recognizing) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("기기에서 한국어 글자 인식 중…") }
            if(bitmap!=null) TextButton(enabled=enabled && !recognizing,onClick={recognizePhoto()}) { Text("이 사진에서 글자 다시 인식") }
            if(recognized.isNotEmpty()) Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(if(practice) "개발 모드 연습 글" else "OCR 인식 결과",style=MaterialTheme.typography.titleMedium)
                Text(recognized,maxLines=if(showFullText) Int.MAX_VALUE else 8,overflow=TextOverflow.Ellipsis)
                if(recognized.length>300) TextButton(onClick={showFullText=!showFullText}) { Text(if(showFullText) "접기" else "전체 글 보기") }
            } }
            if(developer) TextButton(enabled=enabled && !recognizing,onClick={
                practice=true; recognized="진주성의 성벽은 적의 침입을 막기 위한 방어 시설이다."; cameraMessage="직접 인식한 결과가 아닌 개발 모드 연습 자료입니다."
            }) { Text("개발 모드: 안내판 연습 글 사용") }
            if(cameraMessage.isNotEmpty()) Text(cameraMessage)
            Text("안내판의 성벽은 어떤 기능을 맡았나요?")
            listOf("방어","운송","제례").forEach { option ->
                OutlinedButton(enabled=enabled && !recognizing && isMissionSignText(recognized) && (!practice || developer),onClick={check(option)}) { Text(option) }
            }
            Text("한국어 OCR 모델은 앱에 포함됩니다. 사진과 인식 결과는 서버로 전송하지 않습니다. 글자 인식은 위치·안내판 진위·역사적 사실을 자동 검증하는 기능이 아닙니다.",style=MaterialTheme.typography.bodySmall)
        }
        2 -> {
            var orderText by rememberSaveable { mutableStateOf("2,1,0") }
            val order=orderText.split(',').map(String::toInt)
            val labels=listOf("1592 · 제1차 진주성 전투","1593 · 제2차 진주성 전투","후대 · 의기사에서 논개 추모")
            Text("시간의 조각 · 오래된 사건부터 배치",style=MaterialTheme.typography.titleLarge)
            order.forEachIndexed { position, value ->
                Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(8.dp)) {
                    Text("${position+1}. ${labels[value]}",Modifier.weight(1f))
                    TextButton(enabled=enabled && position>0,onClick={orderText=swapMissionItems(order,position,position-1).joinToString(",")}) { Text("↑") }
                    TextButton(enabled=enabled && position<order.lastIndex,onClick={orderText=swapMissionItems(order,position,position+1).joinToString(",")}) { Text("↓") }
                } }
            }
            Button(enabled=enabled,onClick={check(order.joinToString("|"))}) { Text("시간 순서로 경비 암호 열기") }
        }
        3 -> {
            var first by rememberSaveable { mutableIntStateOf(0) }
            var second by rememberSaveable { mutableIntStateOf(0) }
            var third by rememberSaveable { mutableIntStateOf(0) }
            val eras=listOf("1593 당시","후대")
            val people=listOf("김시민","논개")
            val purposes=listOf("군량 창고","추모")
            Text("기억의 보관함 · 세 개의 잠금 고리",style=MaterialTheme.typography.titleLarge)
            Text("의기사의 시기 · 기리는 인물 · 목적을 맞추세요.")
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf(Triple(eras[first],"시기", {first=(first+1)%eras.size}),Triple(people[second],"인물", {second=(second+1)%people.size}),Triple(purposes[third],"목적", {third=(third+1)%purposes.size})).forEach { (label,category,rotate) ->
                    Card(Modifier.weight(1f)) { Column(Modifier.padding(8.dp)) {
                        Text(category,style=MaterialTheme.typography.labelSmall)
                        Text(label)
                        TextButton(enabled=enabled,onClick=rotate) { Text("돌리기") }
                    } }
                }
            }
            Button(enabled=enabled,onClick={check("${eras[first]}|${people[second]}|${purposes[third]}")}) { Text("보관함 열기") }
        }
        else -> {
            var delivered by rememberSaveable { mutableFloatStateOf(0f) }
            Text("마지막 부탁 · 가락지 전달",style=MaterialTheme.typography.titleLarge)
            Canvas(Modifier.fillMaxWidth().height(130.dp)) {
                val x=40.dp.toPx()+(size.width-80.dp.toPx())*delivered
                drawCircle(Color(0xFFB7D5BF),28.dp.toPx(),Offset(x,size.height/2),style=androidx.compose.ui.graphics.drawscope.Stroke(8.dp.toPx()))
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text("옥가락지");Text("화면 속 논개") }
            Slider(value=delivered,onValueChange={delivered=it},enabled=enabled)
            Button(enabled=enabled && canDeliverRing(delivered),onClick={check("전달")}) { Text("가락지 건네기") }
            Text("슬라이더를 오른쪽 끝으로 옮겨 화면 속 가락지를 전달하세요. 실제 바위나 강가에 접근할 필요는 없습니다.")
        }
    }
}
