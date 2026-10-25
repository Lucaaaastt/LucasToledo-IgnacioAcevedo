package com.example.prueba2.ui.reservas



data class EstadoMisReservas(
    val reservas: List<ReservaResumen> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null
)
