# TripPilot 개인정보·외부 전송 경계

## AI와 Codex 상태

- `debug`/instrumentation의 `FakeCodexRuntime`은 앱 안의 결정적 fixture만 사용하며 HTTP 호출을 하지 않는다. `secureDebug`/release에는 공식 Codex Device OAuth·CLI 통신을 위한 `INTERNET` permission이 포함된다.
- 여행 초안 요청 화면은 목적지, 시작일·종료일, 동행 유형, 예산 범위, 관심사, 여행 목적만 `TripPlanningRequest`로 구성한다. 여행 DB ID, 기존 예약 원문, 공유 텍스트, 계정 정보는 계약에 넣지 않는다.
- 수동 JSON 붙여넣기는 과거 채팅·클립보드 기록을 검색하지 않는다. 입력 문자열은 strict parser를 통과시키는 동안만 메모리에 존재하며, 화면을 닫거나 검토를 끝내면 Room, DataStore, backup, export에 저장하지 않는다.
- fake runtime도 원문 JSON을 UI/저장소에 넘기지 않는다. 파싱·검증한 구조화 draft와 진행 상태만 일시적으로 전달한다.
- 날씨 advisory는 정보 전용 in-memory 화면이다. 일정·예약·준비·짐·출처 DB write, Calendar, 알림, 브라우저·지도 실행을 만들 수 없다.

## 사용자가 확정해 저장하는 데이터

사용자가 초안 검토 화면에서 선택하고 수정한 일정, 예약, Preparation, Packing, 출처만 하나의 Room transaction으로 저장한다. 가정, AI 원문 요청·응답, OAuth 상태·토큰·자격 증명은 저장 대상이 아니다. 기존 체크리스트는 대체하거나 삭제하지 않으며, 같은 항목은 추가하지 않는다.

## Codex 연동 경계

- Device OAuth와 token/credential/auth file 수명주기는 Alpine Codex runtime이 소유한다. TripPilot은 token, credential, `auth.json`, raw command 또는 raw argument를 읽거나 저장·백업·로그 출력하지 않는다.
- 실제 runtime으로 전송 가능한 값은 이 문서의 `TripPlanningRequest` 및 별도 versioned reservation/weather request로 제한한다. 사용자는 요청 전 전송 범위를 보고, Device OAuth browser handoff도 별도 dialog에서 직접 승인해야 한다.
- raw prompt/response, 모델 transcript, 계정 세부정보는 Room, DataStore, backup/export, analytics/crash report, logcat에 기록하지 않는다.
- `AlpineCodexRuntime`은 reference runtime의 app-private Unix domain socket carrier만 사용한다. TCP localhost server, custom OAuth redirect/token exchange, API key, fallback Provider, browser automation은 만들지 않는다.
- verification URL/user code는 로그인 완료·취소·오류 시 지워지는 process-memory 값이다. CLI-owned credential directory는 Android backup/D2D 대상에서 제외되며, TripPilot은 파일 내용에 접근하지 않는다.

## Phase 5: 사용자가 승인하는 외부 실행

- 모든 외부 실행은 TripPilot의 **사용자 확인** dialog를 거친다. dialog를 닫거나 취소하면 DB·Calendar·파일·외부 앱 실행은 일어나지 않는다.
- **Calendar**: 사용자가 일정별 체크 → 미리보기 → Calendar 권한 승인(필요 시) → `선택한 일정 추가`를 각각 직접 수행해야 한다. TripPilot은 `CalendarAction` ledger의 marker로 같은 일정을 중복 추가하지 않으며, 실패 상태만 재시도한다. 기존 이벤트의 수정·삭제·자동 동기화는 하지 않는다.
- **지도/링크**: 사용자가 확인 창에서 승인한 장소 또는 `http/https` URL만 Android `ACTION_VIEW`로 전달한다. 지도/브라우저 화면을 읽지 않고 WebView·로그인 자동화·복귀 후 자동 반영을 하지 않는다.
- **SAF 파일**: ICS 및 JSON backup export는 확인 뒤 Android 저장 위치 선택기를 연다. JSON import는 파일 선택 → schema/크기 검토 → 새 사본 생성 확인의 두 승인 단계다. export/import에는 AI 원문, OAuth, CalendarAction, reminder 상태가 포함되지 않는다.
- **알림**: 사용자가 알림 설정과 Android notification permission을 모두 승인한 경우에만 D-7~D-1 미완료 항목을 하루 한 번 알린다. boot/package update 후에도 Room 설정을 기준으로 다시 예약한다. 권한 거부 시 알림을 보내거나 기록하지 않는다.
- 앱은 analytics SDK·자체 서버를 포함하지 않는다. `INTERNET` permission은 사용자가 명시적으로 시작한 Codex runtime/OAuth 경로에만 필요하며, Android cloud backup/D2D는 차단하고 cleartext traffic을 허용하지 않는다. 앱 코드에서 AI 원문, OAuth 정보, 여행 민감 텍스트를 `Log` 또는 `println`으로 출력하지 않는다.

- **안전 메모(Safety Hub)**: 사용자 연락값·메모는 Room 로컬에만 저장되며 debug log, analytics, AI prompt, 알림 본문, backup v3 외 어떤 경로로도 나가지 않는다. 연락값 탭은 클립보드 복사만 하고 전화·브라우저·지도 실행은 기존 확인 창 승인 후에만 한다. 정적 문제 대응 문구는 기관 번호·국가별 사실을 포함하지 않는다.

## 화면 캡처 정책

Device OAuth 화면은 TripPilot의 transient Device Code card와 사용자가 연 외부 브라우저다. token/password는 TripPilot 화면에 표시되지 않는다. 사용자가 자신의 여행 계획을 공유할 수 있도록 전체 앱 `FLAG_SECURE`는 적용하지 않으며, 실제 OAuth 승인 화면의 캡처 정책은 Android/browser 소유 설정을 따른다.
