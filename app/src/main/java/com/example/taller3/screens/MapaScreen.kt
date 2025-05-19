package com.example.taller3.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.maps.android.compose.*

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

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> permissionGranted = granted }
    )

    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Rastreo de ubicación
    DisposableEffect(trackingEnabled) {
        if (trackingEnabled) {
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { location ->
                        val latLng = LatLng(location.latitude, location.longitude)
                        currentPosition = latLng
                        route = route + latLng

                        database.child(userId).updateChildren(
                            mapOf(
                                "lat" to latLng.latitude,
                                "lng" to latLng.longitude,
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

            val request = LocationRequest.create().apply {
                interval = 5000
                fastestInterval = 2000
                priority = Priority.PRIORITY_HIGH_ACCURACY
            }

            fusedLocationClient.requestLocationUpdates(request, callback, context.mainLooper)

            onDispose {
                fusedLocationClient.removeLocationUpdates(callback)
                database.child(userId).updateChildren(mapOf(
                    "enLinea" to false,
                    "ruta" to null
                ))
            }
        } else {
            database.child(userId).updateChildren(mapOf(
                "enLinea" to false,
                "ruta" to null
            ))
        }
        onDispose { }
    }

    // Escuchar otros usuarios
    LaunchedEffect(true) {
        database.limitToFirst(100).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val others = mutableMapOf<String, LatLng>()
                val routes = mutableMapOf<String, List<LatLng>>()
                snapshot.children.forEach { userSnap ->
                    val uid = userSnap.key ?: return@forEach
                    if (uid != userId && userSnap.child("enLinea").value == true) {
                        val lat = userSnap.child("lat").getValue(Double::class.java) ?: return@forEach
                        val lng = userSnap.child("lng").getValue(Double::class.java) ?: return@forEach
                        others[uid] = LatLng(lat, lng)

                        val poly = userSnap.child("ruta").children.mapNotNull {
                            val plat = it.child("lat").getValue(Double::class.java)
                            val plng = it.child("lng").getValue(Double::class.java)
                            if (plat != null && plng != null) LatLng(plat, plng) else null
                        }
                        routes[uid] = poly
                    }
                }
                otherUsers = others
                otherRoutes = routes
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // Interfaz
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Rastreo activo")
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
            // Marcador propio
            currentPosition?.let {
                Marker(
                    state = MarkerState(position = it),
                    title = "Tú",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                )
                Polyline(points = route, color = androidx.compose.ui.graphics.Color.Blue)
            }

            // Marcadores de otros
            otherUsers.forEach { (id, loc) ->
                Marker(
                    state = MarkerState(position = loc),
                    title = "Usuario",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                )
                Polyline(points = otherRoutes[id] ?: emptyList(), color = androidx.compose.ui.graphics.Color.Gray)
            }
        }
    }
}
