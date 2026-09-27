plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.kotlin.compose)
}

kotlin {
  jvmToolchain(17)

  jvm()

  sourceSets {
    val jvmMain by getting {
      dependencies {
        implementation(project(":shared"))
        implementation(compose.desktop.currentOs)
        implementation(compose.material3)
        implementation(libs.kotlinx.coroutines.swing)
        implementation(libs.androidx.room.runtime)
        implementation(libs.androidx.sqlite.bundled)
        implementation(libs.kotlinx.serialization.json)
      }
    }
    val jvmTest by getting {
      dependencies {
        implementation(kotlin("test"))
        implementation(libs.kotlinx.coroutines.test)
      }
    }
  }
}

compose.desktop {
  application {
    mainClass = "com.example.desktop.MainKt"

    nativeDistributions {
      packageName = "TypeArticle"
      packageVersion = "1.0.0"
      description = "文章跟打与单词默写（桌面版）"
      vendor = "TypeArticle"
      modules(
        "java.instrument",
        "java.sql",
        "java.naming",
        "java.management",
        "java.desktop",
        "java.net.http",
        "jdk.unsupported",
      )
      windows {
        menuGroup = "TypeArticle"
        shortcut = true
        dirChooser = true
        iconFile.set(project.file("icons/app.ico"))
        upgradeUuid = "b7f1c2a4-3d5e-4f6a-9c8b-1e2d3f4a5b60"
      }
    }
  }
}
