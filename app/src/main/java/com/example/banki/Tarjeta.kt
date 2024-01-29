package com.example.banki

import com.google.firebase.Timestamp

//inicializamos con valores para que no nos de un crasheo
data class Tarjeta(
    var Numero: String? = "",
    var fechacaducidad: String? = "",
    var numerosecreto: String? = "",
    var Estado: String? = "",
    var added_date: Timestamp? = Timestamp.now()
)

