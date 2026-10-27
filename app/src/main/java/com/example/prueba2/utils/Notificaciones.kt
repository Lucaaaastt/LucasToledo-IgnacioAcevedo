package com.example.prueba2.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.prueba2.R
import com.example.prueba2.data.local.EstadoReserva

object Notificaciones {
    private const val CANAL_ID = "canal_reservas"
    private const val CANAL_NOMBRE = "Notificaciones Reservas"

    // Se ejecuta una vez al iniciar la vista o app para registrar el canal en Android
    fun crearCanalNotificaciones(context: Context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val canal = NotificationChannel(
                CANAL_ID,
                CANAL_NOMBRE,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones sobre el estado de reserva"
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(canal)
        }
    }

    // Se genera notificacion nativa en la barra de estado
    fun enviarNotificacionReserva(context: Context, espacio: String, estado: EstadoReserva){
        val esAprobada = estado == EstadoReserva.APROBADA
        val titulo = if (esAprobada) "SOLICITUD APROBADA!!" else "SOLICITUD RECHAZADA!!"
        val mensaje = if (esAprobada){
            "La reserva para \"$espacio\" ha sido aprobada!"
        }else {
            "La reserva para \"$espacio\" ha sido rechazada!"
        }

        val builder = NotificationCompat.Builder(context, CANAL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // ICONO DEL SISTEMA/APP
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}