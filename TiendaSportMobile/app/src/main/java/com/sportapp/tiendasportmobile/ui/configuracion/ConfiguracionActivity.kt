package com.sportapp.tiendasportmobile.ui.configuracion

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.sportapp.tiendasportmobile.R
import com.sportapp.tiendasportmobile.ui.base.BaseDrawerActivity
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class ConfiguracionActivity : BaseDrawerActivity() {

    private lateinit var tvMontoActual: TextView
    private lateinit var etNuevoMonto: EditText
    private lateinit var btnGuardar: Button
    private val formatoMoneda = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply { maximumFractionDigits = 0 }

    override fun layoutRecursoPropio(): Int = R.layout.activity_configuracion
    override fun tituloPantalla(): String = "Configuración"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tvMontoActual = findViewById(R.id.tvMontoActual)
        etNuevoMonto = findViewById(R.id.etNuevoMonto)
        btnGuardar = findViewById(R.id.btnGuardarConfig)

        cargarConfiguracion()
        btnGuardar.setOnClickListener { guardar() }
    }

    private fun cargarConfiguracion() {
        lifecycleScope.launch {
            val resultado = repository.obtenerConfiguracion()
            resultado.onSuccess {
                tvMontoActual.text = "Monto actual: ${formatoMoneda.format(it.monto_minimo_compra)}"
                etNuevoMonto.setText(it.monto_minimo_compra.toInt().toString())
            }.onFailure {
                tvMontoActual.text = "No se pudo cargar el monto actual."
            }
        }
    }

    private fun guardar() {
        val nuevoMonto = etNuevoMonto.text.toString().toDoubleOrNull()
        if (nuevoMonto == null || nuevoMonto < 0) {
            Toast.makeText(this, "Ingresa un monto válido", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            val resultado = repository.actualizarConfiguracion(nuevoMonto)
            resultado.onSuccess {
                Toast.makeText(this@ConfiguracionActivity, it, Toast.LENGTH_SHORT).show()
                cargarConfiguracion()
            }.onFailure {
                Toast.makeText(this@ConfiguracionActivity, "Error al guardar: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
