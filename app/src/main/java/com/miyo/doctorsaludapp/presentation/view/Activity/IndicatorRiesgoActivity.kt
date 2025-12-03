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
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.datepicker.MaterialDatePicker
import com.miyo.doctorsaludapp.databinding.ActivityIndicatorRiesgoBinding
import com.miyo.doctorsaludapp.domain.model.stats.StatsFilters
import com.miyo.doctorsaludapp.presentation.viewmodel.StatsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Riesgo (ECG) — SIEMPRE por MES (sin granularidad seleccionable).
 * Filtros: Desde/Hasta. El gráfico muestra la distribución total del período.
 */
class IndicatorRiesgoActivity : AppCompatActivity() {

    private lateinit var b: ActivityIndicatorRiesgoBinding
    private val vm: StatsViewModel by viewModels()

    private var selectedStart: Date? = null
    private var selectedEnd: Date? = null
    private val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityIndicatorRiesgoBinding.inflate(layoutInflater)
        setContentView(b.root)

        setupUi()
        observe()
        vm.load() // el VM debe asumir granularity="month" por defecto
    }

    private fun setupUi() = with(b) {
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.title = "Riesgo (ECG)"

        // Ocultar por completo controles de granularidad si existen en el layout
        try { acGranularity.isVisible = false } catch (_: Throwable) {}

        chart.description = Description().apply { text = "" }
        chart.axisLeft.setDrawGridLines(false)
        chart.axisRight.isEnabled = false
        chart.xAxis.setDrawGridLines(false)
        chart.legend.isEnabled = false

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
            if (isStart) {
                selectedStart = d
                b.etStart.setText(df.format(d))
            } else {
                selectedEnd = d
                b.etEnd.setText(df.format(d))
            }
        }
        picker.show(supportFragmentManager, if (isStart) "start_risk" else "end_risk")
    }

    private fun applyFilters() {
        val f = StatsFilters(
            start = selectedStart ?: vm.state.value.filters.start,
            end   = selectedEnd ?: vm.state.value.filters.end,
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
                        Toast.makeText(this@IndicatorRiesgoActivity, it, Toast.LENGTH_LONG).show()
                    }

                    s.data?.let { data ->
                        val r = data.risk
                        // KPI compacto por categoría
                        b.tvKpi.text = "B:${r.bajo}  M:${r.moderado}  A:${r.alto}  C:${r.critico}"
                        b.tvSubtitle.text = "Distribución total en el período"

                        val entries = listOf(
                            BarEntry(0f, r.bajo.toFloat()),
                            BarEntry(1f, r.moderado.toFloat()),
                            BarEntry(2f, r.alto.toFloat()),
                            BarEntry(3f, r.critico.toFloat())
                        )
                        val set = BarDataSet(entries, "").apply {
                            setDrawValues(false)
                        }
                        b.chart.data = BarData(set).apply { barWidth = 0.6f }
                        b.chart.xAxis.valueFormatter =
                            IndexAxisValueFormatter(listOf("Bajo", "Moderado", "Alto", "Crítico"))
                        b.chart.invalidate()
                    }
                }
            }
        }
    }
}
