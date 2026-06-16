package com.fauzan0022.farmtrack.ui.screen

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fauzan0022.farmtrack.BuildConfig
import com.fauzan0022.farmtrack.localDatabase.LivestockEntity
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import com.fauzan0022.farmtrack.R
import com.fauzan0022.farmtrack.ui.component.DashboardHeader
import com.fauzan0022.farmtrack.ui.component.LivestockCard
import com.fauzan0022.farmtrack.ui.component.StatisticCard
import com.fauzan0022.farmtrack.ui.component.UserAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: LivestockViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val livestockList by viewModel.livestockList.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    var showProfileDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingEntity by remember { mutableStateOf<LivestockEntity?>(null) }
    var deletingEntity by remember { mutableStateOf<LivestockEntity?>(null) }

    val scope = rememberCoroutineScope()

    fun triggerGoogleSignIn() {
        scope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(BuildConfig.API_KEY)
                    .setAutoSelectEnabled(true)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    context = context,
                    request = request
                )

                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val displayName = googleIdTokenCredential.displayName ?: "Tidak ada nama"

                    viewModel.loginWithGoogle(
                        name = displayName,
                        email = googleIdTokenCredential.id,
                        photoUrl = googleIdTokenCredential.profilePictureUri?.toString() ?: "https://googleusercontent.com/profile/picture/0"
                    )
                    Toast.makeText(context, context.getString(R.string.welcome_farmer, displayName), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, context.getString(R.string.unknown_credentials), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Throwable) {
                Log.e("MainScreen", "Google Sign In bermasalah", e)
                Toast.makeText(context, "Gagal masuk menggunakan akun Google", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is UiState.Success -> {
                val message = when {
                    state.message.contains("Sign in successful") || state.message.contains("Masuk berhasil") -> {
                        val nameStr = state.message.substringAfter("as ", "").ifEmpty { state.message.substringAfter("sebagai ", "") }
                        if (nameStr.isNotEmpty()) context.getString(R.string.welcome_farmer, nameStr)
                        else context.getString(R.string.welcome_farmer_fallback)
                    }
                    state.message.contains("Signed out") || state.message.contains("Berhasil keluar") -> context.getString(R.string.toast_sucess_logout)
                    state.message.contains("added successfully") || state.message.contains("berhasil ditambahkan") -> context.getString(R.string.toast_sucess_add)
                    state.message.contains("updated successfully") || state.message.contains("berhasil diperbarui") -> context.getString(R.string.toast_sucess_update)
                    state.message.contains("deleted successfully") || state.message.contains("berhasil dihapus") -> context.getString(R.string.toast_sucess_delete)
                    else -> state.message
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                viewModel.triggerSync()
                viewModel.clearUiState()
            }
            is UiState.Error -> {
                Toast.makeText(context, context.getString(R.string.failed_with_prefix, state.errorMessage), Toast.LENGTH_LONG).show()
                viewModel.clearUiState()
            }
            else -> {}
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFF5F7F2),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pets,
                            contentDescription = stringResource(R.string.app_name),
                            tint = Color(0xFF1B6A4F)
                        )
                        Text(
                            text = stringResource(R.string.app_name),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B6A4F),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                actions = {
                    if (!currentUser.isEmpty()) {
                        IconButton(onClick = { showProfileDialog = true }) {
                            UserAvatar(
                                name = currentUser.name.ifEmpty { "Tidak ada nama" },
                                photoUrl = currentUser.photoUrl,
                                size = 32.dp,
                                borderWidth = 1.5.dp,
                                borderColor = Color(0xFF10B981)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (!currentUser.isEmpty()) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = Color(0xFF10B981),
                    contentColor = Color.White,
                    shape = CircleShape,
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.tambah_ternak_baru), modifier = Modifier.size(28.dp))
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (currentUser.isEmpty()) {
                LoginContent(
                    onGoogleSignInClick = { triggerGoogleSignIn() }
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    DashboardHeader(
                        user = currentUser,
                        isOnline = isOnline
                    )

                    StatisticCard(
                        count = livestockList.size
                    )

                    val unsyncedCount = livestockList.count { it.syncStatus != "SYNCED" }
                    if (unsyncedCount > 0) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 2.dp),
                            color = Color(0xFFFFFBEB),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFEF3C7))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Ikon sinkronisasi tertunda",
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.pending_sync_bar_text, unsyncedCount),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF92400E),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Button(
                                    onClick = { viewModel.triggerSync() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFEF3C7),
                                        contentColor = Color(0xFF92400E)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(stringResource(R.string.sinkron_button_text), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    AnimatedVisibility(visible = isSyncing) {
                        Surface(
                            color = Color(0xFFECFDF5),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = Color(0xFF047857)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.sync_progress_text),
                                    fontSize = 10.sp,
                                    color = Color(0xFF047857)
                                )
                            }
                        }
                    }

                    if (livestockList.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pets,
                                contentDescription = "Ikon data kosong",
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.belum_ada_ternak),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.belum_ada_ternak_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { viewModel.triggerSync() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B6A4F))
                            ) {
                                Icon(Icons.Default.Refresh, "Muat Ulang")
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.muat_ulang_data))
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(bottom = 80.dp, start = 12.dp, end = 12.dp, top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(livestockList) { livestock ->
                                LivestockCard(
                                    livestock = livestock,
                                    onEdit = { editingEntity = livestock },
                                    onDelete = { deletingEntity = livestock }
                                )
                            }
                        }
                    }
                }
            }

            if (uiState is UiState.Loading) {
                Surface(
                    color = Color.Black.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(24.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(color = Color(0xFF1B6A4F))
                                Text(
                                    text = stringResource(R.string.memproses_data),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1E293B)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showProfileDialog) {
        ProfileDialog(
            user = currentUser,
            onDismissRequest = { showProfileDialog = false },
            onLogout = { viewModel.logout() }
        )
    }

    if (showAddDialog) {
        AddEditLivestockDialog(
            onDismissRequest = { showAddDialog = false },
            onConfirm = { name, type, age, weight, bitmap ->
                viewModel.addLivestock(name, type, age, weight, bitmap)
            }
        )
    }

    val entityToEdit = editingEntity
    if (entityToEdit != null) {
        AddEditLivestockDialog(
            entity = entityToEdit,
            onDismissRequest = { editingEntity = null },
            onConfirm = { name, type, age, weight, bitmap ->
                viewModel.updateLivestock(entityToEdit, name, type, age, weight, bitmap)
            }
        )
    }

    val entityToDelete = deletingEntity
    if (entityToDelete != null) {
        DeleteConfirmDialog(
            livestockName = entityToDelete.name,
            onDismissRequest = { deletingEntity = null },
            onConfirm = { viewModel.deleteLivestock(entityToDelete) }
        )
    }
}