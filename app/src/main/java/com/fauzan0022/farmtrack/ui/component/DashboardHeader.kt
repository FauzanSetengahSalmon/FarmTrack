package com.fauzan0022.farmtrack.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fauzan0022.farmtrack.R
import com.fauzan0022.farmtrack.model.User

@Composable
fun DashboardHeader(
    user: User,
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                UserAvatar(
                    name = user.name.ifEmpty { stringResource(R.string.peternak_smart) },
                    photoUrl = user.photoUrl,
                    size = 44.dp,
                    borderWidth = 2.dp,
                    borderColor = Color(0xFF10B981)
                )
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = user.name.ifEmpty { stringResource(R.string.peternak_smart) },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = user.email.ifEmpty { stringResource(R.string.default_email) },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val connectionColor = if (isOnline) Color(0xFF10B981) else Color(0xFFF59E0B)
                val connectionText = if (isOnline) stringResource(R.string.online_text) else stringResource(R.string.offline_only_text)

                Surface(
                    color = connectionColor.copy(alpha = 0.1f),
                    contentColor = connectionColor,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(connectionColor, CircleShape)
                        )
                        Text(
                            text = connectionText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isOnline) stringResource(R.string.tersinkron_text) else stringResource(R.string.mode_offline_text),
                    fontSize = 8.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}
