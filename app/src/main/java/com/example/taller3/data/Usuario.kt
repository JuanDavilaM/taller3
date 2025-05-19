package com.example.taller3.data

data class Usuario(
    var uid: String = "",
    var nombre: String = "",
    var email: String = "",
    var telefono: String = "",
    var enLinea: Boolean = false,
    var lat: Double = 0.0,
    var lng: Double = 0.0,
    var fotoUrl: String = ""
) {
    constructor() : this("", "", "", "", false, 0.0, 0.0, "")
}
