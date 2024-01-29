package com.example.banki

import android.content.Context
import android.content.Intent
import android.icu.text.DecimalFormat
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.roundToInt

class IngresoRetiro_confirmacion : AppCompatActivity() {
    val db = FirebaseFirestore.getInstance()
    val usuarios = db.collection("Usuarios")
    val historial = db.collection("Historial")
    val auten = FirebaseAuth.getInstance()
    val currentUser = auten.currentUser

    private val aceptar by lazy {
        findViewById(R.id.btaceptar) as Button
    }
    private val volver by lazy {
        findViewById(R.id.btvolver) as Button
    }
    private val tvCifra by lazy {
        findViewById(R.id.Cifraoperacion) as TextView
    }
    private val tvSaldo by lazy {
        findViewById(R.id.Saldo) as TextView
    }
    private val tvTipoOperacion by lazy {
        findViewById(R.id.operacion) as TextView
    }
    private val tvCifraTrasOperacion by lazy {
        findViewById(R.id.CifraPostOperacion) as TextView
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        if (currentUser == null) {
            val intentvolver = Intent(this@IngresoRetiro_confirmacion, MainActivity::class.java)
            startActivity(intentvolver)
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ingreso_retiro_confirmacion)
        val SaldoIntent: String = intent.getStringExtra("Saldo").toString()
        val Cifra: Float = intent.getStringExtra("Cifra").toString().toFloat()
        val Operacion: String = intent.getStringExtra("operacion").toString()
        getsaldoAndTotal(SaldoIntent, Cifra, Operacion)
        volver.setOnClickListener {
            aceptar.setEnabled(false)
            val intentUserScreen = Intent(this@IngresoRetiro_confirmacion, UserScreen::class.java)
            startActivity(intentUserScreen)
        }
        aceptar.setOnClickListener {
            operacion(Cifra, Operacion)
        }
    }

    //obtenemos el saldo y la cantidad que tendremos tras la operacion
    fun getsaldoAndTotal(SaldoIntent: String, Cifra: Float, Operacion: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val df = DecimalFormat("0.00")
            var total: Float = 0.00F
            tvCifra.text = df.format(Cifra).toString()
            tvSaldo.text = SaldoIntent
            var saldo = tvSaldo.text.toString()
            if (saldo.takeLast(1) == "0") {
                saldo = saldo.dropLast(1)
                if (saldo.takeLast(1) == "0") {
                    saldo = saldo.dropLast(2)
                }
            }
            saldo = saldo.replace(",", ".")
            if (Operacion == "Ingresar") {
                withContext(Dispatchers.Main) {
                    tvTipoOperacion.text = "se añadira esta cifra: "
                }
                total = saldo.toFloat() +
                        Cifra

            } else {
                withContext(Dispatchers.Main) {
                    tvTipoOperacion.text = "se reducira esta cifra: "
                }
                total = saldo.toFloat() -
                        Cifra
            }
            val totalrounded = (total * 100.0).roundToInt() / 100.0
            tvCifraTrasOperacion.text = df.format(totalrounded)
            aceptar.setEnabled(true)
        }.start()
    }

    //se hace la operacion y la guardamos en el historial
    @RequiresApi(Build.VERSION_CODES.O)
    fun operacion(Cifra: Float, Operacion: String) {
        aceptar.setEnabled(false)
        volver.setEnabled(false)
        GlobalScope.launch(Dispatchers.IO) {
            val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss:SSS")
            val currentDate = LocalDateTime.now().format(formatter)
            var total = findViewById<TextView>(R.id.CifraPostOperacion).text.toString()
            //si los digitos decimales son 0 hay que quitarselos antes de que se haga cualquier operacion o si no sale error
            if (total.takeLast(1) == "0") {
                total = total.dropLast(1)
                if (total.takeLast(1) == "0") {
                    total = total.dropLast(2)
                }
            }
            total = total.replace(",", ".")
            val updateSaldo = db.collection("Usuarios").document(currentUser?.email.toString())
            updateSaldo.update("saldo", total.toFloat().round(2))
            val operacion = Historial(
                currentDate.toString(),
                currentUser?.email.toString(),
                Operacion,
                "",
                Cifra.toString().toFloat().round(2),
                "si",
            )
            historial.document(operacion.fecha).set(operacion)
            withContext(Dispatchers.Main) {
                showMessage(this@IngresoRetiro_confirmacion,"saldo actualizado")
            }
            val intentAcept = Intent(this@IngresoRetiro_confirmacion, UserScreen::class.java)
            startActivity(intentAcept)

        }
    }

    fun Float.round(decimals: Int): Float {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return (kotlin.math.round(this * multiplier) / multiplier).toFloat()
    }

    private fun showMessage(context: Context, mensaje: String) {
        GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
        }
    }

    data class Historial(
        val fecha: String,
        val correo1: String,
        val operacion: String,
        val correo2: String,
        val cifra: Float,
        val cancelable: String,

        @ServerTimestamp
        var added_date: Date? = null,
    ) {
        override fun toString(): String {
            return "historial ( " +
                    "fecha='$fecha'," +
                    "correopagador='$correo1'," +
                    "operacion='$operacion'," +
                    "destinatario='$correo2'," +
                    "cancelable='$cancelable'," +
                    ")"
        }
    }
}