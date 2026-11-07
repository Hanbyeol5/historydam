# 역사담 (歷史談) — Android 앱

> 페르소나 에이전트 · AR · LBS 기반 **실감형 역사 소통 플랫폼**.
> 유적지 현장에서 역사 인물을 만나 대화하고, 카메라로 유물을 인식해 해설을 듣는다.

프로젝트 규칙·아키텍처의 기준 문서는 [`CLAUDE.md`](CLAUDE.md) 입니다.

## 주요 기능

| 기능 | 모듈 | 내용 |
|---|---|---|
| 홈 · 내 주변 인물 | `feature:home` | 현재 위치 기준 가까운 인물 초상 캐러셀, 거리 표시 |
| 지도 | `feature:map` | 네이버 지도 + 인물 초상 핀 · 유적 핀, 핀 → 카드 → AR 진입 |
| AR 탐색 · 대면 | `feature:ar` | 카메라 화면 위에 GPS·나침반으로 유적 라벨과 인물 방향 표시, 「찾기」 시 3D 인물(정약용)을 유적 쪽에 세움 (100m 안 실제 위치, 밖은 6m 앞 미리보기) |
| 유물 인식 카메라 | `feature:camera` | 촬영 → Gemini 판별(주변 3km 문화재 8곳을 힌트로) → 해설(🔊) + 관련 인물 (앱 인물 연결 · 「AI 추정」 추천) |
| 인물 대화(텍스트) | `feature:chat` | **백엔드 RAG**(기본) — 공공기관 자료를 근거로 인물 말투로 답하고 출처 표시 |
| 초상화 음성 대화 | `feature:voice` | STT 입력 + 인물 보이스 TTS + 실시간 자막, 챗봇과 대화 기록 공유. 3D 모델이 있는 인물은 바닥 위에 실제 키로 세움 |
| 모든 인물 · 인물 상세 | `feature:figure` | 가나다순 / 주변 필터, 관련 유적 |
| 역사의 전당(도감) | `feature:profile` | 발견한 유적 · 인물 · 유물 수집, 대화 방식 · 지식 경계 필터 설정 |
| 위치 알림 | `feature:notification` | 유적 지오펜스 진입 시 알림 |

## 백엔드 RAG (`core:backend`)

팀 백엔드([`backend/main.py`](backend/main.py), FastAPI)의 인물 대화 로직을 **앱 안으로 옮긴 모듈**입니다.
서버 없이 휴대폰이 공공기관 API 와 Gemini 를 직접 호출합니다.

```
질문 ─① 의도 분류 (backend 의 TF-IDF + LogisticRegression 을 앱에서 같은 점수로 계산)
     ├─② 역사 질문이면 공공기관 자료 7곳을 동시에 검색
     └─③ 인물 설정 + 자료 + 질문 → Gemini → 인물 말투 답변 + 출처
```

| # | 자료원 | 기관 | 키 |
|---|---|---|---|
| 1 | 소장품 | 국립중앙박물관 (data.go.kr) | `PUBLIC_API_KEY` |
| 2 | 나라기록물 | 국가기록원 (data.go.kr) | `PUBLIC_API_KEY` |
| 3 | 한국학자료통합플랫폼 | 한국학중앙연구원 | `KDP_API_KEY` |
| 4 | 국가유산 정보 (인물의 관련 유적) | 국가유산청 | 없음 |
| 5 | 조선시대 개인일기 | 국가유산 지식이음 | 없음 |
| 6 | 조상화 국가지정목록 | 국가유산 지식이음 | 없음 |
| 7 | 고문헌(미술분야) | 국가유산 지식이음 | 없음 |

- 근거 신뢰도를 위해 **공공기관 자료만** 씁니다 (위키백과 등 누구나 고치는 자료 제외).
- 지식이음 5~7 은 검색어를 받지 않는 API 라, 대화를 시작할 때 뒤에서 전체 목록을 받아 두고 그 안에서 찾습니다.
- 인물 설정: backend 의 세종대왕 · 이순신 · 장영실은 그대로, 나머지 인물은 앱 인물 자료(직함 · 생몰년 · 말씨)로 만듭니다.
- 의도 분류 모델을 다시 학습했으면(`backend/train_intent.py`) [`scripts/export_intent_model.py`](scripts/export_intent_model.py) 로 앱용 값을 다시 뽑습니다.

메뉴 → 설정에서 대화 방식을 바꿀 수 있습니다: **백엔드 RAG**(기본) · 기본 · RAG 서버 · 외부 RAG 서버(`backend/main.py` 를 직접 띄운 주소).

## 서버 · 데이터

| 무엇 | 어디 |
|---|---|
| 인물 · 유적 자료, 초상 | 역사담 웹앱 데이터 (`samcho93.github.io/arHeritage/data/…`) |
| 「기본」·「RAG 서버」 대화, 인물 보이스 TTS, 사진 인식 폴백 | 역사담 Cloudflare Worker (`yeoksadam-api`) |
| 백엔드 RAG 대화, 사진 인식 · 관련 인물 추천 | 앱 안 (`core:backend`) → 공공기관 API · Gemini |
| 3D 인물 모델 | `core/ui/src/main/assets/models/<인물 id>.glb` (저장소에 없음 — 아래 「3D 모델 파일」 참고) |
| 팀 백엔드 원본 | [`backend/`](backend) (FastAPI, 직접 실행 시 「외부 RAG 서버」로 연결) |

## 실행하기

Android Studio 에서 폴더를 열면 Gradle Sync 가 진행됩니다. 이미 받아 둔 경우 `git pull origin main` 후 **Gradle Sync** 를 한 번 해 주세요.

### 1) `local.properties` 준비 (커밋 금지)

