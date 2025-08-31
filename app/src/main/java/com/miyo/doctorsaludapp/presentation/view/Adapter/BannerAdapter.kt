package com.miyo.doctorsaludapp.presentation.view.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.miyo.doctorsaludapp.databinding.ItemBannerBinding
import com.miyo.doctorsaludapp.domain.model.Banner

class BannerAdapter(private var banners: List<Banner>) : RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {

    inner class BannerViewHolder(private val binding: ItemBannerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(banner: Banner) {
            Glide.with(binding.imageView.context)
                .load(banner.imageUrl)
                .into(binding.imageView)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val binding = ItemBannerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BannerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        holder.bind(banners[position])
    }

    override fun getItemCount(): Int = banners.size

    fun updateBanners(banners: List<Banner>) {
        this.banners = banners
        notifyDataSetChanged()
    }
}