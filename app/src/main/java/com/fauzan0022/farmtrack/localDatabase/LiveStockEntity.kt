package com.fauzan0022.farmtrack.localDatabase

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "livestock")
data class LivestockEntity(
    @PrimaryKey(autoGenerate = true) val localId: Int = 0,
    val id: Int? = null,
    val userEmail: String,
    val name: String,
    val type: String,
    val age: Int,
    val weight: Double,
    val photo: String,
    val syncStatus: String = "SYNCED",
    val createdAt: String? = null,
    val updatedAt: String? = null
)

