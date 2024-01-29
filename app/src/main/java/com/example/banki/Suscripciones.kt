package com.example.banki

import android.content.Context
import android.content.Intent
import android.icu.text.DecimalFormat
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
class Suscripciones : AppCompatActivity() {
    val db = FirebaseFirestore.getInstance()
    val usuarios = db.collection("Usuarios")
    val historial = db.collection("Historial")
    val scope = MainScope()
    var job: Job? = null
    val auten = FirebaseAuth.getInstance()
    val currentUser = auten.currentUser
    var BackButtonAbled = true
    private val EstadoRevistas by lazy {
        findViewById(R.id.Revistas) as TextView
    }
    private val EstadoAnime by lazy {
        findViewById(R.id.Anime) as TextView
    }
    private val EstadoPeliculas by lazy {
        findViewById(R.id.Peliculas) as TextView
    }
    private val viewSaldo by lazy {
        findViewById(R.id.tvSaldo) as TextView
    }
    private val Cobrar by lazy {
        findViewById(R.id.btcobrar) as Button
    }
    private val Parar by lazy {
        findViewById(R.id.btpararsub) as Button
    }
    private val volver by lazy {
        findViewById(R.id.btvolver) as Button
    }
    private val btRevistas by lazy {
        findViewById(R.id.btRevistas) as Button
    }
    private val btAnime by lazy {
        findViewById(R.id.btAnime) as Button
    }
    private val btPeliculas by lazy {
        findViewById(R.id.btPeliculas) as Button
    }
    private val formatter: DateTimeFormatter by lazy {
        DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss:SSS")
    }
    val df: DecimalFormat by lazy {
        DecimalFormat("0.00")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        if (currentUser == null) {
            val intentvolver = Intent(this@Suscripciones, MainActivity::class.java)
            startActivity(intentvolver)
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_suscripciones)
        getSaldo()
        checksub()
        btRevistas.setOnClickListener {
            CambiarEstado("Revistas", "suscripcionRevistas")
        }
        btAnime.setOnClickListener {
            CambiarEstado("Anime", "suscripcionPeliculas")
        }
        btPeliculas.setOnClickListener {
            CambiarEstado("Peliculas", "suscripcionAnime")
        }
        Cobrar.setOnClickListener {
        //cuando hacemos click en el boton de cobrar desabilitamos el boton
            Cobrar.setEnabled(false)
            volver.setEnabled(false)
            CobrarSuscripcion()
        }
        Parar.setOnClickListener {
        //tras parar el cobro, se rehabilita el boton de cobrar
            PararCobro()
            Cobrar.setEnabled(true)
            volver.setEnabled(true)
        }
        volver.setOnClickListener {
            Cobrar.setEnabled(false)
            PararCobro()
            val intentUserScreen = Intent(this@Suscripciones, UserScreen::class.java)
            val extras = Bundle()
            intentUserScreen.putExtras(extras)
            startActivity(intentUserScreen)
        }

    }

    //cuando se este cobrando tendremos el boton de ir atras deshabilitado
    override fun onBackPressed() {
        if (BackButtonAbled) {
            onBackPressedDispatcher.onBackPressed()
        } else {
            handleOnBackPressed()
        }
    }

    private fun handleOnBackPressed() {

    }

