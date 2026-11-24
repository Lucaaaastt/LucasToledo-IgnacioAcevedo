package com.example.prueba2.data.repository

import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.prueba2.data.local.AppDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Reanuda la cola al abrir la app; no necesita un servicio permanente en segundo plano. */
internal fun ComponentActivity.sincronizarPendientes(usuarioId: Int) {
    if (usuarioId <= 0) return
    val database = AppDatabase.getDatabase(applicationContext)
    val reservas = ReservasCompartidas(database)
    val reportes = ReporteRepository(database, applicationContext)
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                try {
                    reservas.sincronizar(usuarioId)
                    reportes.sincronizar(usuarioId)
                } catch (cancelacion: CancellationException) {
                    throw cancelacion
                } catch (_: Exception) {
                    // La cola sigue en Room y se reintenta al recuperar el servicio.
                }
                delay(5_000)
            }
        }
    }
}
