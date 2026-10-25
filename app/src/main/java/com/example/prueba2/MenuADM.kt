package com.example.prueba2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class MenuADM : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val nombreUsuario = intent.getStringExtra("USER_NOMBRE")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: "ADMIN"

        setContent {

            val context = LocalContext.current
            var mostrarAviso by remember { mutableStateOf(false) }

            if (mostrarAviso) {
                AlertDialog(
                    onDismissRequest = { mostrarAviso = false },
                    title = { Text("Cerrar sesión") },
                    text = { Text("¿Estás seguro de que deseas salir?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                mostrarAviso = false
                                Toast.makeText(context, "VOLVIENDO...", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this@MenuADM, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                        ) {
                            Text("Aceptar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { mostrarAviso = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            Column(Modifier.padding(top = 100.dp, start = 32.dp, end = 32.dp )) {
                Text(
                    text = "Marcela Paz",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Hola, $nombreUsuario",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                )

                OutlinedButton(
                    onClick = {
                        // Pendiente: conectar la pantalla de mis solicitudes.
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Text("Revisar Solicitudes")
                }

                TextButton(
                    onClick = { mostrarAviso = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {
                    Text("Cerrar sesión")
                }
            }
        }
    }
}
