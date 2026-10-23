package com.example.prueba2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class Menu : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                                val intent = Intent(this@Menu, MainActivity::class.java)
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
                    modifier = Modifier.padding(start = 90.dp),
                    text = "MARCELA PAZ KT",
                    color = Color(0xFF6750A4)
                )

                Column(Modifier.padding(horizontal = 90.dp)) {
                    Button(onClick = {
                        mostrarAviso = true
                    }) {
                        Text("CERRAR SESION")
                    }
                }
            }
        }
    }
}