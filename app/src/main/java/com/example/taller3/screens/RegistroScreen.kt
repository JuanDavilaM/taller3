package com.example.taller3.screens

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.taller3.R
import com.example.taller3.data.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RegistroScreen(navController: NavController) {
    val context = LocalContext.current
    val nombre = remember { mutableStateOf("") }
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val telefono = remember { mutableStateOf("") }
    val isLoading = remember { mutableStateOf(false) }
    val photoUri = remember { mutableStateOf<Uri?>(null) }
    val tempImageUri = remember { mutableStateOf<Uri?>(null) }

    //pasos para hacer lo de la foto y guardarla en firebase
    //esto se usara para los marcadores personalizados y el bono
    fun createImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoUri.value = tempImageUri.value
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val imageFile = createImageFile(context)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                imageFile
            )
            tempImageUri.value = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, context.getString(R.string.permiso_camara_denegado), Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(context.getString(R.string.registro), style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = nombre.value,
            onValueChange = { nombre.value = it },
            label = { Text(context.getString(R.string.nombre)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = email.value,
            onValueChange = { email.value = it },
            label = { Text(context.getString(R.string.correo_electronico)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password.value,
            onValueChange = { password.value = it },
            label = { Text(context.getString(R.string.contrasena)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = telefono.value,
            onValueChange = { telefono.value = it },
            label = { Text(context.getString(R.string.telefono)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }) {
            Text(context.getString(R.string.tomar_foto))
        }

        photoUri.value?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Image(
                painter = rememberAsyncImagePainter(it),
                contentDescription = null,
                modifier = Modifier.size(120.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                //valores para nuestro firebase utilizadas para cumplir requrimientos del taller3
                val auth = FirebaseAuth.getInstance()
                val db = FirebaseDatabase.getInstance().getReference("usuarios")
                val storage = FirebaseStorage.getInstance().reference

                val correo = email.value
                val pass = password.value
                val nombreVal = nombre.value
                val tel = telefono.value
                val foto = photoUri.value

                if (correo.isNotBlank() && pass.isNotBlank() && nombreVal.isNotBlank() && tel.isNotBlank() && foto != null) {
                    isLoading.value = true
                    auth.createUserWithEmailAndPassword(correo, pass)
                        .addOnSuccessListener { result ->
                            val uid = result.user?.uid ?: return@addOnSuccessListener
                            val fotoRef = storage.child("fotos_perfil/$uid.jpg")

                            fotoRef.putFile(foto).addOnSuccessListener {
                                fotoRef.downloadUrl.addOnSuccessListener { url ->
                                    val usuario = Usuario(
                                        uid = uid,
                                        nombre = nombreVal,
                                        email = correo,
                                        telefono = tel,
                                        fotoUrl = url.toString()
                                    )
                                    db.child(uid).setValue(usuario)
                                        .addOnSuccessListener {
                                            Toast.makeText(context, context.getString(R.string.usuario_registrado), Toast.LENGTH_SHORT).show()
                                            navController.navigate("main")
                                        }
                                }
                            }
                        }
                        .addOnFailureListener {
                            isLoading.value = false
                            Toast.makeText(context, context.getString(R.string.error_generico, it.message), Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(context, context.getString(R.string.completa_campos), Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading.value
        ) {
            Text(context.getString(R.string.registrarse))
        }
    }
}
