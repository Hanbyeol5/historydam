# 역사담 (歷史談) — Android 클라이언트

> 페르소나 에이전트 · AR · LBS 기반 **실감형 역사 소통 플랫폼**의 Android 앱.
> 유적지 현장에서 역사 인물을 만나 대화하고, 카메라로 유물을 인식해 해설을 듣는다.

프로젝트 규칙·아키텍처의 단일 기준은 [`CLAUDE.md`](CLAUDE.md) 입니다.

## 기술 스택

- **언어/UI:** Kotlin · Jetpack Compose · Material 3
- **아키텍처:** Clean Architecture + MVI (단방향 흐름, `StateFlow`)
- **멀티모듈:** `build-logic` 컨벤션 플러그인 + Version Catalog(`gradle/libs.versions.toml`)
- **DI:** Hilt · **네비게이션:** Navigation Compose(type-safe)
- **지도:** 네이버 지도 SDK(`io.github.fornewid:naver-map-compose`)
- **카메라:** CameraX · **음성:** `SpeechRecognizer`(STT) + `TextToSpeech`(TTS)
- **위치:** FusedLocation + Geofencing · **디자인:** 단청(丹靑)·한지(韓紙) 테마, 나눔명조

## 현재 구현 상태

| 화면 / 영역 | 모듈 | 상태 |
|---|---|---|
| 멀티모듈 Gradle 골격 + 디자인시스템 | `build-logic`, `core:designsystem` | ✅ |
| **홈 · 내 주변 인물** (위치 기반 거리·방위 실시간 계산) | `feature:home` | ✅ |
| **지도** — 네이버 지도 + 유적지별 인물 핑 + 내 위치 | `feature:map` | ✅ |
| **유물 인식 카메라** — 일반 카메라 → 촬영 → 인식 → 인물 선택 → 해설(🔊) | `feature:camera` | ✅ |
| **챗봇(텍스트)** — 페르소나 응답 + 입력 | `feature:chat` | ✅ |
| **음성 대화** — STT 입력 + TTS 응답 + 실시간 자막 | `feature:voice` | ✅ |
| **위치 알림** — 유적지 지오펜싱 진입 알림 | `feature:notification` | ✅ |
| 인물 상세 / 프로필(도감) | `feature:figure`, `feature:profile` | ✅ |
| AR 탐색·대면 (ARCore Geospatial) | `feature:ar` | 🟡 스캐폴드 |

> 홈·지도·카메라·챗봇·음성은 백엔드 없이 **온디바이스로 동작**합니다.
> 인물 응답·유물 해설은 `core:ui` 의 샘플 데이터를 사용하며, 실제 API 연동 시
> `core:data` 의 Repository 를 서버(`/v1/*`)로 교체하도록 설계돼 있습니다.

## 빌드 / 실행

Android Studio (Ladybug 이상) 에서 **폴더를 열면** Gradle 동기화가 진행됩니다.

### 1) `local.properties` 준비 (커밋 금지)

```bash
cp local.properties.example local.properties
```

그런 다음 값을 채웁니다:

```properties
sdk.dir=C\:\\Users\\<사용자>\\AppData\\Local\\Android\\Sdk   # Android Studio 가 자동 설정
API_BASE_URL=https://api.example.com/
NAVER_MAP_CLIENT_ID=발급받은_NCP_지도_Client_ID              # 지도 타일 렌더링에 필요
GEOSPATIAL_API_KEY=                                          # (AR 확장용, 선택)
```

> **네이버 지도 키:** [네이버 클라우드 플랫폼](https://www.ncloud.com) → **Maps → Mobile Dynamic Map** 에서
> Client ID 를 발급하고, **애플리케이션 등록 시 패키지명 `com.samdori93.yeoksadam` 을 반드시 추가**해야
> 인증(401)이 통과되고 지도 타일이 표시됩니다.

### 2) 빌드 / 설치 / 검증

```bash
./gradlew assembleDebug          # 디버그 APK 빌드
./gradlew installDebug           # 연결된 기기에 설치
./gradlew testDebugUnitTest      # 단위 테스트
./gradlew spotlessApply detekt   # 포맷 + 정적 분석
```

> **Android SDK 필요.** `ANDROID_HOME` 또는 `local.properties` 의 `sdk.dir` 가 설정돼 있어야 합니다.
> **minSdk 26** — 실기기(개발자 모드) 또는 API 26+ 에뮬레이터에서 실행하세요.

## 모듈 의존 규칙 (요약)

- `feature:*` → `core:domain`, `core:ui`, `core:designsystem`, `core:common` 만 의존.
  **feature 간 직접 의존 금지** — 화면 전환은 `:app` 의 `YeoksadamNavHost` 가 콜백으로 연결.
- `core:domain` · `core:common` 은 **순수 Kotlin**(Android 비의존).
- `:app` 만 전체 모듈을 조립.

자세한 규칙은 [`CLAUDE.md`](CLAUDE.md) 참고.

## 다음 작업 후보

- 백엔드 연동: `/v1/*` 실연결 + Room 캐시 + JWT 인터셉터 + SSE 스트리밍
- `feature:ar` ARCore Geospatial 앵커 + 2D 컷아웃 빌보드 + 립싱크
- FCM 푸시 + 지오펜싱 백그라운드 재등록(`BOOT_COMPLETED`)
