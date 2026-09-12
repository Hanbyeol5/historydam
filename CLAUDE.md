# CLAUDE.md — 역사담(歷史談)

> 페르소나 에이전트 · AR · LBS 기반 **실감형 역사 소통 플랫폼**의 Android 클라이언트.
> 유적지 현장에서 역사 인물을 만나 대화하고, 카메라로 유물을 인식해 해설을 듣는 앱.
> 이 문서는 Claude Code가 본 저장소에서 작업할 때 따르는 **단일 기준(source of truth)**이다.

---

## 1. 프로젝트 개요

- **앱 이름:** 역사담 (歷史談) — *명칭은 잠정, 브랜딩 확정 시 일괄 교체*
- **플랫폼:** Android (휴대폰), **Kotlin** 단일 언어
- **핵심 가치:** "유적지 현장 + 역사 인물 + 실시간 대화"를 AR/LBS로 연결
- **언어:** UI 한국어 우선(`ko`), 영어(`en`) 보조
- **대상 사용자:** 유적지를 방문하는 일반 탐방객 / 학생 / 가족

### 핵심 기능
1. **내 주변 인물(홈):** 현재 위치 기준 가까운 역사 인물을 초상으로 표시, 좌우로 탐색
2. **인물 대화:** 페르소나 챗봇(텍스트) + 초상화 음성 대화(STT/TTS), RAG 기반 사실 응답
3. **AR 인물 탐색·대면:** 지도 핑 → AR 카메라 → 현장을 돌며 인물 탐색 → 근접 시 초상 등장 → 유적지 해설
4. **AR 유물 인식 카메라:** 문화재/유물 촬영 → 인식 → 관련 인물이 해설(텍스트 + 음성)
5. **지도:** 주변 인물/유적지 핑, 길찾기, AR 진입 트리거
6. **위치 알림:** 지오펜싱으로 유적지 진입/방문 시 푸시 알림(FCM)
7. **역사의 전당(프로필):** 경험한 유적지·인물·유물 수집 기록 및 도감

---

## 2. 기술 스택 (고정)

| 영역 | 채택 | 비고 |
|---|---|---|
| 언어 | Kotlin | `jvmTarget` 17 |
| UI | Jetpack Compose + Material 3 | XML 레이아웃 신규 작성 금지 |
| 아키텍처 | Clean Architecture + **MVI** | 단방향 데이터 흐름 |
| 비동기 | Coroutines + Flow | `StateFlow`로 UI 상태 노출 |
| DI | Hilt | |
| 네비게이션 | Navigation Compose (type-safe routes) | |
| 네트워크 | Retrofit + OkHttp + kotlinx.serialization | SSE 스트리밍 지원 |
| 로컬 DB | Room | 인물/유적지/대화 캐시, 도감 |
| 환경설정 | DataStore (Preferences) | 세션/온보딩/권한 상태 |
| 이미지 | Coil | |
| **AR** | **ARCore + SceneView** (`io.github.sceneview:arsceneview`) | Geospatial API 사용 |
| 카메라 | CameraX | 유물 인식 프리뷰/캡처 |
| 지도 | 카카오맵 SDK (`com.kakao.maps:android`) | Google Maps는 대체 옵션 |
| 위치 | FusedLocationProviderClient + Geofencing API | |
| 음성(STT) | Android `SpeechRecognizer` + 서버 STT 폴백 | |
| 음성(TTS) | 서버 인물 보이스 TTS(우선) + Android `TextToSpeech`(폴백) | |
| 오디오 재생 | Media3 / ExoPlayer | TTS 스트림 재생 |
| 푸시 | Firebase Cloud Messaging | |
| 빌드 | Gradle (Kotlin DSL) + **Version Catalog** (`gradle/libs.versions.toml`) | |
| 정적분석 | ktlint(Spotless) + detekt | PR 전 필수 통과 |

- **minSdk 26 / targetSdk 최신 / compileSdk 최신.** (ARCore 지원 단말 전제)
- 라이브러리 버전은 **반드시 `libs.versions.toml`에서만 관리.** 코드/모듈 빌드스크립트에 버전 하드코딩 금지.

---

## 3. 모듈 구조 (멀티모듈)

