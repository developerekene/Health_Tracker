package com.devekene.healthtracker.data.repository

import com.devekene.healthtracker.core.model.HealthRecord
import com.devekene.healthtracker.core.model.UnitSystem
import com.devekene.healthtracker.data.local.HealthRecordDao
import com.devekene.healthtracker.data.local.toDomain
import com.devekene.healthtracker.data.local.toEntity
import com.devekene.healthtracker.domain.HealthBackupSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface HealthRepository {
    val recordsFlow: Flow<List<HealthRecord>>
    val latestRecordFlow: Flow<HealthRecord?>

    suspend fun saveRecord(record: HealthRecord): Long
    suspend fun deleteRecord(id: Long)
    suspend fun clearAllRecords()
    suspend fun buildJsonBackup(unitSystem: UnitSystem): Pair<Int, String>
    suspend fun buildCsvBackup(): Pair<Int, String>
}

class HealthRepositoryImpl(
    private val dao: HealthRecordDao
) : HealthRepository {

    override val recordsFlow: Flow<List<HealthRecord>> =
        dao.getAllRecordsFlow().map { entities -> entities.map { it.toDomain() } }

    override val latestRecordFlow: Flow<HealthRecord?> =
        dao.getLatestRecordFlow().map { it?.toDomain() }

    override suspend fun saveRecord(record: HealthRecord): Long = withContext(Dispatchers.IO) {
        dao.insertRecord(record.toEntity())
    }

    override suspend fun deleteRecord(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteRecordById(id)
    }

    override suspend fun clearAllRecords() = withContext(Dispatchers.IO) {
        dao.deleteAllRecords()
    }

    override suspend fun buildJsonBackup(unitSystem: UnitSystem): Pair<Int, String> =
        withContext(Dispatchers.IO) {
            val records = dao.getAllRecordsSnapshot().map { it.toDomain() }
            records.size to HealthBackupSerializer.toJson(records, unitSystem)
        }

    override suspend fun buildCsvBackup(): Pair<Int, String> =
        withContext(Dispatchers.IO) {
            val records = dao.getAllRecordsSnapshot().map { it.toDomain() }
            records.size to HealthBackupSerializer.toCsv(records)
        }
}
