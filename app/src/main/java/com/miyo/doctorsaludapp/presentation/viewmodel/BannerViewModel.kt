package com.miyo.doctorsaludapp.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.miyo.doctorsaludapp.domain.model.Banner

class BannerViewModel : ViewModel() {
    private val _banners = MutableLiveData<List<Banner>>()
    val banners: LiveData<List<Banner>> get() = _banners

    fun getBanners() {
        val database = FirebaseDatabase.getInstance().getReference("banners")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val bannerList = mutableListOf<Banner>()
                for (bannerSnapshot in snapshot.children) {
                    val banner = bannerSnapshot.getValue(Banner::class.java)
                    if (banner != null) {
                        bannerList.add(banner)
                    }
                }
                _banners.value = bannerList
            }

            override fun onCancelled(error: DatabaseError) {
                // manejar el error
            }
        })
    }
}

