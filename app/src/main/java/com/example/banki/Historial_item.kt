package com.example.banki


import com.google.firebase.Timestamp

//inicializamos con valores para que no nos de un crasheo
data class Historial_item(
    var fecha: String? = "",
    var correo1: String? = "",
    var operacion: String? = "",
    var correo2: String? = "",
    var cifra: Double = 0.0,
    var cancelable: String? = "",
    var added_date: Timestamp? = Timestamp.now()
)