```
:app                         # 앱 진입점, NavHost, DI 조립, Application
core/
  :core:designsystem         # 단청·한지 테마, 색/타이포/컴포넌트(MedallionPortrait, NavBar 등)
  :core:ui                   # 공통 Compose UI, 네비게이션 contract, 공통 상태 holder
  :core:common               # Result, DispatcherProvider, 확장함수, 에러 모델
  :core:network              # Retrofit/OkHttp, 인터셉터, 인증, DTO 베이스, SSE
  :core:database             # Room (Entity/Dao/DB)
  :core:datastore            # DataStore
  :core:domain               # Entity, Repository 인터페이스, UseCase
  :core:data                 # Repository 구현, Mapper, Remote/Local DataSource
feature/
  :feature:home              # 내 주변 인물(홈)
  :feature:figure            # 인물 선택 시트 / 인물 상세 / 모든 인물(가나다·주변 필터)
  :feature:map               # 지도, 인물 핑, AR 진입
  :feature:ar                # AR 탐색(나침반/레이더) + 대면(초상 등장·해설)
  :feature:camera            # 유물/문화재 인식 카메라
  :feature:chat              # 페르소나 챗봇(텍스트)
  :feature:voice             # 초상화 음성 대화 + 실시간 자막
  :feature:profile           # 내 프로필 / 역사의 전당(도감)
  :feature:notification      # 위치 기반 알림 목록
```

**의존 규칙 (위반 금지):**
- `feature:*` → `core:domain`, `core:ui`, `core:designsystem` 만 의존. **feature 간 직접 의존 금지** (네비게이션은 `:app`/`core:ui` contract 경유).
- `core:data` → `core:domain`, `core:network`, `core:database`, `core:datastore`.
- `core:domain` 은 **순수 Kotlin 모듈**(Android 의존 금지). UseCase·Entity·Repository 인터페이스만.
- `:app` 만 모든 모듈을 조립한다.

---

## 4. 아키텍처 원칙

- **레이어:** `presentation(Compose+ViewModel)` → `domain(UseCase)` → `data(Repository)`.
- **MVI 계약** (각 화면 ViewModel):
  - `XxxUiState` (data class, 불변) — `StateFlow<XxxUiState>`로 노출
  - `XxxUiEvent` (sealed) — 사용자 입력 → `onEvent(event)`
  - `XxxUiEffect` (sealed) — 일회성 효과(네비게이션, 토스트) → `Channel`/`SharedFlow`
- **데이터 흐름:** 단방향. Composable은 상태를 받고 이벤트만 올려보낸다(stateless 지향).
- **Repository 반환:** 목록/스트림은 `Flow<T>`, 단발 작업은 `Result<T>`(`core:common`의 sealed Result).
- **에러:** 도메인 에러(`AppError`)로 매핑해 표면화. raw 예외를 UI까지 던지지 않는다.
- **DI:** 생성자 주입 우선. `@HiltViewModel`, 모듈은 `core/*`·`feature/*` 내부 `di` 패키지.

---

## 5. 패키지 / 네이밍 컨벤션

- 패키지 루트: `com.samdori93.yeoksadam` *(applicationId 확정 시 교체)*
- 화면 패키지: `feature.<name>.{ui, viewmodel, navigation, di}`
- 네이밍:
  - 화면 진입 Composable: `XxxRoute`(상태 연결) + `XxxScreen`(stateless UI)
  - 상태/이벤트/효과: `XxxUiState` / `XxxUiEvent` / `XxxUiEffect`
  - UseCase: 동사형 + `UseCase` (`GetNearbyFiguresUseCase`)
  - Repository: `XxxRepository`(domain) / `XxxRepositoryImpl`(data)
  - DTO: `XxxDto`(remote), Entity(Room): `XxxEntity`, 도메인: `Xxx`
- `@Preview` 는 화면/주요 컴포넌트마다 작성, 단청·한지 테마 래핑.

---

## 6. 화면 ↔ 모듈 매핑 (구현 명세는 `/docs/screens/`)

| # | 화면 | 모듈 | 핵심 동작 |
|---|---|---|---|
| 1 | 홈 · 내 주변 인물 | `feature:home` | 위치 기반 인물 초상 캐러셀, 거리 표시, 하단 5탭(카메라·지도·홈·Q&A·메뉴) |
| 2 | 인물 선택 시트 | `feature:figure` | 초상 탭 → 배경 딤 + [관련 유적지 / 대화하기] |
| 3 | 유물 인식 카메라 | `feature:camera` | CameraX 프리뷰 → 인식 → 관련 인물 칩 → 해설(텍스트/🔊) |
| 4 | 지도 | `feature:map` | 카카오맵 + 인물 핑 → 카드 → **[AR 카메라로 이동]** |
| 5 | 챗봇(텍스트) | `feature:chat` | RAG 페르소나 응답(SSE 스트리밍) + [초상화 대화로 전환] |
| 6 | 음성 대화 | `feature:voice` | STT 입력 + TTS·립싱크 초상 + 실시간 자막 |
| 7 | 모든 인물 | `feature:figure` | 가나다순(기본)/주변 필터, 검색 |
| 8 | 내 프로필 · 역사의 전당 | `feature:profile` | 유적지/인물/유물 도감(발견·미발견) |
| — | **AR 탐색·대면** | `feature:ar` | 지도 핑→AR 진입→탐색(나침반/레이더/거리)→근접 발견→초상 등장·해설→대화 연계 |

