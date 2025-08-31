package com.miyo.doctorsaludapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val uid: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val specialization: String,
    val experienceYears: Int
)
