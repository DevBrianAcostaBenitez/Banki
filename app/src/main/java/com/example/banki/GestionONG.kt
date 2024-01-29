package com.example.banki

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class GestionONG : AppCompatActivity() {
    val db = FirebaseFirestore.getInstance()
    val usuarios = db.collection("Usuarios")
    val auten = FirebaseAuth.getInstance()
    val currentUser = auten.currentUser
    private val modificar by lazy {
        findViewById(R.id.btmodificar) as Button
    }
    private val tvFunDacionInternacionalDeDerechosHumanos by lazy {
        findViewById(R.id.DonacionFunDacionInternacionalDeDerechosHumanos) as EditText
    }
    private val tvGreenpeace by lazy { findViewById(R.id.DonacionGreenpeace) as EditText }
    private val tvAccionContraElHambre by lazy {
        findViewById(R.id.DonacionAccionContraElHambre) as EditText
    }
    private val tvMedicosSinFronteras by lazy {
        findViewById<EditText>(R.id.DonacionMedicosSinFronteras) as EditText
    }
    private val tvUnicef by lazy {
        findViewById(R.id.DonacionUnicef) as EditText
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (currentUser == null) {
            val intentvolver = Intent(this@GestionONG, MainActivity::class.java)
            startActivity(intentvolver)
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gestion_ong)
        val volver = findViewById<Button>(R.id.btvolver)

        getONGvalues()
        modificar.setOnClickListener {
            actualizar()

        }
        volver.setOnClickListener {
            modificar.setEnabled(false)
            ReturnToUserScreen()
        }
    }

    //los campos se rellenaran con los valores de la base de datos
    fun getONGvalues() {
        modificar.setEnabled(false)
        GlobalScope.launch(Dispatchers.Default) {
            var value = 0

            tvFunDacionInternacionalDeDerechosHumanos.setFilters(
                arrayOf<InputFilter>(
                    NumberFilter(
                        "0",
                        "5"
                    )
                )
            )
            tvGreenpeace.setFilters(arrayOf<InputFilter>(NumberFilter("0", "5")))
            tvAccionContraElHambre.setFilters(arrayOf<InputFilter>(NumberFilter("0", "5")))
            tvMedicosSinFronteras.setFilters(arrayOf<InputFilter>(NumberFilter("0", "5")))
            tvUnicef.setFilters(arrayOf<InputFilter>(NumberFilter("0", "5")))
            usuarios.document(currentUser?.email.toString()).get().addOnSuccessListener {
                if (it.exists()) {
                    it.get("nombre").toString()
                    value = (it.get("donacionFIDH") as Long).toInt()
                    tvFunDacionInternacionalDeDerechosHumanos.setText(value.toString())
                    value = (it.get("donacionGP") as Long).toInt()
                    tvGreenpeace.setText(value.toString())
                    value = (it.get("donacionACH") as Long).toInt()
                    tvAccionContraElHambre.setText(value.toString())
                    value = (it.get("donacionMSF") as Long).toInt()
                    tvMedicosSinFronteras.setText(value.toString())
                    value = (it.get("donacionUnicef") as Long).toInt()
                    tvUnicef.setText(value.toString())
                }
            }
            withContext(Dispatchers.Main) {
                modificar.setEnabled(true)
            }
        }
    }

    //actualizacion de los valores de la base de datos
    fun actualizar() {
        GlobalScope.launch(Dispatchers.IO) {
            //ningun campo debe estar vacio, si lo estan, entonces nos saldra un mensaje indicando que hay que rellenarlos
            if (tvFunDacionInternacionalDeDerechosHumanos.text.isNotEmpty() && tvGreenpeace.text.isNotEmpty() && tvAccionContraElHambre.text.isNotEmpty() &&
                tvMedicosSinFronteras.text.isNotEmpty() && tvUnicef.text.isNotEmpty()
            ) {
                //tambien la suma de las cifras de los campos debe ser 5 o menor
                if (tvFunDacionInternacionalDeDerechosHumanos.text.toString()
                        .toInt() + tvGreenpeace.text.toString().toInt() +
                    tvAccionContraElHambre.text.toString()
                        .toInt() + tvMedicosSinFronteras.text.toString()
                        .toInt() + tvUnicef.text.toString().toInt() <= 5
                ) {
                    //actualizamos los datos sobre las cantidades que el usuario esta dispuesto a donar
                    update(
                        "donacionFIDH",
                        tvFunDacionInternacionalDeDerechosHumanos.text.toString().toInt()
                    )
                    update("donacionGP", tvGreenpeace.text.toString().toInt())
                    update("donacionACH", tvAccionContraElHambre.text.toString().toInt())
                    update("donacionMSF", tvMedicosSinFronteras.text.toString().toInt())
                    update("donacionUnicef", tvUnicef.text.toString().toInt())
                    ReturnToUserScreen()
                //el total debe ser 5 o menor
                } else {
                    showMessage(this@GestionONG, "el total debe de ser 5 o menor")
                }
                //y ningun campo debe estar vacio
            } else {
                showMessage(this@GestionONG, "ningun campo debe estar vacio")
            }
        }
    }

    fun update(field: String, tvValue: Int) {
        val updateDonaciones =
            db.collection("Usuarios").document(currentUser?.email.toString())
        updateDonaciones.update(
            field,
            tvValue
        )
    }

    fun ReturnToUserScreen() {
        val intenreturn = Intent(this@GestionONG, UserScreen::class.java)
        val extras = Bundle()
        intenreturn.putExtras(extras)
        startActivity(intenreturn)
    }

    private fun showMessage(context: Context, mensaje: String) {
        GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
        }
    }


}