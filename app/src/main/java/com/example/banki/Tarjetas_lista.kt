package com.example.banki

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ServerTimestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList
import com.google.firebase.firestore.Query
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class Tarjetas_lista : AppCompatActivity() {

    val db = FirebaseFirestore.getInstance()
    val tarjetas = db.collection("Tarjetas")
    private lateinit var listaTarjetas: ArrayList<Tarjeta>
    val auten = FirebaseAuth.getInstance()
    val currentUser = auten.currentUser
    lateinit var query: Query
    private var filtro: String = "todas"
    private val solicitar by lazy {
        findViewById(R.id.btSolicitarTarjeta) as Button
    }
    private val volver by lazy {
        findViewById(R.id.btvolver) as Button
    }
    private val ftTodas by lazy {
        findViewById(R.id.btAmbas) as Button
    }
    private val ftActivas by lazy {
        findViewById(R.id.btActivas) as Button
    }
    private val ftCanceladas by lazy {
        findViewById(R.id.btCanceladas) as Button
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        if (currentUser == null) {
            val intentvolver = Intent(this@Tarjetas_lista, MainActivity::class.java)
            startActivity(intentvolver)
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tarjetas_lista)
        val solicitar = findViewById<Button>(R.id.btSolicitarTarjeta)
        val volver = findViewById<Button>(R.id.btvolver)
        val ftTodas: RadioButton = findViewById(R.id.btAmbas)
        val ftActivas: RadioButton = findViewById(R.id.btActivas)
        val ftCanceladas: RadioButton = findViewById(R.id.btCanceladas)
        SubscribeToRealtimeUpdates()
        solicitar.setOnClickListener {
            CreateCard()
        }
        volver.setOnClickListener {
            val intentUserScreen = Intent(this@Tarjetas_lista, UserScreen::class.java)
            startActivity(intentUserScreen)
        }
        ftTodas.setOnClickListener {
            filtro = "todas"
            SubscribeToRealtimeUpdates()
        }
        ftActivas.setOnClickListener {
            filtro = "Activa"
            SubscribeToRealtimeUpdates()
        }
        ftCanceladas.setOnClickListener {
            filtro = "Cancelada"
            SubscribeToRealtimeUpdates()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    //creacion de tarjetas, el numero es al azar, pero no se debe repetir, si se da el caso, se hace otro numero al azar
    private fun CreateCard() {
        val solicitar = findViewById<Button>(R.id.btSolicitarTarjeta)
        val volver = findViewById<Button>(R.id.btvolver)
        volver.setEnabled(false)
        solicitar.setEnabled(false)
        GlobalScope.launch(Dispatchers.IO) {
            //se ve que random no me deja hacer numero de mas de 16 digitos, asi que hago esto para hacer 1 de 20
            var numeropart1 = (0..99999999999999999).random().toString().format("%16d")
            var numeropart2 = (0..9999).random().toString().format("%4d")
            var numero = numeropart1 + numeropart2
            val dayandmonth = DateTimeFormatter.ofPattern("dd-MM")
            val year = DateTimeFormatter.ofPattern("yyyy")
            val currentdayandmonth = LocalDateTime.now().format(dayandmonth).toString()
            val currentyear = LocalDateTime.now().format(year).toInt()
            val futureyear = currentyear + 4
            val numerosecreto = (0..9999).random().toString().format("%4d")
            val exists = async(Dispatchers.IO) {
                val document = tarjetas.document(numero).get().await()
                document.exists()
            }.await()
            if (exists) {
                while (async(Dispatchers.IO) {
                        tarjetas.document(numero).get().await().exists()
                    }.await()) {
                    numeropart1 = (0..99999999999999999).random().toString().format("%16d")
                    numeropart2 = (0..9999).random().toString().format("%4d")
                    numero = numeropart1 + numeropart2
                }
            }
            val tarjeta = Tarjetas(
                numero,
                currentUser?.email.toString(),
                currentdayandmonth + "-" + futureyear.toString(),
                numerosecreto,
                "Activa"
            )
            tarjetas.document(tarjeta.numero).set(tarjeta)
            showMessage(this@Tarjetas_lista, "Tarjeta concedida")
            withContext(Dispatchers.Main) {
                volver.setEnabled(true)
                solicitar.setEnabled(true)
            }
        }
    }

    //si hay un cambio en la lista, se actualiza en la vista del usuario
    @RequiresApi(Build.VERSION_CODES.O)
    private fun SubscribeToRealtimeUpdates() {
        val recyclerView = findViewById<RecyclerView>(R.id.ListaTarjetas)
        val linearLayoutManager = LinearLayoutManager(this)
        recyclerView.itemAnimator = null
        listaTarjetas = arrayListOf()
        recyclerView.adapter = Tarjeta_Adapter(listaTarjetas)
        recyclerView.setLayoutManager(linearLayoutManager)

        GlobalScope.launch(Dispatchers.IO) {
            try {
                tarjetas.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    firebaseFirestoreException?.let {
                        GlobalScope.launch(Dispatchers.Main) {
                            Toast.makeText(this@Tarjetas_lista, it.message, Toast.LENGTH_LONG)
                                .show()
                        }
                        return@addSnapshotListener
                    }
                    querySnapshot?.let {
                        val tarjetasList = mutableListOf<Tarjeta>()
                        //si filtramos la lista de tarjetas entoces solo nos saldras las canceladas o no canceladas segun elijamos
                        if (filtro == "todas") {
                            query = tarjetas.whereEqualTo("correo", currentUser?.email.toString())
                        } else {
                            query = tarjetas.whereEqualTo("correo", currentUser?.email.toString())
                                .whereEqualTo("estado", filtro)
                        }
                        query.get()
                            .addOnSuccessListener {
                                GlobalScope.launch(Dispatchers.IO) {
                                    if (!it.isEmpty)
                                        for (data in it) {
                                            val tarjeta: Tarjeta? =
                                                data.toObject(Tarjeta::class.java)
                                            tarjeta?.let { t ->
                                                tarjetasList.add(t)
                                                //los datos mas nuevos van primero
                                                tarjetasList.sortByDescending { it.added_date }
                                            }
                                        }
                                    //borramos la lista para luego rellenarla de nuevo con los datos filtrados
                                    listaTarjetas.clear()
                                    listaTarjetas.addAll(tarjetasList)
                                    withContext(Dispatchers.Main) {
                                        recyclerView.adapter?.notifyDataSetChanged()
                                    }
                                }
                            }
                            .addOnFailureListener() {
                                showMessage(this@Tarjetas_lista, "error al obtener datos")
                            }
                    }

                }
            } catch (e: Exception) {
                showMessage(this@Tarjetas_lista, "error al obtener datos: ${e.message}")
            }
        }
    }


    private fun showMessage(context: Context, mensaje: String) {
        GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
        }
    }

    data class Tarjetas(
        val numero: String,
        val correo: String,
        val fechacaducidad: String,
        val numerosecreto: String,
        val estado: String,
        @ServerTimestamp
        var added_date: Date? = null,
    )

}