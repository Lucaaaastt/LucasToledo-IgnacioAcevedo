package com.example.prueba2.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.prueba2.R
import com.example.prueba2.data.local.AppDatabase
import com.example.prueba2.ui.menu.Menu
import com.example.prueba2.ui.menu.MenuADM
import com.example.prueba2.ui.registro.CapaRegistro
import com.example.prueba2.ui.registro.CapaRegistroAdm
import kotlin.jvm.java
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

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

            Column(Modifier.padding(top = 100.dp, start = 32.dp, end = 32.dp )){
                Text(
                    modifier = Modifier.padding(start = 90.dp),
                    text = "MARCELA PAZ KT",
                    color = Color(0xFF6750A4)
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
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Text(text = "Correo o contraseña erróneos!",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                TextField(
                    value = correo,
                    onValueChange = { correo = it; errorLoginVisible = false},
                    label = { Text("Correo") },
                    singleLine = true,
                    isError = errorCorreo,
                    supportingText = {
                        if(errorCorreo){
                            Text("CORREO ES OBLIGATORIO!", color = MaterialTheme.colorScheme.error)
                        }
                    })

                TextField(
                    value = password,
                    onValueChange = { password = it; errorLoginVisible = false },
                    label = { Text("Contraseña") },
                    singleLine = true, // Restringe que al apretar enter, se salte abajo
                    visualTransformation = if (isPasswordVisible) {
                        VisualTransformation.None
                    }else{
                        PasswordVisualTransformation()
                    },
                    isError = errorPassword,
                    supportingText = {
                        if(errorPassword){
                            Text("CONTRASEÑA ES OBLIGATORIA!", color = MaterialTheme.colorScheme.error)
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
                        errorLoginVisible = false
                        if(correo.isNotBlank() && password.isNotBlank()){
                            scope.launch(Dispatchers.IO){
                                val dao = db.usuarioDao()
                                val usuarioEncontrado = dao.login(correo.trim(), password)

                                withContext(Dispatchers.Main){
                                    if(usuarioEncontrado != null){
                                        Toast.makeText(context, "BIENVENIDO ${usuarioEncontrado.nombre}", Toast.LENGTH_LONG).show()

                                        val destinoClass = if (usuarioEncontrado.rol == "ADMIN"){
                                            MenuADM::class.java
                                        }else{
                                            Menu::class.java
                                        }
                                        val intent = Intent( this@MainActivity, destinoClass).apply {
                                            putExtra("USER_ID", usuarioEncontrado.id)
                                            putExtra("USER_ROL", usuarioEncontrado.rol)
                                            putExtra("USER_NOMBRE", usuarioEncontrado.nombre)
                                        }
                                        startActivity(intent)
                                    }else  {
                                        errorLoginVisible = true
                                    }
                                }
                            }
                        }
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

                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 100.dp, start = 32.dp, end = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                    ){
                    Image(
                        painter = painterResource(id = R.drawable.insignia),
                        contentDescription = "Acceso Administracion",
                        modifier = Modifier.size(48.dp).clickable{
                            val intent = Intent( this@MainActivity, CapaRegistroAdm::class.java)
                            startActivity(intent)
                        }
                    )
                }
            }

        }
    }
}
