package com.fauzan0022.farmtrack.ui.screen

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fauzan0022.farmtrack.R
import com.fauzan0022.farmtrack.localDatabase.LivestockEntity
import java.io.File
import java.io.IOException
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditLivestockDialog(
    entity: LivestockEntity? = null,
    onDismissRequest: () -> Unit,
    onConfirm: (name: String, type: String, age: Int, weight: Double, imageBitmap: Bitmap?) -> Unit
) {
    val context = LocalContext.current
    val isEdit = entity != null

    var name by remember { mutableStateOf(entity?.name ?: "") }
    var type by remember { mutableStateOf(entity?.type ?: "") }
    var ageStr by remember { mutableStateOf(entity?.age?.toString() ?: "") }
    var weightStr by remember { mutableStateOf(entity?.weight?.toString() ?: "") }

    var imageUriForDisplay by remember { mutableStateOf<Uri?>(null) }
    var showSourceOptions by remember { mutableStateOf(false) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val oldServerPhotoUrl = remember(entity) {
        entity?.photo?.let { photoPath ->
            if (photoPath.startsWith("http") || photoPath.startsWith("/")) {
                photoPath
            } else {
                "https://farmtrack-api-production-51d6.up.railway.app/storage/$photoPath"
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            imageUriForDisplay = uri
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            imageUriForDisplay = cameraImageUri
        }
    }

    fun createCameraImageUri(): Uri {
        val directory = File(context.cacheDir, "camera_images")
        if (!directory.exists()) directory.mkdirs()
        val file = File(directory, "capture_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun getSafeCompressedBitmap(uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 4
                }
                BitmapFactory.decodeStream(inputStream, null, options)
            }
        } catch (e: IOException) {
            Log.e("BitmapError", "Gagal kompres gambar", e)
            null
        }
    }

    val ageParsed = ageStr.toIntOrNull()
    val isAgeError = ageStr.isNotEmpty() && (ageParsed == null || ageParsed <= 0 || ageParsed > 480)
    val ageErrorText = when {
        ageStr.isNotEmpty() && ageParsed == null -> "Harus angka bulat"
        ageParsed != null && ageParsed <= 0 -> "Harus > 0"
        ageParsed != null && ageParsed > 480 -> "Tidak realistis"
        else -> ""
    }

    val weightParsed = weightStr.toDoubleOrNull()
    val isWeightError =
        weightStr.isNotEmpty() && (weightParsed == null || weightParsed <= 0.5 || weightParsed > 3000.0)
    val weightErrorText = when {
        weightStr.isNotEmpty() && weightParsed == null -> "Harus angka desimal"
        weightParsed != null && weightParsed <= 0.1 -> "Harus > 0.1 kg"
        weightParsed != null && weightParsed > 30000.0 -> "Terlalu berat"
        else -> ""
    }

    val isNameError = name.isNotEmpty() && (name.isBlank() || name.length < 2 || name.length > 50)
    val nameErrorText = when {
        name.isNotEmpty() && name.isBlank() -> "Tidak boleh kosong"
        name.isNotEmpty() && name.length < 2 -> "Terlalu pendek (min 2)"
        name.isNotEmpty() && name.length > 50 -> "Terlalu panjang (max 50)"
        else -> ""
    }

    val isTypeError = type.isNotEmpty() && (type.isBlank() || type.length < 2 || type.length > 50)
    val typeErrorText = when {
        type.isNotEmpty() && type.isBlank() -> "Tidak boleh kosong"
        type.isNotEmpty() && type.length < 2 -> "Terlalu pendek (min 2)"
        type.isNotEmpty() && type.length > 50 -> "Terlalu panjang (max 50)"
        else -> ""
    }

    val isFormComplete = name.isNotBlank() && type.isNotBlank() &&
            ageParsed != null && ageParsed > 0 &&
            weightParsed != null && weightParsed > 0.0 &&
            !isAgeError && !isWeightError && !isNameError && !isTypeError

    val forestGreen = Color(0xFF1B6A4F)

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedBorderColor = forestGreen,
        unfocusedBorderColor = Color(0xFFCBD5E1),
        focusedLabelColor = forestGreen,
        unfocusedLabelColor = Color(0xFF64748B),
        cursorColor = forestGreen,
        errorBorderColor = Color(0xFFEF4444),
        errorLabelColor = Color(0xFFEF4444),
        errorTextColor = MaterialTheme.colorScheme.onSurface
    )

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 12.dp)
                .systemBarsPadding(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isEdit) stringResource(R.string.ubah_data_ternak) else stringResource(
                        R.string.tambah_ternak_baru
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = forestGreen,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                val finalImageSource: Any? = when {
                    imageUriForDisplay != null -> imageUriForDisplay
                    oldServerPhotoUrl != null -> oldServerPhotoUrl
                    else -> null
                }

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(
                            BorderStroke(
                                1.5.dp,
                                if (finalImageSource != null) forestGreen else Color(0xFFE2E8F0)
                            ),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { showSourceOptions = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (finalImageSource != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(finalImageSource)
                                .crossfade(true)
                                .placeholder(R.drawable.broken_image)
                                .error(R.drawable.broken_image)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Ubah Foto",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = null,
                                tint = forestGreen,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                stringResource(R.string.pilih_foto),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = forestGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.nama_ternak), fontSize = 13.sp) },
                    placeholder = {
                        Text(
                            "Contoh: Si Black, Boni, Moly",
                            color = Color(0xFF94A3B8)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (isNameError) Color(0xFFEF4444) else forestGreen
                        )
                    },
                    isError = isNameError,
                    supportingText = {
                        if (isNameError) Text(
                            nameErrorText,
                            color = Color(0xFFEF4444),
                            fontSize = 10.sp
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text(stringResource(R.string.tipe_jenis_ternak), fontSize = 13.sp) },
                    placeholder = {
                        Text(
                            "Contoh: Sapi Perah, Kambing",
                            color = Color(0xFF94A3B8)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Pets,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (isTypeError) Color(0xFFEF4444) else forestGreen
                        )
                    },
                    isError = isTypeError,
                    supportingText = {
                        if (isTypeError) Text(
                            typeErrorText,
                            color = Color(0xFFEF4444),
                            fontSize = 10.sp
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = ageStr,
                        onValueChange = { ageStr = it },
                        label = { Text(stringResource(R.string.umur_bulan), fontSize = 13.sp) },
                        placeholder = { Text("Contoh: 14", color = Color(0xFF94A3B8)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (isAgeError) Color(0xFFEF4444) else forestGreen
                            )
                        },
                        isError = isAgeError,
                        supportingText = {
                            if (isAgeError) Text(
                                ageErrorText,
                                color = Color(0xFFEF4444),
                                fontSize = 9.sp
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors
                    )

                    OutlinedTextField(
                        value = weightStr,
                        onValueChange = { weightStr = it },
                        label = { Text(stringResource(R.string.berat_kg), fontSize = 13.sp) },
                        placeholder = { Text("Contoh: 350.5", color = Color(0xFF94A3B8)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.LinearScale,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (isWeightError) Color(0xFFEF4444) else forestGreen
                            )
                        },
                        isError = isWeightError,
                        supportingText = {
                            if (isWeightError) Text(
                                weightErrorText,
                                color = Color(0xFFEF4444),
                                fontSize = 9.sp
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(
                            stringResource(R.string.batal),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF64748B)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (isFormComplete) {
                                val finalBitmap =
                                    imageUriForDisplay?.let { getSafeCompressedBitmap(it) }
                                onConfirm(name, type, ageParsed, weightParsed, finalBitmap)
                                onDismissRequest()
                            }
                        },
                        enabled = isFormComplete,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = forestGreen,
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFE2E8F0),
                            disabledContentColor = Color(0xFF94A3B8)
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = if (isEdit) stringResource(R.string.simpan_ubahan) else stringResource(
                                R.string.tambah
                            ),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        if (showSourceOptions) {
            AlertDialog(
                onDismissRequest = { showSourceOptions = false },
                title = {
                    Text(
                        "Pilih Sumber Foto",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = forestGreen
                    )
                },
                text = {
                    Text(
                        "Ambil gambar ternak langsung dari kamera atau pilih melalui galeri berkas HP.",
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showSourceOptions = false
                                val uri = createCameraImageUri()
                                cameraImageUri = uri
                                cameraLauncher.launch(uri)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, forestGreen)
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = forestGreen
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Kamera", color = forestGreen, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                showSourceOptions = false
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = forestGreen)
                        ) {
                            Icon(
                                Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Galeri", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            )
        }
    }
}