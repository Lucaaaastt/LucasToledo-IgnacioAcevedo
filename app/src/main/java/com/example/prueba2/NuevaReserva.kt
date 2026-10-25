package com.example.prueba2

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.semantics.error as semanticError
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.prueba2.data.AppDatabase
import com.example.prueba2.data.Reserva
import com.example.prueba2.data.ReservaNoDisponibleException
import com.example.prueba2.data.ReservaRepository
import com.example.prueba2.ui.theme.Prueba2Theme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class NuevaReserva : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val usuarioId = intent.getIntExtra("USER_ID", -1)
        if (usuarioId <= 0) {
            Toast.makeText(this, "Vuelve a iniciar sesión para enviar una solicitud.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val repository = ReservaRepository(AppDatabase.getDatabase(applicationContext))
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(NuevaReservaViewModel::class.java))
                return NuevaReservaViewModel(usuarioId, repository::enviarSolicitud) as T
            }
        }
        val viewModel = ViewModelProvider(this, factory)[NuevaReservaViewModel::class.java]

        setContent {
            Prueba2Theme {
                NuevaReservaPantalla(
                    estado = viewModel.estado,
                    onTipo = viewModel::seleccionarTipo,
                    onEspacio = viewModel::seleccionarEspacio,
                    onFecha = viewModel::seleccionarFecha,
                    onInicio = viewModel::cambiarInicio,
                    onTermino = viewModel::cambiarTermino,
                    onMotivo = viewModel::cambiarMotivo,
                    onRevisar = viewModel::revisar,
                    onCerrarResumen = viewModel::cerrarResumen,
                    onEnviar = viewModel::enviar,
                    onCerrarExito = {
                        startActivity(Intent(this@NuevaReserva, MisReservas::class.java).apply {
                            putExtra("USER_ID", usuarioId)
                        })
                        finish()
                    },
                    onVolver = { finish() }
                )
            }
        }
    }
}

enum class TipoEspacio(val etiqueta: String) {
    SALA("Sala"),
    LABORATORIO("Laboratorio de computación"),
    BIBLIOTECA("Biblioteca"),
    GIMNASIO("Gimnasio")
}

data class EspacioReserva(val id: String, val nombre: String)

fun espaciosDelTipo(tipo: TipoEspacio?): List<EspacioReserva> = when (tipo) {
    TipoEspacio.SALA -> (1..12).map { EspacioReserva("SALA-$it", "Sala $it") }
    TipoEspacio.LABORATORIO -> (1..3).map {
        EspacioReserva("LAB-$it", "Laboratorio de Computación $it")
    }
    TipoEspacio.BIBLIOTECA -> listOf(EspacioReserva("BIBLIOTECA", "Biblioteca"))
    TipoEspacio.GIMNASIO -> listOf(EspacioReserva("GIMNASIO", "Gimnasio"))
    null -> emptyList()
}

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
    val reservaGuardadaId: Long? = null
) {
    val espacioSeleccionado: EspacioReserva?
        get() = espaciosDelTipo(tipo).find { it.id == espacioId }

    val horaInicio: LocalTime?
        get() = interpretarHoraReserva(horaInicioTexto)

    val horaTermino: LocalTime?
        get() = interpretarHoraReserva(horaTerminoTexto)
}

private val patronHoraReserva = Regex("([01][0-9]|2[0-3]):[0-5][0-9]")

fun interpretarHoraReserva(texto: String): LocalTime? {
    if (!patronHoraReserva.matches(texto)) return null
    return LocalTime.of(texto.substring(0, 2).toInt(), texto.substring(3, 5).toInt())
}

data class ErroresNuevaReserva(
    val tipo: String? = null,
    val espacio: String? = null,
    val fecha: String? = null,
    val inicio: String? = null,
    val termino: String? = null,
    val motivo: String? = null
) {
    val hayErrores: Boolean
        get() = listOf(tipo, espacio, fecha, inicio, termino, motivo).any { it != null }
}

