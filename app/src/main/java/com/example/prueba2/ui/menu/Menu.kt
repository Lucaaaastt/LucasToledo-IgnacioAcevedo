package com.example.prueba2.ui.menu

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.prueba2.ui.login.MainActivity
import com.example.prueba2.ui.reservas.MisReservas
import com.example.prueba2.ui.reservas.NuevaReserva

class Menu : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                android.graphics.Color.TRANSPARENT
            )
        )

        val usuarioId = intent.getIntExtra("USER_ID", -1)

        val nombreUsuario = intent.getStringExtra("USER_NOMBRE")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: "Solicitante"

        setContent {
            val context = LocalContext.current
            var mostrarAviso by remember { mutableStateOf(false) }
            var mostrarMenuUsuario by remember { mutableStateOf(false) }
            var itemSeleccionado by remember { mutableIntStateOf(0) }

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

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {},
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        actions = {
                            Box {
                                IconButton(onClick = { mostrarMenuUsuario = true }) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = "Menú de usuario"
                                    )
                                }

                                DropdownMenu(
                                    expanded = mostrarMenuUsuario,
                                    onDismissRequest = { mostrarMenuUsuario = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Cerrar sesión") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                                contentDescription = null
                                            )
                                        },
                                        onClick = {
                                            mostrarMenuUsuario = false
                                            mostrarAviso = true
                                        }
                                    )
                                }
                            }
                        }
                    )
                },
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = itemSeleccionado == 0,
                            onClick = { itemSeleccionado = 0 },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                            label = { Text("Inicio") }
                        )
                        NavigationBarItem(
                            selected = itemSeleccionado == 1,
                            onClick = { itemSeleccionado = 1 },
                            icon = { Icon(Icons.Default.Folder, contentDescription = "Reportes") },
                            label = { Text("Reportes") }
                        )
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 32.dp, vertical = 24.dp)
                ) {
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

                    Button(
                        onClick = {
                            startActivity(Intent(this@Menu, NuevaReserva::class.java).apply {
                                putExtra("USER_ID", usuarioId)
                            })
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Nueva reserva")
                    }

                    OutlinedButton(
                        onClick = {
                            startActivity(Intent(this@Menu, MisReservas::class.java).apply {
                                putExtra("USER_ID", usuarioId)
                            })
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Text("Mis reservas")
                    }
                }
            }
        }
    }
}