import java.text.SimpleDateFormat
import java.util.Calendar

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

// 1. 定义版本号
fun getBuildTime(): String {
    val date = Calendar.getInstance().time
    val sdf = SimpleDateFormat("yyyyMMddHHmmss")
    return sdf.format(date)
}
android {
    namespace = "com.yunxiao.kits"
    compileSdk = 35

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            buildConfigField("boolean", "GLOBAL_LOG", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            buildConfigField("boolean", "GLOBAL_LOG", "true")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    sourceSets {
        getByName("debug") {
            res.srcDirs("src/debug/res")
            assets.srcDirs("src/debug/assets")
            java.srcDirs("src/debug/java")
        }
        getByName("release") {
            res.srcDirs("src/release/res")
            assets.srcDirs("src/release/assets")
            java.srcDirs("src/release/java")
        }
    }

    libraryVariants.all {
        outputs.forEach { output ->
            val buildType = this.buildType.name
            val fileName = "${project.name}-${buildType}-${getBuildTime()}.aar"
            output.outputFile.takeIf { it.isFile && it.exists() }?.run {
                renameTo(File(output.outputFile.parentFile, fileName))
            }
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    debugApi("com.facebook.stetho:stetho-okhttp3:1.6.0")
    debugApi("com.facebook.stetho:stetho:1.6.0")
    debugApi("com.facebook.stetho:stetho-js-rhino:1.6.0")

    debugApi("com.github.chuckerteam.chucker:library:4.1.0")
    releaseApi("com.github.chuckerteam.chucker:library-no-op:4.1.0")
    releaseImplementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.16")
    val lastversion = "3.7.1"
    //核心模块

    debugApi("io.github.didi.dokit:dokitx:${lastversion}") {
        exclude(group = "com.android.volley", module = "volley")
        exclude(group = "com.github.ybq", module = "Android-SpinKit")

    }
    debugApi("com.android.volley:volley:1.2.1")
    debugApi("com.github.ybq:Android-SpinKit:1.4.0")

}