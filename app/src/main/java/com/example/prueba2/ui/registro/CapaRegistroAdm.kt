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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

class CapaRegistroAdm: ComponentActivity() {

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
            var codigoSeguridad by remember { mutableStateOf("") }

            val nombreEsValido = nombre.trim().isNotEmpty()
            val correoEsValido = correoValido(correo)
            val passwordEsValida = passwordValida(password)
            val codigoEsValido = codigoSeguridad.isNotBlank()
            var codigoRechazado by remember { mutableStateOf(false) }

            var guardando by remember { mutableStateOf(false) }
            var errorRegistro by remember { mutableStateOf<String?>(null) }
            var intentoGuardar by remember { mutableStateOf(false) }

            val errorNombre = intentoGuardar && !nombreEsValido
            val errorCorreo = intentoGuardar && !correoEsValido
            val errorPassword = intentoGuardar && !passwordEsValida
            val errorCodigo = (intentoGuardar && !codigoEsValido) || codigoRechazado

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Registro Administracion") },
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
                        .verticalScroll(rememberScrollState())
                ) {
                    TextField(
                        enabled = !guardando,
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
                        enabled = !guardando,
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
                        enabled = !guardando,
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

                    TextField(
                        enabled = !guardando,
                        value = codigoSeguridad,
                        onValueChange = {
                            codigoSeguridad = it
                            codigoRechazado = false
                        },
                        label = { Text("Codigo Seguridad") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        isError = errorCodigo,
                        supportingText = {
                            if (errorCodigo) {
                                Text(
                                    text = if (codigoSeguridad.isBlank()) "CÓDIGO OBLIGATORIO!" else "CÓDIGO DE ADMIN INCORRECTO",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )

                    errorRegistro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Column(Modifier.padding(horizontal = 90.dp)) {
                        Button(onClick = {
                            if (guardando) return@Button
                            errorRegistro = null
                            intentoGuardar = true
                            if (nombreEsValido && correoEsValido && passwordEsValida && codigoEsValido) {
                                guardando = true
                                val nombreIngresado = nombre
                                val correoIngresado = correo
                                val passwordIngresada = password
                                val codigoIngresado = codigoSeguridad
                                codigoRechazado = false
                                scope.launch {
                                    try {
                                        api.registrar(nombreIngresado, correoIngresado, passwordIngresada, codigoIngresado)
                                        Toast.makeText(context, "REGISTRO EXITOSO!", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(this@CapaRegistroAdm, MainActivity::class.java))
                                        finish()
                                    } catch (error: ErrorUsuarios) {
                                        if (error.estadoHttp == 403 && codigoSeguridad == codigoIngresado) {
                                            codigoRechazado = true
                                        }
                                        errorRegistro = error.message ?: "No se pudo registrar la cuenta. Inténtalo nuevamente."
                                    } finally {
                                        guardando = false
                                    }
                                }
                            } else {
                                Toast.makeText(context, "COMPLETE LOS CAMPOS", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }, enabled = !guardando) {
                            Text(if (guardando) "Guardando…" else if (errorRegistro != null) "Reintentar" else "Ingresar")
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
