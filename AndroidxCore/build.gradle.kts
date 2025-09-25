plugins {
//    id("com.alibaba.arouter")
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
}

val appVersionName = "1.0.6"

// 根据版本名称生成版本代码
// 例如: "1.2.3" -> 10203, "2.0.0" -> 20000
fun getVersionCodeByName(versionName: String): Int {
    val parts = versionName.split(".")
    return if (parts.size >= 3) {
        val major = parts[0].toIntOrNull() ?: 0
        val minor = parts[1].toIntOrNull() ?: 0
        val patch = parts[2].toIntOrNull() ?: 0
        major * 10000 + minor * 100 + patch
    } else {
        1 // 默认版本代码
    }
}
android {
    signingConfigs {
        create("product") {
            enableV1Signing = true
            enableV2Signing = true
        }
    }
    namespace = "com.haofenshu.AndroidxCore"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.haofenshu.AndroidxCore"
        minSdk = 28
        targetSdk = 34
        versionCode = getVersionCodeByName(appVersionName)
        versionName = appVersionName
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        signingConfig = signingConfigs.getByName("product")
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            buildConfigField("boolean", "GLOBAL_LOG", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("product")
            ndk {
                abiFilters.addAll(arrayListOf<String>("armeabi-v7a", "arm64-v8a"))
            }

        }
        debug {
            signingConfig = signingConfigs.getByName("debug")
//            applicationIdSuffix = ".debug"
            buildConfigField("boolean", "GLOBAL_LOG", "true")

            ndk {
                abiFilters.addAll(arrayListOf<String>("armeabi-v7a", "arm64-v8a"))
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.hilt.android)
    implementation(project(":floatball"))
    implementation(project(":kits"))
    implementation(project(":kiosk"))
    kapt(libs.hilt.android.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
//    kapt("com.alibaba:arouter-compiler:1.5.2")
    implementation("com.github.cymchad:BaseRecyclerViewAdapterHelper:2.9.47-androidx")
    implementation("io.github.scwang90:refresh-layout-kernel:3.0.0-alpha")      //核心必须依赖

    implementation("io.github.scwang90:refresh-header-classics:3.0.0-alpha")    //经典刷新头
}