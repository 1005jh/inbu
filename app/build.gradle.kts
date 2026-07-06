import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}
val kakaoNativeAppKey = localProperties.getProperty("KAKAO_NATIVE_APP_KEY", "")
val uiReviewMode = localProperties.getProperty("UI_REVIEW_MODE", "false").toBoolean()
val apiBaseUrl = localProperties.getProperty("API_BASE_URL", "http://10.0.2.2:8080")
val apiDevAuth = localProperties.getProperty("API_DEV_AUTH", "false").toBoolean()

android {
    namespace = "com.inbu.ledger"
    compileSdk = 37

    flavorDimensions += "storage"

    defaultConfig {
        applicationId = "com.inbu.ledger"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "0.1.1"

        manifestPlaceholders["KAKAO_NATIVE_APP_KEY"] =
            kakaoNativeAppKey.ifBlank { "not_configured" }
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$kakaoNativeAppKey\"")
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("boolean", "API_DEV_AUTH", apiDevAuth.toString())
        buildConfigField("boolean", "UI_REVIEW_MODE", uiReviewMode.toString())
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    productFlavors {
        create("online") {
            dimension = "storage"
            buildConfigField("boolean", "OFFLINE_MODE", "false")
        }
        create("offline") {
            dimension = "storage"
            applicationIdSuffix = ".offline"
            versionNameSuffix = "-offline"
            buildConfigField("boolean", "OFFLINE_MODE", "true")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    "onlineImplementation"("com.kakao.sdk:v2-user:2.24.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}
