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
    namespace = "com.xcore.floatball"
    compileSdk = 35

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    libraryVariants.all {
        outputs.forEach { output ->
            val buildType = this.buildType.name
            val fileName = "${project.name}-${buildType}-${getBuildTime()}.aar"
            output.outputFile.takeIf { it.isFile && it.exists() }?.run {
                renameTo(File(output.outputFile.parentFile, fileName))
                this.deleteOnExit()
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
}