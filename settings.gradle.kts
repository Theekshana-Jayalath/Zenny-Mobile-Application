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
        // THIS LINE IS REQUIRED to find the calendar library
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "ZENNY"
include(":app")
 