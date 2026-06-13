package com.fauzan0022.farmtrack.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LivestockDto(
    @Json(name = "id") val id: Int,
    @Json(name = "user_email") val userEmail: String,
    @Json(name = "name") val name: String,
    @Json(name = "type") val type: String,
    @Json(name = "age") val age: Int,
    @Json(name = "weight") val weight: Double,
    @Json(name = "photo") val photo: String,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class OpStatusDto(
    @Json(name = "status") val status: String,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class LivestockUpdateDto(
    @Json(name = "name") val name: String,
    @Json(name = "type") val type: String,
    @Json(name = "age") val age: Int,
    @Json(name = "weight") val weight: Double
)
