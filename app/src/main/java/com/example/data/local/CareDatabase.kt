package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cached_records")
data class CachedRecordEntity(
    @PrimaryKey val cacheKey: String,
    val ownerUid: String,
    val collectionName: String,
    val payloadJson: String,
    val updatedAtMillis: Long
)

@Entity(tableName = "pending_sync_ops")
data class PendingSyncOpEntity(
    @PrimaryKey(autoGenerate = true) val opId: Long = 0L,
    val ownerUid: String,
    val collectionName: String,
    val documentId: String,
    val opType: String, // "SET" or "UPDATE"
    val payloadJson: String,
    val createdAtMillis: Long
)

@Dao
interface CareDao {
    @Query("SELECT * FROM cached_records WHERE cacheKey = :cacheKey LIMIT 1")
    suspend fun getCachedRecord(cacheKey: String): CachedRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCachedRecord(entity: CachedRecordEntity)

    @Query("SELECT * FROM pending_sync_ops WHERE ownerUid = :ownerUid ORDER BY createdAtMillis ASC")
    fun observePendingOps(ownerUid: String): Flow<List<PendingSyncOpEntity>>

    @Query("SELECT * FROM pending_sync_ops WHERE ownerUid = :ownerUid ORDER BY createdAtMillis ASC")
    suspend fun getPendingOps(ownerUid: String): List<PendingSyncOpEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingOp(op: PendingSyncOpEntity)

    @Query("DELETE FROM pending_sync_ops WHERE opId = :opId")
    suspend fun deletePendingOp(opId: Long)

    @Query("DELETE FROM cached_records WHERE ownerUid = :ownerUid")
    suspend fun clearCacheForUser(ownerUid: String)
}

@Database(
    entities = [CachedRecordEntity::class, PendingSyncOpEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CareDatabase : RoomDatabase() {
    abstract fun careDao(): CareDao

    companion object {
        @Volatile
        private var INSTANCE: CareDatabase? = null

        fun getInstance(context: Context): CareDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CareDatabase::class.java,
                    "care_connect_cache.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