> 모든 상단바에 **홈(집) 아이콘**으로 홈 복귀 동선 통일.

---

## 7. 핵심 도메인 모델 (`core:domain`)

```kotlin
data class Figure(                 // 역사 인물
    val id: String,
    val name: String,              // 예: "채제공"
    val title: String,             // 호/직함 예: "번암"
    val portraitUrl: String,       // 라이선스 확인된 초상 (배경제거 컷아웃 url 별도)
    val cutoutUrl: String?,        // AR용 배경제거 PNG
    val relatedSiteIds: List<String>,
    val voiceId: String?,          // TTS 보이스 프로필
)

data class HeritageSite(           // 유적지
    val id: String, val name: String,
    val lat: Double, val lng: Double,
    val description: String,
    val geofenceRadiusM: Float,    // 지오펜싱 반경
)

data class Relic(val id: String, val name: String, val era: String, val relatedFigureIds: List<String>)

data class NearbyFigure(val figure: Figure, val site: HeritageSite, val distanceM: Float, val bearingDeg: Float)

data class ArTarget(               // AR 탐색 대상
    val figureId: String, val siteId: String,
    val lat: Double, val lng: Double, val altitude: Double?, val headingDeg: Float?,
    val foundThresholdM: Float = 5f, val bearingToleranceDeg: Float = 20f,
)

data class ChatMessage(val role: Role, val text: String, val citations: List<Citation> = emptyList())
enum class Role { USER, FIGURE }
data class Citation(val source: String, val excerpt: String)

data class Discovery(val type: DiscoveryType, val refId: String, val discoveredAt: Long)
enum class DiscoveryType { SITE, FIGURE, RELIC }
```

---

## 8. 백엔드 연동 (계약 요약, 상세는 `/docs/api/`)

- **Base URL / 키는 `local.properties` → `BuildConfig`** (저장소에 절대 커밋 금지)
- 인증: JWT (`Authorization: Bearer`), 만료 시 refresh 인터셉터

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/v1/nearby?lat&lng&radius` | 주변 인물·유적지 (지도/홈) |
| GET | `/v1/figures`, `/v1/figures/{id}` | 인물 목록/상세 |
| POST | `/v1/chat` / `/v1/chat/stream`(SSE) | RAG 페르소나 응답 `{figureId, sessionId, message}` → `{reply, citations}` |
| POST | `/v1/vision/recognize` (multipart) | 유물 인식 → `{relicId, name, confidence, relatedFigureIds}` |
| POST | `/v1/tts` | `{figureId, text}` → 오디오(스트림/URL) |
| POST | `/v1/stt` | 오디오 → 텍스트 (온디바이스 폴백) |
| POST | `/v1/discoveries` | 도감 기록 적재 |
| POST | `/v1/devices` | FCM 토큰 등록 |

---

## 9. AR 구현 가이드 (`feature:ar`)

핵심 동선: **지도 핑 → `AR 카메라로 이동` → 탐색 → 근접 발견 → 초상 등장·해설 → 대화 연계**

- **앵커링:**
  - 1순위: **ARCore Geospatial API**(VPS, lat/lng/altitude/heading)로 실제 좌표에 인물 배치.
  - 폴백: GPS 거리 + 나침반 방위(bearing) 휴리스틱으로 방향 안내(화살표/레이더).
- **탐색 UI:** 타깃 칩(인물·거리), 방향 화살표(`bearingToleranceDeg` 기준), 레이더 미니맵, 남은 거리 게이지.
- **발견 판정:** `distanceM ≤ foundThresholdM` **AND** 카메라 heading 이 타깃 bearing 의 `±bearingToleranceDeg` 이내 → `Found`.
- **렌더링:**
  - **MVP:** 배경 제거 **2D 컷아웃(PNG) 빌보드** anchor (라이선스 확인 에셋만).
  - **확장:** `.glb` **3D 리깅 아바타**(SceneView), 표준영정/초상 텍스처 매핑.
- **립싱크(Talking-Head):** TTS 음소(viseme) 타임라인에 입 모양 동기화.
  - 추상화: `TalkingHeadController` (2D = 입 스프라이트/모프, 3D = blendshape). 음성 재생 위치와 viseme 큐를 동기화.
  - 서버가 viseme 타임라인을 함께 내려주는 것을 우선(없으면 진폭 기반 간이 동기화).
- **권한:** CAMERA, ACCESS_FINE_LOCATION. ARCore 미지원 단말은 graceful 폴백(지도/카드 안내).

> ⚠️ **전신 입상**은 현 흉상 초상으로 불가. 전신은 별도 전신 초상/3D 리깅/일러스트 에셋이 있을 때만 활성화.

---

## 10. 위치 / 지오펜싱 (`feature:notification`, `core:data`)

- `FusedLocationProviderClient` 로 위치, `GeofencingClient` 로 유적지 진입/체류 트리거 → 로컬/FCM 알림.
- 백그라운드 위치는 최소 권한·배터리 고려. `ACCESS_BACKGROUND_LOCATION` 은 필요한 경우에만, 사유 고지 후 요청.
- 지오펜스 재등록: 부팅(`BOOT_COMPLETED`)·앱 시작 시 동기화.

## 11. 음성 (`feature:voice`)

- 입력: `SpeechRecognizer`(온디바이스) → 실패/품질 미달 시 서버 STT.
- 출력: 서버 인물 보이스 TTS 스트림을 ExoPlayer 로 재생, 실시간 자막은 `chat/stream` 토큰과 동기화.
- 음성·텍스트 대화는 **세션(sessionId) 공유** — 전환해도 맥락 유지.

---

## 12. ⚠️ 에셋 / 저작권 정책 (필수 준수)

- 인물 초상은 **(1) 퍼블릭 도메인 진본 초상화, (2) 정식 라이선스 에셋, (3) 자체 제작 일러스트** 만 사용.
- **현대 표준영정·지폐 도상 등 저작권 있는 이미지의 무단 사용·커밋 금지.**
- AR 배경 제거 컷아웃은 **라이선스가 확인된 원본**에서만 생성.
- 라이선스 메타데이터(`출처/권리/사용범위`)를 에셋과 함께 `/assets/figures/<id>/license.json` 으로 관리.

---

## 13. 빌드 · 실행 · 검증

```bash
# 디버그 빌드 / 설치
./gradlew assembleDebug
./gradlew installDebug

