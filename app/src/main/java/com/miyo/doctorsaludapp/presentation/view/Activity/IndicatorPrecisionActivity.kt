package com.miyo.doctorsaludapp.presentation.view.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.datepicker.MaterialDatePicker
import com.miyo.doctorsaludapp.databinding.ActivityIndicatorPrecisionBinding
import com.miyo.doctorsaludapp.domain.model.stats.StatsFilters
import com.miyo.doctorsaludapp.presentation.viewmodel.StatsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Pantalla: Número EGC (por MES)
 * - Sin selector de granularidad (siempre "month").
 * - El gráfico muestra el TOTAL de EGC por mes.
 * - KPI superior = suma de EGC del período.
 *
 * Nota de compatibilidad:
 * Como no conocemos exactamente los nombres de campo del modelo,
 * leemos de forma segura con reflexión:
 *   - Etiqueta de mes: "monthLabel" | "label" | "month" | "period"
 *   - Total mensual:   "totalEgc"   | "total" | "count" | "numEgc" | "value"
 * Si tus nombres son otros, dímelos y lo fijo explícitamente.
 */
class IndicatorPrecisionActivity : AppCompatActivity() {

    private lateinit var b: ActivityIndicatorPrecisionBinding
    private val vm: StatsViewModel by viewModels()

    private var selectedStart: Date? = null
    private var selectedEnd: Date? = null
    private val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityIndicatorPrecisionBinding.inflate(layoutInflater)
        setContentView(b.root)

        setupUi()
        observe()
        // Carga inicial (el VM debe asumir granularity = "month" por defecto)
        vm.load()
    }

    private fun setupUi() = with(b) {
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.title = "Número EGC"

        // Oculta controles de granularidad si existen en el layout
        acGranularity?.isVisible = false

        chart.description = Description().apply { text = "" }
        chart.axisLeft.setDrawGridLines(false)
        chart.axisLeft.granularity = 1f
        chart.axisRight.isEnabled = false
        chart.xAxis.setDrawGridLines(false)
        chart.legend.isEnabled = false

        // Date pickers
        etStart.setOnClickListener { pickDate(true) }
        etEnd.setOnClickListener { pickDate(false) }

        btnApply.setOnClickListener { applyFilters() }
    }

    private fun pickDate(isStart: Boolean) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(if (isStart) "Desde" else "Hasta")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            val d = Date(millis)
            if (isStart) { selectedStart = d; b.etStart.setText(df.format(d)) }
            else { selectedEnd = d; b.etEnd.setText(df.format(d)) }
        }
        picker.show(supportFragmentManager, if (isStart) "start_ecg_total" else "end_ecg_total")
    }

    private fun applyFilters() {
        val f = StatsFilters(
            start = selectedStart ?: vm.state.value.filters.start,
            end = selectedEnd ?: vm.state.value.filters.end,
            granularity = "month" // SIEMPRE por mes
        )
        vm.setFilters(f)
        vm.load(f)
    }

    private fun observe() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.state.collect { s ->
                    b.progress.isVisible = s.loading
                    b.content.isVisible = !s.loading

                    s.error?.let {
                        Toast.makeText(this@IndicatorPrecisionActivity, it, Toast.LENGTH_LONG).show()
                    }

                    s.data?.let { data ->
                        // Labels y valores (totales de EGC por mes)
                        val labels = data.monthly.map { readString(it, "monthLabel", "label", "month", "period") ?: "" }
                        val values = data.monthly.map {
                            readNumber(it, "totalEgc", "total", "count", "numEgc", "value") ?: 0.0
                        }

                        // KPI: suma del período
                        val totalPeriodo = values.sum().roundToInt()
                        b.tvKpi.text = totalPeriodo.toString()
                        b.tvSubtitle.text = "Número EGC del período"

                        // Gráfico (totales por mes)
                        val entries = values.mapIndexed { i, y -> Entry(i.toFloat(), y.toFloat()) }
                        val set = LineDataSet(entries, "").apply {
                            setDrawCircles(true)
                            setDrawValues(false)
                        }
                        b.chart.data = LineData(set)
                        b.chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                        b.chart.invalidate()
                    }
                }
            }
        }
    }

    // -------------------- Utilidades de lectura segura --------------------

    private fun readString(obj: Any, vararg fieldNames: String): String? {
        for (name in fieldNames) {
            try {
                val f = obj.javaClass.getDeclaredField(name)
                f.isAccessible = true
                val v = f.get(obj)
                if (v is String) return v
            } catch (_: Exception) { /* siguiente nombre */ }
        }
        return null
    }

    private fun readNumber(obj: Any, vararg fieldNames: String): Double? {
        for (name in fieldNames) {
            try {
                val f = obj.javaClass.getDeclaredField(name)
                f.isAccessible = true
                val v = f.get(obj)
                when (v) {
                    is Number -> return v.toDouble()
                    is String -> return v.toDoubleOrNull()
                }
            } catch (_: Exception) { /* siguiente nombre */ }
        }
        return null
    }
}
