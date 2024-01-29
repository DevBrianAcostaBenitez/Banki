package com.example.banki

import android.content.Intent
import android.icu.text.DecimalFormat
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class UserScreen : AppCompatActivity() {
    val db = FirebaseFirestore.getInstance()
    val usuarios = db.collection("Usuarios")
    val auten = FirebaseAuth.getInstance()
    val currentUser = auten.currentUser

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        if (currentUser == null) {
            val intentvolver = Intent(this@UserScreen, MainActivity::class.java)
            startActivity(intentvolver)
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_screen)
        val volver = findViewById<Button>(R.id.btvolver)
        val IngresarRetirar = findViewById<Button>(R.id.btIngresoRetiro)
        val ONG = findViewById<Button>(R.id.btONG)
        val Tarjetas = findViewById<Button>(R.id.btTarjetas)
        val Transferencias = findViewById<Button>(R.id.btTransferencia)
        val Subs = findViewById<Button>(R.id.btSuscripciones)
        val Historial = findViewById<Button>(R.id.btHistorial)
        getsaldoandname()
        //los botones nos llevaran a donde deseamos ir
        IngresarRetirar.setOnClickListener {
            val intentIngresoRetiro = Intent(this@UserScreen, IngresoRetiro::class.java)
            val extras = Bundle()
            intentIngresoRetiro.putExtras(extras)
            startActivity(intentIngresoRetiro)
        }
        ONG.setOnClickListener {
            val intentONG = Intent(this@UserScreen, GestionONG::class.java)
            val extras = Bundle()
            intentONG.putExtras(extras)
            startActivity(intentONG)
        }
        Tarjetas.setOnClickListener {
            val intentTarjetas = Intent(this@UserScreen, Tarjetas_lista::class.java)
            val extras = Bundle()
            intentTarjetas.putExtras(extras)
            startActivity(intentTarjetas)
        }
        Subs.setOnClickListener {
            val intentSuscripciones = Intent(this@UserScreen, Suscripciones::class.java)
            val extras = Bundle()
            intentSuscripciones.putExtras(extras)
            startActivity(intentSuscripciones)
        }
        Transferencias.setOnClickListener {
            val intentTransferencias = Intent(this@UserScreen, Transferencia::class.java)
            val extras = Bundle()
            intentTransferencias.putExtras(extras)
            startActivity(intentTransferencias)
        }
        Historial.setOnClickListener {
            val intentHistorial = Intent(this@UserScreen, Historial_View::class.java)
            val extras = Bundle()
            intentHistorial.putExtras(extras)
            startActivity(intentHistorial)
        }
        volver.setOnClickListener {
            FirebaseAuth.getInstance().signOut();
            val intentvolver = Intent(this@UserScreen, MainActivity::class.java)
            startActivity(intentvolver)
        }
    }

    //desabilitar boton de atras del movil, lo hacemos porque si venimos de un ingreso o un retiro entonces
    //pueden pasar cosas no deseables
    override fun onBackPressed() {
        handleOnBackPressed()
    }

    private fun handleOnBackPressed() {

    }

    //obtenemos el saldo y el nombre del usuario
    fun getsaldoandname() {
        GlobalScope.launch(Dispatchers.Default) {
            val df = DecimalFormat("0.00")
            val tvSaldo = findViewById<TextView>(R.id.Saldo)
            val tvNombre = findViewById<TextView>(R.id.Nombrecompleto)
            usuarios.document(currentUser?.email.toString()).get().addOnSuccessListener {
                if (it.exists()) {
                    tvSaldo.text =
                        df.format(it.get("saldo").toString().toFloat()).toString() + " €"
                    tvNombre.text = " " + it.get("nombre").toString()
                }
            }
        }
    }


}




