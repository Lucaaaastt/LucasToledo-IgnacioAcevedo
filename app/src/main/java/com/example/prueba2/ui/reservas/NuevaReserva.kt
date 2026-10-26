package com.example.prueba2.ui.reservas

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.prueba2.data.local.AppDatabase
import com.example.prueba2.data.repository.ReservaRepository
import com.example.prueba2.ui.login.MainActivity
import com.example.prueba2.ui.theme.Prueba2Theme

class NuevaReserva : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                android.graphics.Color.TRANSPARENT
            )
        )

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
                var mostrarMenuUsuario by remember { mutableStateOf(false) }
                var mostrarAviso by remember { mutableStateOf(false) }
                val context = LocalContext.current
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
                                    val intent = Intent(this@NuevaReserva, MainActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    }
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
                                onClick = {
                                    itemSeleccionado = 0
                                    finish()
                                },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                                label = { Text("Inicio") }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
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
    }
}