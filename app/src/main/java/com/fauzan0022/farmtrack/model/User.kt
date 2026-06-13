package com.fauzan0022.farmtrack.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class User(
    val name: String = "",
    val email: String = "",
    val photoUrl: String = ""
) {
    fun isEmpty(): Boolean = email.isEmpty()
}

