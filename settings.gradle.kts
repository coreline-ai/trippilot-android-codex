pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "TripPilot"
include(":app")
include(":baselineprofile")
// The Codex runtime is a pinned, source-only vendor slice. Its original UI/app executable is
// intentionally not part of this build; TripPilot consumes only fixed runtime packs and its own
// narrow adapter.
include(":alpine-runtime-api")
project(":alpine-runtime-api").projectDir = file("third_party/alpine-codex-cli-client/alpine-runtime-api")
include(":alpine-runtime-android")
project(":alpine-runtime-android").projectDir = file("third_party/alpine-codex-cli-client/alpine-runtime-android")
include(":alpine-runtime-host")
project(":alpine-runtime-host").projectDir = file("third_party/alpine-codex-cli-client/alpine-runtime-host")
include(":alpine-runtime-pack-bundled")
project(":alpine-runtime-pack-bundled").projectDir = file("third_party/alpine-codex-cli-client/alpine-runtime-pack-bundled")
include(":alpine-python-pack-bundled")
project(":alpine-python-pack-bundled").projectDir = file("third_party/alpine-codex-cli-client/alpine-python-pack-bundled")
include(":codex-cli-pack")
project(":codex-cli-pack").projectDir = file("third_party/alpine-codex-cli-client/codex-cli-pack")
include(":codex-gateway-pack-bundled")
project(":codex-gateway-pack-bundled").projectDir = file("third_party/alpine-codex-cli-client/codex-gateway-pack-bundled")
include(":codex-runtime-bridge")
project(":codex-runtime-bridge").projectDir = file("third_party/alpine-codex-cli-client/codex-runtime-bridge")
