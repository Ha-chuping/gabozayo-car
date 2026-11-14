# 가보자요 차량 앱

> 폰에서 고른 아이 나들이 장소를 차에서 이어받아 한 번에 내비 경로로 보내고, 달리는 중에는 손 대신 목소리로 제안을 받는 Pleos Connect 차량 앱

팀 **夏秋핑** · 현대자동차그룹 차량용 앱 개발 공모전 출품작
짝꿍 앱(휴대폰): https://github.com/hyundai-ha-chuping/gabozayo-mobile

## 이런 순간을 위한 앱

주말 나들이 장소는 집에서 고르는데, 길 안내가 필요한 건 차에 탄 뒤다. 가보자요는 이 둘을 끊김 없이 잇는다.

1. **집에서** 아이 취향(동물, 곤충, 물놀이 등)으로 추천된 장소를 폰에서 찜하거나 "가보자요!"로 차에 보낸다.
2. **차에 타면** 차량 홈에 보낸 장소가 카드로 기다리고 있다. **가보자요!** 를 한 번 누르면 Pleos 내비에 경로가 만들어지고, 도착 예정 시간과 거리를 내비가 계산한 값으로 보여 준다.
3. **달리는 중에** 3km 안에 아이 취향 장소가 있으면 카드와 음성으로 알려 준다. "좋아" 또는 "다음에"라고 대답하면 되고, 손은 핸들에 둔다.

## 제출 정보

| 항목 | 내용 |
|---|---|
| 앱 이름 | 가보자요 (차량 앱) · 패키지 `com.gabozayo.car` |
| 한 줄 소개 | 폰에서 고른 아이 나들이 장소를 차에서 이어받아 한 번에 내비 경로로 보내고, 달리는 중에는 음성으로 제안받는 차량 앱 |
| Pleos Connect 에뮬레이터 | v2.1.0 APIs (Android 14, API 34) |
| 화면 규격 | 2560 x 1440 px, 160 dpi, 가로 화면 (12.8인치) |
| 개발 환경 | Android Studio Rabbit 2 (2026.2.2 Patch 1), Kotlin 2.0.20, AGP 8.5.2, minSdk 28 |
| APK | `apk/夏秋핑_가보자요_car-release.apk` |

## 5분 만에 둘러보기

폰 앱과 차량 앱은 같은 Firebase 데이터베이스를 쓴다. 폰 앱은 휴대폰 저장소의 APK를 폰 에뮬레이터에 설치하면 된다.

1. 차량 에뮬레이터에 APK를 설치한다. `-g` 는 권한을 한 번에 허용하는 옵션이다.
   ```bash
   adb install -g -r apk/夏秋핑_가보자요_car-release.apk
   ```
2. 차량 앱을 열고, 숫자판에 폰 앱 **내 정보 → 내 차량**의 6자리 코드를 입력한다. 연결되면 옵션 화면에 연결 상태가 표시된다.
3. 폰에서 장소를 **찜**한다. 차량 앱 홈의 카드와 **찜** 탭에 바로 나타난다.
4. 차량 카드의 **가보자요!** 를 누르거나, 폰 장소 상세에서 **차량으로 보내기**를 누른다. 차량 패널이 `경로 요청 중 → 경로 준비됨 → 안내 중`으로 바뀌고 도착 예정 시간·거리가 표시된다. 폰의 전송 화면에도 같은 상태가 보인다.
5. 에뮬레이터의 위치를 움직여 속도를 5km/h 이상으로 만들고 추천 장소 3km 안으로 들어가면, 홈에 제안 카드가 뜨고 음성이 나온다. 약 8초 뒤 음성 인식이 켜지면 "좋아"라고 말하면 경로가 시작되고, "다음에"라고 말하면 닫힌다.

처음 실행하면 권한 창이 한 번 뜬다. 나중에 허용하려면 옵션 화면의 **허용하기**를 누른다.

## 화면 구성

| 화면 | 내용 |
|---|---|
| 홈 | 폰에서 찜하거나 보낸 장소를 좌우로 넘겨 보는 노란 카드와 **가보자요!**, 아이별 관심사 추천, 오늘의 행사 |
| 찜 | 사진 카드 격자, 상세(사진 슬라이드, 운영시간, 이용연령, 편의시설, 소개) |
| 옵션 | 연결 상태·해제, 경로 선호, 음성 안내, 주행 중 맞춤 제안(반경), 위치·알림·차량 데이터 권한 |
| 경로 패널 | 화면 아래 가운데에 뜨는 안내 단계 패널(요청 중, 준비됨, 안내 중) |
| 근처 장소 제안 | 홈 카드, 음성, 상단 알림(내비 화면 위에서도 [가보자요!]·[다음에] 선택) |

차량 시스템의 주간·야간 설정에 따라 라이트·다크 화면이 자동으로 바뀐다. 한글은 어절 단위로 줄을 바꾼다.

## 운전 중에는 이렇게 동작한다

- 속도가 5km/h 이상이면 주행 중으로 보고, 터치로 새 경로를 시작하는 버튼을 막는다. 폰에서 보낸 목적지와 제안 수락만 예외다.
- 장소 제안은 음성으로 읽어 주고, 대답도 음성으로 받는다. 화면을 보지 않아도 끝낼 수 있다.
- 제안은 같은 장소를 한 번만 하고, 안내 중에는 하지 않는다.

## 쓰는 Pleos SDK