# 단위 테스트 / 계측 테스트
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest

# 정적 분석 (PR 전 필수)
./gradlew spotlessApply        # ktlint 포맷
./gradlew detekt
./gradlew lintDebug
```

- 시크릿은 `local.properties` 에:
  `API_BASE_URL=`, `KAKAO_MAP_KEY=`, `GEOSPATIAL_API_KEY=` 등 → `BuildConfig` 노출.
- `google-services.json` 은 로컬 배치(커밋 금지). FCM 사용.

---

## 14. 코딩 규칙 & DO / DON'T

**DO**
- 라이브러리 버전은 `libs.versions.toml` 로만 관리.
- UI 문자열은 `strings.xml`(한국어 기본). 하드코딩 금지.
- 비즈니스 로직은 **UseCase** 에. ViewModel 은 상태 조립/이벤트 처리만.
- I/O 는 `Dispatchers.IO`(주입된 `DispatcherProvider`).
- Compose 는 stateless + `XxxRoute`/`XxxScreen` 분리, `@Preview` 작성.
- 새 화면은 해당 `feature` 모듈에, 도메인은 `core:domain` 에 추가.

**DON'T**
- `GlobalScope` / 메인스레드 블로킹 / `runBlocking`(테스트 외) 금지.
- feature 간 직접 의존, `core:domain` 의 Android 의존 금지.
- API 키·`google-services.json`·저작권 에셋 커밋 금지.
- XML 신규 레이아웃, 단발 효과를 `UiState` 에 담는 것 금지(→ `UiEffect`).
- 인물 발화에 사료 근거 없는 단정/창작 사실 주입 금지(RAG citation 우선).

---

## 15. 디렉토리(참고) 예시 — `:feature:ar`

```
feature/ar/
  src/main/kotlin/.../feature/ar/
    ui/        ArSearchScreen.kt, ArEncounterScreen.kt, ArRoute.kt, components/
    viewmodel/ ArViewModel.kt, ArUiState.kt, ArUiEvent.kt, ArUiEffect.kt
    ar/        ArAnchorManager.kt, TalkingHeadController.kt, BearingResolver.kt
    navigation/ArNavigation.kt
    di/        ArModule.kt
```

---

## 16. 작업 시 참고 문서

- `/docs/screens/` — 8개 화면 + AR 동선 상세 명세(시안 HTML 포함)
- `/docs/architecture/system-diagram.html` — 시스템 구성도
- `/docs/api/` — 백엔드 API 계약
- `/assets/figures/` — 인물 초상·컷아웃·라이선스

> Claude Code 는 코드를 작성/수정하기 전에 관련 `feature`·`core` 모듈의 기존 패턴과 본 문서의 규칙을 먼저 확인할 것.
