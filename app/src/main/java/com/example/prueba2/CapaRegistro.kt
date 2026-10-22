package com.example.prueba2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

class CapaRegistro: ComponentActivity()  {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            val context = LocalContext.current

            var correo by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }

            val correoEsValido = correoValido(correo)
            val passwordEsValida = passwordValida(password)

            var intentoGuardar by remember { mutableStateOf(false) }

            val errorCorreo = intentoGuardar && !correoEsValido
            val errorPassword = intentoGuardar && !passwordEsValida

            Column(Modifier.padding(top = 100.dp, start = 32.dp, end = 32.dp )) {
                Text(
                    modifier = Modifier.padding(start = 110.dp),
                    text = "REGISTRO",
                    color = Color(0xFF6750A4)
                )

                TextField(
                    value = correo,
                    onValueChange = { correo = it },
                    label = { Text("Ingrese Correo") },
                    singleLine = true,
                    isError = errorCorreo,
                    supportingText = {
                        if(errorCorreo){
                            val mensajeCorreo = when {
                                correo.isBlank() -> "CORREO OBLIGATORIO!"
                                !correo.trim().endsWith("@gmail.com") -> "DEBE SER @gmail.com"
                                else -> "CORREO DEMASIADO CORTO"
                            }
                            Text(text = mensajeCorreo, color = MaterialTheme.colorScheme.error)
                        }
                    })

                TextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    singleLine = true, // Restringe que al apretar enter, se salte abajo
                    visualTransformation = PasswordVisualTransformation(),
                    isError = errorPassword,
                    supportingText = {
                        if(errorPassword){
                            val mensajePassword = when {
                                password.isBlank() -> "CONTRASEÑA OBLIGATORIA!"
                                else -> "DEBE POSEER AL MENOS 8 CARACTERES"
                            }
                            Text(text = mensajePassword, color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                Column(Modifier.padding(horizontal = 90.dp)) {
                    Button(onClick = {
                        intentoGuardar = true
                        if (correoEsValido && passwordEsValida){
                            Toast.makeText(context, "REGISTRO EXITOSO!", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@CapaRegistro, MainActivity::class.java)
                            startActivity(intent)
                        }else{
                            Toast.makeText(context, "COMPLETE LOS CAMPOS", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text("Ingresar")
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
        return password.length >= 8
    }
}