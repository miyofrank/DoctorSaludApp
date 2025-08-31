package com.miyo.doctorsaludapp.data.mappers

import com.miyo.doctorsaludapp.data.remote.UserDTO
import com.miyo.doctorsaludapp.domain.model.User

fun UserDTO.toDomain(): User {
    return User(email = this.email, userId = this.userId)
}