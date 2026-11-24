package com.example.prueba2.ui.reportes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.prueba2.data.local.ReporteConAutor
import com.example.prueba2.data.local.ReporteIncidencia
import com.example.prueba2.data.repository.ReporteRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ReporteViewModel(
    private val repository: ReporteRepository
) : ViewModel() {

    private val _reportesUsuario = MutableStateFlow<List<ReporteIncidencia>>(emptyList())
    val reportesUsuario: StateFlow<List<ReporteIncidencia>> = _reportesUsuario.asStateFlow()

    private val _todosLosReportes = MutableStateFlow<List<ReporteConAutor>>(emptyList())
    val todosLosReportes: StateFlow<List<ReporteConAutor>> = _todosLosReportes.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _mensajeExito = MutableStateFlow<String?>(null)
    val mensajeExito: StateFlow<String?> = _mensajeExito.asStateFlow()

    private val _mensajeError = MutableStateFlow<String?>(null)
    val mensajeError: StateFlow<String?> = _mensajeError.asStateFlow()

    private var jobUsuario: Job? = null
    private var jobAdmin: Job? = null

    fun cargarReportesUsuario(usuarioId: Int) {
        jobUsuario?.cancel()
        jobUsuario = viewModelScope.launch {
            repository.obtenerPorUsuario(usuarioId)
                .catch { e -> _mensajeError.value = e.message ?: "Error al cargar tus reportes." }
                .collect { lista -> _reportesUsuario.value = lista }
        }
    }

    fun cargarTodosLosReportes(administradorId: Int) {
        jobAdmin?.cancel()
        jobAdmin = viewModelScope.launch {
            repository.obtenerTodosConAutor(administradorId)
                .catch { e -> _mensajeError.value = e.message ?: "Error al cargar los reportes." }
                .collect { lista -> _todosLosReportes.value = lista }
        }
    }

    fun enviarReporte(
        usuarioId: Int,
        espacioId: String,
        descripcion: String,
        fotoUri: String?
    ) {
        viewModelScope.launch {
            _cargando.value = true
            _mensajeError.value = null
            _mensajeExito.value = null
            try {
                val nuevoReporte = ReporteIncidencia(
                    usuarioId = usuarioId,
                    espacioId = espacioId,
                    descripcion = descripcion,
                    fotoUri = fotoUri
                )
                repository.enviarReporte(nuevoReporte)
                _mensajeExito.value = "Reporte de incidencia enviado correctamente."
            } catch (e: Exception) {
                _mensajeError.value = e.message ?: "No se pudo enviar el reporte."
            } finally {
                _cargando.value = false
            }
        }
    }

    fun responderReporte(
        administradorId: Int,
        reporteId: Long,
        comentario: String
    ) {
        viewModelScope.launch {
            _cargando.value = true
            _mensajeError.value = null
            _mensajeExito.value = null
            try {
                repository.responderReporte(administradorId, reporteId, comentario)
                _mensajeExito.value = "Respuesta enviada y reporte marcado como revisado."
            } catch (e: Exception) {
                _mensajeError.value = e.message ?: "No se pudo guardar la respuesta."
            } finally {
                _cargando.value = false
            }
        }
    }

    fun limpiarMensajes() {
        _mensajeExito.value = null
        _mensajeError.value = null
    }
}

class ReporteViewModelFactory(
    private val repository: ReporteRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReporteViewModel::class.java)) {
            return ReporteViewModel(repository) as T
        }
        throw IllegalArgumentException("ViewModel desconocido: ${modelClass.name}")
    }
}