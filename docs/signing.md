# Release Signing 준비 — 가이드 (초안)

상태: **준비 문서**. 실제 키 생성·적용은 배포 owner 결정 사항이다. 저장소에 키·비밀번호를 커밋하지 않는다(`.gitignore`의 `*.jks`가 이미 차단).

## 1. 키 생성

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  scripts/generate_release_keystore.sh            # 기본: ~/.android/trippilot-release.jks
```

- RSA 4096, validity 10000일 (Play 권장 2033-10-01 이후 만료)
- 키 분실 시 동일 앱 업데이트 불가 — **백업과 비밀번호 보관 정책을 먼저 정한다** (Play App Signing 사용 시 업로드 키 재발급 가능)

## 2. signingConfigs 템플릿 (app/build.gradle.kts에 적용)

```kotlin
signingConfigs {
    create("release") {
        storeFile = System.getenv("TRIPPPILOT_KEYSTORE")?.let { file(it) }
        storePassword = System.getenv("TRIPPPILOT_KEYSTORE_PASSWORD")
        keyAlias = System.getenv("TRIPPPILOT_KEY_ALIAS")
        keyPassword = System.getenv("TRIPPPILOT_KEY_PASSWORD")
    }
}
buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release") // env 없으면 null → 기존 unsigned 동작
    }
}
```

환경변수가 없으면 서명이 건너뛰어지도록 null 처리를 유지해 CI·로컬 빌드를 깨뜨리지 않는다.

## 3. 빌드·검증

```bash
export TRIPPPILOT_KEYSTORE=~/.android/trippilot-release.jks
export TRIPPPILOT_KEYSTORE_PASSWORD=...
export TRIPPPILOT_KEY_ALIAS=trippilot
export TRIPPPILOT_KEY_PASSWORD=...
./gradlew :app:bundleRelease          # AAB (Play 업로드)
./gradlew :app:assembleRelease        # APK (직접 배포 시)
# 서명 확인
"$ANDROID_HOME/build-tools/<ver>/apksigner" verify --print-certs app/build/outputs/apk/release/app-release.apk
```

## 4. 체크리스트

- [ ] Play App Signing enrolled 여부 결정 (권장: 업로드 키 재발급 가능)
- [ ] 키 백업·비밀번호 보관 방침 확정 (암호 매니저/조직 금고)
- [ ] signingConfigs 적용 + env 미설정 시 unsigned 동작 회귀 테스트
- [ ] 서명 artifact에서 baseline profile 적용 재확인 (D8 warning, `verify_phase5_release.py`)
- [ ] 서명 후 GPL source-offer 링크·SBOM을 동일 태그로 고정
