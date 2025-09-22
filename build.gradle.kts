// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("com.google.dagger.hilt.android") version libs.versions.hilt apply false
    alias(libs.plugins.android.library) apply false
}
buildscript {
    buildscript {
        repositories {
            mavenCentral()
        }

        dependencies {
            classpath("com.alibaba:arouter-register:1.0.2")
        }
    }
}