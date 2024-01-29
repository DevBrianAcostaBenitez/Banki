package com.example.banki

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class signup : AppCompatActivity() {
    private lateinit var auten: FirebaseAuth
    val db = FirebaseFirestore.getInstance()
    val usuarios = db.collection("Usuarios")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
        val etemail = findViewById<EditText>(R.id.Correo)
        val etpassword = findViewById<EditText>(R.id.Contraseña)
        val etNombre = findViewById<EditText>(R.id.Nombrecompleto)
        val etTelefono = findViewById<EditText>(R.id.Telefono)
        val etDireccion = findViewById<EditText>(R.id.Direccion)
        val registrar = findViewById<Button>(R.id.signup)
        val volver = findViewById<Button>(R.id.btvolver)
        auten = FirebaseAuth.getInstance()
        volver.setOnClickListener {
            val intentvolver = Intent(this@signup, MainActivity::class.java)
            startActivity(intentvolver)
        }
        //debemos rellenar todos los campos
        registrar.setOnClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                if (etemail.text.isNotEmpty() && etpassword.text.isNotEmpty() && etNombre.text.isNotEmpty() && etTelefono.text.isNotEmpty() && etTelefono.text.length == 9 && etDireccion.text.isNotEmpty()) {
                    val etemail = findViewById<EditText>(R.id.Correo)
                    usuarios.document(etemail.text.toString()).get().addOnSuccessListener {
                        if (it.exists()) {
                            //no queremos que haya correo repetidos
                            showMessage(this@signup, "correo ya registrado, use otro")
                        } else {
                            var email = etemail.text.toString()
                            var password = etpassword.text.toString()
                            auten.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener(this@signup) {
                                    if (it.isSuccessful) {
                                        val usuario = Usuarios(
                                            etemail.text.toString(),
                                            etpassword.text.toString(),
                                            etNombre.text.toString(),
                                            etTelefono.text.toString(),
                                            etDireccion.text.toString(),
                                            "0.00", 5, 0, 0, 0,
                                            0, 0, 0, 0
                                        )
                                        usuarios.document(usuario.correo)
                                            .set(usuario)
                                        showMessage(this@signup, "usuario creado correctamente")
                                        auten.signInWithEmailAndPassword(email, password)
                                            .addOnCompleteListener(this@signup) {
                                                if (it.isSuccessful) {
                                                    val intentUserScreen =
                                                        Intent(
                                                            this@signup,
                                                            UserScreen::class.java
                                                        )
                                                    startActivity(intentUserScreen)
                                                } else {
                                                    showMessage(
                                                        this@signup,
                                                        "error al crear el usuario"
                                                    )
                                                }
                                            }
                                    } else {
                                        showMessage(this@signup, "error al crear el usuario, revise los datos introducidos")
                                    }
                                }
                        }
                    }
                } else {
                    showMessage(this@signup, "por favor complete todos los campos")
                }
            }
        }
    }

    //antes de poder registrarnos, se comprueba si el correo introducido existe,no podemos tener cuentas con correo repetido


    private fun showMessage(context: Context, mensaje: String) {
        GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
        }
    }


    data class Usuarios(
        val correo: String,
        val contraseña: String,
        val nombre: String,
        val telefono: String,
        val direccion: String,
        val saldo: String,
        val donacionFIDH: Int,
        val donacionGP: Int,
        val donacionACH: Int,
        val donacionMSF: Int,
        val donacionUnicef: Int,
        val suscripcionRevistas: Int,
        val suscripcionAnime: Int,
        val suscripcionPeliculas: Int
    ) {
        override fun toString(): String {
            return "usuario (correo='$correo'," +
                    "contraseña='$contraseña'," +
                    "nombre='$nombre'," +
                    "telefono='$telefono'," +
                    "direccion='$direccion'," +
                    "saldo='$saldo'," +
                    "donacionFIDH='$donacionFIDH'," +
                    "donacionGP='$donacionGP'," +
                    "donacionACH='$donacionACH'," +
                    "donacionMSF='$donacionMSF'," +
                    "donacionUnicef='$donacionUnicef'," +
                    "suscripcionRevistas='$suscripcionRevistas'," +
                    "suscripcionAnime='$suscripcionAnime'," +
                    "suscripcionPeliculas='$suscripcionPeliculas')"
        }


    }
}
