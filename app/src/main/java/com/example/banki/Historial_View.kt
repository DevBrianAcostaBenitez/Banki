package com.example.banki

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class Historial_View : AppCompatActivity() {
    val db = FirebaseFirestore.getInstance()
    val historial = db.collection("Historial")
    val usuarios = db.collection("Usuarios")
    val auten = FirebaseAuth.getInstance()
    val currentUser = auten.currentUser
    lateinit var query: Query
    private lateinit var HistorialArray: ArrayList<Historial_item>

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        if (currentUser == null) {
            val intentvolver = Intent(this@Historial_View, MainActivity::class.java)
            startActivity(intentvolver)
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial_view)
        val volver = findViewById<Button>(R.id.btvolver)
        val filtrar = findViewById<Button>(R.id.btfiltrar)
        val Cifra = findViewById<EditText>(R.id.Cifra)
        Cifra.setFilters(arrayOf<InputFilter>(DecimalLimiter(99, 2)))
        val Fcifra = resources.getStringArray(R.array.FiltroCifra)
        val FCancelable = resources.getStringArray(R.array.FiltroCancelable)
        val spCifra = findViewById<Spinner>(R.id.spinnerCifra)
        val spCancelable = findViewById<Spinner>(R.id.spinnerCancelable)
        val adapterCifra: ArrayAdapter<Any?> = ArrayAdapter<Any?>(
            this,
            R.layout.spinner_list, Fcifra
        )
        adapterCifra.setDropDownViewResource(R.layout.spinner_list)
        spCifra.adapter = adapterCifra
        val adapterCancelable: ArrayAdapter<Any?> = ArrayAdapter<Any?>(
            this,
            R.layout.spinner_list, FCancelable
        )
        adapterCancelable.setDropDownViewResource(R.layout.spinner_list)
        spCancelable.adapter = adapterCancelable
        val CBIngreso = findViewById<CheckBox>(R.id.checkboxIngresos)
        val CBRetiro = findViewById<CheckBox>(R.id.checkboxRetiros)
        val CBDonacion = findViewById<CheckBox>(R.id.checkboxDonaciones)
        val CBSuscripcion = findViewById<CheckBox>(R.id.checkboxSuscripciones)
        val CBTransferencia = findViewById<CheckBox>(R.id.checkboxTransferencias)
        val CBRecibo = findViewById<CheckBox>(R.id.checkboxRecibos)
        var CifraFiltrada = 0.00f
        var FiltroCifra = "Indiferente"
        var FiltroCancelable = "Indiferente"
        var FTOperaciones = mutableListOf<String>()
        SubscribeToRealtimeUpdates(
            CifraFiltrada,
            FiltroCifra,
            FiltroCancelable,
            FTOperaciones
        )
        //hacemos que los spinners tengan sus opciones sacadas del archivo strings.xml
        spCifra.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                FiltroCifra = parent!!.getItemAtPosition(position) as String
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                FiltroCifra = "Indiferente"
            }
        }
        spCancelable.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                FiltroCancelable = parent!!.getItemAtPosition(position) as String
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                FiltroCancelable = "Indiferente"
            }
        }
        //hacemos que los checkbox cambien el filtro de las operaciones cuando presionamos sobre ellos
        CBIngreso.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                FTOperaciones.add("Ingresar")
            } else {
                FTOperaciones.remove("Ingresar")
            }
        }
        CBRetiro.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                FTOperaciones.add("Retirar")
            } else {
                FTOperaciones.remove("Retirar")
            }
        }
        CBDonacion.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                FTOperaciones.add("donacion")
            } else {
                FTOperaciones.remove("donacion")
            }
        }
        CBSuscripcion.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                FTOperaciones.add("pago de suscripcion")
            } else {
                FTOperaciones.remove("pago de suscripcion")
            }
        }
        CBTransferencia.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                FTOperaciones.add("Transferencia")
            } else {
                FTOperaciones.remove("Transferencia")
            }
        }
        CBRecibo.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                FTOperaciones.add("Transferencia recibida")
            } else {
                FTOperaciones.remove("Transferencia recibida")
            }
        }
        filtrar.setOnClickListener {
            if (Cifra.text.toString().trim().isNotEmpty()) {
                CifraFiltrada = Cifra.text.toString().toFloat()
            }
            SubscribeToRealtimeUpdates(
                CifraFiltrada,
                FiltroCifra,
                FiltroCancelable,
                FTOperaciones
            )
        }

        volver.setOnClickListener {
            val intentUserScreen = Intent(this@Historial_View, UserScreen::class.java)
            val extras = Bundle()
            intentUserScreen.putExtras(extras)
            startActivity(intentUserScreen)
        }
    }

    //cuando ocurre un cambio en el historial ,se actualiza lo que ve el usuario
    private fun SubscribeToRealtimeUpdates(
        CifraFiltrada: Float,
        FiltroCifra: String,
        FiltroCancelable: String,
        FTOperaciones: MutableList<String>,
    ) {
        val recyclerView = findViewById<RecyclerView>(R.id.Historial_RecyclerView)
        val linearLayoutManager = LinearLayoutManager(this)
        recyclerView.itemAnimator = null
        HistorialArray = arrayListOf()
        recyclerView.setLayoutManager(linearLayoutManager)
        GlobalScope.launch(Dispatchers.IO) {
            try {
                historial.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    firebaseFirestoreException?.let {
                        GlobalScope.launch(Dispatchers.Main) {
                            Toast.makeText(this@Historial_View, it.message, Toast.LENGTH_LONG)
                                .show()
                        }
                        return@addSnapshotListener
                    }
                    querySnapshot?.let {
                        val historialList = mutableListOf<Historial_item>()
                        query = historial.whereEqualTo("correo1", currentUser?.email.toString())
                        //se aplican los filtros si los deseamos, al final solo saldran en la lista las operaciones que cumplan
                        //todas las condiciones
                        GlobalScope.launch(Dispatchers.IO) {
                            if (FiltroCifra != "Indiferente") {
                                if (FiltroCifra == "mayor que") {
                                    query =
                                        query.whereGreaterThan("cifra", CifraFiltrada.toDouble());
                                } else {
                                    query = query.whereLessThan("cifra", CifraFiltrada.toDouble());
                                }
                            }
                            if (FiltroCancelable != "Indiferente") {
                                if (FiltroCancelable == "Cancelable") {
                                    query = query.whereEqualTo("cancelable", "si");
                                } else {
                                    query = query.whereEqualTo("cancelable", "no");
                                }
                            }
                            if (FTOperaciones.isNotEmpty()) {
                                query = query.whereIn("operacion", FTOperaciones)
                            }
                            query.get()
                                .addOnSuccessListener {
                                    if (!it.isEmpty)
                                        for (data in it) {
                                            val historial: Historial_item? =
                                                data.toObject(Historial_item::class.java)
                                            if (historial != null) {
                                                historialList.add(historial)
                                                //los datos mas nuevos van primero
                                                historialList.sortByDescending { it.added_date }
                                            }
                                        }
                                    //borramos la lista para luego rellenarla de nuevo con los datos filtrados
                                    HistorialArray.clear()
                                    HistorialArray.addAll(historialList)
                                    GlobalScope.launch(Dispatchers.Main) {
                                        recyclerView.adapter =
                                            Historial_adapter(HistorialArray)
                                    }
                                }

                                .addOnFailureListener {
                                    showMessage(this@Historial_View, "error al obtener datos")
                                }
                        }
                    }
                }
            } catch (e: Exception) {
                showMessage(this@Historial_View, "error al obtener datos: ${e.message}")
            }
        }
    }

    private fun showMessage(context: Context, mensaje: String) {
        GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
        }
    }


}

