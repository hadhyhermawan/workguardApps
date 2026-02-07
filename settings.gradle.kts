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

rootProject.name = "WorkGuard"

include(
    ":app",
    ":core",
    ":auth",
    ":home",
    ":attendance",
    ":face",
    ":task",
    ":patrol",
    ":tracking",
    ":payroll",
    ":profile",
    ":navigation",
    ":notification"
)
