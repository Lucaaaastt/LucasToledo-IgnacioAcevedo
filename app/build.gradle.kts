import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
}

val urlUsuarios = providers.gradleProperty("usuariosApiUrl")

fun urlComoLiteral(url: String): String {
    val uri = if (url.isEmpty()) null else URI(url)
    require(uri == null || (uri.host != null && uri.userInfo == null &&
        uri.query == null && uri.fragment == null && uri.scheme in listOf("http", "https"))) {
        "usuariosApiUrl debe ser una URL HTTP o HTTPS sin credenciales, query ni fragmento."
    }
    return "\"${url.trimEnd('/')}\""
}

android {
    namespace = "com.example.prueba2"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.prueba2"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "USUARIOS_API_URL",
                urlComoLiteral(urlUsuarios.orElse("http://10.0.2.2:8081").get()))
        }
        release {
            val url = urlUsuarios.orElse("").get()
            buildConfigField("String", "USUARIOS_API_URL",
                urlComoLiteral(if (url.startsWith("https://")) url else ""))
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    jvmToolchain(11)
}

// Obliga a Room a generar código en Kotlin nativo
ksp {
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation("androidx.compose.material:material-icons-extended")

    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")
}