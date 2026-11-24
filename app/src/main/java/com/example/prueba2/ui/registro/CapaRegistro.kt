package com.example.prueba2.ui.registro

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.prueba2.data.remote.ErrorUsuarios
import com.example.prueba2.data.remote.UsuariosApi
import com.example.prueba2.ui.login.MainActivity
import kotlinx.coroutines.launch

class CapaRegistro: ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val api = remember { UsuariosApi() }
            var isPasswordVisible by remember { mutableStateOf(false) }

            var nombre by remember { mutableStateOf("") }
            var correo by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }

            val nombreEsValido = nombre.trim().isNotEmpty()
            val correoEsValido = correoValido(correo)
            val passwordEsValida = passwordValida(password)

            var guardando by remember { mutableStateOf(false) }
            var intentoGuardar by remember { mutableStateOf(false) }

            val errorNombre = intentoGuardar && !nombreEsValido
            val errorCorreo = intentoGuardar && !correoEsValido
            val errorPassword = intentoGuardar && !passwordEsValida

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("REGISTRO") },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Volver atrás"
                                )
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(horizontal = 32.dp, vertical = 16.dp)
                ) {
                    TextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Ingrese Nombre") },
                        singleLine = true,
                        isError = errorNombre,
                        supportingText = {
                            if (errorNombre) {
                                Text(
                                    text = "NOMBRE OBLIGATORIO!",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )

                    TextField(
                        value = correo,
                        onValueChange = { correo = it },
                        label = { Text("Ingrese Correo") },
                        singleLine = true,
                        isError = errorCorreo,
                        supportingText = {
                            if (errorCorreo) {
                                val mensajeCorreo = when {
                                    correo.isBlank() -> "CORREO OBLIGATORIO!"
                                    !correo.trim().endsWith("@gmail.com") -> "DEBE SER @gmail.com"
                                    else -> "CORREO DEMASIADO CORTO"
                                }
                                Text(text = mensajeCorreo, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )

                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible){
                            VisualTransformation.None
                        }else{
                            PasswordVisualTransformation()
                        },
                        isError = errorPassword,
                        supportingText = {
                            if (errorPassword) {
                                val mensajePassword = when {
                                    password.isEmpty() -> "CONTRASEÑA OBLIGATORIA!"
                                    password.isBlank() -> "LA CONTRASEÑA NO PUEDE CONTENER SOLO ESPACIOS"
                                    else -> "DEBE POSEER AL MENOS 8 CARACTERES"
                                }
                                Text(
                                    text = mensajePassword,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)){
                        Checkbox(
                            checked = isPasswordVisible,
                            onCheckedChange = { isPasswordVisible = it }
                        )
                        Text(text = "Mostrar Contraseña")
                    }

                    Column(Modifier.padding(horizontal = 90.dp)) {
                        Button(onClick = {
                            intentoGuardar = true
                            if (nombreEsValido && correoEsValido && passwordEsValida) {
                                guardando = true
                                val nombreIngresado = nombre
                                val correoIngresado = correo
                                val passwordIngresada = password
                                scope.launch {
                                    try {
                                        api.registrar(nombreIngresado, correoIngresado, passwordIngresada)
                                        Toast.makeText(context, "REGISTRO EXITOSO!", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(this@CapaRegistro, MainActivity::class.java))
                                        finish()
                                    } catch (error: ErrorUsuarios) {
                                        Toast.makeText(context, error.message, Toast.LENGTH_LONG).show()
                                    } finally {
                                        guardando = false
                                    }
                                }
                            } else {
                                Toast.makeText(context, "COMPLETE LOS CAMPOS", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }, enabled = !guardando) {
                            Text(if (guardando) "Guardando…" else "Ingresar")
                        }
                    }
                }
            }
        }
    }

    private fun correoValido(correo: String): Boolean {
        val correoLimpio = correo.trim()
        return correoLimpio.endsWith("@gmail.com") && correoLimpio.length > 18
    }

    private fun passwordValida(password: String): Boolean {
        return password.isNotBlank() && password.length >= 8
    }
}
