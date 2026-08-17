# Alpine Codex runtime Spike — Phase 1 history and Phase 4 decision

## 대상과 재현 기준

- 참조 저장소: `https://github.com/coreline-ai/alpine-codex-cli-client`
- 조사 commit: `c8c7ca9eade2992f40ccc6b3aecf6b9a04f04b35`
- 조사 시각: 2026-08-16
- Phase 1 당시 TripPilot 소스/Gradle 의존성: **없음**

## 확인한 공개 경계 후보

| 후보 | 관찰 | TripPilot Phase 4 적합성 |
|---|---|---|
| `:codex-runtime-bridge` | Codex gateway client·runtime controller를 제공하지만 `AgentId`에 `GROK`도 포함하고 conversation/agent 범위가 넓음 | 부적합 — 그대로 의존하지 않음 |
| `:alpine-runtime-*` | Android runtime, PRoot, pack, host 책임으로 분리됨 | 단독으로는 Codex 로그인·draft stream 제한 계약을 제공하지 않음 |
| 전체 `:app` | Codex/Grok 선택, 일반 Agent UI, runtime orchestration을 포함 | 금지 — TripPilot 의존성으로 넣지 않음 |

## Phase 1 결론 (이력)

Phase 1 시점에는 TripPilot 요구에 맞는 **Codex-only, credential-inaccessible, structured-draft-only** 공개 adapter가 확인되지 않았다. 따라서 앱은 `FakeCodexRuntime`으로 Phase 2/3을 진행한다.

Phase 4 시작 전에는 아래 중 하나가 필요하다.

1. upstream에 Codex-only restricted façade가 추가되어 exact commit과 API contract를 고정할 수 있음.
2. Alpine 프로젝트가 Grok·generic chat·terminal 없이 `runtimeStatus/authStatus/login/models/structured stream/stop/logout`만 제공하는 별도 공개 module을 제공함.

둘 다 없으면 custom OAuth, credential file access, raw CLI invocation으로 우회하지 않는다. 이 경우 Phase 4는 차단으로 기록하고 local-first MVP와 분리한다.

## Phase 4 결정 (2026-08-16)

사용자의 명시적 요청에 따라 public bridge를 앱 기능 경계로 노출하지 않고, 정확히 고정한
소스에서 runtime에 필요한 최소 모듈만 vendor slice로 가져왔다. TripPilot adapter는 bridge의
broad API를 그대로 공개하지 않으며 다음을 강제한다.

1. `AgentId.CODEX`만 선택하며 `grok-cli-pack` executable, Grok UI, fallback은 포함하지 않는다.
2. official CLI가 Device OAuth, credential file 및 refresh 수명주기를 앱-private no-backup guest
   `HOME`에서 소유한다. TripPilot은 파일 내용을 읽지 않는다.
3. app-private Unix domain socket의 HMAC-authenticated client carrier만 사용한다. custom OAuth,
   raw CLI command, TCP localhost server는 만들지 않는다.
4. conversation resume/history를 꺼 두고, raw response는 process memory에서 parser 검증 뒤
   즉시 clear한다. `CodexRuntimePort` 밖에는 구조화 draft만 전달한다.

이 결정은 shared Gateway source에 dormant multi-agent code가 남아 있다는 공급망 위험을
없애지는 않는다. 그래서 public release 전 Codex-only upstream facade 또는 동등한 source
review가 남아 있으며, 실제 OAuth 성공/취소/복귀는 사용자가 자신의 계정으로 별도 검증해야 한다.

## 확인한 보안 특성

참조 README와 source 구조는 CLI-owned Device OAuth, app-private runtime, private UDS, credential file 미접근을 표방한다. Phase 4 vendor adapter는 해당 경계를 좁혀 적용했지만, 실제 연동 승인은 API surface·license·packaging·arm64 device verification을 다시 통과한 뒤에만 가능하다.
