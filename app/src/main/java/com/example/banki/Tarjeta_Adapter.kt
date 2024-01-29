package com.example.banki

import android.icu.text.SimpleDateFormat
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList


class Tarjeta_Adapter(private val Lista_Tarjetas: ArrayList<Tarjeta>) :
    RecyclerView.Adapter<Tarjeta_Adapter.MyViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.tarjeta, parent, false
        )
        return MyViewHolder(itemView)

    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        val currentitem = Lista_Tarjetas[position]
        //cogemos el timestamp y le ponemos un formato mas facil de leer
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
        val date: Date? = currentitem.added_date?.toDate()
        if (date != null) {
            holder.fecha.text = formatter.format(date)
        }else{
            holder.fecha.text = "error"
        }
        holder.NumeroTarjeta.text = currentitem.Numero
        holder.FechaDeCaducidad.text = currentitem.fechacaducidad
        holder.NSecreto.text = currentitem.numerosecreto
        holder.estado.text = currentitem.Estado
        //si una tarjeta esta cancelada, ocultamos e desabilitamos el boton de cancelar
        if (holder.estado.text.toString() == "Cancelada") run {
            holder.ButtonCancelar.visibility = View.INVISIBLE
            holder.ButtonCancelar.isEnabled = false
            holder.ButtonCancelar.isClickable = false
        } else {
            holder.ButtonCancelar.visibility = View.VISIBLE
            holder.ButtonCancelar.isEnabled = true
            holder.ButtonCancelar.isClickable = true
        }

    }


    override fun getItemCount(): Int {
        return Lista_Tarjetas.size
    }

    class MyViewHolder(Tarjetas_Credito: View) : RecyclerView.ViewHolder(Tarjetas_Credito) {
        val db = FirebaseFirestore.getInstance()
        val tarjetas = db.collection("Tarjetas")
        val fecha: TextView = itemView.findViewById(R.id.FechaCreacion)
        val NumeroTarjeta: TextView = itemView.findViewById(R.id.Numero)
        val FechaDeCaducidad: TextView = itemView.findViewById(R.id.FechaDeCaducidad)
        val NSecreto: TextView = itemView.findViewById(R.id.NSecreto)
        val estado: TextView = itemView.findViewById(R.id.estado)
        val Cancelar = itemView.findViewById<Button>(R.id.btCancelar)
        val ButtonCancelar: Button = itemView.findViewById(R.id.btCancelar)
        init {
        //cancelacion de las tarjetas
            Cancelar.setOnClickListener {
                Cancelar.setEnabled(false)
                tarjetas.document(NumeroTarjeta.text.toString()).get().addOnSuccessListener() {
                    if (it.exists()) {
                        GlobalScope.launch(Dispatchers.IO) {
                            val updateEstado =
                                db.collection("Tarjetas").document(NumeroTarjeta.text.toString())
                            updateEstado.update("estado", "Cancelada")
                        }
                    }
                }

            }

        }
    }
}
