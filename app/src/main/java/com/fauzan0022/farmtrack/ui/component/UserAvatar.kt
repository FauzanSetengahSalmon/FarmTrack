package com.fauzan0022.farmtrack.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun UserAvatar(
    modifier: Modifier = Modifier,
    name: String,
    photoUrl: String,
    size: Dp,
    borderWidth: Dp = 2.dp,
    borderColor: Color = Color(0xFF1B6A4F)
) {
    val context = LocalContext.current

    var isError by remember(photoUrl) { mutableStateOf(false) }

    val safePhotoUrl = remember(photoUrl) {
        if (photoUrl.startsWith("http://")) {
            photoUrl.replace("http://", "https://")
        } else {
            photoUrl
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(borderWidth, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (safePhotoUrl.isNotEmpty() && !isError) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(safePhotoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Foto profil $name",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onError = {
                    isError = true
                }
            )
        } else {
            val initial = name.trim().firstOrNull()?.uppercase() ?: "K"
            val bgColors = listOf(
                Color(0xFF1B6A4F),
                Color(0xFF0F766E),
                Color(0xFF0369A1),
                Color(0xFF4338CA),
                Color(0xFF7C3AED),
                Color(0xFFB91C1C),
                Color(0xFF0D9488),
                Color(0xFF0891B2)
            )
            val index = (name.hashCode() and 0x7FFFFFFF) % bgColors.size
            val bgColor = bgColors[index]

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.45).dp.value.sp
                )
            }
        }
    }
}