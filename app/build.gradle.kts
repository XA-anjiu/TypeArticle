import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
}

// 本地私密配置（local.properties 已在 .gitignore，不入库）：小米 MiMo TTS 的 API Key / 音色
val localProps = Properties().apply {
  val f = rootProject.file("local.properties")
  if (f.exists()) f.inputStream().use { load(it) }
}
val mimoApiKey: String = localProps.getProperty("MIMO_API_KEY")
  ?: System.getenv("MIMO_API_KEY") ?: ""
val mimoTtsVoice: String = localProps.getProperty("MIMO_TTS_VOICE")
  ?: System.getenv("MIMO_TTS_VOICE") ?: "Mia"

android {
  namespace = "com.example"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.aistudio.typingarticle.hwmpkb"
    minSdk = 26
    targetSdk = 34
    versionCode = 1
    versionName = "1.0"

    buildConfigField("String", "MIMO_API_KEY", "\"$mimoApiKey\"")
    buildConfigField("String", "MIMO_TTS_VOICE", "\"$mimoTtsVoice\"")

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  // release 签名：仅当提供 keystore 时才启用；debug 使用 AGP 默认调试签名，无需额外文件。
  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      val keystoreFile = file(keystorePath)
      if (keystoreFile.exists()) {
        storeFile = keystoreFile
        storePassword = System.getenv("STORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
        keyPassword = System.getenv("KEY_PASSWORD")
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      val releaseSigning = signingConfigs.getByName("release")
      if (releaseSigning.storeFile != null) {
        signingConfig = releaseSigning
      }
    }
    debug {
      // 不设置自定义 debug.keystore，交由 AGP 自动生成 ~/.android/debug.keystore
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// 纯本地：无 GMS / Firebase / Google 服务依赖
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material3.window.size)
  implementation(libs.androidx.compose.material3.adaptive)
  implementation(libs.androidx.compose.material3.adaptive.layout)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)

  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.turbine)
  testImplementation(libs.robolectric)

  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)

  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)

  "ksp"(libs.androidx.room.compiler)
}