| SDK | 쓰임 |
|---|---|
| NaviHelper 3.0.5 | 경로 요청, 탐색 완료·오류 수신, 도착 예정 시간·남은 거리(`requestTrip` → `onTripInfo`)를 받아 패널·장소 카드에 표시. 내비 화면은 `NavigationScreenRequest.Foreground`로 앞에 가져온다 |
| FusedLocation 1.1.3 | 현재 위치와 속도. 5km/h 이상이면 주행 중으로 판단하고, 근처 장소 거리 계산에 쓴다 |
| Vehicle 2.3.2 | 브레이크 페달 상태 읽기(`CAR_DRIVE`). 옵션 화면에 차량 데이터 연결 상태 표시 |
| TextToSpeech 3.3.1 | 경로 안내와 제안 음성 (On-device) |
| SpeechToText 3.3.1 | 제안 뒤 자동으로 듣고 "좋아", "다음에"를 알아듣기 (On-device) |

내비 SDK에는 안내를 코드로 시작하는 호출이 없어서, 내비 화면이 앞에 나오면 운전자가 내비에서 **안내 시작**을 누른다. 장소 카드의 시간·거리는 직선거리가 아니라 내비에 물어본 값만 보여 준다. 음성은 Pleos 서버 계정 없이 On-device로 동작한다.

## 폰과 이어지는 방법

별도 서버 없이 Firebase Realtime Database(REST)로 주고받는다. 로그인 대신 폰이 만든 **6자리 연결 코드**로 가족을 묶는다.

```
폰 앱  --찜·보낸 목적지·아이·추천-->  Firebase families/{코드}  --1초마다 읽기-->  차량 앱
폰 앱  <--경로 상태·시간·거리--------  Firebase families/{코드}  <--ack 쓰기-----  차량 앱
```

| 쓰는 쪽 | 키 | 내용 |
|---|---|---|
| 폰 | `savedPlaces`, `sendToCar` | 찜한 장소(사진·운영시간·이용연령·편의시설), 차량으로 보낸 목적지 |
| 폰 | `recs`, `events`, `kids`, `interests` | 아이별 추천, 오늘의 행사, 아이 목록, 가족 관심사 순위 |
| 차량 | `ack` | 경로 진행 상태, 내비가 계산한 `etaSec`(초)·`distM`(m) |
| 차량 | `carConnectedAt`, `carBeat` | 연결 시각, 5초마다 갱신하는 생존 신호 |

보낸 목적지는 3시간 안의 것만, 처음 받은 것만 처리한다. 읽기·쓰기 규칙은 `firebase/database.rules.json`에 있다.

## 테스트 환경 및 필요한 설정

| 항목 | 내용 |
|---|---|
| 차량 에뮬레이터 | Pleos Connect v2.1.0 APIs, Default boot = Cold, RAM 4GB (만드는 방법은 아래) |
| 폰 에뮬레이터 | 연동 확인용. 짝꿍 휴대폰 앱 APK를 설치한다 |
| 인터넷 | 필요. 폰과 차량이 Firebase로 데이터를 주고받는다 |
| `local.properties` | `FIREBASE_DB_URL` 한 줄 (소스로 빌드할 때만. 제출한 APK는 필요 없다) |
| Firebase 규칙 | 콘솔 → Realtime Database → 규칙에 `firebase/database.rules.json` 게시 |
| 권한 | 위치(FusedLocation), 차량 데이터(`CAR_DRIVE`), 알림. 처음 실행할 때 한 번 요청한다 |
| 연결 | 폰 앱 **내 정보 → 내 차량**의 6자리 코드를 차량 앱에 입력 |

## 직접 빌드해 보기

1. `local.properties.example`을 `local.properties`로 복사해 `FIREBASE_DB_URL`을 채운다. `PLEOS_CLIENT_ID`와 `PLEOS_CLIENT_SECRET`은 비워 둔다(비우면 On-device 음성).
2. Android Studio에서 이 폴더를 열고 Gradle Sync를 한다. Pleos SDK는 공개 Maven 저장소에서 받으며 `settings.gradle.kts`에 이미 들어 있다.
3. 실행 대상 `car`, 기기 Pleos Connect 에뮬레이터로 Run 한다. 권한 없이 설치되지 않게 하려면 Run → Edit Configurations → `car` → Installation Options → Install Flags에 `-g`를 넣는다.
4. release APK는 아래 명령으로 만든다.
   ```bash
   ./gradlew :car:assembleRelease   # car/build/outputs/apk/release/
   ```

Firebase 규칙 적용: Firebase 콘솔 → Realtime Database → 규칙에 `firebase/database.rules.json` 내용을 붙여 넣고 게시한다.

## Pleos Connect 에뮬레이터 만들기

공식 문서: https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator

1. Android Studio → SDK Manager → SDK Update Sites → `+`
   - Name: `Pleos Connect System Image`
   - URL (Windows/Linux): `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml`
   - URL (macOS): `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-arm64-sys-img.xml`
2. SDK Platforms → Show Package Details → Android API 34 → Pleos Connect v2.1.0 APIs 설치 → Studio 재시작
3. Device Manager → Create device → Automotive → Pleos Connect v2.1.0
4. Advanced Settings: Default boot = Cold, RAM = 4GB

## 개인정보와 데이터

- API 키와 Firebase 주소는 저장소에 올리지 않는다(`local.properties`는 `.gitignore`에 포함).
- 폰 앱 설정의 **내 데이터 모두 삭제**를 누르면 서버에 올라간 찜, 아이, 이야기, 연결 정보가 모두 지워진다.

## 향후 계획

- 계정 로그인으로 연결 코드 대신 가족 단위 인증
- 안내 중 경유지 추가 제안(내비 SDK가 지원하면)
- 차량 안에서 아이 취향 갱신