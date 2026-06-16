package com.fauzan0022.farmtrack.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import androidx.core.graphics.toColorInt
import com.fauzan0022.farmtrack.localDatabase.LivestockDao
import com.fauzan0022.farmtrack.localDatabase.LivestockEntity
import com.fauzan0022.farmtrack.network.FarmTrackApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class LivestockRepository(
    private val context: Context,
    private val livestockDao: LivestockDao,
    private val apiService: FarmTrackApiService
) {
    companion object {
        private const val TAG = "LivestockRepository"
    }

    private fun getCurrentFormattedTime(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
    }

    fun getLivestockFlow(email: String): Flow<List<LivestockEntity>> =
        livestockDao.getLivestockFlow(email)

    fun saveBitmapToCache(bitmap: Bitmap): String {
        val directory = File(context.filesDir, "saved_livestock_images")
        if (!directory.exists()) directory.mkdirs()

        val file = File(directory, "livestock_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            out.flush()
        }
        return file.absolutePath
    }

    fun createPlaceholderImage(name: String): String {
        val bitmap = createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = "#4CAF50".toColorInt()
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, 256f, 256f, paint)

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 64f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val firstChar = if (name.isNotEmpty()) name.substring(0, 1).uppercase() else "L"
        val yPos = (canvas.height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(firstChar, canvas.width / 2f, yPos, textPaint)

        return saveBitmapToCache(bitmap)
    }

    suspend fun addLivestock(
        userEmail: String,
        name: String,
        type: String,
        age: Int,
        weight: Double,
        photoPath: String,
        isOnline: Boolean
    ): String = withContext(Dispatchers.IO) {
        if (isOnline) {
            try {
                val photoFile = File(photoPath)
                if (photoFile.exists()) {
                    val requestFile = photoFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val multipartPhoto = MultipartBody.Part.createFormData("photo", photoFile.name, requestFile)

                    val emailBody = userEmail.toRequestBody("text/plain".toMediaTypeOrNull())
                    val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
                    val typeBody = type.toRequestBody("text/plain".toMediaTypeOrNull())
                    val ageBody = age.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                    val weightBody = weight.toString().toRequestBody("text/plain".toMediaTypeOrNull())

                    val result = apiService.storeLivestock(
                        emailBody, nameBody, typeBody, ageBody, weightBody, multipartPhoto
                    )
                    if (result.status == "success") {
                        sync(userEmail, true)
                        return@withContext "success"
                    } else {
                        return@withContext result.message ?: "Ditolak oleh server"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gagal menambah data ternak secara online", e)
            }
        }

        val now = getCurrentFormattedTime()
        val entity = LivestockEntity(
            userEmail = userEmail,
            name = name,
            type = type,
            age = age,
            weight = weight,
            photo = photoPath,
            syncStatus = "PENDING_INSERT",
            createdAt = now,
            updatedAt = now
        )
        livestockDao.insert(entity)
        "saved_offline"
    }

    suspend fun updateLivestock(
        entity: LivestockEntity,
        name: String,
        type: String,
        age: Int,
        weight: Double,
        newBitmap: Bitmap?,
        isOnline: Boolean
    ): String = withContext(Dispatchers.IO) {

        var finalPhotoPath = entity.photo
        var photoPart: MultipartBody.Part? = null

        if (newBitmap != null) {
            finalPhotoPath = saveBitmapToCache(newBitmap)
            val file = File(finalPhotoPath)
            val reqFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            photoPart = MultipartBody.Part.createFormData("photo", file.name, reqFile)
        }

        if (isOnline && entity.id != null) {
            try {
                val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
                val typeBody = type.toRequestBody("text/plain".toMediaTypeOrNull())
                val ageBody = age.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val weightBody = weight.toString().toRequestBody("text/plain".toMediaTypeOrNull())

                val result = apiService.updateLivestock(
                    entity.id, nameBody, typeBody, ageBody, weightBody, photoPart
                )

                if (result.status == "success") {
                    val now = getCurrentFormattedTime()
                    livestockDao.update(
                        entity.copy(
                            name = name,
                            type = type,
                            age = age,
                            weight = weight,
                            photo = finalPhotoPath,
                            syncStatus = "SYNCED",
                            updatedAt = now
                        )
                    )
                    sync(entity.userEmail, true)

                    return@withContext "success"
                } else {
                    return@withContext result.message ?: "Gagal memperbarui data"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gagal memperbarui data ternak secara online", e)
            }
        }

        val now = getCurrentFormattedTime()
        val newStatus = if (entity.syncStatus == "PENDING_INSERT") "PENDING_INSERT" else "PENDING_UPDATE"
        val updatedEntity = entity.copy(
            name = name,
            type = type,
            age = age,
            weight = weight,
            photo = finalPhotoPath,
            syncStatus = newStatus,
            updatedAt = now
        )
        livestockDao.update(updatedEntity)
        "saved_offline"
    }

    suspend fun deleteLivestock(
        entity: LivestockEntity,
        isOnline: Boolean
    ): String = withContext(Dispatchers.IO) {
        if (isOnline && entity.id != null) {
            try {
                val result = apiService.deleteLivestock(entity.id)
                if (result.status == "success") {
                    livestockDao.delete(entity)
                    return@withContext "success"
                } else {
                    return@withContext result.message ?: "Gagal menghapus data"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gagal menghapus data ternak secara online", e)
            }
        }
        if (entity.syncStatus == "PENDING_INSERT") {
            livestockDao.delete(entity)
        } else {
            livestockDao.update(entity.copy(syncStatus = "PENDING_DELETE"))
        }
        "deleted_offline"
    }

    suspend fun sync(userEmail: String, isOnline: Boolean) = withContext(Dispatchers.IO) {
        if (!isOnline || userEmail.isEmpty()) return@withContext

        val unsynced = livestockDao.getUnsyncedLivestock()
        val pendingDelete = unsynced.filter { it.userEmail == userEmail && it.syncStatus == "PENDING_DELETE" }
        for (item in pendingDelete) {
            if (item.id != null) {
                try {
                    val result = apiService.deleteLivestock(item.id)
                    if (result.status == "success") {
                        livestockDao.delete(item)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Gagal menyinkronkan penghapusan yang tertunda untuk ID: ${item.id}", e)
                }
            } else {
                livestockDao.delete(item)
            }
        }

        val pendingInsert = unsynced.filter { it.userEmail == userEmail && it.syncStatus == "PENDING_INSERT" }
        for (item in pendingInsert) {
            try {
                val photoFile = File(item.photo)
                if (photoFile.exists()) {
                    val requestFile = photoFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val multipartPhoto = MultipartBody.Part.createFormData("photo", photoFile.name, requestFile)

                    val emailBody = item.userEmail.toRequestBody("text/plain".toMediaTypeOrNull())
                    val nameBody = item.name.toRequestBody("text/plain".toMediaTypeOrNull())
                    val typeBody = item.type.toRequestBody("text/plain".toMediaTypeOrNull())
                    val ageBody = item.age.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                    val weightBody = item.weight.toString().toRequestBody("text/plain".toMediaTypeOrNull())

                    val result = apiService.storeLivestock(
                        emailBody, nameBody, typeBody, ageBody, weightBody, multipartPhoto
                    )
                    if (result.status == "success") {
                        livestockDao.delete(item)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gagal menyinkronkan penambahan data baru yang tertunda", e)
            }
        }

        val pendingUpdate = unsynced.filter { it.userEmail == userEmail && it.syncStatus == "PENDING_UPDATE" }
        for (item in pendingUpdate) {
            if (item.id != null) {
                try {
                    val nameBody = item.name.toRequestBody("text/plain".toMediaTypeOrNull())
                    val typeBody = item.type.toRequestBody("text/plain".toMediaTypeOrNull())
                    val ageBody = item.age.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                    val weightBody = item.weight.toString().toRequestBody("text/plain".toMediaTypeOrNull())

                    var photoPart: MultipartBody.Part? = null

                    if (item.photo.isNotBlank() && !item.photo.startsWith("http")) {
                        val file = File(item.photo)
                        if (file.exists()) {
                            val reqFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                            photoPart = MultipartBody.Part.createFormData("photo", file.name, reqFile)
                        }
                    }

                    val result = apiService.updateLivestock(
                        item.id, nameBody, typeBody, ageBody, weightBody, photoPart
                    )
                    if (result.status == "success") {
                        livestockDao.update(item.copy(syncStatus = "SYNCED"))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Gagal menyinkronkan pembaruan data yang tertunda untuk ID: ${item.id}", e)
                }
            }
        }

        try {
            val serverList = apiService.getLivestock(userEmail)
            livestockDao.deleteSyncedForUser(userEmail)

            for (dto in serverList) {
                val cached = livestockDao.getByServerId(dto.id)
                if (cached == null) {
                    val entity = LivestockEntity(
                        id = dto.id,
                        userEmail = dto.userEmail,
                        name = dto.name,
                        type = dto.type,
                        age = dto.age,
                        weight = dto.weight,
                        photo = dto.photo,
                        syncStatus = "SYNCED",
                        createdAt = dto.createdAt,
                        updatedAt = dto.updatedAt
                    )
                    livestockDao.insert(entity)
                } else {
                    livestockDao.update(
                        cached.copy(
                            id = dto.id,
                            name = dto.name,
                            type = dto.type,
                            age = dto.age,
                            weight = dto.weight,
                            photo = dto.photo,
                            syncStatus = "SYNCED",
                            createdAt = dto.createdAt ?: cached.createdAt,
                            updatedAt = dto.updatedAt ?: cached.updatedAt
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengambil dan menyinkronkan daftar data dari server", e)
        }
    }
}