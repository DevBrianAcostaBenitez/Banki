package com.example.banki

import android.content.Context
import android.icu.text.DecimalFormat
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import java.text.Format
import java.time.format.DateTimeFormatter

class Historial_adapter(private val HistorialArray: ArrayList<Historial_item>) :
    RecyclerView.Adapter<Historial_adapter.MyViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.activity_historial_item,
            parent, false
        )
        return MyViewHolder(itemView)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val df = DecimalFormat("0.00")
        val currentitem = HistorialArray[position]
//cogemos el timestamp y le ponemos un formato mas facil de leer
        holder.Fecha.text = currentitem.fecha
        holder.Operacion.text = currentitem.operacion
        holder.Correo2.text = currentitem.correo2
        holder.Cifra.text = df.format(currentitem.cifra)
        holder.Cancelable.text = currentitem.cancelable
//no queremos que no se vea el correo del usuario que hizo las operaciones que se ven en el historial
//al fin de al cabo estas viendo tu propio historial, uno ya sabe su propio email
//pero lo necesitamos para cancelar operaciones
//tampoco queremos mostrar la fecha con milisegundos
        holder.Correo1.text = currentitem.correo1
        holder.Correo1.visibility = View.INVISIBLE
        holder.Fecha.visibility = View.INVISIBLE
//no obstante si queremos ver quien nos ha pagado si hemos recibido una transferencia
//recordemos que en estos casos los campos de correo 1 y 2  estan invertidos
        if (holder.Operacion.text == "Transferencia recibida") {
            holder.TextViewCorreo2.text = "Pagador"
        }
//si la operacion es un ingreso o un retiro, no mostraremos destinatario,
// porque tu mismo recives o retiraste el dinero
        if (holder.Operacion.text == "Ingresar" || holder.Operacion.text == "Retirar") {
            holder.TextViewCorreo2.visibility = View.INVISIBLE
            holder.Correo2.visibility = View.INVISIBLE
        }
//si una operacion en el historial NO es cancelable, entonces el boton se oculta y se inutiliza
        if (holder.Cancelable.text.toString() == "no") run {
            holder.ButtonCancelar.visibility = View.INVISIBLE
            holder.ButtonCancelar.isEnabled = false
            holder.ButtonCancelar.isClickable = false
        } else {
            holder.ButtonCancelar.visibility = View.VISIBLE
            holder.ButtonCancelar.isEnabled = true
            holder.ButtonCancelar.isClickable = true
        }
//hacemos que las fechas esten sin los milisegundos
        holder.FechaAMostrar.text = holder.Fecha.text.substring(0, 19);
    }

    override fun getItemCount(): Int {
        return HistorialArray.size
    }


    class MyViewHolder(Historial: View) : RecyclerView.ViewHolder(Historial) {

        val db = FirebaseFirestore.getInstance()
        val historial = db.collection("Historial")
        val usuarios = db.collection("Usuarios")
        val Fecha: TextView = itemView.findViewById(R.id.tvFechaReal)
        val FechaAMostrar: TextView = itemView.findViewById(R.id.tvfecha)
        val Operacion: TextView = itemView.findViewById(R.id.tvOperacion)
        val Correo2: TextView = itemView.findViewById(R.id.tvCorreo2)
        val TextViewCorreo2: TextView = itemView.findViewById(R.id.textViewCorreo2)
        val Cifra: TextView = itemView.findViewById(R.id.tvCifra)
        val Cancelable: TextView = itemView.findViewById(R.id.tvCancelable)
        val Correo1: TextView = itemView.findViewById(R.id.correo1)
        val ButtonCancelar: Button = itemView.findViewById(R.id.btCancelar)

        fun Float.round(decimals: Int): Float {
            var multiplier = 1.0
            repeat(decimals) { multiplier *= 10 }
            return (kotlin.math.round(this * multiplier) / multiplier).toFloat()
        }

        init {
            var Saldo = 0.00f
            var SaldoDestinatario = 0.00f
            //cancelacion de operaciones al pulsar el boton
            ButtonCancelar.setOnClickListener {
                ButtonCancelar.setEnabled(false)
                usuarios.document(Correo1.text.toString()).get().addOnSuccessListener {

                    if (it.exists()) {
                        GlobalScope.launch(Dispatchers.IO) {
                            Saldo = it.get("saldo").toString().toFloat()
                            var Cifra = Cifra.text.toString()
                            if (Cifra.takeLast(1) == "0") {
                                Cifra = Cifra.dropLast(1)
                                if (Cifra.takeLast(1) == "0") {
                                    Cifra = Cifra.dropLast(2)
                                }
                            }
                            Cifra = Cifra.replace(",", ".")
                            //obviamente dependiendo de lo que cancelemos, tenemos que hacer lo contrario a la operacion
                            val updateSaldo =
                                db.collection("Usuarios").document(Correo1.text.toString())
                            if (Operacion.text.toString() == "Retirar") {
                                Saldo = Saldo + Cifra.toFloat()
                                updateSaldo.update("saldo", Saldo.round(2).toString())
                                delete(Fecha.text.toString())
                            } else if (Operacion.text.toString() == "Ingresar") {
                                Saldo = Saldo - Cifra.toFloat()
                                if (Saldo < 0) {
                                    showMessage(
                                        Historial.getContext(),
                                        "No tienes saldo suficiente para cancelar el ingreso",
                                    )
                                    cancel()
                                } else {
                                    updateSaldo.update("saldo", Saldo.round(2).toString())
                                    delete(Fecha.text.toString())
                                }
                            } else {
                                val updateSaldoDestinatario =
                                    db.collection("Usuarios").document(Correo2.text.toString())
                                usuarios.document(Correo2.text.toString()).get()
                                    .addOnSuccessListener {
                                        SaldoDestinatario = it.get("saldo").toString().toFloat()
                                        SaldoDestinatario = SaldoDestinatario - Cifra.toFloat()
                                        updateSaldoDestinatario.update(
                                            "saldo",
                                            SaldoDestinatario.round(2).toString()
                                        )
                                    }
                                Saldo = Saldo + Cifra.toFloat()
                                updateSaldo.update("saldo", Saldo.round(2).toString())
                                delete(Fecha.text.toString())
                            }

                        }
                    }
                }


            }
        }

        fun delete(Fecha: String) {
            historial.document(Fecha).get().addOnSuccessListener() {
                if (it.exists()) {
                    it.reference.delete()
                }
            }
        }

        private fun showMessage(context: Context, message: String) {
            GlobalScope.launch(Dispatchers.Main) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

}
