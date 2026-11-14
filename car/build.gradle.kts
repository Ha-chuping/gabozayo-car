import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.gabozayo.car"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.gabozayo.car"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
        buildConfigField("String", "FIREBASE_DB_URL", "\"${localProps.getProperty("FIREBASE_DB_URL", "")}\"")
        // Pleos Playground > My Project 의 SDK 자격 증명 (TextToSpeech / SpeechToText 서버 모드용, 비워 두면 On-device 로 동작해요)
        buildConfigField("String", "PLEOS_CLIENT_ID", "\"${localProps.getProperty("PLEOS_CLIENT_ID", "")}\"")
        buildConfigField("String", "PLEOS_CLIENT_SECRET", "\"${localProps.getProperty("PLEOS_CLIENT_SECRET", "")}\"")
    }
    // 제출용 release: 별도 키스토어 없이 디버그 키로 서명해서 바로 설치할 수 있어요
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
}
dependencies {
    val bom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(bom)
    implementation(project(":core"))
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("io.coil-kt:coil-compose:2.7.0")   // 장소 사진

    // ── Pleos Playground SDK (저장소: settings.gradle.kts) ──
    implementation("ai.pleos.playground:NaviHelper:3.0.5")       // 길 안내 연동
    implementation("ai.pleos.playground:Vehicle:2.3.2")           // 차량 상태
    implementation("ai.pleos.playground:FusedLocation:1.1.3")     // 위치 정보
    implementation("ai.pleos.playground:TextToSpeech:3.3.1")      // 음성 출력
    implementation("ai.pleos.playground:SpeechToText:3.3.1")      // 음성 입력
}
