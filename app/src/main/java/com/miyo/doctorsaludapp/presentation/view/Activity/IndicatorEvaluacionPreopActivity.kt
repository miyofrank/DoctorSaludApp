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
import com.miyo.doctorsaludapp.databinding.ActivityIndicatorEvaluacionPreopBinding
import com.miyo.doctorsaludapp.domain.model.stats.StatsFilters
import com.miyo.doctorsaludapp.presentation.viewmodel.StatsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Evaluación preoperatoria — SIEMPRE por MES (sin granularidad).
 * - Filtros: Desde / Hasta.
 * - Gráfico: tiempo ahorrado promedio por mes (en minutos).
 * - KPI: tiempo ahorrado promedio del período.
 */
class IndicatorEvaluacionPreopActivity : AppCompatActivity() {

    private lateinit var b: ActivityIndicatorEvaluacionPreopBinding
    private val vm: StatsViewModel by viewModels()

    private var selectedStart: Date? = null
    private var selectedEnd: Date? = null
    private val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityIndicatorEvaluacionPreopBinding.inflate(layoutInflater)
        setContentView(b.root)

        setupUi()
        observe()
        vm.load() // el VM debe asumir granularity="month" por defecto
    }

    private fun setupUi() = with(b) {
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.title = "Evaluación preoperatoria"

        // Ocultar por completo controles de granularidad si existen en el layout
        try { acGranularity.isVisible = false } catch (_: Throwable) {}

        chart.description = Description().apply { text = "" }
        chart.axisLeft.setDrawGridLines(false)
        chart.axisLeft.granularity = 1f
        chart.axisRight.isEnabled = false
        chart.xAxis.setDrawGridLines(false)
        chart.legend.isEnabled = false

        // Calendarios
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
        picker.show(supportFragmentManager, if (isStart) "start_preop" else "end_preop")
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
                        Toast.makeText(this@IndicatorEvaluacionPreopActivity, it, Toast.LENGTH_LONG).show()
                    }

                    s.data?.let { data ->
                        val avgManual = data.avgManualMinutes ?: 0.0
                        val saved = data.savedMinutes ?: 0.0

                        // KPI superior
                        b.tvKpi.text = String.format(Locale.getDefault(), "%.1f min", saved)
                        b.tvSubtitle.text = "Tiempo ahorrado promedio (sobre manual ${String.format(Locale.getDefault(), "%.1f", avgManual)} min)"

                        // Eje X: etiqueta mensual; Eje Y: minutos ahorrados (manual - IA/60)
                        val labels = data.monthly.map { it.monthLabel }
                        val entries = data.monthly.mapIndexed { i, m ->
                            val iaSeconds = m.avgIaSeconds ?: 0.0
                            val savedM = (avgManual - (iaSeconds / 60.0)).coerceAtLeast(0.0)
                            Entry(i.toFloat(), savedM.toFloat())
                        }

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
}
