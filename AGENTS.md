# TripPilot 작업 지침

## 개발 계획 작성

- 일반 개발 계획과 서로 강하게 결합된 작업은 기본 `dev-plan-generator` 절차로 작성한다.
- 사용자가 **`병렬개발계획`**, **`병렬 개발 계획`**, 또는 `$parallel-dev-plan-orchestrator`를 명시한 경우에만 프로젝트 스킬 `dev-plan-v2`를 사용한다.
- 스킬 정본: [`.grok/skills/dev-plan-v2/SKILL.md`](.grok/skills/dev-plan-v2/SKILL.md)
- 도구 디렉터리: `.grok/skills/dev-plan-v2/scripts/`
- 진입 시 먼저 `python3.11 .grok/skills/dev-plan-v2/scripts/assess_parallelism.py`로 병렬성 평가(ASSESS)를 수행한다.
- 평가가 `SERIAL_RECOMMENDED`이면 병렬 산출물을 만들지 않고 일반 개발 계획으로 전환한다.
- `COMMON_FIRST` 또는 `PARALLEL_SAFE`일 때만 `dev-plan/parallel/`에 JSON 정본과 Markdown 계획을 함께 만든다.
- 병렬 실행 전에는 clean Git baseline, 선언된 write path, 테스트 명령 및 scope 검증을 반드시 확인한다. 사용자 변경을 임의로 stash, commit 또는 삭제하지 않는다.
- 기존 계획 문서는 이력으로 보존하며, 이 지침은 다음 계획부터 적용한다.
