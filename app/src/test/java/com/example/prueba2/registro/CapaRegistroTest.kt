package com.example.prueba2.registro

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import com.example.prueba2.ui.registro.CapaRegistro
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
class CapaRegistroTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun registroUsuario_camposVacios_muestraErroresYToastDeValidacion() {
        ActivityScenario.launch(CapaRegistro::class.java).use {
            composeTestRule.onNodeWithText("REGISTRO").assertIsDisplayed()
            composeTestRule.onNodeWithText("Ingresar").performClick()

            composeTestRule.onNodeWithText("NOMBRE OBLIGATORIO!").assertIsDisplayed()
            composeTestRule.onNodeWithText("CORREO OBLIGATORIO!").assertIsDisplayed()
            composeTestRule.onNodeWithText("CONTRASEÑA OBLIGATORIA!").assertIsDisplayed()
            assertEquals("COMPLETE LOS CAMPOS", ShadowToast.getTextOfLatestToast())
        }
    }

    @Test
    fun registroUsuario_correoYPasswordInvalidos_muestraMensajesEspecificos() {
        ActivityScenario.launch(CapaRegistro::class.java).use {
            composeTestRule.onNodeWithText("Ingrese Nombre").performTextInput("Ignacio")
            composeTestRule.onNodeWithText("Ingrese Correo").performTextInput("ignacio@duoc.cl")
            composeTestRule.onNodeWithText("Contraseña").performTextInput("1234")

            composeTestRule.onNodeWithText("Ingresar").performClick()

            composeTestRule.onNodeWithText("DEBE SER @gmail.com").assertIsDisplayed()
            composeTestRule.onNodeWithText("DEBE POSEER AL MENOS 8 CARACTERES").assertIsDisplayed()
        }
    }

    @Test
    fun registroUsuario_botonVolver_cierraActividad() {
        ActivityScenario.launch(CapaRegistro::class.java).use { scenario ->
            composeTestRule.onNodeWithContentDescription("Volver atrás").performClick()
            scenario.onActivity { activity ->
                assertTrue(activity.isFinishing)
            }
        }
    }
}