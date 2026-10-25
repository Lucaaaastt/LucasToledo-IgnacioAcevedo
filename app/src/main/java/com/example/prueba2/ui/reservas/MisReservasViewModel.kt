package com.example.prueba2.ui.reservas

import androidx.lifecycle.ViewModel
import com.example.prueba2.data.local.Reserva
import com.example.prueba2.data.local.ReservaConSolicitante
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

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
