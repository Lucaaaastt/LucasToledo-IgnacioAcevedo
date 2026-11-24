package com.example.prueba2.ui.reportes

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.prueba2.data.catalogo.EspacioReserva
import com.example.prueba2.data.catalogo.TipoEspacio
import com.example.prueba2.data.catalogo.espaciosDelTipo
import com.example.prueba2.data.local.EstadoReporte
import com.example.prueba2.data.local.ReporteConAutor
import com.example.prueba2.data.local.ReporteIncidencia
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun VistaReportesUsuario(
    usuarioId: Int,
    viewModel: ReporteViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val reportes by viewModel.reportesUsuario.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val mensajeExito by viewModel.mensajeExito.collectAsState()
    val mensajeError by viewModel.mensajeError.collectAsState()

    var tipoSeleccionado by remember { mutableStateOf<TipoEspacio?>(null) }
    var espacioSeleccionado by remember { mutableStateOf<EspacioReserva?>(null) }
    var descripcion by remember { mutableStateOf("") }
    var fotoUri by remember { mutableStateOf<String?>(null) }
    var uriTemporalCamara by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(usuarioId) {
        if (usuarioId > 0) {
            viewModel.cargarReportesUsuario(usuarioId)
        }
    }

    LaunchedEffect(mensajeExito) {
        mensajeExito?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            tipoSeleccionado = null
            espacioSeleccionado = null
            descripcion = ""
            fotoUri = null
            viewModel.limpiarMensajes()
        }
    }

    val camaraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { fotoTomada ->
        if (fotoTomada && uriTemporalCamara != null) {
            fotoUri = uriTemporalCamara.toString()
        }
    }

    val permisoCamaraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            val nuevaUri = crearUriParaCamara(context)
            uriTemporalCamara = nuevaUri
            camaraLauncher.launch(nuevaUri)
        } else {
            Toast.makeText(
                context,
                "Permiso de cámara denegado. Puedes adjuntar desde galería.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val galeriaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uriElegida ->
        if (uriElegida != null) {
            val uriLocal = copiarImagenAAlmacenamientoInterno(context, uriElegida)
            if (uriLocal != null) {
                fotoUri = uriLocal
            } else {
                Toast.makeText(context, "No se pudo cargar la imagen.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun abrirCamaraConPermiso() {
        val estadoPermiso = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        )
        if (estadoPermiso == PackageManager.PERMISSION_GRANTED) {
            val nuevaUri = crearUriParaCamara(context)
            uriTemporalCamara = nuevaUri
            camaraLauncher.launch(nuevaUri)
        } else {
            permisoCamaraLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val listaEspacios = remember(tipoSeleccionado) { espaciosDelTipo(tipoSeleccionado) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Reportar Incidencia",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Informa problemas en salas, laboratorios o espacios comunes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SelectorDesplegable(
                        etiqueta = "Tipo de espacio",
                        valorActual = tipoSeleccionado?.etiqueta ?: "Seleccionar tipo...",
                        opciones = TipoEspacio.entries,
                        textoOpcion = { it.etiqueta },
                        alSeleccionar = { nuevoTipo ->
                            tipoSeleccionado = nuevoTipo
                            val disponibles = espaciosDelTipo(nuevoTipo)
                            espacioSeleccionado = if (disponibles.size == 1) disponibles.first() else null
                        }
                    )

                    if (tipoSeleccionado != null) {
                        SelectorDesplegable(
                            etiqueta = "Espacio específico",
                            valorActual = espacioSeleccionado?.nombre ?: "Seleccionar espacio...",
                            opciones = listaEspacios,
                            textoOpcion = { it.nombre },
                            alSeleccionar = { espacioSeleccionado = it }
                        )
                    }

                    OutlinedTextField(
                        value = descripcion,
                        onValueChange = { if (it.length <= 500) descripcion = it },
                        label = { Text("Descripción del problema") },
                        placeholder = { Text("Ej: Proyector sin señal o silla dañada...") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Evidencia fotográfica (opcional)",
                        style = MaterialTheme.typography.labelLarge
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { abrirCamaraConPermiso() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Cámara")
                        }

                        OutlinedButton(
                            onClick = { galeriaLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Galería")
                        }
                    }

                    if (fotoUri != null) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            ImagenEvidencia(
                                uriString = fotoUri!!,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            IconButton(
                                onClick = { fotoUri = null },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Quitar foto",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(4.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (mensajeError != null) {
                        Text(
                            text = mensajeError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.enviarReporte(
                                usuarioId = usuarioId,
                                espacioId = espacioSeleccionado?.nombre.orEmpty(),
                                descripcion = descripcion,
                                fotoUri = fotoUri
                            )
                        },
                        enabled = !cargando && espacioSeleccionado != null && descripcion.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (cargando) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.height(20.dp).width(20.dp)
                            )
                        } else {
                            Text("Enviar reporte")
                        }
                    }
                }
            }
        }

        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = "Mis reportes enviados (${reportes.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (reportes.isEmpty()) {
            item {
                Text(
                    text = "Aún no has enviado reportes de incidencia.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(reportes, key = { it.id }) { reporte ->
                TarjetaReporteUsuario(reporte = reporte)
            }
        }
    }
}

@Composable
fun VistaReportesAdmin(
    administradorId: Int,
    viewModel: ReporteViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val todosLosReportes by viewModel.todosLosReportes.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val mensajeExito by viewModel.mensajeExito.collectAsState()
    val mensajeError by viewModel.mensajeError.collectAsState()

    var reporteAResponder by remember { mutableStateOf<ReporteConAutor?>(null) }
    var textoRespuesta by remember { mutableStateOf("") }

    LaunchedEffect(administradorId) {
        if (administradorId > 0) {
            viewModel.cargarTodosLosReportes(administradorId)
        }
    }

    LaunchedEffect(mensajeExito) {
        mensajeExito?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            reporteAResponder = null
            textoRespuesta = ""
            viewModel.limpiarMensajes()
        }
    }

    if (reporteAResponder != null) {
        AlertDialog(
            onDismissRequest = { reporteAResponder = null },
            title = { Text("Responder incidencia") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Espacio: ${reporteAResponder!!.reporte.espacioId}",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = "Reportado por: ${reporteAResponder!!.autorNombre}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = textoRespuesta,
                        onValueChange = { if (it.length <= 500) textoRespuesta = it },
                        label = { Text("Comentario / Solución") },
                        placeholder = { Text("Ej: Equipo revisado y reparado.") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.responderReporte(
                            administradorId = administradorId,
                            reporteId = reporteAResponder!!.reporte.id,
                            comentario = textoRespuesta
                        )
                    },
                    enabled = !cargando && textoRespuesta.isNotBlank()
                ) {
                    Text("Guardar respuesta")
                }
            },
            dismissButton = {
                TextButton(onClick = { reporteAResponder = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Gestión de Reportes",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Revisa las incidencias reportadas y deja una respuesta.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (mensajeError != null) {
            item {
                Text(
                    text = mensajeError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (todosLosReportes.isEmpty()) {
            item {
                Text(
                    text = "No hay reportes de incidencia registrados.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        } else {
            items(todosLosReportes, key = { it.reporte.id }) { itemReporte ->
                TarjetaReporteAdmin(
                    item = itemReporte,
                    alResponder = {
                        textoRespuesta = itemReporte.reporte.comentarioAdmin.orEmpty()
                        reporteAResponder = itemReporte
                    }
                )
            }
        }
    }
}

@Composable
private fun TarjetaReporteUsuario(reporte: ReporteIncidencia) {
    val esRevisado = reporte.estado == EstadoReporte.REVISADO
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = reporte.espacioId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                AssistChip(
                    onClick = {},
                    label = { Text(if (esRevisado) "REVISADO" else "PENDIENTE") },
                    leadingIcon = if (esRevisado) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null
                )
            }

            Text(
                text = formatearFecha(reporte.fechaCreacion),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = reporte.descripcion,
                style = MaterialTheme.typography.bodyMedium
            )

            if (!reporte.fotoUri.isNullOrBlank()) {
                ImagenEvidencia(
                    uriString = reporte.fotoUri,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }

            if (!reporte.comentarioAdmin.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Respuesta del Administrador:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = reporte.comentarioAdmin,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaReporteAdmin(
    item: ReporteConAutor,
    alResponder: () -> Unit
) {
    val reporte = item.reporte
    val esRevisado = reporte.estado == EstadoReporte.REVISADO

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reporte.espacioId,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Por: ${item.autorNombre} • ${formatearFecha(reporte.fechaCreacion)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AssistChip(
                    onClick = {},
                    label = { Text(if (esRevisado) "REVISADO" else "PENDIENTE") }
                )
            }

            Text(
                text = reporte.descripcion,
                style = MaterialTheme.typography.bodyMedium
            )

            if (!reporte.fotoUri.isNullOrBlank()) {
                ImagenEvidencia(
                    uriString = reporte.fotoUri,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }

            if (!reporte.comentarioAdmin.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Tu respuesta:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = reporte.comentarioAdmin,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = alResponder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (esRevisado) "Editar respuesta" else "Responder y marcar revisado")
            }
        }
    }
}

@Composable
private fun <T> SelectorDesplegable(
    etiqueta: String,
    valorActual: String,
    opciones: List<T>,
    textoOpcion: (T) -> String,
    alSeleccionar: (T) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = valorActual,
            onValueChange = {},
            readOnly = true,
            label = { Text(etiqueta) },
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expandido = true }
        )
        DropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false }
        ) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(textoOpcion(opcion)) },
                    onClick = {
                        alSeleccionar(opcion)
                        expandido = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ImagenEvidencia(
    uriString: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, uriString) {
        value = withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(uriString)
                val opcionesBounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, opcionesBounds)
                }

                var escala = 1
                while (opcionesBounds.outWidth / escala > 900 || opcionesBounds.outHeight / escala > 900) {
                    escala *= 2
                }

                val opcionesDecode = BitmapFactory.Options().apply { inSampleSize = escala }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, opcionesDecode)?.asImageBitmap()
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = "Evidencia fotográfica",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

private fun crearArchivoFoto(context: Context): File {
    val carpeta = File(context.filesDir, "reportes_fotos")
    if (!carpeta.exists()) carpeta.mkdirs()
    return File(carpeta, "incidencia_${System.currentTimeMillis()}.jpg")
}

private fun crearUriParaCamara(context: Context): Uri {
    val archivo = crearArchivoFoto(context)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        archivo
    )
}

private fun copiarImagenAAlmacenamientoInterno(context: Context, uriOrigen: Uri): String? {
    return try {
        val destino = crearArchivoFoto(context)
        context.contentResolver.openInputStream(uriOrigen)?.use { input ->
            destino.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        Uri.fromFile(destino).toString()
    } catch (_: Exception) {
        null
    }
}

private fun formatearFecha(timestamp: Long): String {
    val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return formato.format(Date(timestamp))
}