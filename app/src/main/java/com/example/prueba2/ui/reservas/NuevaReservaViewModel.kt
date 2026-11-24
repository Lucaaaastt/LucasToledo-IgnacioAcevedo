package com.example.prueba2.ui.reservas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prueba2.data.catalogo.TipoEspacio
import com.example.prueba2.data.catalogo.EspacioReserva
import com.example.prueba2.data.local.Reserva
import com.example.prueba2.data.remote.ErrorReservaRemota
import com.example.prueba2.data.repository.ReservaNoDisponibleException
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

class NuevaReservaViewModel(
    private val usuarioId: Int = 0,
    private val enviarSolicitud: suspend (Reserva) -> Long = { error("Guardado no configurado") },
    private val scopeGuardado: CoroutineScope? = null,
    catalogo: Flow<Map<TipoEspacio, List<EspacioReserva>>> = emptyFlow()
) : ViewModel() {
    var estado by mutableStateOf(EstadoNuevaReserva())
        private set

    private val edicionBloqueada: Boolean
        get() = estado.guardando || estado.reservaGuardadaId != null

    init {
        viewModelScope.launch {
            catalogo.collect(::actualizarCatalogo)
        }
    }

    internal fun actualizarCatalogo(catalogo: Map<TipoEspacio, List<EspacioReserva>>) {
        val tipo = estado.tipo?.takeIf { it in catalogo }
        val disponibles = catalogo[tipo].orEmpty()
        val espacioId = estado.espacioId?.takeIf { id -> disponibles.any { it.id == id } }
            ?: disponibles.singleOrNull()?.id
        estado = estado.copy(
            catalogo = catalogo,
            tipo = tipo,
            espacioId = espacioId,
            mostrarResumen = estado.mostrarResumen && espacioId == estado.espacioId
        )
    }

    fun seleccionarTipo(tipo: TipoEspacio) {
        if (edicionBloqueada || tipo !in estado.catalogo) return
        estado = estado.copy(
            tipo = tipo,
            espacioId = estado.catalogo[tipo].orEmpty().singleOrNull()?.id,
            mostrarResumen = false,
            errorGuardado = null
        )
    }

    fun seleccionarEspacio(id: String) {
        if (edicionBloqueada) return
        if (estado.espaciosDisponibles.any { it.id == id }) {
            estado = estado.copy(espacioId = id, mostrarResumen = false, errorGuardado = null)
        }
    }

    fun seleccionarFecha(fecha: LocalDate) {
        if (edicionBloqueada) return
        estado = estado.copy(fecha = fecha, mostrarResumen = false, errorGuardado = null)
    }

    fun cambiarInicio(texto: String) {
        if (edicionBloqueada) return
        estado = estado.copy(horaInicioTexto = texto, mostrarResumen = false, errorGuardado = null)
    }

    fun cambiarTermino(texto: String) {
        if (edicionBloqueada) return
        estado = estado.copy(horaTerminoTexto = texto, mostrarResumen = false, errorGuardado = null)
    }

    fun cambiarMotivo(motivo: String) {
        if (edicionBloqueada) return
        estado = estado.copy(motivo = motivo, mostrarResumen = false, errorGuardado = null)
    }

    fun revisar() {
        if (edicionBloqueada) return
        estado = estado.copy(
            intentoRevisar = true,
            mostrarResumen = !validarNuevaReserva(estado).hayErrores,
            errorGuardado = null
        )
    }

    fun cerrarResumen() {
        if (estado.guardando) return
        estado = estado.copy(mostrarResumen = false)
    }

    fun enviar() {
        if (edicionBloqueada || !estado.mostrarResumen) return
        if (validarNuevaReserva(estado).hayErrores) {
            estado = estado.copy(intentoRevisar = true, mostrarResumen = false)
            return
        }
        if (usuarioId <= 0) {
            estado = estado.copy(errorGuardado = "No se pudo identificar tu usuario. Vuelve a iniciar sesión.")
            return
        }
        val reserva = Reserva(
            espacioId = requireNotNull(estado.espacioId),
            usuarioId = usuarioId,
            fecha = requireNotNull(estado.fecha),
            horaInicio = requireNotNull(estado.horaInicio),
            horaTermino = requireNotNull(estado.horaTermino),
            motivo = estado.motivo.trim()
        )
        estado = estado.copy(guardando = true, errorGuardado = null)
        (scopeGuardado ?: viewModelScope).launch {
            try {
                val id = enviarSolicitud(reserva)
                estado = estado.copy(mostrarResumen = false, reservaGuardadaId = id)
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (_: ReservaNoDisponibleException) {
                estado = estado.copy(errorGuardado = "El espacio ya está ocupado por una solicitud pendiente o una reserva aprobada para ese horario. Elige otro horario o espacio.")
            } catch (error: ErrorReservaRemota) {
                estado = estado.copy(errorGuardado = error.message)
            } catch (_: Exception) {
                estado = estado.copy(errorGuardado = "No se pudo enviar la solicitud. Tus datos se conservaron; puedes volver a intentarlo.")
            } finally {
                estado = estado.copy(guardando = false)
            }
        }
    }
}
