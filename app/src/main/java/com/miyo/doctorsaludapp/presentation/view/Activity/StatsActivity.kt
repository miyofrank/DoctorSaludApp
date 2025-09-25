package com.miyo.doctorsaludapp.presentation.view.Activity

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.datepicker.MaterialDatePicker
import com.miyo.doctorsaludapp.databinding.ActivityStatsBinding
import com.miyo.doctorsaludapp.domain.stats.Granularity
import com.miyo.doctorsaludapp.domain.stats.StatsFilters
import com.miyo.doctorsaludapp.presentation.view.activity.IndicatorEvaluacionPreopActivity
import com.miyo.doctorsaludapp.presentation.view.activity.IndicatorPrecisionActivity
import com.miyo.doctorsaludapp.presentation.view.activity.IndicatorRiesgoActivity
import com.miyo.doctorsaludapp.presentation.view.activity.IndicatorTiempoInterpretacionActivity
import com.miyo.doctorsaludapp.presentation.viewmodel.StatsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class StatsActivity : AppCompatActivity() {

    private lateinit var b: ActivityStatsBinding
    private val vm: StatsViewModel by viewModels()

    private val fmt by lazy { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    private var startDate: Date? = null
    private var endDate: Date? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(b.root)

        setupUi()
        setupFilters()
        observe()

        // Carga inicial: últimos 6 meses en granularidad mensual
        val cal = Calendar.getInstance()
        val end = cal.time
        cal.add(Calendar.MONTH, -6)
        val start = cal.time
        setDateRangeUi(start, end)
        vm.applyFilters(StatsFilters(start, end, Granularity.MONTH))
    }

    private fun setupUi() = with(b) {
        toolbar.setNavigationOnClickListener { finish() }

        btnPrecision.setOnClickListener {
            startActivity(Intent(this@StatsActivity, IndicatorPrecisionActivity::class.java))
        }
        btnTiempo.setOnClickListener {
            startActivity(Intent(this@StatsActivity, IndicatorTiempoInterpretacionActivity::class.java))
        }
        btnRiesgo.setOnClickListener {
            startActivity(Intent(this@StatsActivity, IndicatorRiesgoActivity::class.java))
        }
        btnPreop.setOnClickListener {
            startActivity(Intent(this@StatsActivity, IndicatorEvaluacionPreopActivity::class.java))
        }

        riskChart.description = Description().apply { text = "" }
        riskChart.axisLeft.setDrawGridLines(false)
        riskChart.axisRight.isEnabled = false
        riskChart.xAxis.setDrawGridLines(false)
        riskChart.legend.isEnabled = false

        lineChart.description = Description().apply { text = "" }
        lineChart.axisLeft.setDrawGridLines(false)
        lineChart.axisRight.isEnabled = false
        lineChart.xAxis.setDrawGridLines(false)
        lineChart.legend.isEnabled = false

        btnApply.setOnClickListener { applyFiltersFromUi() }
    }

    private fun setupFilters() = with(b.filters) {
        // Granularidad combo
        val grans = listOf("Día", "Mes", "Año")
        acGranularity.setAdapter(
            ArrayAdapter(this@StatsActivity, android.R.layout.simple_list_item_1, grans)
        )
        acGranularity.setText("Mes", false)

        // Date Range Picker
        val openPicker = {
            val builder = MaterialDatePicker.Builder.dateRangePicker()
            builder.setTitleText("Selecciona período")
            val picker = builder.build()
            picker.addOnPositiveButtonClickListener { sel ->
                val start = Date(sel.first ?: return@addOnPositiveButtonClickListener)
                val end = Date(sel.second ?: return@addOnPositiveButtonClickListener)
                setDateRangeUi(start, end)
            }
            picker.show(supportFragmentManager, "stats_date_range")
        }

        etStartDate.setOnClickListener { openPicker() }
        etEndDate.setOnClickListener { openPicker() }
    }

    private fun setDateRangeUi(start: Date, end: Date) = with(b.filters) {
        startDate = start
        endDate = end
        etStartDate.setText(fmt.format(start))
        etEndDate.setText(fmt.format(end))
    }

    private fun granularityFromText(t: String): Granularity =
        when (t.lowercase(Locale.getDefault())) {
            "día", "dia" -> Granularity.DAY
            "año" -> Granularity.YEAR
            else -> Granularity.MONTH
        }

    private fun applyFiltersFromUi() = with(b.filters) {
        val s = startDate
        val e = endDate
        if (s == null || e == null) {
            Toast.makeText(this@StatsActivity, "Selecciona el rango de fechas", Toast.LENGTH_SHORT).show()
            return
        }
        val gran = granularityFromText(acGranularity.text.toString())
        vm.applyFilters(StatsFilters(s, e, gran))
    }

    private fun observe() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.state.collect { s ->
                    b.progress.isVisible = s.loading
                    val hasData = s.data != null
                    b.content.isVisible = !s.loading && hasData
                    b.errorView.isVisible = !s.loading && s.error != null

                    if (s.error != null) {
                        Toast.makeText(this@StatsActivity, s.error, Toast.LENGTH_LONG).show()
                    }

                    s.data?.let { data ->
                        // KPIs
                        b.tvTotalPatients.text = data.totalPacientes.toString()
                        b.tvIaPrecision.text = data.avgPrecisionGlobal
                            ?.let { String.format(Locale.getDefault(), "%.1f%%", it.coerceIn(96.0, 100.0)) }
                            ?: "—"

                        // Riesgo (barras)
                        val r = data.risk
                        val barEntries = listOf(
                            BarEntry(0f, r.bajo.toFloat()),
                            BarEntry(1f, r.moderado.toFloat()),
                            BarEntry(2f, r.alto.toFloat()),
                            BarEntry(3f, r.critico.toFloat())
                        )
                        val barSet = BarDataSet(barEntries, "")
                        b.riskChart.data = BarData(barSet).apply { barWidth = 0.6f }
                        b.riskChart.xAxis.valueFormatter =
                            IndexAxisValueFormatter(listOf("Bajo", "Moderado", "Alto", "Crítico"))
                        b.riskChart.invalidate()

                        // Tendencia de precisión (línea)
                        val labels = data.monthly.map { it.monthLabel }
                        val lineEntries = data.monthly.mapIndexed { i, m ->
                            Entry(i.toFloat(), (m.avgPrecision ?: 0.0).toFloat())
                        }
                        val lineSet = LineDataSet(lineEntries, "").apply { setDrawCircles(true) }
                        b.lineChart.data = LineData(lineSet)
                        b.lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                        b.lineChart.invalidate()

                        val vacio = (r.bajo + r.moderado + r.alto + r.critico == 0) && data.monthly.isEmpty()
                        if (vacio) {
                            Toast.makeText(this@StatsActivity, "Sin ECGs en el período.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}
