// ╔══════════════════════════════════════════════════════════════════╗
// ║ PLAIN-ENGLISH GUIDE — build.gradle.kts (top level)                ║
// ║ Lists the build plugins used by the app module. "apply false"     ║
// ║ means "make it available, the app module turns it on".            ║
// ║ Versions live in gradle/libs.versions.toml (same as Gym & Ñam).   ║
// ╚══════════════════════════════════════════════════════════════════╝
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
