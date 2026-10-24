package com.example.prueba2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.prueba2.ui.reservas.MisReservasPantalla
import com.example.prueba2.ui.reservas.reservasDeEjemplo
import com.example.prueba2.ui.theme.Prueba2Theme

class MisReservas : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Prueba2Theme {
                MisReservasPantalla(
                    reservas = reservasDeEjemplo,
                    mostrarAvisoDeEjemplo = true,
                    onVolver = { finish() }
                )
            }
        }
    }
}
