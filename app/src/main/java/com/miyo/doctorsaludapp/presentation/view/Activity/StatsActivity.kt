package com.miyo.doctorsaludapp.presentation.view.Activity

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.datepicker.MaterialDatePicker
import com.miyo.doctorsaludapp.databinding.ActivityStatsBinding
import com.miyo.doctorsaludapp.domain.model.stats.StatsFilters
import com.miyo.doctorsaludapp.presentation.view.activity.IndicatorEvaluacionPreopActivity
import com.miyo.doctorsaludapp.presentation.view.activity.IndicatorPrecisionActivity
import com.miyo.doctorsaludapp.presentation.view.activity.IndicatorRiesgoActivity
import com.miyo.doctorsaludapp.presentation.view.activity.IndicatorTiempoInterpretacionActivity
import com.miyo.doctorsaludapp.presentation.view.adapter.MonthlyTrendAdapter
import com.miyo.doctorsaludapp.presentation.viewmodel.StatsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class StatsActivity : AppCompatActivity() {

    private lateinit var b: ActivityStatsBinding
    private val vm: StatsViewModel by lazy { StatsViewModel() }
    private lateinit var adapter: MonthlyTrendAdapter

    private var selectedStart: Date? = null
    private var selectedEnd: Date? = null
    private var selectedGran = "month" // day|month|year

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(b.root)

        setupUi()
        observe()

        // Carga inicial con filtros del VM
        vm.load()

        // Pinta fechas del VM
        val f = vm.state.value.filters
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        b.etStart.setText(sdf.format(f.start))
        b.etEnd.setText(sdf.format(f.end))
        selectedGran = f.granularity
        (b.acGranularity as? AutoCompleteTextView)?.setText(
            when (selectedGran) { "day" -> "Día"; "year" -> "Año"; else -> "Mes" }, false
        )
    }

    private fun setupUi() = with(b) {
        toolbar.setNavigationOnClickListener { finish() }

        // Recycler tendencias
        adapter = MonthlyTrendAdapter()
        rvMonthly.layoutManager = LinearLayoutManager(this@StatsActivity)
        rvMonthly.adapter = adapter

        // Gráficos
        riskChart.description = Description().apply { text = "" }
        riskChart.axisLeft.setDrawGridLines(false)
        riskChart.axisRight.isEnabled = false
        riskChart.xAxis.setDrawGridLines(false)
        riskChart.legend.isEnabled = false
        riskChart.setNoDataText("Sin datos para el período.")

        lineChart.description = Description().apply { text = "" }
        lineChart.axisLeft.setDrawGridLines(false)
        lineChart.axisRight.isEnabled = false
        lineChart.xAxis.setDrawGridLines(false)
        lineChart.legend.isEnabled = false
        lineChart.setNoDataText("Sin datos para el período.")
        // Eje Y 0..100 y formato %
        b.lineChart.axisLeft.apply {
            axisMinimum = 0f
            axisMaximum = 100f
            granularity = 10f
            valueFormatter = object : ValueFormatter() {
                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                    return String.format(Locale.getDefault(), "%.0f%%", value)
                }
            }
        }

        // Calendarios
        etStart.setOnClickListener { pickDate(true) }
        etEnd.setOnClickListener { pickDate(false) }

