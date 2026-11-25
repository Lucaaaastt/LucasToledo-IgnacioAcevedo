package com.example.prueba2.menus

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.prueba2.ui.login.MainActivity
import com.example.prueba2.ui.menu.Menu
import com.example.prueba2.ui.reservas.MisReservas
import com.example.prueba2.ui.reservas.NuevaReserva
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MenuTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun menuUsuario_muestraSaludoYNavegaANuevaYMisReservasConUserId() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, Menu::class.java).apply {
            putExtra("USER_ID", 7)
            putExtra("USER_NOMBRE", "Ignacio")
        }

        ActivityScenario.launch<Menu>(intent).use { scenario ->
            composeTestRule.onNodeWithText("Marcela Paz").assertIsDisplayed()
            composeTestRule.onNodeWithText("Hola, Ignacio").assertIsDisplayed()

            composeTestRule.onNodeWithText("Nueva reserva").performClick()
            scenario.onActivity { activity ->
                val siguiente = shadowOf(activity).nextStartedActivity
                assertEquals(NuevaReserva::class.java.name, siguiente.component?.className)
                assertEquals(7, siguiente.getIntExtra("USER_ID", -1))
            }

            composeTestRule.onNodeWithText("Mis reservas").performClick()
            scenario.onActivity { activity ->
                val siguiente = shadowOf(activity).nextStartedActivity
                assertEquals(MisReservas::class.java.name, siguiente.component?.className)
                assertEquals(7, siguiente.getIntExtra("USER_ID", -1))
            }
        }
    }

    @Test
    fun menuUsuario_sinNombreUsaSolicitanteYCierraSesionAlConfirmar() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, Menu::class.java).apply {
            putExtra("USER_ID", 3)
            putExtra("USER_NOMBRE", "   ")
        }

        ActivityScenario.launch<Menu>(intent).use { scenario ->
            composeTestRule.onNodeWithText("Hola, Solicitante").assertIsDisplayed()

            composeTestRule.onNodeWithContentDescription("Menú de usuario").performClick()
            composeTestRule.onNodeWithText("Cerrar sesión").performClick()
            composeTestRule.onNodeWithText("¿Estás seguro de que deseas salir?").assertIsDisplayed()
            composeTestRule.onNodeWithText("Aceptar").performClick()

            scenario.onActivity { activity ->
                val siguiente = shadowOf(activity).nextStartedActivity
                assertEquals(MainActivity::class.java.name, siguiente.component?.className)
                assertTrue(activity.isFinishing)
            }
        }
    }
}