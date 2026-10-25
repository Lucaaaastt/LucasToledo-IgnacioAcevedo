package com.example.prueba2.ui.reservas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prueba2.data.local.EstadoReserva
import com.example.prueba2.data.local.Reserva
import com.example.prueba2.data.local.ReservaConSolicitante
import com.example.prueba2.data.repository.ReservaNoDisponibleException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class MisReservasViewModel private constructor(
    resumenes: Flow<List<ReservaResumen>>,
    mensajeError: String,
    private val resolverSolicitud: (suspend (Long, EstadoReserva) -> Unit)? = null
) : ViewModel() {
    constructor(reservas: Flow<List<Reserva>>) : this(
        reservas.map { lista -> lista.map { it.aResumenReserva() } },
        "No se pudieron cargar tus reservas. Vuelve al menú e inténtalo nuevamente."
    )

    companion object {
        fun paraAdministrador(
            reservas: Flow<List<ReservaConSolicitante>>,
            resolverSolicitud: suspend (Long, EstadoReserva) -> Unit
        ) = MisReservasViewModel(
            reservas.map { lista ->
                lista.map { it.reserva.aResumenReserva().copy(solicitanteNombre = it.solicitanteNombre) }
            },
            "No se pudieron cargar las solicitudes. Verifica tu sesión de administrador e inténtalo nuevamente.",
            resolverSolicitud
        )
    }

    private val decision = MutableStateFlow(EstadoDecisionReserva())

    private val listado: Flow<EstadoMisReservas> = resumenes
        .map { lista ->
            EstadoMisReservas(reservas = lista, cargando = false)
        }
        .onStart { emit(EstadoMisReservas()) }
        .catch {
            emit(EstadoMisReservas(cargando = false, error = mensajeError))
        }

    val estado: Flow<EstadoMisReservas> = combine(listado, decision) { lista, accion ->
        lista.copy(decision = accion)
    }

    fun solicitarDecision(reserva: ReservaResumen, estado: EstadoReserva) {
        if (resolverSolicitud == null || decision.value.guardando) return
        if (reserva.estado != EstadoReserva.PENDIENTE) return
        if (estado != EstadoReserva.APROBADA && estado != EstadoReserva.RECHAZADA) return
        decision.value = EstadoDecisionReserva(reserva = reserva, estadoDestino = estado)
    }

    fun cancelarDecision() {
        if (!decision.value.guardando) decision.value = EstadoDecisionReserva()
    }

    fun confirmarDecision() {
        val resolver = resolverSolicitud ?: return
        val accion = decision.value
        if (accion.guardando) return
        val reservaId = accion.reserva?.id?.toLongOrNull() ?: return
        val destino = accion.estadoDestino ?: return
        decision.value = accion.copy(guardando = true, error = null)
        viewModelScope.launch {
            try {
                resolver(reservaId, destino)
                decision.value = EstadoDecisionReserva(
                    mensaje = if (destino == EstadoReserva.APROBADA) {
                        "Solicitud aprobada. El horario permanece ocupado."
                    } else {
                        "Solicitud rechazada. Se liberó el horario de esta solicitud."
                    }
                )
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (_: ReservaNoDisponibleException) {
                decision.value = accion.copy(error = "Hay otra solicitud o reserva que ocupa ese horario. Revisa las solicitudes antes de aprobar.")
            } catch (_: Exception) {
                decision.value = accion.copy(error = "No se pudo guardar la decisión. Verifica tu sesión y que la solicitud siga pendiente.")
            }
        }
    }
}