fun validarNuevaReserva(
    estado: EstadoNuevaReserva,
    ahora: LocalDateTime = LocalDateTime.now()
): ErroresNuevaReserva {
    val horaInicio = estado.horaInicio
    val horaTermino = estado.horaTermino
    return ErroresNuevaReserva(
        tipo = if (estado.tipo == null) "Selecciona un tipo de espacio." else null,
        espacio = if (estado.tipo != null && estado.espacioSeleccionado == null) {
            "Selecciona un espacio de la categoría elegida."
        } else null,
        fecha = when {
            estado.fecha == null -> "Selecciona la fecha de la reserva."
            estado.fecha.isBefore(ahora.toLocalDate()) -> "La fecha no puede estar en el pasado."
            else -> null
        },
        inicio = when {
            estado.horaInicioTexto.isBlank() -> "Ingresa la hora de inicio."
            horaInicio == null -> "Usa HH:mm: horas de 00 a 23 y minutos de 00 a 59."
            estado.fecha == ahora.toLocalDate() && horaInicio.isBefore(ahora.toLocalTime()) ->
                "La hora de inicio no puede estar en el pasado."
            else -> null
        },
        termino = when {
            estado.horaTerminoTexto.isBlank() -> "Ingresa la hora de término."
            horaTermino == null -> "Usa HH:mm: horas de 00 a 23 y minutos de 00 a 59."
            horaInicio != null && !horaTermino.isAfter(horaInicio) ->
                "La hora de término debe ser posterior al inicio."
            else -> null
        },
        motivo = when {
            estado.motivo.isBlank() -> "Ingresa el motivo de la reserva."
            estado.motivo.length > 300 -> "El motivo admite hasta 300 caracteres."
            else -> null
        }
    )
}

class NuevaReservaViewModel(
    private val usuarioId: Int = 0,
    private val enviarSolicitud: suspend (Reserva) -> Long = { error("Guardado no configurado") },
    private val scopeGuardado: CoroutineScope? = null
) : ViewModel() {
    var estado by mutableStateOf(EstadoNuevaReserva())
        private set

    private val edicionBloqueada: Boolean
        get() = estado.guardando || estado.reservaGuardadaId != null

    fun seleccionarTipo(tipo: TipoEspacio) {
        if (edicionBloqueada) return
        estado = estado.copy(
            tipo = tipo,
            espacioId = espaciosDelTipo(tipo).singleOrNull()?.id,
            mostrarResumen = false,
            errorGuardado = null
        )
    }

    fun seleccionarEspacio(id: String) {
        if (edicionBloqueada) return
        if (espaciosDelTipo(estado.tipo).any { it.id == id }) {
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
            } catch (_: Exception) {
                estado = estado.copy(errorGuardado = "No se pudo enviar la solicitud. Tus datos se conservaron; puedes volver a intentarlo.")
            } finally {
                estado = estado.copy(guardando = false)
            }
        }
    }
}

