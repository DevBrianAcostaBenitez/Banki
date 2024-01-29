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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class log : AppCompatActivity() {
    private lateinit var auten: FirebaseAuth
    val db = FirebaseFirestore.getInstance()
    val usuarios = db.collection("Usuarios")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)
        val etemail = findViewById<EditText>(R.id.Correo)
        val etpassword = findViewById<EditText>(R.id.Contraseña)
        var email = ""
        var password = ""
        val acceder = findViewById<Button>(R.id.log)
        val volver = findViewById<Button>(R.id.btvolver)
        auten = FirebaseAuth.getInstance()
        volver.setOnClickListener {
            val intentvolver = Intent(this@log, MainActivity::class.java)
            startActivity(intentvolver)
        }
        //antes de intentar acceder al contenido de loss campos estos deben estar rellenos
        acceder.setOnClickListener {
            if (etemail.text.isNotEmpty() && etpassword.text.isNotEmpty()) {
                lifecycleScope.launch {
                    withContext(Dispatchers.Main) {
                        email = etemail.text.toString()
                        password = etpassword.text.toString()
                        auten.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val intentUserScreen =
                                        Intent(this@log, UserScreen::class.java)
                                    startActivity(intentUserScreen)
                                } else {
                                    showMessage(this@log, "correo o contraseña incorrecta")
                                }
                            }
                    }
                }
            } else {
                showMessage(this@log, "debes completar ambos campos")
            }
        }
    }

    private fun showMessage(context: Context, mensaje: String) {
        GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
        }
    }


}

