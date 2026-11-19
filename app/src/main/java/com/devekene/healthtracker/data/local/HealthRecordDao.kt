package com.devekene.healthtracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthRecordDao {

    @Query("SELECT * FROM health_records ORDER BY timestampMillis DESC")
    fun getAllRecordsFlow(): Flow<List<HealthRecordEntity>>

    @Query("SELECT * FROM health_records ORDER BY timestampMillis DESC LIMIT 1")
    fun getLatestRecordFlow(): Flow<HealthRecordEntity?>

    @Query("SELECT * FROM health_records ORDER BY timestampMillis DESC")
    suspend fun getAllRecordsSnapshot(): List<HealthRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(entity: HealthRecordEntity): Long

    @Query("DELETE FROM health_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)

    @Query("DELETE FROM health_records")
    suspend fun deleteAllRecords()
}
