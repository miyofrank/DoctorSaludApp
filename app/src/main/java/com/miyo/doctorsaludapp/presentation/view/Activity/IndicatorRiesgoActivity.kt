package com.miyo.doctorsaludapp.presentation.view.activity

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.activity.viewModels
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

class IndicatorRiesgoActivity : AppCompatActivity() {

    private lateinit var b: ActivityIndicatorRiesgoBinding
    private val vm: StatsViewModel by viewModels()

    private var selectedStart: Date? = null
    private var selectedEnd: Date? = null
    private var selectedGran = "month"
    private val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityIndicatorRiesgoBinding.inflate(layoutInflater)
        setContentView(b.root)

        setupUi()
        observe()
        vm.load()
    }

    private fun setupUi() = with(b) {
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.title = "Riesgo (ECG)"

        chart.description = Description().apply { text = "" }
        chart.axisLeft.setDrawGridLines(false)
        chart.axisRight.isEnabled = false
        chart.xAxis.setDrawGridLines(false)
        chart.legend.isEnabled = false

        etStart.setOnClickListener { pickDate(true) }
        etEnd.setOnClickListener { pickDate(false) }

        val items = listOf("Día", "Mes", "Año")
        (acGranularity as? AutoCompleteTextView)?.setAdapter(
            ArrayAdapter(this@IndicatorRiesgoActivity, android.R.layout.simple_list_item_1, items)
        )
        acGranularity.setText("Mes", false)
        acGranularity.setOnItemClickListener { _, _, pos, _ ->
            selectedGran = when (pos) { 0 -> "day"; 2 -> "year"; else -> "month" }
        }

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
        picker.show(supportFragmentManager, if (isStart) "start_risk" else "end_risk")
    }

    private fun applyFilters() {
        val f = StatsFilters(
            start = selectedStart ?: vm.state.value.filters.start,
            end = selectedEnd ?: vm.state.value.filters.end,
            granularity = selectedGran
        )
        vm.setFilters(f); vm.load(f)
    }

    private fun observe() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.state.collect { s ->
                    b.progress.isVisible = s.loading
                    b.content.isVisible = !s.loading
                    if (s.error != null) Toast.makeText(this@IndicatorRiesgoActivity, s.error, Toast.LENGTH_LONG).show()

                    s.data?.let { data ->
                        val r = data.risk
                        b.tvKpi.text = "B:${r.bajo}  M:${r.moderado}  A:${r.alto}  C:${r.critico}"
                        b.tvSubtitle.text = "Distribución total en el período"

                        val entries = listOf(
                            BarEntry(0f, r.bajo.toFloat()),
                            BarEntry(1f, r.moderado.toFloat()),
                            BarEntry(2f, r.alto.toFloat()),
                            BarEntry(3f, r.critico.toFloat())
                        )
                        val set = BarDataSet(entries, "")
                        b.chart.data = BarData(set).apply { barWidth = 0.6f }
                        b.chart.xAxis.valueFormatter = IndexAxisValueFormatter(listOf("Bajo","Moderado","Alto","Crítico"))
                        b.chart.invalidate()
                    }
                }
            }
        }
    }
}
