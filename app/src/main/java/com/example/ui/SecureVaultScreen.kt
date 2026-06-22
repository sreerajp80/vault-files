package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.data.VaultFile
import com.example.utils.BiometricHelper
import `in`.sreerajp.vault_files.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureVaultScreen(
    viewModel: StorageViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsState()
    val vaultItems by viewModel.vaultFiles.collectAsState()
    val customPinValue by viewModel.customPin.collectAsState()

    var showPinUnlockDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.vault_screen_title), fontSize = 24.sp, fontWeight = FontWeight.Normal) },
                actions = {
                    if (isVaultUnlocked) {
                        IconButton(
                            onClick = { viewModel.markVaultUnlockedState(unlocked = false) },
                            modifier = Modifier.testTag("lock_vault_btn")
                        ) {
                            Icon(Icons.Filled.LockOpen, contentDescription = stringResource(R.string.cd_lock_vault), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = isVaultUnlocked,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "vault_screen_switch"
            ) { unlocked ->
                if (unlocked) {
                    // --- Vault Contents list display ---
                    if (vaultItems.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                                .testTag("empty_vault_state"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                modifier = Modifier.size(96.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.FolderSpecial,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = stringResource(R.string.vault_empty_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.vault_empty_message),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Disclaimer card top listing
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.VerifiedUser,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = stringResource(R.string.vault_disclaimer),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }

                            items(vaultItems, key = { it.id }) { item ->
                                VaultRowItem(
                                    item = item,
                                    onRestore = { viewModel.restoreFileFromVault(item) },
                                    onDelete = { viewModel.deleteFileFromVault(item) }
                                )
                            }
                        }
                    }
                } else {
                    // --- Vault Locked Welcome Cover page ---
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .testTag("vault_locked_screen"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                            modifier = Modifier.size(110.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = stringResource(R.string.cd_lock),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(56.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = stringResource(R.string.vault_locked_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.vault_locked_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        val activity = context as? FragmentActivity
                        
                        // Main Action trigger: Biometric popup authentication
                        Button(
                            onClick = {
                                if (activity != null && BiometricHelper.isBiometricHardwareAvailable(context)) {
                                    BiometricHelper.authenticate(
                                        activity = activity,
                                        title = context.getString(R.string.vault_biometric_title),
                                        subtitle = context.getString(R.string.vault_biometric_subtitle),
                                        onSuccess = {
                                            viewModel.markVaultUnlockedState(unlocked = true)
                                        },
                                        onError = { _ ->
                                            if (customPinValue != null) {
                                                showPinUnlockDialog = true
                                            } else {
                                                viewModel.dispatchMessage(context.getString(R.string.msg_setup_pin_fallback))
                                            }
                                        }
                                    )
                                } else if (customPinValue != null) {
                                    showPinUnlockDialog = true
                                } else {
                                    viewModel.dispatchMessage(context.getString(R.string.msg_vault_requires_pin))
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(50.dp)
                                .testTag("unlock_vault_trigger_btn")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.VpnKey, contentDescription = null)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(stringResource(R.string.vault_unlock_btn))
                            }
                        }

                        // Code fallback text
                        if (customPinValue != null) {
                            TextButton(
                                onClick = { showPinUnlockDialog = true },
                                modifier = Modifier.padding(top = 8.dp).testTag("pin_fallback_btn")
                            ) {
                                Text(stringResource(R.string.vault_unlock_pin_alt))
                            }
                        }
                    }
                }
            }

            // --- Fallback Passcode Prompt ---
            if (showPinUnlockDialog && customPinValue != null) {
                var inputPin by remember { mutableStateOf("") }
                var pinError by remember { mutableStateOf(false) }

                AlertDialog(
                    onDismissRequest = { showPinUnlockDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.vault_enter_pin_title))
                        }
                    },
                    text = {
                        Column {
                            Text(
                                stringResource(R.string.vault_pin_message),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            OutlinedTextField(
                                value = inputPin,
                                onValueChange = {
                                    inputPin = it
                                    pinError = false
                                },
                                label = { Text(stringResource(R.string.vault_app_code_label)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth().testTag("vault_unlock_pin_text"),
                                isError = pinError,
                                singleLine = true,
                                supportingText = {
                                    if (pinError) {
                                        Text(stringResource(R.string.vault_pin_error), color = Color.Red)
                                    }
                                }
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (inputPin == customPinValue) {
                                    viewModel.markVaultUnlockedState(unlocked = true)
                                    showPinUnlockDialog = false
                                } else {
                                    pinError = true
                                }
                            },
                            modifier = Modifier.testTag("vault_unlock_pin_confirm")
                        ) {
                            Text(stringResource(R.string.action_decrypt))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPinUnlockDialog = false }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun VaultRowItem(
    item: VaultFile,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val dateText = remember(item) {
        val format = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
        format.format(java.util.Date(item.dateAdded))
    }
    
    val categoryIcon = getIconByCategoryString(item.category)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vault_item_${item.originalName}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = item.category,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.originalName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = String.format("%.2f KB", item.fileSize / 1024f),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onRestore, modifier = Modifier.testTag("restore_btn_${item.originalName}")) {
                    Icon(Icons.Default.SettingsBackupRestore, contentDescription = stringResource(R.string.cd_restore), tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_btn_${item.originalName}")) {
                    Icon(Icons.Default.DeleteForever, contentDescription = stringResource(R.string.cd_delete_permanently), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

fun getIconByCategoryString(cat: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (cat) {
        "Image" -> Icons.Default.Image
        "Video" -> Icons.Default.Videocam
        "Audio" -> Icons.Default.AudioFile
        "Document" -> Icons.Default.Description
        "Archive" -> Icons.Default.FolderZip
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}
