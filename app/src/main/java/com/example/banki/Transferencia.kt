package com.example.banki

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.coroutines.*
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.round
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.O)
class Transferencia : AppCompatActivity() {
    val db = FirebaseFirestore.getInstance()
    val usuarios = db.collection("Usuarios")
    val tarjetas = db.collection("Tarjetas")
    val historial = db.collection("Historial")
    val auten = FirebaseAuth.getInstance()
    val currentUser = auten.currentUser
    var BackButtonAbled = true
    private val volver by lazy {
        findViewById(R.id.btVolver) as Button
    }
    private val TransferCorreo by lazy {
        findViewById(R.id.btEnviarACorreo) as Button
    }
    private val TransferNumero by lazy {
        findViewById(R.id.btEnviarANumero) as Button
    }
    private val etCifra by lazy {
        findViewById(R.id.etCifra) as EditText
    }
    private val tvSaldo by lazy {
        findViewById(R.id.tvSaldo) as TextView
    }
    private val formatter: DateTimeFormatter by lazy {
        DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss:SSS")
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        if (currentUser == null) {
            val intentvolver = Intent(this@Transferencia, MainActivity::class.java)
            startActivity(intentvolver)
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transferencia)
        etCifra.setFilters(arrayOf<InputFilter>(DecimalLimiter(99, 2)))
        getsaldo()
        volver.setOnClickListener {
            TransferCorreo.setEnabled(false)
            TransferNumero.setEnabled(false)
            val intentUserScreen = Intent(this@Transferencia, UserScreen::class.java)
            startActivity(intentUserScreen)
        }
        TransferCorreo.setOnClickListener {
            val tvDestinatario = findViewById<EditText>(R.id.etDestinatario)
            if (tvDestinatario.text.isNotEmpty()) {
                val Destinatario = tvDestinatario.text.toString()
                EnviarACorreo(Destinatario)
            } else {
                showMessage(this@Transferencia, "se debe especificar el destinatario")
            }
        }
        TransferNumero.setOnClickListener {
            EnviarANumero()
        }
    }

