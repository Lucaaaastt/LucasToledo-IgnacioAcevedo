package com.example.prueba2

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.flowWithLifecycle
import com.example.prueba2.data.AppDatabase
import com.example.prueba2.data.Reserva
import com.example.prueba2.data.ReservaConSolicitante
import com.example.prueba2.data.ReservaRepository
import com.example.prueba2.ui.reservas.MisReservasPantalla
import com.example.prueba2.ui.reservas.ReservaResumen
import com.example.prueba2.ui.theme.Prueba2Theme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class MisReservas : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val usuarioId = intent.getIntExtra("USER_ID", -1)
        val esAdministrador = intent.getStringExtra("USER_ROL") == "ADMIN"
        if (usuarioId <= 0) {
            Toast.makeText(this, "Vuelve a iniciar sesión para consultar tus reservas.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val repository = ReservaRepository(AppDatabase.getDatabase(applicationContext))
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(MisReservasViewModel::class.java))
                return if (esAdministrador) {
                    MisReservasViewModel.paraAdministrador(repository.obtenerPendientes(usuarioId))
                } else {
                    MisReservasViewModel(repository.obtenerPorUsuario(usuarioId))
                } as T
            }
        }
        val viewModel = ViewModelProvider(this, factory)[MisReservasViewModel::class.java]

        setContent {
            Prueba2Theme {
                val flujo = remember(viewModel) {
                    viewModel.estado.flowWithLifecycle(this@MisReservas.lifecycle, Lifecycle.State.STARTED)
                }
                val estado by flujo.collectAsState(initial = EstadoMisReservas())
                MisReservasPantalla(
                    reservas = estado.reservas,
                    cargando = estado.cargando,
                    errorCarga = estado.error,
                    onVolver = { finish() },
                    titulo = if (esAdministrador) "Solicitudes pendientes" else "Mis reservas",
                    descripcion = if (esAdministrador) "Solicitudes enviadas por los usuarios para revisión." else "Consulta los datos de tus reservas.",
                    mensajeVacio = if (esAdministrador) "No hay solicitudes pendientes." else "Aún no tienes solicitudes de reserva."
                )
            }
        }
    }
}

data class EstadoMisReservas(
    val reservas: List<ReservaResumen> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null
)

class MisReservasViewModel private constructor(
    resumenes: Flow<List<ReservaResumen>>,
    mensajeError: String
) : ViewModel() {
    constructor(reservas: Flow<List<Reserva>>) : this(
        reservas.map { lista -> lista.map { it.aResumenReserva() } },
        "No se pudieron cargar tus reservas. Vuelve al menú e inténtalo nuevamente."
    )

    companion object {
        fun paraAdministrador(reservas: Flow<List<ReservaConSolicitante>>) = MisReservasViewModel(
            reservas.map { lista ->
                lista.map { it.reserva.aResumenReserva().copy(solicitanteNombre = it.solicitanteNombre) }
            },
            "No se pudieron cargar las solicitudes. Verifica tu sesión de administrador e inténtalo nuevamente."
        )
    }

    val estado: Flow<EstadoMisReservas> = resumenes
        .map { lista ->
            EstadoMisReservas(reservas = lista, cargando = false)
        }
        .onStart { emit(EstadoMisReservas()) }
        .catch {
            emit(EstadoMisReservas(cargando = false, error = mensajeError))
        }
}

internal fun Reserva.aResumenReserva(): ReservaResumen {
    val nombre = TipoEspacio.entries.asSequence()
        .flatMap { espaciosDelTipo(it).asSequence() }
        .firstOrNull { it.id == espacioId }?.nombre ?: espacioId
    return ReservaResumen(
        id = id.toString(),
        espacio = nombre,
        fecha = fecha,
        horaInicio = horaInicio,
        horaTermino = horaTermino,
        motivo = motivo,
        estado = estado
    )
}
