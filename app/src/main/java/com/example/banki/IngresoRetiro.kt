package com.example.banki

import android.content.Context
import android.content.Intent
import android.icu.text.DecimalFormat
import android.os.Bundle
import android.text.InputFilter
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IngresoRetiro :  AppCompatActivity() {
    private val Ingresar by lazy {
        findViewById(R.id.btIngresar) as Button
    }
    private val Retirar by lazy {
        findViewById(R.id.btRetirar) as Button
    }
    private val tvSaldo by lazy {
        findViewById(R.id.Saldo) as TextView
    }
    private val CifraOperacion by lazy {
        findViewById(R.id.Cifra) as EditText
    }
    val db = FirebaseFirestore.getInstance()
    val usuarios = db.collection("Usuarios")
    val auten = FirebaseAuth.getInstance()
    val currentUser = auten.currentUser
    override fun onCreate(savedInstanceState: Bundle?) {
        if (currentUser == null) {
            val intentvolver = Intent(this@IngresoRetiro, MainActivity::class.java)
            startActivity(intentvolver)
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ingreso_retiro)
        val volver = findViewById<Button>(R.id.btvolver)
        getsaldo()
        CifraOperacion.setFilters(arrayOf<InputFilter>(DecimalLimiter(99, 2)))
        volver.setOnClickListener {
            val intentUserScreen = Intent(this@IngresoRetiro, UserScreen::class.java)
            val extras = Bundle()
            intentUserScreen.putExtras(extras)
            startActivity(intentUserScreen)
        }
        Ingresar.setOnClickListener {

            SeleccionOperacion("Ingresar")
        }

        Retirar.setOnClickListener {
            SeleccionOperacion("Retirar")
        }
    }

    //obtenemos el saldo actual
    fun getsaldo() {
        Ingresar.setEnabled(false)
        Retirar.setEnabled(false)

        GlobalScope.launch(Dispatchers.IO) {
            val df = DecimalFormat("0.00")
            usuarios.document(currentUser?.email.toString()).get().addOnSuccessListener {
                if (it.exists()) {
                    tvSaldo.text = df.format(it.get("saldo").toString().toFloat())

                }
            }
            withContext(Dispatchers.Main) {
                Ingresar.setEnabled(true)
                Retirar.setEnabled(true)
            }
        }
    }

    //sacamos que operacion queremos
    fun SeleccionOperacion(operacion: String) {
        //si los digitos decimales son 0 hay que quitarselos antes de que se haga cualquier operacion o si no sale error
        var saldo = tvSaldo.text.toString()
        if (saldo.takeLast(1) == "0") {
            saldo = saldo.dropLast(1)
            if (saldo.takeLast(1) == "0") {
                saldo = saldo.dropLast(2)
            }
        }
        saldo = saldo.replace(",", ".")
        if (CifraOperacion.text.isEmpty()) {
            showMessage(this@IngresoRetiro,"inserte una cifra antes de continuar")
        } else {
            val intentRetiro =
                Intent(this@IngresoRetiro, IngresoRetiro_confirmacion::class.java)
            if (CifraOperacion.text.toString()
                    .toFloat() <= (saldo.toFloat()) || operacion == "Ingresar"
            ) {
                val extras = Bundle()
                extras.putString("Saldo", tvSaldo.text.toString())
                extras.putString("Cifra", CifraOperacion.text.toString())
                extras.putString("operacion", operacion)
                intentRetiro.putExtras(extras)
                startActivity(intentRetiro)
            } else {
                showMessage(this@IngresoRetiro ,"no hay saldo suficiente para retirar esa cantidad")
            }
        }
    }


    private fun showMessage(context: Context, mensaje: String) {
        GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
        }
    }

}
