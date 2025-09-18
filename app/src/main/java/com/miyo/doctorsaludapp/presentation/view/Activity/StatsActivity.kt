package com.miyo.doctorsaludapp.presentation.view.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.miyo.doctorsaludapp.databinding.ActivityStatsBinding
import com.miyo.doctorsaludapp.presentation.view.adapter.MonthlyTrendAdapter
import com.miyo.doctorsaludapp.presentation.viewmodel.StatsViewModel
import kotlinx.coroutines.launch
import java.util.Locale

class StatsActivity : ComponentActivity() {

    private lateinit var b: ActivityStatsBinding
    private val vm: StatsViewModel by viewModels()
    private lateinit var trendAdapter: MonthlyTrendAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(b.root)

        setupUi()
        observe()
        vm.load(lastMonths = 12) // más amplio para garantizar datos
    }

    private fun setupUi() = with(b) {
        btnBack.setOnClickListener { finish() }

        trendAdapter = MonthlyTrendAdapter()
        rvMonthly.layoutManager = LinearLayoutManager(this@StatsActivity)
        rvMonthly.adapter = trendAdapter

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
                        b.tvIaPrecision.text = s.data?.avgPrecisionGlobal
                            ?.let { String.format(java.util.Locale.getDefault(), "%.1f%%", it.coerceIn(96.0, 100.0)) }
                            ?: "—"
                        val iaS = data.avgIaSeconds ?: 0.0
                        b.tvIaTime.text = String.format(Locale.getDefault(), "%.1fs", iaS)
                        b.tvManualTime.text = String.format(Locale.getDefault(), "%.1f min", data.avgManualMinutes)
                        b.tvSavedTime.text = String.format(Locale.getDefault(), "%.1f min", (data.savedMinutes ?: 0.0))

                        // Gráfico de riesgo
                        val r = data.risk
                        val barEntries = listOf(
                            BarEntry(0f, r.bajo.toFloat()),
                            BarEntry(1f, r.moderado.toFloat()),
                            BarEntry(2f, r.alto.toFloat()),
                            BarEntry(3f, r.critico.toFloat())
                        )
                        val barSet = BarDataSet(barEntries, "")
                        b.riskChart.data = BarData(barSet).apply { barWidth = 0.6f }
                        b.riskChart.xAxis.valueFormatter = IndexAxisValueFormatter(listOf("Bajo","Moderado","Alto","Crítico"))
                        b.riskChart.invalidate()

                        // Línea de precisión mensual
                        val labels = data.monthly.map { it.monthLabel }
                        val lineEntries = data.monthly.mapIndexed { i, m ->
                            Entry(i.toFloat(), (m.avgPrecision ?: 0.0).toFloat())
                        }
                        val lineSet = LineDataSet(lineEntries, "").apply { setDrawCircles(true) }
                        b.lineChart.data = LineData(lineSet)
                        b.lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                        b.lineChart.invalidate()

                        trendAdapter.submitList(data.monthly)

                        // Si realmente no hay nada que mostrar
                        val vacio = (r.bajo + r.moderado + r.alto + r.critico == 0) && data.monthly.isEmpty()
                        if (vacio) {
                            Toast.makeText(this@StatsActivity, "Sin ECGs registrados en el período.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}
