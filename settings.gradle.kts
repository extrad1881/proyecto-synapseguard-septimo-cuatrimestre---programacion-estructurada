pluginManagement {
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
    }
}

rootProject.name = "SynapseGuard"

include(":app")
include(":core-model")
include(":core-database")
include(":core-math-ml")
include(":data-ingestors:biometric-ble-hc")
include(":data-ingestors:biometric-camera-ppg")
include(":data-ingestors:telemetry-usage")
include(":data-ingestors:kinematics-accessibility")
include(":data-ingestors:context-sensors")
include(":domain-orchestrator")
include(":feature-jitai")
include(":feature-dashboard")
include(":feature-local-sovereignty")

