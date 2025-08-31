package com.miyo.doctorsaludapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.miyo.doctorsaludapp.data.local.entity.PacienteEntity

@Dao
interface PacienteDao {
    @Query("SELECT * FROM paciente")
    suspend fun getAllPacientes(): List<PacienteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaciente(paciente: PacienteEntity)

    @Update
    suspend fun updatePaciente(paciente: PacienteEntity)

    @Delete
    suspend fun deletePaciente(paciente: PacienteEntity)
}