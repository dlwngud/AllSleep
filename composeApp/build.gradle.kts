import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val admobPropertiesFile = rootProject.file("admob.properties")
val admobProperties = Properties().apply {
    if (admobPropertiesFile.exists()) {
        load(admobPropertiesFile.inputStream())
    }
}

fun admobProp(name: String): String = admobProperties.getProperty(name)?.trim().orEmpty()

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("com.google.gms.google-services")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.datastore.preferences)
            implementation(libs.androidx.lifecycle.process)
            // 카카오 SDK (Android 전용)
            implementation("com.kakao.sdk:v2-user:2.20.6")

            // RevenueCat
            implementation(libs.revenuecat.purchases)
            implementation(libs.revenuecat.ui)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.navigation.compose)
            
            // Koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            
            // DataStore
            implementation(libs.datastore.preferences.core)
            
            // Icons
            implementation(libs.compose.icons.extended)
            
            // DateTime
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

configurations.all {
    exclude(group = "com.google.guava", module = "listenablefuture")
    resolutionStrategy {
        force("com.google.guava:guava:32.1.3-android")
    }
}

android {
    namespace = "com.wngud.allsleep"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.wngud.allsleep"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 5
        versionName = "1.4"
        // 카카오 로그인 콜백 scheme 주입 (kakao + 네이티브앱키)
        manifestPlaceholders["kakaoScheme"] = "kakaoc8924c995fe54b4b67404bb682347b95"

        // RevenueCat API Key 주입
        val props = Properties().apply {
            val localPropertiesFile = rootProject.file("local.properties")
            if (localPropertiesFile.exists()) {
                load(localPropertiesFile.inputStream())
            }
        }
        val rcApiKey = props.getProperty("revenueCat.apiKey") ?: ""
        val rcTestApiKey = props.getProperty("revenueCat.testApiKey") ?: ""
        buildConfigField("String", "REVENUECAT_API_KEY", "\"$rcApiKey\"")
        buildConfigField("String", "REVENUECAT_TEST_API_KEY", "\"$rcTestApiKey\"")
    }
    buildFeatures {
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    // 서명 설정 생성
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    if (keystorePropertiesFile.exists()) {
        val props = Properties().apply {
            load(keystorePropertiesFile.inputStream())
        }
        signingConfigs {
            create("releaseConfig") {
                storeFile = rootProject.file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        getByName("debug") {
            // 디버그 모드에서도 정식 서명을 적용하여 결제 테스트 가능하게 설정
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("releaseConfig")
            }
            val admobAppId = admobProp("admob.appId.debug")
            val appOpenAdUnitId = admobProp("admob.appOpenAdUnitId.debug")
            require(admobAppId.isNotBlank()) { "admob.appId.debug is missing in admob.properties" }
            require(appOpenAdUnitId.isNotBlank()) { "admob.appOpenAdUnitId.debug is missing in admob.properties" }
            manifestPlaceholders["admobAppId"] = admobAppId
            buildConfigField("String", "ADMOB_APP_OPEN_AD_UNIT_ID", "\"$appOpenAdUnitId\"")
        }
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("releaseConfig")
            }
            val admobAppId = admobProp("admob.appId.release")
            val appOpenAdUnitId = admobProp("admob.appOpenAdUnitId.release")
            require(admobAppId.isNotBlank()) { "admob.appId.release is missing in admob.properties" }
            require(appOpenAdUnitId.isNotBlank()) { "admob.appOpenAdUnitId.release is missing in admob.properties" }
            manifestPlaceholders["admobAppId"] = admobAppId
            buildConfigField("String", "ADMOB_APP_OPEN_AD_UNIT_ID", "\"$appOpenAdUnitId\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)

    implementation(platform("com.google.firebase:firebase-bom:34.9.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-messaging")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
    implementation("com.google.firebase:firebase-functions")  // Cloud Functions 클라이언트
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    
    implementation("com.google.android.gms:play-services-ads:23.0.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    
    // Koin
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
}
