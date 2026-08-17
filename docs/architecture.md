# TripPilot 아키텍처 — Phase 4 adapter 적용

## 원칙

TripPilot은 독립 `:app` Gradle 모듈로 시작한다. 여행 도메인, Room/DataStore, Compose UI, 사용자 승인, Android 외부 Intent는 TripPilot이 소유한다. Codex CLI의 OAuth, credential 수명주기, runtime process와 Provider 통신은 vendored Alpine runtime이 소유한다.

```text
TripPilot Compose UI / ViewModel
          │
          │ CodexRuntimePort (status, login request, model list, draft stream, stop, logout)
          ▼
TripPilot codex-alpine adapter
          │ fixed, Codex-only restricted contract
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

## Phase 3 구조화 draft 상태

- `integration/codex/contract`는 strict JSON의 versioned request/draft 모델과 parser/validator를 소유한다. 알 수 없는 필드, 기간 밖 날짜, 잘못된 URL, enum·길이·항목 수 위반은 검토 전 거부한다.
- `debug`/instrumentation은 `FakeCodexRuntime`으로 결정적 fixture만 사용하며 credentials, URL, user code, prompt/response를 저장·로그 출력하지 않는다. fixture는 정상/빈 결과/계약 위반/stream/stop/late completion/error를 재현한다.
- `CodexRuntimePort`는 lifecycle·진행 상태와 **파싱된 구조화 draft만** 공개한다. raw response chunk는 Port를 넘지 않는다.
- `TripDraftViewModel`과 `DraftPlannerSection`은 request, 수동 JSON, review 상태를 메모리에만 유지한다. `TripRepository.applyApprovedDraft`는 사용자 선택 값만 단일 Room transaction으로 idempotent하게 추가한다.
- Weather advisory는 in-memory 정보 화면이다. repository·Calendar·browser·map·파일 action을 호출하지 않는다.

## Phase 4 Codex adapter와 Phase 5 구현 상태

- `secureDebug`/release는 pinned `alpine-codex-cli-client` source slice를 package하고 `AlpineCodexRuntime`을 선택한다. adapter는 `AgentId.CODEX`만 강제하며, official CLI가 시작한 Device OAuth의 URL/code·상태·live model·구조화 stream만 process memory로 정규화한다. `debug`/instrumentation은 real OAuth를 package/실행하지 않는다.
- adapter는 CLI credential file, token, raw command/argument를 읽지 않는다. Gateway carrier는 app-private Unix domain socket client이며 TCP localhost server나 custom OAuth redirect/token exchange는 없다. browser handoff는 UI의 별도 확인 뒤에만 Android `ACTION_VIEW`로 열린다. 근거와 residual vendor risk는 [`phase4-gate.md`](phase4-gate.md)에 고정했다.
- `feature/external`은 Compose confirmation UI만 소유한다. Calendar permission/ledger, SAF file request, notification permission, `ACTION_VIEW` handoff는 모두 review dialog 이후에만 별도 coordinator로 전달한다.
- `CalendarWriteCoordinator`는 `REVIEW_REQUIRED → APPROVED → EXECUTED|FAILED` ledger를 Room에 기록하고, provider description marker를 다시 조회해 중복 insert를 막는다. provider 실패는 `FAILED`만 남겨 사용자가 재시도할 수 있다. Calendar write와 travel/AI draft transaction은 서로 호출하지 않는다.
- `IcsCodec`은 선택된 local itinerary를 RFC 5545 text로만 변환한다. `TripFileViewModel`은 JSON/ICS 본문을 SAF picker가 끝날 때까지 ViewModel memory에만 두며 import는 parse review 후 새 Trip 사본으로만 반영한다.
- `ReadinessReminderCoordinator`는 opted-in `ReadinessReminderEntity`를 source of truth로 사용한다. boot/package update와 due alarm은 매번 미완료·D-7~D-1·일일 제한을 재평가한다. Alarm은 전달 trigger일 뿐 DB/notification permission 없이 notification을 만들지 않는다.

## Android 지원 기준

- `minSdk = 26`, `compileSdk = 36`, `targetSdk = 35`로 debug foundation을 검증한다.
- 로컬 전용 화면과 Room/DataStore MVP는 ABI를 임의로 제한하지 않는다.
- `secureDebug`/release의 Codex runtime 지원·검증 대상은 `arm64-v8a`다. `debug`/instrumentation은 fake runtime을 사용하고 ABI filter가 없다.
- runtime pack/CLI asset packaging은 검증됐지만, official OAuth 성공·취소·복귀·stream은 사용자가 자신의 계정으로 arm64 기기에서 검증하기 전까지 release Gate를 통과하지 않는다.

## 신뢰 경계 불변식

1. AI 결과는 `TripPlanDraft` 검증과 사용자 검토 전 TripPilot DB에 쓸 수 없다.
2. Calendar, map, browser, SAF file action은 UI confirm action 없이 시작할 수 없다.
3. credential/token/auth file/raw command/raw argument/local TCP endpoint는 Port와 TripPilot 저장소에 존재할 수 없다.
4. runtime 실패, OAuth 취소, stream stop은 local travel data를 바꾸거나 prompt를 자동 재전송할 수 없다.
5. Calendar, file, map, browser, reminder delivery는 confirmation UI 없이 coordinator/handoff에 도달할 수 없다.
