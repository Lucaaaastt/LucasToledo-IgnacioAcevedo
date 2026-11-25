package com.example.prueba2.menus

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.prueba2.ui.login.MainActivity
import com.example.prueba2.ui.menu.MenuADM
import com.example.prueba2.ui.reservas.MisReservas
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
class MenuAdmTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun menuAdmin_muestraSaludoYAbreRevisarSolicitudesConRolAdmin() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, MenuADM::class.java).apply {
            putExtra("USER_ID", 1)
            putExtra("USER_NOMBRE", "Lucas")
        }

        ActivityScenario.launch<MenuADM>(intent).use { scenario ->
            composeTestRule.onNodeWithText("Marcela Paz").assertIsDisplayed()
            composeTestRule.onNodeWithText("Hola, Lucas").assertIsDisplayed()

            composeTestRule.onNodeWithText("Revisar Solicitudes").performClick()
            scenario.onActivity { activity ->
                val siguiente = shadowOf(activity).nextStartedActivity
                assertEquals(MisReservas::class.java.name, siguiente.component?.className)
                assertEquals(1, siguiente.getIntExtra("USER_ID", -1))
                assertEquals("ADMIN", siguiente.getStringExtra("USER_ROL"))
            }
        }
    }

    @Test
    fun menuAdmin_sinNombreUsaAdminYCierraSesionAlConfirmar() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, MenuADM::class.java).apply {
            putExtra("USER_ID", 1)
        }

        ActivityScenario.launch<MenuADM>(intent).use { scenario ->
            composeTestRule.onNodeWithText("Hola, ADMIN").assertIsDisplayed()

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