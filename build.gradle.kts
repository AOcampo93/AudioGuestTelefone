// Top-level build file. AGP 9 compiles Kotlin itself (built-in Kotlin),
// so the org.jetbrains.kotlin.android plugin is not needed.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}
