package com.example.prueba2.ui.reservas

import com.example.prueba2.data.catalogo.EspacioReserva
import com.example.prueba2.data.catalogo.TipoEspacio
import com.example.prueba2.data.catalogo.espaciosDelTipo
import java.time.LocalDate
import java.time.LocalTime

data class EstadoNuevaReserva(
    val tipo: TipoEspacio? = null,
    val espacioId: String? = null,
    val fecha: LocalDate? = null,
    val horaInicioTexto: String = "",
    val horaTerminoTexto: String = "",
    val motivo: String = "",
    val intentoRevisar: Boolean = false,
    val mostrarResumen: Boolean = false,
    val guardando: Boolean = false,
    val errorGuardado: String? = null,
    val reservaGuardadaId: Long? = null,
    val pendienteEnvio: Boolean = false,
    val catalogo: Map<TipoEspacio, List<EspacioReserva>> = TipoEspacio.entries.associateWith(::espaciosDelTipo)
) {
    val espaciosDisponibles: List<EspacioReserva>
        get() = catalogo[tipo].orEmpty()

    val espacioSeleccionado: EspacioReserva?
        get() = espaciosDisponibles.find { it.id == espacioId }

    val horaInicio: LocalTime?
        get() = interpretarHoraReserva(horaInicioTexto)

    val horaTermino: LocalTime?
        get() = interpretarHoraReserva(horaTerminoTexto)
}
