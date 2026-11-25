import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
}

val urlUsuarios = providers.gradleProperty("usuariosApiUrl")
val urlEspacios = providers.gradleProperty("espaciosApiUrl")
val usuariosRelease = urlUsuarios.orElse("https://marcelapaz-usuarios.onrender.com")
val espaciosRelease = urlEspacios.orElse("https://marcelapaz-reservas.onrender.com")

fun urlComoLiteral(url: String): String {
    val uri = if (url.isEmpty()) null else URI(url)
    require(uri == null || (uri.host != null && uri.userInfo == null &&
        uri.query == null && uri.fragment == null && uri.scheme in listOf("http", "https"))) {
        "La URL del servicio debe ser HTTP o HTTPS sin credenciales, query ni fragmento."
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
            buildConfigField("String", "ESPACIOS_API_URL",
                urlComoLiteral(urlEspacios.orElse("http://10.0.2.2:8080").get()))
        }
        release {
            val url = usuariosRelease.get()
            buildConfigField("String", "USUARIOS_API_URL",
                urlComoLiteral(if (url.startsWith("https://")) url else ""))
            val espacios = espaciosRelease.get()
            buildConfigField("String", "ESPACIOS_API_URL",
                urlComoLiteral(if (espacios.startsWith("https://")) espacios else ""))
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
    // Necesario para que Robolectric acceda a los recursos y contexto de Android en tests locales
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

kotlin {
    jvmToolchain(11)
}

// Evita entregar un instalador release sin conexión a los servicios.
tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    inputs.property("usuariosApiUrl", usuariosRelease)
    inputs.property("espaciosApiUrl", espaciosRelease)
    doFirst {
        for (propiedad in listOf("usuariosApiUrl", "espaciosApiUrl")) {
            val url = inputs.properties[propiedad] as String
            require(url.startsWith("https://")) {
                "Falta configurar $propiedad con la URL HTTPS del backend antes de generar release."
            }
        }
    }
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
    implementation("androidx.compose.material:material-icons-extended")

    // Pruebas unitarias locales (app/src/test)
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)

    // Pruebas instrumentadas (app/src/androidTest)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")
}
