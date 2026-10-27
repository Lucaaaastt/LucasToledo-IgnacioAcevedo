package com.example.prueba2.ui.reservas

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.example.prueba2.utils.Notificaciones
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.prueba2.data.local.EstadoReserva
import com.example.prueba2.ui.theme.Prueba2Theme
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import android.Manifest

// Datos exclusivos de demostración: no se guardan en Room ni pertenecen a un usuario.
private val reservasDeEjemplo = listOf(
    ReservaResumen(
        id = "ejemplo-1",
        espacio = "Laboratorio de Computación 2",
        fecha = LocalDate.of(2026, 10, 26),
        horaInicio = LocalTime.of(10, 0),
        horaTermino = LocalTime.of(11, 0),
        motivo = "Clase de tecnología"
    ),
    ReservaResumen(
        id = "ejemplo-2",
        espacio = "Biblioteca",
        fecha = LocalDate.of(2026, 10, 27),
        horaInicio = LocalTime.of(14, 0),
        horaTermino = LocalTime.of(15, 30),
        motivo = "Taller de lectura",
        estado = EstadoReserva.APROBADA
    )
)

private val formatoFecha = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-CL"))
private val formatoHora = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisReservasPantalla(
    reservas: List<ReservaResumen>,
    onVolver: () -> Unit,
    cargando: Boolean = false,
    errorCarga: String? = null,
    titulo: String = "Mis reservas",
    descripcion: String = "Consulta los datos de tus reservas.",
    mensajeVacio: String = "Aún no tienes solicitudes de reserva.",
    mostrarAccionesAdministrativas: Boolean = false,
    decision: EstadoDecisionReserva = EstadoDecisionReserva(),
    onSolicitarDecision: (ReservaResumen, EstadoReserva) -> Unit = { _, _ -> },
    onConfirmarDecision: () -> Unit = {},
    onCancelarDecision: () -> Unit = {}
) {
    val context = LocalContext.current
    // PARA PEDIR PERMISO POR NOTIFIACIONES
    val launcherPermiso = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Se ejecuta al responder el dialogo */ }

    // SE CREAR CANAL Y PEDIR PERMISO AL CARGAR PANTALLA
    LaunchedEffect(Unit) {
        Notificaciones.crearCanalNotificaciones(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcherPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titulo) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver al menú")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = descripcion,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            decision.mensaje?.let { mensaje ->
                item { Text(mensaje, color = MaterialTheme.colorScheme.primary) }
            }

            if (cargando) {
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text("Cargando solicitudes…")
                    }
                }
            } else if (errorCarga != null) {
                item {
                    Text(errorCarga, color = MaterialTheme.colorScheme.error)
                }
            } else if (reservas.isEmpty()) {
                item {
                    Text(
                        text = mensajeVacio,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            } else {
                items(reservas, key = { it.id }) { reserva ->
                    ReservaTarjeta(
                        reserva = reserva,
                        mostrarAccionesAdministrativas = mostrarAccionesAdministrativas,
                        accionesHabilitadas = !decision.guardando,
                        onSolicitarDecision = onSolicitarDecision
                    )
                }
            }
        }
    }

    val seleccionada = decision.reserva
    val destino = decision.estadoDestino
    if (mostrarAccionesAdministrativas && seleccionada != null && destino != null) {
        val aprobar = destino == EstadoReserva.APROBADA
        AlertDialog(
            onDismissRequest = onCancelarDecision,
            title = { Text(if (aprobar) "Aprobar solicitud" else "Rechazar solicitud") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(seleccionada.espacio)
                    Text(seleccionada.fecha.format(formatoFecha))
                    Text("${seleccionada.horaInicio.format(formatoHora)} – ${seleccionada.horaTermino.format(formatoHora)}")
                    Text(
                        if (aprobar) "La reserva quedará aprobada y mantendrá ocupado el horario."
                        else "La solicitud quedará rechazada y dejará de ocupar este horario."
                    )
                    decision.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (decision.guardando) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text("Guardando decisión…")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (seleccionada != null && destino != null){
                            Notificaciones.enviarNotificacionReserva(
                                context = context,
                                espacio = seleccionada.espacio,
                                estado = destino
                            )
                        }
                        onConfirmarDecision()
                    },
                    enabled = !decision.guardando
                ) {
                    Text(if (aprobar) "Aprobar" else "Rechazar")
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelarDecision, enabled = !decision.guardando) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun ReservaTarjeta(
    reserva: ReservaResumen,
    mostrarAccionesAdministrativas: Boolean,
    accionesHabilitadas: Boolean,
    onSolicitarDecision: (ReservaResumen, EstadoReserva) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(reserva.espacio, style = MaterialTheme.typography.titleLarge)

            reserva.solicitanteNombre?.let { nombre ->
                Text("Solicitante: $nombre", style = MaterialTheme.typography.bodyLarge)
            }

            Surface(
                color = when (reserva.estado) {
                    EstadoReserva.APROBADA -> MaterialTheme.colorScheme.secondaryContainer
                    EstadoReserva.RECHAZADA -> MaterialTheme.colorScheme.errorContainer
                    EstadoReserva.PENDIENTE -> MaterialTheme.colorScheme.tertiaryContainer
                },
                shape = MaterialTheme.shapes.large
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        when (reserva.estado) {
                            EstadoReserva.APROBADA -> Icons.Default.Check
                            EstadoReserva.RECHAZADA -> Icons.Default.Close
                            EstadoReserva.PENDIENTE -> Icons.Default.Schedule
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        when (reserva.estado) {
                            EstadoReserva.APROBADA -> "Aprobada"
                            EstadoReserva.RECHAZADA -> "Rechazada"
                            EstadoReserva.PENDIENTE -> "Pendiente de aprobación"
                        },
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            ReservaDato(Icons.Default.DateRange, reserva.fecha.format(formatoFecha))
            ReservaDato(
                Icons.Default.Schedule,
                "${reserva.horaInicio.format(formatoHora)} – ${reserva.horaTermino.format(formatoHora)}"
            )

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Motivo",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(reserva.motivo, style = MaterialTheme.typography.bodyLarge)
            }

            if (mostrarAccionesAdministrativas && reserva.estado == EstadoReserva.PENDIENTE) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onSolicitarDecision(reserva, EstadoReserva.APROBADA) },
                        enabled = accionesHabilitadas,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Aprobar")
                    }
                    OutlinedButton(
                        onClick = { onSolicitarDecision(reserva, EstadoReserva.RECHAZADA) },
                        enabled = accionesHabilitadas,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Rechazar")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReservaDato(icono: ImageVector, texto: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Text(texto, style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
private fun MisReservasPreview() {
    Prueba2Theme {
        MisReservasPantalla(reservasDeEjemplo, onVolver = {})
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 640)
@Composable
private fun MisReservasVaciasPreview() {
    Prueba2Theme {
        MisReservasPantalla(emptyList(), onVolver = {})
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 800)
@Composable
private fun SolicitudesAdministradorPreview() {
    Prueba2Theme {
        MisReservasPantalla(
            reservas = listOf(reservasDeEjemplo.first().copy(solicitanteNombre = "Marcela")),
            onVolver = {},
            titulo = "Solicitudes pendientes",
            descripcion = "Solicitudes enviadas por los usuarios para revisión.",
            mensajeVacio = "No hay solicitudes pendientes.",
            mostrarAccionesAdministrativas = true
        )
    }
}
