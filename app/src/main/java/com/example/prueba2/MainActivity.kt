package com.example.prueba2

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.prueba2.ui.theme.Prueba2Theme
import kotlin.jvm.java

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var correo by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }

            Column(Modifier.padding(top = 100.dp, start = 32.dp, end = 32.dp )){
                Text(
                    modifier = Modifier.padding(start = 90.dp),
                    text = "MARCELA PAZ KT",
                    color = Color(0xFF6750A4))

                TextField(
                    value = correo,
                    onValueChange = { correo = it },
                    label = { Text("Correo") })

                TextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") })

                Column(Modifier.padding(horizontal = 90.dp)) {
                    Button(onClick = {
                        val intent = Intent( this@MainActivity, CapaPrueba::class.java)
                        startActivity(intent)
                    }) {
                        Text("Ingresar")
                    }
                }

                Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text("¿No estas registrado?")

                    TextButton(onClick = {
                        val intent = Intent( this@MainActivity, CapaRegistro::class.java)
                        startActivity(intent)
                    }) {
                        Text(
                            text = "Ingrese aqui",
                            color = Color.Blue)
                    }
                }
            }

        }
    }
}