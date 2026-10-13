# 진주성: 마지막 가락지

Kotlin + Jetpack Compose + Material 3로 구현한 Android 전용 현장 방탈출 게임 시제품입니다. Flutter를 사용하지 않습니다. 로그인, 서버, 결제, 지도 API 키 없이 기기 안에서 동작합니다.

## Android Studio에서 실행

1. Android Studio Meerkat 이상에서 이 저장소 폴더를 **Open** 합니다.
2. SDK Manager에서 **Android SDK Platform 35**, SDK Build Tools 및 Platform Tools를 설치합니다. Gradle JDK는 내장 JDK 21 또는 JDK 17로 설정합니다.
3. Gradle Sync를 실행합니다. AGP 8.9.2, Gradle 8.11.1, Kotlin 2.1.20을 사용합니다. Wrapper 스크립트와 JAR, 배포 ZIP SHA-256이 포함되어 있습니다.
4. Android 8.0(API 26) 이상의 실제 기기에서 개발자 옵션 → USB 디버깅을 켜고 USB로 연결합니다. 기기의 디버깅 허용 대화상자를 승인합니다.
5. Android Studio에서 `app` 구성과 연결된 기기를 선택하고 Run을 누릅니다.

현장 위치 기능에는 Google Play 서비스가 있는 기기를 권장합니다. Google Play 서비스가 없거나 GPS가 꺼져 있어도 개발 모드로 게임을 진행할 수 있습니다. 권한은 현장 모드 화면의 버튼을 눌렀을 때 요청하며 백그라운드 위치는 요청하지 않습니다.

## APK 생성

Android Studio 메뉴 **Build → Build App Bundle(s) / APK(s) → Build APK(s)** 또는 아래 명령을 사용합니다.

```bash
# SDK 경로는 Android Studio가 local.properties에 자동 기록합니다.
# 명령줄에서는 SDK 설치 경로에 맞춰 ANDROID_HOME을 설정할 수도 있습니다.
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Windows에서는 `gradlew.bat`을 사용합니다. 디버그 APK 경로는 `app/build/outputs/apk/debug/app-debug.apk`입니다. 배포용 APK는 Android Studio의 **Generate Signed App Bundle or APK**에서 본인의 키로 서명하세요. 서명 키는 저장소에 넣지 않습니다.

## 플레이 흐름

- 기본값은 **개발 모드**입니다. 홈 → 여정 시작 → 첫 스팟 → ‘이곳에 도착’ → 퍼즐 정답 → 다음 스팟 순으로 실제 위치 없이 결말까지 진행합니다.
- 공북문, 우물, 촉석루, 의암의 네 기록을 순서대로 수집합니다. 정답이 틀리면 다시 입력할 수 있으며 힌트를 볼 수 있습니다.
- 진행 상황, 수집 기록, 개발 모드 설정은 DataStore에 자동 저장됩니다. 강제 종료 후 이어할 수 있고 설정에서 확인 후 초기화할 수 있습니다.
- **현장 모드**에서는 스팟 목록에 거리를 표시합니다. 대략적 위치만 허용해도 앱을 사용할 수 있지만 도착 판정은 잠깁니다. 정확한 위치 권한을 허용해야 합니다.
- 도착 조건: 스팟 반경 80m 이내, GPS 정확도 50m 이하, 위치 수신 후 60초 이내. 오래된 위치와 대략적 위치는 도착 근거로 사용하지 않습니다.
- 위치 업데이트는 Activity가 보이는 동안만 수행하며 화면을 떠나면 중지합니다. 권한 거부·회수·위치 요청 실패 시 안내를 표시합니다.

## 구조

- `MainActivity.kt`: Navigation Compose 화면, Material 3 UI, 사용 중 위치 권한과 Fused Location Provider
- `Game.kt`: 네 스팟의 창작 스토리와 퍼즐, 거리 계산, ViewModel + StateFlow, DataStore
- `GameTest.kt`: 거리 단위, 정답 처리, 챕터 데이터 검증

좌표는 **현장 답사 전 근사 좌표**이며 특히 우물 위치는 현장 확인과 보정이 필요합니다. 실제 운영 전 안전한 공개 관람 동선에서 좌표와 판정 반경을 검증하세요. 의암·강변 위험 구역으로 접근할 필요가 없습니다. 스토리와 퍼즐은 역사에서 영감을 받은 창작이며 역사적 사실을 재현하는 교육 자료가 아닙니다.

## 수동 검증 체크리스트

1. 위치 권한 없이 개발 모드에서 네 퍼즐과 결말을 완료합니다.
2. 두 번째 기록까지 진행 후 앱을 종료·재실행해 진행과 모드가 유지되는지 확인합니다.
3. 현장 모드에서 권한 거부, 대략적 위치, 정확한 위치를 각각 테스트합니다. 대략적 위치에서는 도착이 허용되지 않아야 합니다.
4. GPS를 끄거나 권한을 시스템 설정에서 회수한 후 복귀하여 안내와 재요청을 확인합니다.
5. 앞 스팟 완료 전 다음 스팟이 잠기는지, 초기화 취소 시 기록이 보존되는지 확인합니다.

## 클라우드 검증 상태

이 클라우드 머신에는 Android SDK와 Java 컴파일러(`javac`)가 없습니다. Java 21 런타임으로 Gradle 8.11.1 실행과 프로젝트 구성 단계는 확인했지만, APK 빌드 시 Java 컴파일러 부재로 실패했습니다. Gradle 배포 ZIP과 Wrapper JAR는 공식 SHA-256으로 검증했습니다. 따라서 Android 앱 빌드, JVM 테스트 및 실제 기기 실행 결과는 아직 검증되지 않았습니다. Android Studio에서 위 빌드·테스트와 기기 체크리스트를 실행해야 합니다. 저장소의 실제 검증 결과는 작업 완료 보고를 함께 참고하세요.
