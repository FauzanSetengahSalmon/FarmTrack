package com.fauzan0022.farmtrack.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LivestockDto(
    @field:Json(name = "id") val id: Int,
    @field:Json(name = "user_email") val userEmail: String,
    @field:Json(name = "name") val name: String,
    @field:Json(name = "type") val type: String,
    @field:Json(name = "age") val age: Int,
    @field:Json(name = "weight") val weight: Double,
    @field:Json(name = "photo") val photo: String,
    @field:Json(name = "created_at") val createdAt: String? = null,
    @field:Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class OpStatusDto(
    @field:Json(name = "status") val status: String,
    @field:Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class LivestockUpdateDto(
    @field:Json(name = "name") val name: String,
    @field:Json(name = "type") val type: String,
    @field:Json(name = "age") val age: Int,
    @field:Json(name = "weight") val weight: Double
)
