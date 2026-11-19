package com.devekene.healthtracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.devekene.healthtracker.data.security.DatabaseKeyManager
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [HealthRecordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class HealthTrackerDatabase : RoomDatabase() {

    abstract fun healthRecordDao(): HealthRecordDao

    companion object {
        private const val DATABASE_NAME = "health_tracker_encrypted.db"

        @Volatile
        private var INSTANCE: HealthTrackerDatabase? = null

        fun getInstance(context: Context): HealthTrackerDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(appContext: Context): HealthTrackerDatabase {
            val builder = Room.databaseBuilder(
                appContext,
                HealthTrackerDatabase::class.java,
                DATABASE_NAME
            )

            if (!DatabaseKeyManager.isRobolectricEnvironment()) {
                System.loadLibrary("sqlcipher")
                val passphrase = DatabaseKeyManager.getOrCreateDatabasePassphrase(appContext)
                builder.openHelperFactory(SupportOpenHelperFactory(passphrase))
            }

            return builder
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }
    }
}
