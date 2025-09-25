package com.miyo.doctorsaludapp.presentation.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.miyo.doctorsaludapp.databinding.ItemMonthTrendBinding
import com.miyo.doctorsaludapp.domain.model.stats.MonthlyPoint
import java.util.Locale

class MonthlyTrendAdapter : RecyclerView.Adapter<MonthlyTrendAdapter.VH>() {

    private val data = mutableListOf<MonthlyPoint>()

    fun submitList(items: List<MonthlyPoint>) {
        data.clear(); data.addAll(items); notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemMonthTrendBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(data[position])

    override fun getItemCount(): Int = data.size

    class VH(private val b: ItemMonthTrendBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(m: MonthlyPoint) = with(b) {
            tvMonth.text = m.monthLabel
            tvPrecision.text = m.avgPrecision?.let { String.format(Locale.getDefault(), "%.1f%%", it) } ?: "—"
            tvTime.text = m.avgIaSeconds?.let { String.format(Locale.getDefault(), "%.1fs", it) } ?: "—"
        }
    }
}
