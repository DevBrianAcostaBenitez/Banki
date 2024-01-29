package com.example.banki

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val login = findViewById<Button>(R.id.autentificacion)
        val registrar = findViewById<Button>(R.id.registro)
        login.setOnClickListener {
            val logscreen = Intent(this@MainActivity, log::class.java)
            startActivity(logscreen)

        }
        registrar.setOnClickListener {
            val signscreen = Intent(this@MainActivity, signup::class.java)
            startActivity(signscreen)

        }
    }
}