    //obtenemos el saldo que tenemos
    private fun getSaldo() {
        Cobrar.setEnabled(false)
        EstadoRevistas.setEnabled(false)
        EstadoAnime.setEnabled(false)
        EstadoPeliculas.setEnabled(false)
        GlobalScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                val df = DecimalFormat("0.00")
                usuarios.document(currentUser?.email.toString()).get().addOnSuccessListener {
                    if (it.exists()) {
                        viewSaldo.text = df.format(it.get("saldo").toString().toFloat()).toString()

                    }
                }
            }
            withContext(Dispatchers.Main) {
                Cobrar.setEnabled(true)
                EstadoPeliculas.setEnabled(true)
                EstadoRevistas.setEnabled(true)
                EstadoAnime.setEnabled(true)
            }
        }.start()

    }

    //obtenemos los datos sobre donde estamos subscritos
    private fun checksub() {
        EstadoRevistas.setEnabled(false)
        EstadoAnime.setEnabled(false)
        EstadoPeliculas.setEnabled(false)
        GlobalScope.launch(Dispatchers.IO) {
            usuarios.document(currentUser?.email.toString()).get().addOnSuccessListener {
                if (it.exists()) {
                    if (it.get("suscripcionRevistas").toString() == "0") {
                        EstadoRevistas.text = "no suscrito"
                    } else {
                        EstadoRevistas.text = "suscrito"
                    }
                    if (it.get("suscripcionAnime").toString() == "0") {
                        EstadoAnime.text = "no suscrito"
                    } else {
                        EstadoAnime.text = "suscrito"
                    }
                    if (it.get("suscripcionPeliculas").toString() == "0") {
                        EstadoPeliculas.text = "no suscrito"
                    } else {
                        EstadoPeliculas.text = "suscrito"
                    }
                }
            }
            withContext(Dispatchers.Main) {
                EstadoRevistas.setEnabled(true)
                EstadoAnime.setEnabled(true)
                EstadoPeliculas.setEnabled(true)
            }
        }.start()
    }

    //al pulsar el respectivo boton se cambiara el estado de si estamos sucritos o no
    private fun CambiarEstado(Suscripcion: String, campo: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val buttonId = Suscripcion
            val boton = findViewById<TextView>(resources.getIdentifier(buttonId, "id", packageName))
            if (boton.text == "no suscrito") {
                withContext(Dispatchers.Main) {
                    boton.text = "suscrito"
                }
                val updatesub =
                    db.collection("Usuarios").document(currentUser?.email.toString())
                updatesub.update(campo, 1)
            } else {
                withContext(Dispatchers.Main) {
                    boton.text = "no suscrito"
                }
                val updatesub =
                    db.collection("Usuarios").document(currentUser?.email.toString())
                updatesub.update(campo, 0)

            }
        }
    }

    //cuando pulsemos el boton, cada 2 segundos se hara un cobro,tambien se añadiran los pagos al historial,
    // hasta que pulsemos el otro boton
    // o nos quedemos sin saldo,
    @RequiresApi(Build.VERSION_CODES.O)
    private fun CobrarSuscripcion() {
        BackButtonAbled = false
        GlobalScope.launch(Dispatchers.Default) {
            if (EstadoRevistas.text == "no suscrito" && EstadoAnime.text == "no suscrito" && EstadoPeliculas.text == "no suscrito") {
                showMessage(this@Suscripciones, "no esta suscrito a ningun servicio")
                withContext(Dispatchers.Main) {
                Cobrar.setEnabled(true)
                volver.setEnabled(true)
                }
            } else {
                usuarios.document(currentUser?.email.toString()).get()
                    .addOnSuccessListener {
                        if (it.exists()) {
                            showMessage(this@Suscripciones, "ejecutando pagos")
                            var saldo = it.get("saldo").toString().toFloat()
                            job = scope.launch(Dispatchers.Default) {
                                while (true) {
                                    if (EstadoRevistas.text == "suscrito") {
                                        saldo = cobro(1.00f, saldo, "Revistas")
                                    }
                                    if (EstadoAnime.text == "suscrito") {
                                        saldo = cobro(2.00f, saldo, "Anime")
                                    }
                                    if (EstadoPeliculas.text == "suscrito") {
                                        saldo = cobro(3.00f, saldo, "Peliculas")
                                    }
                                    //se ejecutara el proceso cada 2 segundos hasta que lo detengamos
                                    delay(timeMillis = 2_000)
                                }
                            }
                        }

                    }
            }

        }
    }

    //cobro de las suscripciones
    @RequiresApi(Build.VERSION_CODES.O)
    private fun cobro(pago: Float, saldo: Float, Destinatario: String): Float {
        val updateSaldo = db.collection("Usuarios").document(currentUser?.email.toString())

        if (saldo < pago) {
            showMessage(this@Suscripciones, "no hay saldo suficiente para pagar, deteniendo cobros")
            PararCobro()
        } else {
            var newsaldo = (saldo - pago)
            var saldorounded = (newsaldo * 100.0).roundToInt() / 100.0
            updateSaldo.update("saldo", saldorounded.toString())
            var currentDate = LocalDateTime.now().format(formatter)
            var operacion = Historial(
                currentDate.toString(),
                currentUser?.email.toString(),
                "pago de suscripcion",
                Destinatario,
                pago,
                "no",
            )
            historial.document(operacion.fecha).set(operacion)
            GlobalScope.launch(Dispatchers.Main) {
                viewSaldo.text = df.format(saldorounded)
            }
            return newsaldo
        }
        return saldo
    }

    //paramos los cobros cuando pulsamos el boton
    private fun PararCobro() {
        GlobalScope.launch(Dispatchers.Main) {
            job?.cancel()
            job = null
            BackButtonAbled = true
        }
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
