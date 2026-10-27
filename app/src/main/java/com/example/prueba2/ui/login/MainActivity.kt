package com.example.prueba2.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.prueba2.R
import com.example.prueba2.data.local.AppDatabase
import com.example.prueba2.ui.menu.Menu
import com.example.prueba2.ui.menu.MenuADM
import com.example.prueba2.ui.registro.CapaRegistro
import com.example.prueba2.ui.registro.CapaRegistroAdm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LoginPantalla()
        }
    }
}

@Composable
fun LoginPantalla() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var intentoGuardar by remember { mutableStateOf(false) }
    var errorLoginVisible by remember { mutableStateOf(false) }

    val errorCorreo = intentoGuardar && correo.isBlank()
    val errorPassword = intentoGuardar && password.isBlank()

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 28.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "MARCELA PAZ KT",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "GESTIÓN DE RESERVAS",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            AnimatedVisibility(
                visible = errorLoginVisible,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "Correo o contraseña erróneos!",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            OutlinedTextField(
                value = correo,
                onValueChange = {
                    correo = it
                    errorLoginVisible = false
                },
                label = { Text("Correo") },
                singleLine = true,
                isError = errorCorreo,
                supportingText = {
                    if (errorCorreo) {
                        Text("CORREO ES OBLIGATORIO!", color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorLoginVisible = false
                },
                label = { Text("Contraseña") },
                singleLine = true,
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                isError = errorPassword,
                supportingText = {
                    if (errorPassword) {
                        Text("CONTRASEÑA ES OBLIGATORIA!", color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = isPasswordVisible,
                    onCheckedChange = { isPasswordVisible = it }
                )
                Text(
                    text = "Mostrar Contraseña",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    intentoGuardar = true
                    errorLoginVisible = false
                    if (correo.isNotBlank() && password.isNotBlank()) {
                        scope.launch(Dispatchers.IO) {
                            val dao = db.usuarioDao()
                            val usuarioEncontrado = dao.login(correo.trim(), password)

                            withContext(Dispatchers.Main) {
                                if (usuarioEncontrado != null) {
                                    Toast.makeText(
                                        context,
                                        "BIENVENIDO ${usuarioEncontrado.nombre}",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    val destinoClass = if (usuarioEncontrado.rol == "ADMIN") {
                                        MenuADM::class.java
                                    } else {
                                        Menu::class.java
                                    }
                                    val intent = Intent(context, destinoClass).apply {
                                        putExtra("USER_ID", usuarioEncontrado.id)
                                        putExtra("USER_ROL", usuarioEncontrado.rol)
                                        putExtra("USER_NOMBRE", usuarioEncontrado.nombre)
                                    }
                                    context.startActivity(intent)
                                } else {
                                    errorLoginVisible = true
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "Ingresar",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "¿No estás registrado?",
                    style = MaterialTheme.typography.bodyMedium
                )

                TextButton(onClick = {
                    val intent = Intent(context, CapaRegistro::class.java)
                    context.startActivity(intent)
                }) {
                    Text(
                        text = "Ingrese aquí",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Image(
                painter = painterResource(id = R.drawable.insignia),
                contentDescription = "Acceso Administración",
                modifier = Modifier
                    .size(80.dp)
                    .clickable {
                        val intent = Intent(context, CapaRegistroAdm::class.java)
                        context.startActivity(intent)
                    }
            )
        }
    }
}