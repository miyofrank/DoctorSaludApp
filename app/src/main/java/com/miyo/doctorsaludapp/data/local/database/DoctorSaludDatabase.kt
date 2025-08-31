package com.miyo.doctorsaludapp.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.miyo.doctorsaludapp.data.local.dao.PacienteDao
import com.miyo.doctorsaludapp.data.local.dao.UserDao

abstract class DoctorSaludDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun pacienteDao(): PacienteDao
    companion object {
        @Volatile
        private var INSTANCE: DoctorSaludDatabase? = null

        fun getDatabase(context: Context): DoctorSaludDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DoctorSaludDatabase::class.java,
                    "doctorsalud_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }


}