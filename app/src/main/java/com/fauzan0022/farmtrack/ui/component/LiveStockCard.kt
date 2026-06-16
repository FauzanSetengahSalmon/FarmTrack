package com.fauzan0022.farmtrack.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fauzan0022.farmtrack.R
import com.fauzan0022.farmtrack.localDatabase.LivestockEntity
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun LivestockCard(
    livestock: LivestockEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val forestGreen = Color(0xFF1B6A4F)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(340.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val photoSource = if (livestock.photo.startsWith("http") || livestock.photo.startsWith("/")) {
                    livestock.photo
                } else {
                    "https://farmtrack-api-production-51d6.up.railway.app/storage/" + livestock.photo
                }

                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photoSource)
                        .crossfade(true)
                        .placeholder(R.drawable.loading_img)
                        .error(R.drawable.broken_image)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                val syncIcon = when (livestock.syncStatus) {
                    "PENDING_INSERT" -> Icons.Default.CloudUpload
                    "PENDING_UPDATE" -> Icons.Default.Sync
                    else -> Icons.Default.CloudDone
                }
                val syncColor = when (livestock.syncStatus) {
                    "PENDING_INSERT" -> Color(0xFFF59E0B)
                    "PENDING_UPDATE" -> Color(0xFF3B82F6)
                    else -> Color(0xFF10B981)
                }

                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.4f),
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = syncIcon,
                            contentDescription = null,
                            tint = syncColor,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = livestock.name,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))

                        Surface(
                            color = forestGreen.copy(alpha = 0.1f),
                            contentColor = forestGreen,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(0.5.dp, forestGreen.copy(alpha = 0.3f)),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = livestock.type.uppercase(),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    if (livestock.syncStatus != "SYNCED") {
                        Text(
                            text = stringResource(R.string.lokal_belum_sinkron),
                            fontSize = 9.sp,
                            color = Color(0xFFD97706),
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.tersinkron_dengan_cloud),
                            fontSize = 9.sp,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                            .border(0.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = forestGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.umur_label_badge).uppercase(),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.umur_mths_suffix, livestock.age),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                            .border(0.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LinearScale,
                                contentDescription = null,
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.berat_label_badge).uppercase(),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.berat_kg_suffix, livestock.weight),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                val isUpdated = !livestock.updatedAt.isNullOrBlank() && livestock.updatedAt != livestock.createdAt
                val timeText = if (isUpdated) {
                    stringResource(R.string.diupdate, formatDisplayTime(livestock.updatedAt))
                } else {
                    stringResource(R.string.dibuat, formatDisplayTime(livestock.createdAt))
                }

                Text(
                    text = timeText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = forestGreen,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(14.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

fun formatDisplayTime(rawTime: String?): String {
    if (rawTime.isNullOrBlank()) return "-"
    return try {
        if (rawTime.contains("T")) {
            val clean = rawTime.substringBefore(".").substringBefore("Z")
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            parser.timeZone = TimeZone.getTimeZone("UTC")
            val date = parser.parse(clean)
            if (date != null) {
                val outputFormatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                outputFormatter.timeZone = TimeZone.getDefault()
                outputFormatter.format(date)
            } else {
                rawTime
            }
        } else {
            val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val date = parser.parse(rawTime)
            if (date != null) {
                SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(date)
            } else {
                rawTime
            }
        }
    } catch (_: Exception) {
        rawTime
    }
}