```bash
cp local.properties.example local.properties
```

```properties
sdk.dir=…                    # Android Studio 가 자동 설정
NAVER_MAP_CLIENT_ID=…        # 지도 (없으면 지도가 보이지 않음)
GEMINI_API_KEY=…             # 백엔드 RAG 대화 (없으면 메뉴→설정→「기본」으로 대화)
PUBLIC_API_KEY=…             # 공공데이터포털 — 박물관 · 국가기록원 (없으면 이 두 곳만 빠짐)
KDP_API_KEY=…                # 한국학자료통합플랫폼 (없으면 이 곳만 빠짐)
```

> **네이버 지도 키:** [네이버 클라우드 플랫폼](https://www.ncloud.com) → **Maps → Mobile Dynamic Map** 에서
> Client ID 를 발급하고, 애플리케이션에 패키지명 `com.samdori93.yeoksadam` 을 등록해야 지도가 표시됩니다.
>
> 키는 빌드 시 앱에 들어갑니다. 발표 · 시연용이며, 일반 배포 전에는 키를 서버로 옮기고 새로 발급하세요.

### 2) 3D 모델 파일 (커밋 금지)

정약용 3D 모델은 라이선스(Tripo AI 이용 조건 · 원본 초상 권리)와 고증 확인 전이라 저장소에 올리지 않았습니다.
팀에서 받은 `jeong-yakyong.glb` 를 아래 위치에 직접 넣어 주세요.

```
core/ui/src/main/assets/models/jeong-yakyong.glb
```

- 등록된 모델 목록은 `core:ui` 의 `Figure3dModels`, 출처 · 권리 메모는 [`assets/figures/jeong-yakyong/license.json`](assets/figures/jeong-yakyong/license.json).
- 권리 확인이 끝나면 `license.json` 의 `rights` · `verifiedAt` 을 채운 뒤 커밋합니다.

### 3) 빌드 · 설치 · 검증

```bash
./gradlew assembleDebug                     # 디버그 APK
./gradlew installDebug                      # 연결된 기기에 설치
./gradlew :app:assembleRelease              # 배포용 APK (CPU 별 분할, arm64 ≈ 32MB)
./gradlew testDebugUnitTest                 # 단위 테스트 (core:backend 는 파이썬 의도 분류와 같은 점수인지 확인)
./gradlew spotlessApply detekt              # 포맷 + 정적 분석
```

> **minSdk 26** — 실기기(개발자 모드) 또는 API 26+ 에뮬레이터에서 실행하세요. 3D AR 은 ARCore 지원 기기에서만 보이고, 미지원 기기는 2D 초상으로 대체됩니다.
>
> 삼성 보안 폴더가 있는 폰에서 `installDebug` 가 실패하면 `adb install --user 0 -r app/build/outputs/apk/debug/app-arm64-v8a-debug.apk` 로 설치하세요.

## 기술 스택

- **언어 / UI:** Kotlin · Jetpack Compose · Material 3 · 단청(丹靑)·한지(韓紙) 테마, 나눔명조
- **아키텍처:** Clean Architecture + MVI (`StateFlow` 단방향), 멀티모듈 + `build-logic` 컨벤션 플러그인 + Version Catalog
- **DI / 네비게이션:** Hilt · Navigation Compose (type-safe)
- **네트워크 / 저장:** Retrofit · OkHttp · kotlinx.serialization · Room · DataStore
- **지도 / 위치:** 네이버 지도 Compose · FusedLocation · Geofencing
- **카메라 / AR:** CameraX + GPS · 나침반 방위 (위치 기반 AR 라벨), ARCore + SceneView (3D 인물, GLB)
- **음성:** `SpeechRecognizer`(STT) · 서버 인물 보이스 TTS(+ 기기 `TextToSpeech` 폴백)
- **AI:** Gemini (`gemini-3-flash-preview`) · 의도 분류 TF-IDF + LogisticRegression

## 모듈 구조

```
:app                      앱 진입점, NavHost, DI 조립
core/
  designsystem · ui · common
  domain                  Entity · Repository 인터페이스 · UseCase (순수 Kotlin)
  data                    Repository 구현 (카탈로그 · 대화 · 위치 · 도감 …)
  network                 웹앱 데이터 · Worker API
  backend                 백엔드 RAG (의도 분류 · 공공기관 자료 7곳 · Gemini)
  database · datastore    Room · DataStore
feature/
  home · map · ar · camera · chat · voice · figure · profile · notification
backend/                  팀 백엔드 원본 (FastAPI · 의도 분류 학습)
scripts/                  의도 분류 모델 → 앱용 JSON 변환
```

- `feature:*` → `core:domain`, `core:ui`, `core:designsystem`, `core:common` 만 의존. **feature 간 직접 의존 금지** — 화면 전환은 `:app` 의 `YeoksadamNavHost` 가 연결.
- `core:domain` · `core:common` 은 **순수 Kotlin**(Android 비의존).
- `:app` 만 전체 모듈을 조립.

자세한 규칙은 [`CLAUDE.md`](CLAUDE.md) 참고.

## 다음 작업 후보

- 의도 분류 학습 문장 확대 — 지금은 20문장이라 역사 질문도 「단순 대화」로 분류돼 자료 검색을 건너뛰는 경우가 많음
- 일반 배포 전 API 키를 서버로 이전 (`backend/` 서버 배포 + 앱은 「외부 RAG 서버」 방식)
- 3D 인물 추가(정조 · 채제공) · 근접 시 자동 등장 · 립싱크
- 3D 모델 라이선스 · 고증 확인 후 커밋
- ARCore Geospatial 앵커 (지금은 GPS + 나침반)
