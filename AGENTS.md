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

## Alpine Python 패키지 pack과 APK 빌드

`app`은 vendored `:alpine-python-pack-bundled`에 의존한다. 이 모듈은 네트워크에서 런타임 패키지를 내려받지 않고, 이미 검증된 로컬 Alpine Python package pack만 APK asset으로 묶는다. 따라서 pack 입력이 없거나 불완전하면 `assembleDebug`, `installDebug`, emulator 시각 QA, screenshot golden 검증은 **의도적으로 fail-closed** 된다.

### 현재 알려진 차단 상태

- 기본 입력 위치는 `third_party/alpine-codex-cli-client/alpine-python-pack-bundled/src/main/python-pack`이다.
- 현재 checkout에는 `packages/*.apk` 입력이 없어 `:alpine-python-pack-bundled:preparePythonPackagePackAssets`가 실패할 수 있다.
- 이 문제는 앱 GUI/Compose 코드 오류가 아니라 release/runtime 공급 입력 누락이다. 앱 설치 성공이나 emulator 시각 QA 완료로 보고하지 않는다.

### 복구 절차

1. release 담당자 또는 승인된 artifact 저장소에서 검증된 production pack을 받는다. pack 루트에는 정확히 다음 항목이 있어야 한다.

   ```text
   python-pack.lock.json
   sbom.spdx.json
   packages/*.apk
   ```

2. APK, lock, SBOM을 임의로 만들거나 수정하지 않는다. 테스트 fixture, 다른 CPU 아키텍처, hash/size가 lock과 다른 파일은 사용하지 않는다.
3. in-tree 기본 경로에 복사해 Git에 추가하지 않는다. 우선 절대 경로를 `ALPINE_PYTHON_PACKAGE_DIR`로 전달한다.

   ```bash
   export ALPINE_PYTHON_PACKAGE_DIR="/absolute/path/to/approved-python-pack"
   python3 third_party/alpine-codex-cli-client/scripts/verify-python-package-pack.py \
     --verify-source --source "$ALPINE_PYTHON_PACKAGE_DIR" --require-production
   ./gradlew :alpine-python-pack-bundled:verifyProductionPythonPackagePack :app:assembleDebug :app:installDebug --no-daemon
   ```

4. 위 명령이 성공한 뒤에만 emulator에서 `scripts/run_android_design_qa.sh`, `scripts/run_design_journey_capture.sh`, screenshot golden 검증을 실행한다. golden 또는 demo 이미지는 사용자 시각 승인 전에는 갱신하지 않는다.

### 입력을 아직 받지 못한 경우

- 정적 계약, Kotlin 컴파일, UI test source 컴파일 및 제한된 unit/lint 검증은 수행할 수 있다.
- 필요하면 아래처럼 package 준비 task를 명시적으로 제외할 수 있지만, 결과를 APK 패키징·설치·emulator QA 성공으로 해석하지 않는다.

  ```bash
  ./gradlew :app:testDebugUnitTest :app:lintDebug --no-daemon \
    -x :alpine-python-pack-bundled:preparePythonPackagePackAssets
  ```
- 입력 경로나 production artifact의 출처가 없으면, 누락 사실·필요한 입력·차단된 후속 작업을 보고하고 pack을 추정하거나 공급망 검증을 약화하지 않는다.
