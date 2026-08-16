# TripPilot 아키텍처 — Phase 1

## 원칙

TripPilot은 독립 `:app` Gradle 모듈로 시작한다. 여행 도메인, Room/DataStore, Compose UI, 사용자 승인, Android 외부 Intent는 TripPilot이 소유한다. Codex CLI의 OAuth, credential 수명주기, runtime process와 Provider 통신은 미래 Alpine runtime이 소유한다.

```text
TripPilot Compose UI / ViewModel
          │
          │ CodexRuntimePort (status, login request, model list, draft stream, stop, logout)
          ▼
TripPilot codex-alpine adapter  [Phase 4 only]
          │ fixed, restricted public contract
          ▼
Alpine Codex runtime / official Codex CLI
          │ Device OAuth + Provider traffic
          ▼
OpenAI
```

## 책임 경계

| 소유자 | 허용 책임 | 금지 책임 |
|---|---|---|
| TripPilot UI/domain/data | 여행 CRUD, draft 검증·검토, 사용자 승인, 상태 표시 | token·credential·`auth.json` 읽기/복사, raw CLI 실행 |
| `CodexRuntimePort` | runtime/auth projection, 로그인 시작 요청, model/draft stream 정규화 | Android permission, DB write, Calendar/browser 호출 |
| `codex-alpine` adapter | 고정된 런타임 API 호출·오류 mapping | generic terminal/MCP API, Grok fallback, raw method 노출 |
| Alpine runtime/CLI | Device OAuth, refresh, process, private transport | TripPilot 데이터 모델·UI·외부 action 결정 |

## Phase 1 구현 상태

- 현재 `FakeCodexRuntime`만 존재하며 credentials, URL, user code, prompt/response를 저장·로그 출력하지 않는다.
- `CodexRuntimePort`는 `runtimeStatus`, `authStatus`, `beginLogin`, `availableModels`, `createPlanStream`, `stop`, `logout`만 공개한다.
- `PlanStreamEvent`는 Phase 1에서 content-free lifecycle event만 사용한다. Phase 3의 versioned draft validator가 구조화 결과를 받아야 한다.
- navigation, Hilt, Room, DataStore, serialization 의존성은 Phase 2/3 책임 단위를 준비할 뿐, 현재 DB·network·OAuth는 생성하지 않는다.

## Android 지원 기준

- `minSdk = 26`, `compileSdk = 36`, `targetSdk = 35`로 debug foundation을 검증한다.
- 로컬 전용 화면과 Room/DataStore MVP는 ABI를 임의로 제한하지 않는다.
- v1의 Alpine Codex runtime이 실제로 추가되는 경우 지원·검증 대상은 `arm64-v8a`다. Phase 1에는 runtime native asset이 없으므로 ABI filter나 PRoot pack을 넣지 않는다.
- Phase 4 전 runtime asset·CLI·gateway가 arm64 physical device에서 동작한다는 별도 증거가 없으면 Codex 기능은 disabled 상태를 유지한다.

## 신뢰 경계 불변식

1. AI 결과는 `TripPlanDraft` 검증과 사용자 검토 전 TripPilot DB에 쓸 수 없다.
2. Calendar, map, browser, SAF file action은 UI confirm action 없이 시작할 수 없다.
3. credential/token/auth file/raw command/raw argument/local TCP endpoint는 Port와 TripPilot 저장소에 존재할 수 없다.
4. runtime 실패, OAuth 취소, stream stop은 local travel data를 바꾸거나 prompt를 자동 재전송할 수 없다.
