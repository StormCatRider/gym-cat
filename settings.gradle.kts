// ╔══════════════════════════════════════════════════════════════════╗
// ║ PLAIN-ENGLISH GUIDE — settings.gradle.kts                         ║
// ║ Tells Gradle (the build tool) the project's name, where to         ║
// ║ download libraries from, and which "modules" (folders with code)   ║
// ║ belong to the project. Gym Cat has one module: "app".              ║
// ╚══════════════════════════════════════════════════════════════════╝
pluginManagement {
    repositories {          // where Gradle finds build plugins
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {          // where Gradle finds code libraries
        google()
        mavenCentral()
    }
}
rootProject.name = "GymCat"
include(":app")             // the only module
