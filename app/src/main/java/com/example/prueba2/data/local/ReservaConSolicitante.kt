package com.example.prueba2.data.local

import androidx.room.Embedded

data class ReservaConSolicitante(
    @Embedded val reserva: Reserva,
    val solicitanteNombre: String
)
