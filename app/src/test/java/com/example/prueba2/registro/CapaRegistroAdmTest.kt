package com.example.prueba2.registro

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import com.example.prueba2.ui.registro.CapaRegistroAdm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w411dp-h915dp-xxhdpi")
class CapaRegistroAdmTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun registroAdmin_camposVacios_validaTodosLosCamposYCodigoSeguridad() {
        ActivityScenario.launch(CapaRegistroAdm::class.java).use {
            composeTestRule.onNodeWithText("Registro Administracion").assertIsDisplayed()
            composeTestRule.onNodeWithText("Ingresar").performScrollTo().performClick()

            composeTestRule.onNodeWithText("NOMBRE OBLIGATORIO!").assertIsDisplayed()
            composeTestRule.onNodeWithText("CORREO OBLIGATORIO!").assertIsDisplayed()
            composeTestRule.onNodeWithText("CONTRASEÑA OBLIGATORIA!").assertIsDisplayed()
            composeTestRule.onNodeWithText("CÓDIGO OBLIGATORIO!").assertIsDisplayed()
            assertEquals("COMPLETE LOS CAMPOS", ShadowToast.getTextOfLatestToast())
        }
    }

    @Test
    fun registroAdmin_correoCortoYPasswordInvalida_muestraMensajesDeError() {
        ActivityScenario.launch(CapaRegistroAdm::class.java).use {
            composeTestRule.onNodeWithText("Ingrese Nombre").performTextInput("Lucas Admin")
            composeTestRule.onNodeWithText("Ingrese Correo").performTextInput("abc@gmail.com")
            composeTestRule.onNodeWithText("Contraseña").performTextInput("   ")
            composeTestRule.onNodeWithText("Codigo Seguridad").performTextInput("ADMIN-2026")

            composeTestRule.onNodeWithText("Ingresar").performScrollTo().performClick()

            composeTestRule.onNodeWithText("CORREO DEMASIADO CORTO").assertIsDisplayed()
            composeTestRule.onNodeWithText("LA CONTRASEÑA NO PUEDE CONTENER SOLO ESPACIOS").assertIsDisplayed()
        }
    }

    @Test
    fun registroAdmin_botonVolver_cierraActividad() {
        ActivityScenario.launch(CapaRegistroAdm::class.java).use { scenario ->
            composeTestRule.onNodeWithContentDescription("Volver atrás").performClick()
            scenario.onActivity { activity ->
                assertTrue(activity.isFinishing)
            }
        }
    }
}