private val formatoFechaReserva = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val formatoHoraReserva = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevaReservaPantalla(
    estado: EstadoNuevaReserva,
    onTipo: (TipoEspacio) -> Unit,
    onEspacio: (String) -> Unit,
    onFecha: (LocalDate) -> Unit,
    onInicio: (String) -> Unit,
    onTermino: (String) -> Unit,
    onMotivo: (String) -> Unit,
    onRevisar: () -> Unit,
    onCerrarResumen: () -> Unit,
    onEnviar: () -> Unit,
    onCerrarExito: () -> Unit,
    onVolver: () -> Unit
) {
    val context = LocalContext.current
    val errores = if (estado.intentoRevisar) validarNuevaReserva(estado) else ErroresNuevaReserva()
    val espacios = espaciosDelTipo(estado.tipo)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva reserva") },
                navigationIcon = {
                    IconButton(onClick = onVolver, enabled = !estado.guardando) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver al menú")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "Selecciona cuándo y dónde necesitas reservar.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SelectorReserva(
                etiqueta = "Tipo de espacio *",
                valor = estado.tipo?.etiqueta.orEmpty(),
                opciones = TipoEspacio.entries.map { it.name to it.etiqueta },
                error = errores.tipo,
                onSeleccionar = { onTipo(TipoEspacio.valueOf(it)) }
            )

            CampoSelectorReserva(
                etiqueta = "Fecha *",
                valor = estado.fecha?.format(formatoFechaReserva) ?: "Seleccionar fecha",
                icono = Icons.Default.DateRange,
                error = errores.fecha,
                onClick = {
                    val hoy = LocalDate.now()
                    val inicial = estado.fecha?.takeUnless { it.isBefore(hoy) } ?: hoy
                    DatePickerDialog(
                        context,
                        { _, anio, mes, dia -> onFecha(LocalDate.of(anio, mes + 1, dia)) },
                        inicial.year, inicial.monthValue - 1, inicial.dayOfMonth
                    ).apply {
                        datePicker.minDate = hoy.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    }.show()
                }
            )

            CampoHoraReserva(
                etiqueta = "Hora de inicio *",
                valor = estado.horaInicioTexto,
                error = errores.inicio,
                onValueChange = onInicio
            )

            CampoHoraReserva(
                etiqueta = "Hora de término *",
                valor = estado.horaTerminoTexto,
                error = errores.termino,
                onValueChange = onTermino
            )

            if (espacios.size == 1) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                        Column {
                            Text("Espacio seleccionado", style = MaterialTheme.typography.labelMedium)
                            Text(espacios.single().nombre, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            } else {
                SelectorReserva(
                    etiqueta = if (estado.tipo == TipoEspacio.LABORATORIO) "Laboratorio *" else "Espacio *",
                    valor = estado.espacioSeleccionado?.nombre.orEmpty(),
                    opciones = espacios.map { it.id to it.nombre },
                    error = errores.espacio,
                    habilitado = estado.tipo != null,
                    onSeleccionar = onEspacio
                )
            }

            OutlinedTextField(
                value = estado.motivo,
                onValueChange = onMotivo,
                label = { Text("Motivo de la reserva *") },
                placeholder = { Text("Ej.: Clase de tecnología") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                minLines = 3,
                maxLines = 6,
                isError = errores.motivo != null,
                supportingText = {
                    Column {
                        errores.motivo?.let { Text(it) }
                        Text("${estado.motivo.length}/300 caracteres")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            estado.errorGuardado?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { semanticError(it) })
            }
            Button(onClick = onRevisar, enabled = !estado.guardando, modifier = Modifier.fillMaxWidth()) {
                Text("Revisar solicitud")
            }
            Text(
                text = "* Campos obligatorios. La solicitud requiere aprobación del administrador. Al enviarla, el espacio queda ocupado para la fecha y el horario solicitados.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (estado.mostrarResumen) {
        AlertDialog(
            onDismissRequest = onCerrarResumen,
            title = { Text("Revisa los datos") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(estado.espacioSeleccionado?.nombre.orEmpty(), style = MaterialTheme.typography.titleMedium)
                    Text("Fecha: ${estado.fecha?.format(formatoFechaReserva)}")
                    Text("Horario: ${estado.horaInicio?.format(formatoHoraReserva)} – ${estado.horaTermino?.format(formatoHoraReserva)}")
                    Text("Motivo: ${estado.motivo.trim()}")
                    Text("Al enviar, la solicitud se guardará como pendiente de aprobación y el espacio quedará ocupado para la fecha y el horario solicitados.")
                    estado.errorGuardado?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { semanticError(it) })
                    }
                    if (estado.guardando) {
                        CircularProgressIndicator()
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onEnviar, enabled = !estado.guardando) {
                    Text(if (estado.guardando) "Enviando…" else "Enviar solicitud")
                }
            },
            dismissButton = {
                TextButton(onClick = onCerrarResumen, enabled = !estado.guardando) { Text("Editar datos") }
            }
        )
    }

    if (estado.reservaGuardadaId != null) {
        AlertDialog(
            onDismissRequest = onCerrarExito,
            title = { Text("Solicitud enviada") },
            text = { Text("Tu solicitud se guardó y está pendiente de aprobación del administrador. El espacio ya está ocupado para la fecha y el horario solicitados.") },
            confirmButton = {
                TextButton(onClick = onCerrarExito) { Text("Ver mis reservas") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorReserva(
    etiqueta: String,
    valor: String,
    opciones: List<Pair<String, String>>,
    onSeleccionar: (String) -> Unit,
    error: String? = null,
    habilitado: Boolean = true
) {
    var expandido by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expandido && habilitado,
        onExpandedChange = { if (habilitado) expandido = it }
    ) {
        OutlinedTextField(
            value = valor,
            onValueChange = {},
            readOnly = true,
            enabled = habilitado,
            label = { Text(etiqueta) },
            placeholder = { Text("Seleccionar") },
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido && habilitado) },
            isError = error != null,
            supportingText = error?.let { mensaje -> { Text(mensaje) } },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = habilitado)
        )
        ExposedDropdownMenu(expanded = expandido && habilitado, onDismissRequest = { expandido = false }) {
            opciones.forEach { (id, nombre) ->
                DropdownMenuItem(
                    text = { Text(nombre) },
                    onClick = {
                        onSeleccionar(id)
                        expandido = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CampoHoraReserva(
    etiqueta: String,
    valor: String,
    error: String?,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValueChange,
        label = { Text(etiqueta) },
        placeholder = { Text("09:00") },
        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
        isError = error != null,
        supportingText = { Text(error ?: "Formato de 24 horas: HH:mm") },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun CampoSelectorReserva(
    etiqueta: String,
    valor: String,
    icono: ImageVector,
    error: String?,
    onClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.labelLarge)
        OutlinedButton(
            onClick = onClick,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { error?.let { semanticError(it) } }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icono, contentDescription = null)
                Text(valor, style = MaterialTheme.typography.bodyLarge)
            }
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun NuevaReservaPreview() {
    Prueba2Theme {
        NuevaReservaPantalla(
            estado = EstadoNuevaReserva(tipo = TipoEspacio.LABORATORIO, espacioId = "LAB-2"),
            onTipo = {}, onEspacio = {}, onFecha = {}, onInicio = {}, onTermino = {},
            onMotivo = {}, onRevisar = {}, onCerrarResumen = {}, onEnviar = {},
            onCerrarExito = {}, onVolver = {}
        )
    }
}
