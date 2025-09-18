package com.miyo.doctorsaludapp.presentation.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.miyo.doctorsaludapp.databinding.ItemMonthTrendBinding
import com.miyo.doctorsaludapp.domain.model.stats.MonthTrend
import java.util.Locale

class MonthlyTrendAdapter :
    ListAdapter<MonthTrend, MonthlyTrendAdapter.VH>(DIFF) {

    inner class VH(private val b: ItemMonthTrendBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(m: MonthTrend) = with(b) {
            tvMonth.text = m.monthLabel
            tvCount.text = "${m.count} pacientes"
            tvIaTime.text = m.avgIaSeconds?.let { String.format(Locale.getDefault(), "%.1fs", it) } ?: "—"
            tvPrecision.text = m.avgPrecision?.let { String.format(Locale.getDefault(), "%.1f%%", it) } ?: "—"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemMonthTrendBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<MonthTrend>() {
            override fun areItemsTheSame(o: MonthTrend, n: MonthTrend) = o.year == n.year && o.month == n.month
            override fun areContentsTheSame(o: MonthTrend, n: MonthTrend) = o == n
        }
    }
}
