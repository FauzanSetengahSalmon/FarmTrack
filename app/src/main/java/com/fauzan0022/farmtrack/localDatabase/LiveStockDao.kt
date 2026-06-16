package com.fauzan0022.farmtrack.localDatabase

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LivestockDao {
    @Query("SELECT * FROM livestock WHERE userEmail = :email AND syncStatus != 'PENDING_DELETE' ORDER BY localId DESC")
    fun getLivestockFlow(email: String): Flow<List<LivestockEntity>>
    @Query("SELECT * FROM livestock WHERE userEmail = :email AND syncStatus != 'PENDING_DELETE' ORDER BY localId DESC")
    suspend fun getLivestockList(email: String): List<LivestockEntity>
    @Query("SELECT * FROM livestock WHERE syncStatus != 'SYNCED'")
    suspend fun getUnsyncedLivestock(): List<LivestockEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LivestockEntity): Long
    @Update
    suspend fun update(entity: LivestockEntity)
    @Delete
    suspend fun delete(entity: LivestockEntity)
    @Query("DELETE FROM livestock WHERE userEmail = :email AND syncStatus = 'SYNCED'")
    suspend fun deleteSyncedForUser(email: String)
    @Query("SELECT * FROM livestock WHERE localId = :localId LIMIT 1")
    suspend fun getByLocalId(localId: Int): LivestockEntity?
    @Query("SELECT * FROM livestock WHERE id = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Int): LivestockEntity?
}
