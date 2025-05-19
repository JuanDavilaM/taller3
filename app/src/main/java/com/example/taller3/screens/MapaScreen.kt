package com.example.taller3.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.request.ErrorResult
import com.example.taller3.R
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.maps.android.compose.*

//pagina pricnicpal de la app, aqui suamos realtime y storage para las fotos de pefil para hacer el bono
@SuppressLint("MissingPermission")
@Composable
fun MapaScreen(navController: NavController) {
    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val database = FirebaseDatabase.getInstance().reference.child("usuarios")

    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var trackingEnabled by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf<LatLng?>(null) }
    var route by remember { mutableStateOf(listOf<LatLng>()) }
    var otherUsers by remember { mutableStateOf<Map<String, LatLng>>(emptyMap()) }
    var otherRoutes by remember { mutableStateOf<Map<String, List<LatLng>>>(emptyMap()) }
    var otherFotos by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var otherNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val userIcons = remember { mutableStateMapOf<String, BitmapDescriptor>() }
    var myName by remember { mutableStateOf("Tú") }
    var myFoto by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> permissionGranted = granted }
    )

    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val callback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    currentPosition = latLng
                    route = route + latLng

                    database.child(userId).updateChildren(
                        mapOf(
                            "lat" to latLng.latitude,
                            "lng" to latLng.longitude,
                            "enLinea" to true,
                            "ruta" to route.map {
                                mapOf("lat" to it.latitude, "lng" to it.longitude)
                            }
                        )
                    )
                }
            }
        }
    }

    DisposableEffect(trackingEnabled) {
        if (trackingEnabled) {
            val request = LocationRequest.create().apply {
                interval = 5000
                fastestInterval = 2000
                priority = Priority.PRIORITY_HIGH_ACCURACY
            }
            fusedLocationClient.requestLocationUpdates(request, callback, context.mainLooper)
        } else {
            fusedLocationClient.removeLocationUpdates(callback)
            database.child(userId).updateChildren(mapOf(
                "enLinea" to false,
                "ruta" to null
            ))
            route = emptyList()
        }
        onDispose {
            fusedLocationClient.removeLocationUpdates(callback)
            database.child(userId).updateChildren(mapOf(
                "enLinea" to false,
                "ruta" to null
            ))
            route = emptyList()
        }
    }

    LaunchedEffect(true) {
        //database realtime y storage en las fotos de perfil (aqui las guardaremos)
        database.limitToFirst(100).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val others = mutableMapOf<String, LatLng>()
                val routes = mutableMapOf<String, List<LatLng>>()
                val fotos = mutableMapOf<String, String>()
                val names = mutableMapOf<String, String>()

                snapshot.children.forEach { userSnap ->
                    val uid = userSnap.key ?: return@forEach
                    val lat = userSnap.child("lat").getValue(Double::class.java) ?: return@forEach
                    val lng = userSnap.child("lng").getValue(Double::class.java) ?: return@forEach

                    if (uid == userId) {
                        myName = userSnap.child("nombre").getValue(String::class.java) ?: "Tú"
                        myFoto = userSnap.child("fotoUrl").getValue(String::class.java)
                    } else if (userSnap.child("enLinea").value == true) {
                        others[uid] = LatLng(lat, lng)
                        val poly = userSnap.child("ruta").children.mapNotNull {
                            val plat = it.child("lat").getValue(Double::class.java)
                            val plng = it.child("lng").getValue(Double::class.java)
                            if (plat != null && plng != null) LatLng(plat, plng) else null
                        }
                        routes[uid] = poly

                        val foto = userSnap.child("fotoUrl").getValue(String::class.java)
                        fotos[uid] = foto ?: ""
                        val nombre = userSnap.child("nombre").getValue(String::class.java)
                        names[uid] = nombre ?: "Usuario"
                    }
                }
                otherUsers = others
                otherRoutes = routes
                otherFotos = fotos
                otherNames = names
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(LocalContext.current.getString(R.string.rastreo_activo))
            Spacer(Modifier.width(8.dp))
            Switch(checked = trackingEnabled, onCheckedChange = {
                trackingEnabled = it
            })
        }

        val cameraPositionState = rememberCameraPositionState {
            currentPosition?.let {
                position = CameraPosition.fromLatLngZoom(it, 16f)
            }
        }

        GoogleMap(
            modifier = Modifier.weight(1f),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = permissionGranted),
            uiSettings = MapUiSettings(zoomControlsEnabled = true)
        ) {
            currentPosition?.let {
                val myIcon = remember { mutableStateOf<BitmapDescriptor?>(null) }
                LaunchedEffect(myFoto) {
                    myFoto?.let { url ->
                        myIcon.value = getBitmapDescriptorFromUrl(context, url)
                    }
                }
                Marker(
                    state = MarkerState(position = it),
                    title = myName,
                    icon = myIcon.value ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                )
                Polyline(points = route, color = Color.Blue)
            }

            otherUsers.forEach { (id, loc) ->
                val url = otherFotos[id]
                if (url != null && !userIcons.containsKey(id)) {
                    LaunchedEffect(url) {
                        val icon = getBitmapDescriptorFromUrl(context, url)
                        userIcons[id] = icon
                    }
                }
                val icon = userIcons[id] ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                Marker(
                    state = MarkerState(position = loc),
                    title = otherNames[id] ?: "Usuario",
                    icon = icon
                )
                Polyline(points = otherRoutes[id] ?: emptyList(), color = Color.Gray)
            }
        }
    }
}
//para marcadores personalizados y el poder hacer el bono  usamos bitmap
suspend fun getBitmapDescriptorFromUrl(context: Context, imageUrl: String): BitmapDescriptor {
    val loader = ImageLoader(context)
    val request = ImageRequest.Builder(context)
        .data(imageUrl)
        .allowHardware(false)
        .build()

    val result = loader.execute(request)

    return if (result is SuccessResult) {
        val drawable = result.drawable
        val resized = drawable.toBitmap().let { Bitmap.createScaledBitmap(it, 120, 120, false) }
        BitmapDescriptorFactory.fromBitmap(resized)
    } else {
        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
    }
}

fun Drawable.toBitmap(): Bitmap {
    if (this is BitmapDrawable) return bitmap
    val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
}
