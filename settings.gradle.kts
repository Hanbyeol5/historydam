pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // 네이버 지도 SDK
        maven("https://repository.map.naver.com/archive/maven")
        // SceneView / 기타
        maven("https://jitpack.io")
    }
}

rootProject.name = "yeoksadam"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")

// core
include(":core:designsystem")
include(":core:ui")
include(":core:common")
include(":core:network")
include(":core:database")
include(":core:datastore")
include(":core:domain")
include(":core:data")

// feature
include(":feature:home")
include(":feature:figure")
include(":feature:map")
include(":feature:ar")
include(":feature:camera")
include(":feature:chat")
include(":feature:voice")
include(":feature:profile")
include(":feature:notification")