    //obtener saldo nuestro
    fun getsaldo() {
        GlobalScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                val df = DecimalFormat("0.00")
                usuarios.document(currentUser?.email.toString()).get().addOnSuccessListener {
                    if (it.exists()) {
                        tvSaldo.text = df.format(it.get("saldo").toString().toFloat())
                    }
                }
            }
        }
    }

    //cuando hagamos una transferencia no queremos que el usuario pueda volver atras con el boton del movil
    override fun onBackPressed() {
        if (BackButtonAbled) {
            onBackPressedDispatcher.onBackPressed()
        } else {
            handleOnBackPressed()
        }
    }

    private fun handleOnBackPressed() {

    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun EnviarACorreo(Destinatario: String) {
        //deshabilitamos los botones hasta que la transferencia termine o falle
        volver.setEnabled(false)
        TransferCorreo.setEnabled(false)
        TransferNumero.setEnabled(false)
        BackButtonAbled = false
        GlobalScope.launch(Dispatchers.IO) {
            if (currentUser?.email.toString() == Destinatario) {
                showMessage(this@Transferencia, "no se puede hacer una transferencia a uno mismo")
            } else {
                usuarios.document(Destinatario).get().addOnSuccessListener {
                    GlobalScope.launch(Dispatchers.IO) {
                        if (it.exists()) {
                            val df = DecimalFormat("0.00")
                            var Pago = 0.00f
                            var SaldoString = tvSaldo.text.toString()
                            Log.d(SaldoString, "message")
                            if (SaldoString.takeLast(1) == "0") {
                                SaldoString = SaldoString.dropLast(1)
                                if (SaldoString.takeLast(1) == "0") {
                                    SaldoString = SaldoString.dropLast(2)
                                }
                            }

                            SaldoString = SaldoString.replace(",", ".")
                            var Saldo = SaldoString.toFloat()
                            var SaldoDestinatario = 0.00f
                            val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss:SSS")
                            var currentDate = ""
                            //tenemos que introducir una cifra
                            if (etCifra.text.isEmpty()) {
                                showMessage(this@Transferencia, "se debe introducir una cifra")
                            } else {
                                val Cifra = etCifra.text.toString().toFloat()
                                //tampoco debemos tratar de transferir mas de lo que tenemos
                                if (Saldo < Cifra) {
                                    showMessage(
                                        this@Transferencia,
                                        "no se puede transferir mas de lo que tiene en su saldo"
                                    )
                                } else {
                                    val donacionACH: Float =
                                        donacion(Cifra, "donacionACH", "Accion contra el hambre")
                                    val donacionFIDH: Float = donacion(
                                        Cifra,
                                        "donacionFIDH",
                                        "Fundacion internacional de derechos humanos"
                                    )
                                    val donacionGP: Float =
                                        donacion(Cifra, "donacionGP", "Greenpeace")
                                    val donacionMSF: Float =
                                        donacion(Cifra, "donacionMSF", "Medicos Sin Fronteras")
                                    val donacionUnicef: Float =
                                        donacion(Cifra, "donacionUnicef", "Unicef")
                                    //se calcula lo que la otra persona recibe , se realiza el pago y la operacion se añade al historial
                                    Pago =
                                        Cifra - donacionACH - donacionFIDH - donacionGP - donacionMSF - donacionUnicef
                                    //actualizacion del saldo del destinatario
                                    usuarios.document(Destinatario).get()
                                        .addOnSuccessListener {
                                            if (it.exists()) {
                                                var updateSaldo = db.collection("Usuarios")
                                                    .document(Destinatario)
                                                SaldoDestinatario =
                                                    it.get("saldo").toString().toFloat() + Pago
                                                SaldoDestinatario =
                                                    ((SaldoDestinatario * 100.0).roundToInt() / 100.0).toFloat()

                                                updateSaldo.update(
                                                    "saldo",
                                                    SaldoDestinatario.round(2).toString()
                                                )
                                            }
                                        }
                                    //actualizacion del saldo del pagador
                                    usuarios.document(currentUser?.email.toString()).get()
                                        .addOnSuccessListener {
                                            GlobalScope.launch(Dispatchers.IO) {
                                                if (it.exists()) {
                                                    var updateSaldo = db.collection("Usuarios")
                                                        .document(currentUser?.email.toString())
                                                    Saldo = (Saldo - Cifra)
                                                    updateSaldo.update("saldo", Saldo.toString())
                                                    withContext(Dispatchers.Main) {
                                                        tvSaldo.text = df.format(Saldo)
                                                    }
                                                }

                                                currentDate = LocalDateTime.now().format(formatter)
                                                val transferencia = Historial(
                                                    currentDate,
                                                    currentUser?.email.toString(),
                                                    "Transferencia",
                                                    Destinatario,
                                                    Pago.round(2),
                                                    "si",
                                                )
                                                historial.document(transferencia.fecha)
                                                    .set(transferencia)

                                                currentDate = LocalDateTime.now().format(formatter)
                                                //en este ultimo añadido al historial ponemos el correo del destinatario
                                                // y de quien paga al reves porque queremos que el destinatario
                                                //pueda ver quien le pago
                                                val transferenciaRecibida = Historial(
                                                    currentDate,
                                                    Destinatario,
                                                    "Transferencia recibida",
                                                    currentUser?.email.toString(),
                                                    Pago.round(2),
                                                    "no",
                                                )
                                                historial.document(transferenciaRecibida.fecha)
                                                    .set(transferenciaRecibida)
                                                showMessage(
                                                    this@Transferencia,
                                                    "Transferencia realizada con exito"
                                                )
                                            }
                                        }
                                }
                            }
                        } else {
                            showMessage(this@Transferencia, "el correo del destinatario no existe")
                        }
                    }
                }
            }
            //una vez termine o falle la transferencia ,se vuelven a habilitar los botones
            reenablebutton()
        }

    }

    fun reenablebutton() {
        GlobalScope.launch(Dispatchers.Main) {
            volver.setEnabled(true)
            TransferCorreo.setEnabled(true)
            TransferNumero.setEnabled(true)
            BackButtonAbled = true
        }
    }

    //para hacer transferencias via numero de tarjeta
    @RequiresApi(Build.VERSION_CODES.O)
    fun EnviarANumero() {
        volver.setEnabled(false)
        TransferCorreo.setEnabled(false)
        TransferNumero.setEnabled(false)
        BackButtonAbled = false
        GlobalScope.launch(Dispatchers.IO) {
            val tvnumeroDestinatario = findViewById<EditText>(R.id.etDestinatario)
            if (tvnumeroDestinatario.text.isNotEmpty()) {
                val numeroDestinatario = tvnumeroDestinatario.text.toString()
                tarjetas.document(numeroDestinatario).get().addOnSuccessListener {
                    if (it.exists()) {
                        //si la tarjeta esta cancelada, lo sabremos
                        if ((it.get("estado").toString() == "Cancelada")) {
                            showMessage(
                                this@Transferencia,
                                "La tarjeta introducida fue cancelada, transferencia cancelada"
                            )
                            reenablebutton()
                        } else {
                            val correoDestinatario = it.get("correo").toString()
                            EnviarACorreo(correoDestinatario)
                        }
                    } else {
                        showMessage(
                            this@Transferencia,
                            "el numero de tarjeta del destinatario no existe"
                        )
                        reenablebutton()
                    }
                }
            } else {
                showMessage(this@Transferencia, "se debe especificar el destinatario")
                reenablebutton()
            }
        }
    }


    //cogemos los valores de los campos de donaciones en la base de datos, para descontar el porcentaje de lo que enviamos
//cada donacion se incluye al historial
    suspend fun donacion(Cifra: Float, destinatarioabreviado: String, destinatario: String): Float {
        //completableDeferred hace que la corrutina espere a que se complete lo que hay dentro del addOnSuccessListener
        val donado = CompletableDeferred<Float>()
        usuarios.document(currentUser?.email.toString()).get().addOnSuccessListener { document ->
            if (document.get(destinatarioabreviado).toString().toFloat() > 0) {
                val currentDate = LocalDateTime.now().format(formatter)
                val donacion =
                    ((Cifra / 100) * document.get(destinatarioabreviado).toString().toFloat())
                val operacion = Historial(
                    currentDate,
                    currentUser?.email.toString(),
                    "donacion",
                    destinatario,
                    donacion.round(2),
                    "no",
                )
                historial.document(currentDate).set(operacion)
                donado.complete(donacion)
            } else {
                donado.complete(0.00f)
            }
        }.addOnFailureListener {
            donado.completeExceptionally(it)
        }
        return donado.await()
    }

    fun Float.round(decimals: Int): Float {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return (round(this * multiplier) / multiplier).toFloat()
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