import org.gradle.api.initialization.resolve.RepositoriesMode

pluginManagement {
    includeBuild("build-logic")

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

rootProject.name = "SpatialLink"

include(":app")
include(":core:common")
include(":core:designsystem")
include(":core:discovery")
include(":core:handshake")
include(":core:model")
include(":core:capabilities")
include(":core:identity")
include(":connectivity:ble")
include(":connectivity:handshake")
include(":feature:diagnostics")
include(":feature:nearby")
include(":testing")
