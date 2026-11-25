package com.example.prueba2.login

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import com.example.prueba2.ui.login.MainActivity
import com.example.prueba2.ui.registro.CapaRegistro
import com.example.prueba2.ui.registro.CapaRegistroAdm
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w411dp-h915dp-xxhdpi")
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun loginPantalla_conCamposVacios_muestraErroresObligatorios() {
        ActivityScenario.launch(MainActivity::class.java).use {
            composeTestRule.onNodeWithText("MARCELA PAZ KT").assertIsDisplayed()
            composeTestRule.onNodeWithText("GESTIÓN DE RESERVAS").assertIsDisplayed()

            composeTestRule.onNodeWithText("Ingresar").performClick()

            composeTestRule.onNodeWithText("CORREO ES OBLIGATORIO!").assertIsDisplayed()
            composeTestRule.onNodeWithText("CONTRASEÑA ES OBLIGATORIA!").assertIsDisplayed()
        }
    }

    @Test
    fun loginPantalla_navegaARegistroUsuarioYRegistroAdministrador() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            composeTestRule.onNodeWithText("Ingrese aquí").performClick()
            scenario.onActivity { activity ->
                val siguiente = shadowOf(activity).nextStartedActivity
                assertEquals(CapaRegistro::class.java.name, siguiente.component?.className)
            }

            composeTestRule.onNodeWithContentDescription("Acceso Administración").performClick()
            scenario.onActivity { activity ->
                val siguiente = shadowOf(activity).nextStartedActivity
                assertEquals(CapaRegistroAdm::class.java.name, siguiente.component?.className)
            }
        }
    }
}