// Granularidad
        val items = arrayOf("Día", "Mes", "Año")
        (b.acGranularity as? com.google.android.material.textfield.MaterialAutoCompleteTextView)?.apply {
            setSimpleItems(items)
            setOnItemClickListener { _, _, pos, _ ->
                selectedGran = when (pos) { 0 -> "day"; 2 -> "year"; else -> "month" }
            }
            // opcional: abre el dropdown al tocar el campo
            setOnClickListener { showDropDown() }
        }

        // Aplicar filtros
        btnApply.setOnClickListener { applyFilters() }

        // Botones de indicadores
        btnPrecision.setOnClickListener {
            startActivity(Intent(this@StatsActivity, IndicatorPrecisionActivity::class.java))
        }
        btnTiempoIa.setOnClickListener {
            startActivity(Intent(this@StatsActivity, IndicatorTiempoInterpretacionActivity::class.java))
        }
        btnRiesgo.setOnClickListener {
            startActivity(Intent(this@StatsActivity, IndicatorRiesgoActivity::class.java))
        }
        btnPreop.setOnClickListener {
            startActivity(Intent(this@StatsActivity, IndicatorEvaluacionPreopActivity::class.java))
        }
    }

    // DatePicker sin desfase UTC
    private fun pickDate(isStart: Boolean) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(if (isStart) "Desde" else "Hasta")
            .build()

        picker.addOnPositiveButtonClickListener { utcMillis ->
            val calUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
            val calLocal = Calendar.getInstance().apply {
                set(Calendar.YEAR, calUtc.get(Calendar.YEAR))
                set(Calendar.MONTH, calUtc.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, calUtc.get(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val d = calLocal.time
            val txt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(d)
            if (isStart) { selectedStart = d; b.etStart.setText(txt) }
            else { selectedEnd = d; b.etEnd.setText(txt) }
        }
        picker.show(supportFragmentManager, if (isStart) "start_picker" else "end_picker")
    }

    private fun startOfDay(d: Date): Date {
        val cal = Calendar.getInstance()
        cal.time = d
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    private fun endOfDay(d: Date): Date {
        val cal = Calendar.getInstance()
        cal.time = d
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
        return cal.time
    }

    private fun applyFilters() {
        val current = vm.state.value.filters

        val start: Date = startOfDay(selectedStart ?: current.start)
        val end: Date = endOfDay(selectedEnd ?: current.end)

        if (start.after(end)) {
            Toast.makeText(this, "Rango inválido: 'Desde' > 'Hasta'", Toast.LENGTH_SHORT).show()
            return
        }

        val f = StatsFilters(start = start, end = end, granularity = selectedGran)
        vm.setFilters(f)
        vm.load(f)
    }

    private fun observe() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.state.collect { s ->
                    b.progress.isVisible = s.loading
                    b.content.isVisible = !s.loading
                    s.error?.let { Toast.makeText(this@StatsActivity, it, Toast.LENGTH_LONG).show() }
                    val data = s.data ?: return@collect

                    // KPIs
                    b.tvTotalPatients.text = data.totalPacientes.toString()
                    b.tvIaPrecision.text = data.avgPrecisionGlobal
                        ?.let { String.format(Locale.getDefault(), "%.1f%%", it) } ?: "—"
                    b.tvIaTime.text = data.avgIaSeconds?.let {
                        String.format(Locale.getDefault(), "%.1fs", it)
                    } ?: "—"
                    b.tvManualTime.text = String.format(Locale.getDefault(), "%.1f min", data.avgManualMinutes)
                    b.tvSavedTime.text = data.savedMinutes?.let {
                        String.format(Locale.getDefault(), "%.1f min", it)
                    } ?: "—"

                    // Riesgo
                    val r = data.risk
                    val barEntries = listOf(
                        BarEntry(0f, r.bajo.toFloat()),
                        BarEntry(1f, r.moderado.toFloat()),
                        BarEntry(2f, r.alto.toFloat()),
                        BarEntry(3f, r.critico.toFloat())
                    )
                    val barSet = BarDataSet(barEntries, "").apply { setDrawValues(true) }
                    b.riskChart.data = BarData(barSet).apply { barWidth = 0.6f }
                    b.riskChart.xAxis.valueFormatter =
                        IndexAxisValueFormatter(listOf("Bajo", "Moderado", "Alto", "Crítico"))
                    b.riskChart.invalidate()

                    // Línea de precisión
                    val labels = data.monthly.map { it.monthLabel }
                    val lineEntries = data.monthly.mapIndexed { i, m ->
                        Entry(i.toFloat(), (m.avgPrecision ?: 0.0).toFloat())
                    }
                    val lineSet = LineDataSet(lineEntries, "").apply { setDrawCircles(true) }
                    b.lineChart.data = LineData(lineSet)
                    b.lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                    b.lineChart.invalidate()

                    adapter.submitList(data.monthly)
                }
            }
        }
    }
}
