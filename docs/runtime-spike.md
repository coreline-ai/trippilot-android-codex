# Alpine Codex runtime Spike — Phase 1

## 대상과 재현 기준

- 참조 저장소: `https://github.com/coreline-ai/alpine-codex-cli-client`
- 조사 commit: `c8c7ca9eade2992f40ccc6b3aecf6b9a04f04b35`
- 조사 시각: 2026-08-16
- TripPilot 소스/Gradle 의존성: **없음**

## 확인한 공개 경계 후보

| 후보 | 관찰 | TripPilot Phase 4 적합성 |
|---|---|---|
| `:codex-runtime-bridge` | Codex gateway client·runtime controller를 제공하지만 `AgentId`에 `GROK`도 포함하고 conversation/agent 범위가 넓음 | 부적합 — 그대로 의존하지 않음 |
| `:alpine-runtime-*` | Android runtime, PRoot, pack, host 책임으로 분리됨 | 단독으로는 Codex 로그인·draft stream 제한 계약을 제공하지 않음 |
| 전체 `:app` | Codex/Grok 선택, 일반 Agent UI, runtime orchestration을 포함 | 금지 — TripPilot 의존성으로 넣지 않음 |

## 결론

Phase 1 시점에는 TripPilot 요구에 맞는 **Codex-only, credential-inaccessible, structured-draft-only** 공개 adapter가 확인되지 않았다. 따라서 앱은 `FakeCodexRuntime`으로 Phase 2/3을 진행한다.

Phase 4 시작 전에는 아래 중 하나가 필요하다.

1. upstream에 Codex-only restricted façade가 추가되어 exact commit과 API contract를 고정할 수 있음.
2. Alpine 프로젝트가 Grok·generic chat·terminal 없이 `runtimeStatus/authStatus/login/models/structured stream/stop/logout`만 제공하는 별도 공개 module을 제공함.

둘 다 없으면 custom OAuth, credential file access, raw CLI invocation으로 우회하지 않는다. 이 경우 Phase 4는 차단으로 기록하고 local-first MVP와 분리한다.

## 확인한 보안 특성

참조 README와 source 구조는 CLI-owned Device OAuth, app-private runtime, private UDS, credential file 미접근을 표방한다. 이 특성은 TripPilot의 방향과 일치하지만, broad multi-agent bridge를 앱에 포함하는 것은 TripPilot의 단일 Codex scope와 맞지 않는다. 실제 연동 승인은 API surface·license·packaging·arm64 device verification을 다시 통과한 뒤에만 가능하다.
