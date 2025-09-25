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
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.datepicker.MaterialDatePicker
import com.miyo.doctorsaludapp.databinding.ActivityIndicatorPrecisionBinding
import com.miyo.doctorsaludapp.domain.stats.Granularity
import com.miyo.doctorsaludapp.domain.stats.StatsFilters
import com.miyo.doctorsaludapp.presentation.viewmodel.StatsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class IndicatorPrecisionActivity : AppCompatActivity() {

    private lateinit var b: ActivityIndicatorPrecisionBinding
    private val vm: StatsViewModel by viewModels()

    private val fmt by lazy { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    private var startDate: Date? = null
    private var endDate: Date? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityIndicatorPrecisionBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.toolbar.setNavigationOnClickListener { finish() }

        setupFilters()
        setupChart()
        observe()

        // Inicial
        val cal = Calendar.getInstance()
        val end = cal.time
        cal.add(Calendar.MONTH, -6)
        val start = cal.time
        setDateRangeUi(start, end)
        vm.applyFilters(StatsFilters(start, end, Granularity.MONTH))
    }

    private fun setupChart() = with(b.lineChart) {
        description = Description().apply { text = "" }
        axisLeft.setDrawGridLines(false)
        axisRight.isEnabled = false
        xAxis.setDrawGridLines(false)
        legend.isEnabled = false
    }

    private fun setupFilters() = with(b.filters) {
        // Granularidad
        val grans = listOf("Día","Mes","Año")
        acGranularity.setAdapter(
            ArrayAdapter(this@IndicatorPrecisionActivity, android.R.layout.simple_list_item_1, grans)
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
            picker.show(supportFragmentManager, "precision_range")
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
                        b.tvKpi.text = d.avgPrecisionGlobal?.let { String.format(Locale.getDefault(),"%.1f%%", it) } ?: "—"

                        val labels = d.monthly.map { it.monthLabel }
                        val entries = d.monthly.mapIndexed { i, m ->
                            Entry(i.toFloat(), (m.avgPrecision ?: 0.0).toFloat())
                        }
                        val set = LineDataSet(entries, "").apply { setDrawCircles(true) }
                        b.lineChart.data = LineData(set)
                        b.lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                        b.lineChart.invalidate()
                    }
                }
            }
        }
    }
}
