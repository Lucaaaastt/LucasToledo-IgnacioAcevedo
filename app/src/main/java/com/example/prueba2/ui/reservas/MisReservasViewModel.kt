package com.example.prueba2.ui.reservas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prueba2.data.local.EstadoReserva
import com.example.prueba2.data.local.Reserva
import com.example.prueba2.data.local.ReservaConSolicitante
import com.example.prueba2.data.remote.ErrorReservaRemota
import com.example.prueba2.data.repository.ReservaNoDisponibleException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

class MisReservasViewModel private constructor(
    resumenes: Flow<List<ReservaResumen>>,
    mensajeError: String,
    private val resolverSolicitud: (suspend (Long, EstadoReserva) -> Unit)? = null,
    private val decisionGuardada: (ReservaResumen, EstadoReserva) -> Unit = { _, _ -> },
    avisoConexion: Flow<String?> = flowOf(null),
    private val descartar: (suspend (Long) -> Unit)? = null
) : ViewModel() {
    constructor(reservas: Flow<List<Reserva>>, avisoConexion: Flow<String?> = flowOf(null),
                descartar: (suspend (Long) -> Unit)? = null) : this(
        reservas.map { lista -> lista.map { it.aResumenReserva() } },
        "No se pudieron cargar tus reservas. Vuelve al menú e inténtalo nuevamente.",
        avisoConexion = avisoConexion, descartar = descartar
    )

    companion object {
        fun paraAdministrador(
            reservas: Flow<List<ReservaConSolicitante>>,
            decisionGuardada: (ReservaResumen, EstadoReserva) -> Unit = { _, _ -> },
            avisoConexion: Flow<String?> = flowOf(null),
            resolverSolicitud: suspend (Long, EstadoReserva) -> Unit
        ) = MisReservasViewModel(
            reservas.map { lista ->
                lista.map { it.reserva.aResumenReserva().copy(solicitanteNombre = it.solicitanteNombre) }
            },
            "No se pudieron cargar las solicitudes. Verifica tu sesión de administrador e inténtalo nuevamente.",
            resolverSolicitud,
            decisionGuardada, avisoConexion
        )
    }

    private val decision = MutableStateFlow(EstadoDecisionReserva())
    private val reintentos = MutableStateFlow(0)

    private val listado: Flow<EstadoMisReservas> = flow {
        var ultimaLista = emptyList<ReservaResumen>()
        emitAll(resumenes.map { lista ->
            ultimaLista = lista
            EstadoMisReservas(reservas = lista, cargando = false)
        }.catch { error ->
            emit(EstadoMisReservas(reservas = ultimaLista, cargando = false,
                error = (error as? ErrorReservaRemota)?.message ?: mensajeError))
        })
    }.onStart { emit(EstadoMisReservas()) }

    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: Flow<EstadoMisReservas> = reintentos.flatMapLatest {
        combine(listado, decision) { lista, accion ->
            lista.copy(decision = accion)
        }.combine(avisoConexion) { lista, aviso -> lista.copy(avisoConexion = aviso) }
    }

    fun reintentarCarga() {
        reintentos.value += 1
    }

    fun descartarError(reserva: ReservaResumen) {
        val borrar = descartar ?: return
        if (reserva.errorEnvio == null) return
        viewModelScope.launch {
            try { borrar(reserva.id.toLong()) }
            catch (cancelacion: CancellationException) { throw cancelacion }
            catch (_: Exception) { decision.value = EstadoDecisionReserva(mensaje = "No se pudo descartar la solicitud.") }
        }
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
        val reserva = accion.reserva ?: return
        val reservaId = reserva.id.toLongOrNull() ?: return
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
                runCatching { decisionGuardada(reserva, destino) }
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (_: ReservaNoDisponibleException) {
                decision.value = accion.copy(error = "Hay otra solicitud o reserva que ocupa ese horario. Revisa las solicitudes antes de aprobar.")
            } catch (error: ErrorReservaRemota) {
                decision.value = accion.copy(error = error.message)
            } catch (_: Exception) {
                decision.value = accion.copy(error = "No se pudo guardar la decisión. Verifica tu sesión y que la solicitud siga pendiente.")
            }
        }
    }
}
