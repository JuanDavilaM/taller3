package com.example.taller3.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.taller3.R
import com.example.taller3.data.Usuario
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

@Composable
fun PerfilScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    val userId = user?.uid ?: ""
    val dbRef = FirebaseDatabase.getInstance().getReference("usuarios").child(userId)

    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var contrasenaActual by remember { mutableStateOf("") }
    var nuevaContrasena by remember { mutableStateOf("") }

    // Cargar datos desde la base de datos
    LaunchedEffect(userId) {
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val usuario = snapshot.getValue(Usuario::class.java)
                if (usuario != null) {
                    nombre = usuario.nombre
                    telefono = usuario.telefono
                    email = usuario.email
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, context.getString(R.string.error_cargar_perfil), Toast.LENGTH_SHORT).show()
            }
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(context.getString(R.string.mi_perfil), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text(context.getString(R.string.nombre)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {},
            label = { Text(context.getString(R.string.correo_no_editable)) },
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = telefono,
            onValueChange = { telefono = it },
            label = { Text(context.getString(R.string.telefono)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                dbRef.child("nombre").setValue(nombre)
                dbRef.child("telefono").setValue(telefono)
                Toast.makeText(context, context.getString(R.string.datos_actualizados), Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(context.getString(R.string.guardar_cambios))
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = contrasenaActual,
            onValueChange = { contrasenaActual = it },
            label = { Text(context.getString(R.string.contrasena_actual)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = nuevaContrasena,
            onValueChange = { nuevaContrasena = it },
            label = { Text(context.getString(R.string.nueva_contrasena)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                val credential = EmailAuthProvider.getCredential(email, contrasenaActual)

                user?.reauthenticate(credential)
                    ?.addOnSuccessListener {
                        user.updatePassword(nuevaContrasena)
                            .addOnSuccessListener {
                                Toast.makeText(context, context.getString(R.string.contrasena_actualizada), Toast.LENGTH_SHORT).show()
                                contrasenaActual = ""
                                nuevaContrasena = ""
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, context.getString(R.string.error_actualizar_contrasena), Toast.LENGTH_SHORT).show()
                            }
                    }
                    ?.addOnFailureListener {
                        Toast.makeText(context, context.getString(R.string.contrasena_actual_incorrecta), Toast.LENGTH_SHORT).show()
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(context.getString(R.string.cambiar_contrasena))
        }
    }
}
