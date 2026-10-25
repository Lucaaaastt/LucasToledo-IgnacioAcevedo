package com.example.prueba2.ui.reservas

import com.example.prueba2.data.local.EstadoReserva

data class EstadoMisReservas(
    val reservas: List<ReservaResumen> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null,
    val decision: EstadoDecisionReserva = EstadoDecisionReserva()
)

data class EstadoDecisionReserva(
    val reserva: ReservaResumen? = null,
    val estadoDestino: EstadoReserva? = null,
    val guardando: Boolean = false,
    val mensaje: String? = null,
    val error: String? = null
)
