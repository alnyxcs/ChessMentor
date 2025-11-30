// Top-level build file
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false // Теперь это сработает, т.к. Kotlin 2.0
    alias(libs.plugins.ksp) apply false
}