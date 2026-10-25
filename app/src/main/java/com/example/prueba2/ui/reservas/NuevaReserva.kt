package com.example.prueba2.ui.reservas

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.prueba2.data.local.AppDatabase
import com.example.prueba2.data.repository.ReservaRepository
import com.example.prueba2.ui.theme.Prueba2Theme

class NuevaReserva : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val usuarioId = intent.getIntExtra("USER_ID", -1)
        if (usuarioId <= 0) {
            Toast.makeText(this, "Vuelve a iniciar sesión para enviar una solicitud.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val repository = ReservaRepository(AppDatabase.getDatabase(applicationContext))
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(NuevaReservaViewModel::class.java))
                return NuevaReservaViewModel(usuarioId, repository::enviarSolicitud) as T
            }
        }
        val viewModel = ViewModelProvider(this, factory)[NuevaReservaViewModel::class.java]

        setContent {
            Prueba2Theme {
                NuevaReservaPantalla(
                    estado = viewModel.estado,
                    onTipo = viewModel::seleccionarTipo,
                    onEspacio = viewModel::seleccionarEspacio,
                    onFecha = viewModel::seleccionarFecha,
                    onInicio = viewModel::cambiarInicio,
                    onTermino = viewModel::cambiarTermino,
                    onMotivo = viewModel::cambiarMotivo,
                    onRevisar = viewModel::revisar,
                    onCerrarResumen = viewModel::cerrarResumen,
                    onEnviar = viewModel::enviar,
                    onCerrarExito = {
                        startActivity(Intent(this@NuevaReserva, MisReservas::class.java).apply {
                            putExtra("USER_ID", usuarioId)
                        })
                        finish()
                    },
                    onVolver = { finish() }
                )
            }
        }
    }
}
