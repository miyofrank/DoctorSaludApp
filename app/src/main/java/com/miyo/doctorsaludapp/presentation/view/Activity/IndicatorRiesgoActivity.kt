package com.miyo.doctorsaludapp.presentation.view.activity

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
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
import com.miyo.doctorsaludapp.domain.stats.Granularity
import com.miyo.doctorsaludapp.domain.stats.StatsFilters
import com.miyo.doctorsaludapp.presentation.viewmodel.StatsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class IndicatorRiesgoActivity : AppCompatActivity() {

    private lateinit var b: ActivityIndicatorRiesgoBinding
    private val vm: StatsViewModel by viewModels()

    private val fmt by lazy { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    private var startDate: Date? = null
    private var endDate: Date? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityIndicatorRiesgoBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.toolbar.setNavigationOnClickListener { finish() }

        setupFilters()
        setupChart()
        observe()

        val cal = Calendar.getInstance()
        val end = cal.time
        cal.add(Calendar.MONTH, -6)
        val start = cal.time
        setDateRangeUi(start, end)
        vm.applyFilters(StatsFilters(start, end, Granularity.MONTH))
    }

    private fun setupChart() = with(b.barChart) {
        description = Description().apply { text = "" }
        axisLeft.setDrawGridLines(false)
        axisRight.isEnabled = false
        xAxis.setDrawGridLines(false)
        legend.isEnabled = false
    }

    private fun setupFilters() = with(b.filters) {
        val grans = listOf("Día","Mes","Año")
        acGranularity.setAdapter(
            ArrayAdapter(this@IndicatorRiesgoActivity, android.R.layout.simple_list_item_1, grans)
        )
        acGranularity.setText("Mes", false)

        val openPicker = {
            val picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Selecciona período")
                .build()
            picker.addOnPositiveButtonClickListener {
                val s = it.first ?: return@addOnPositiveButtonClickListener
                val e = it.second ?: return@addOnPositiveButtonClickListener
                setDateRangeUi(Date(s), Date(e))
            }
            picker.show(supportFragmentManager, "riesgo_range")
        }
        etStartDate.setOnClickListener { openPicker() }
        etEndDate.setOnClickListener { openPicker() }

        b.btnApply.setOnClickListener { applyFiltersFromUi() }
    }

    private fun setDateRangeUi(start: Date, end: Date) = with(b.filters) {
        startDate = start
        endDate = end
        etStartDate.setText(fmt.format(start))
        etEndDate.setText(fmt.format(end))
    }

    private fun granularityFromText(t: String): Granularity =
        when (t.lowercase(Locale.getDefault())) {
            "día","dia" -> Granularity.DAY
            "año" -> Granularity.YEAR
            else -> Granularity.MONTH
        }

    private fun applyFiltersFromUi() {
        val s = startDate; val e = endDate
        if (s == null || e == null) {
            Toast.makeText(this, "Selecciona el rango", Toast.LENGTH_SHORT).show()
            return
        }
        val gran = granularityFromText(b.filters.acGranularity.text.toString())
        vm.applyFilters(StatsFilters(s, e, gran))
    }

    private fun observe() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.state.collect { s ->
                    b.progress.isVisible = s.loading
                    b.content.isVisible = !s.loading && s.data != null
                    s.data?.let { d ->
                        val r = d.risk
                        b.tvKpi.text = "Bajo: ${r.bajo} • Moderado: ${r.moderado} • Alto: ${r.alto} • Crítico: ${r.critico}"

                        val entries = listOf(
                            BarEntry(0f, r.bajo.toFloat()),
                            BarEntry(1f, r.moderado.toFloat()),
                            BarEntry(2f, r.alto.toFloat()),
                            BarEntry(3f, r.critico.toFloat())
                        )
                        val set = BarDataSet(entries, "")
                        b.barChart.data = BarData(set).apply { barWidth = 0.6f }
                        b.barChart.xAxis.valueFormatter = IndexAxisValueFormatter(listOf("Bajo","Mod.","Alto","Crít."))
                        b.barChart.invalidate()
                    }
                }
            }
        }
    }
}
