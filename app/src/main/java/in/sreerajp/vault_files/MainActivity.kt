package `in`.sreerajp.vault_files

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.IntentCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import `in`.sreerajp.vault_files.ui.FileExplorerScreen
import `in`.sreerajp.vault_files.ui.FolderPickerDialog
import `in`.sreerajp.vault_files.ui.hasAllFilesPermission
import `in`.sreerajp.vault_files.ui.SecureVaultScreen
import `in`.sreerajp.vault_files.ui.SharedImport
import `in`.sreerajp.vault_files.ui.SettingsScreen
import `in`.sreerajp.vault_files.ui.StorageAnalyzerScreen
import `in`.sreerajp.vault_files.ui.StorageViewModel
import `in`.sreerajp.vault_files.ui.theme.MyApplicationTheme
import `in`.sreerajp.vault_files.utils.BiometricHelper
import kotlinx.coroutines.flow.collectLatest

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize the centralized StorageViewModel
        val viewModel = ViewModelProvider(this)[StorageViewModel::class.java]

        // Handle incoming intent (share, view, or pick files)
        handleIntent(intent, viewModel)

        setContent {
            val themePreference by viewModel.themePreference.collectAsState()
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val darkTheme = when (themePreference) {
                "light" -> false
                "dark" -> true
                else -> systemDark
            }

            // Keep status-bar icon contrast correct for the *effective* theme, even when the
            // user forces a theme opposite to the system setting (dark icons on light bar,
            // light icons on dark bar).
            val view = androidx.compose.ui.platform.LocalView.current
            androidx.compose.runtime.SideEffect {
                val window = (view.context as android.app.Activity).window
                androidx.core.view.WindowCompat.getInsetsController(window, view)
                    .isAppearanceLightStatusBars = !darkTheme
            }

            MyApplicationTheme(darkTheme = darkTheme) {
                val passwordProtectApp by viewModel.passwordProtectApp.collectAsState()
                val customPinValue by viewModel.customPin.collectAsState()

                // Lock state initialized on launch if preference enabled and a PIN passcode exists
                var isAppLocked by rememberSaveable(passwordProtectApp, customPinValue) {
                    mutableStateOf(passwordProtectApp && customPinValue != null)
                }

                val snackbarHostState = remember { SnackbarHostState() }

                // Collect user feedback messages from flow and display as Snackbar
                LaunchedEffect(viewModel.userMessage) {
                    viewModel.userMessage.collectLatest { message ->
                        snackbarHostState.showSnackbar(
                            message = message,
                            withDismissAction = true
                        )
                    }
                }

                if (isAppLocked) {
                    AppLockScreen(
                        viewModel = viewModel,
                        onUnlocked = { isAppLocked = false }
                    )
                } else {
                    // First-launch storage permission request. Fires once (guarded by a persisted
                    // flag), only after the app is unlocked and only if access isn't already held.
                    // On R+ this opens the system All-files-access screen (no in-app dialog exists
                    // for it); pre-R it shows the runtime READ_EXTERNAL_STORAGE dialog.
                    val context = LocalContext.current
                    val storagePermissionRequested by viewModel.storagePermissionRequested.collectAsState()
                    val storagePermLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { /* status is re-read on resume by the screens that need it */ }
                    LaunchedEffect(storagePermissionRequested) {
                        if (storagePermissionRequested == false && !hasAllFilesPermission(context)) {
                            viewModel.markStoragePermissionRequested()
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                try {
                                    context.startActivity(
                                        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                    )
                                } catch (e: Exception) {
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                                    } catch (ex: Exception) {
                                        // No settings activity available; the in-app Files-tab button remains.
                                    }
                                }
                            } else {
                                storagePermLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                        }
                    }

                    // Files shared into the app from another app's share sheet. Once unlocked, ask
                    // the user each time where to save them: the encrypted vault or a folder.
                    val pendingShares by viewModel.pendingSharedImports.collectAsState()
                    var showShareFolderPicker by remember { mutableStateOf(false) }
                    pendingShares?.let { shares ->
                        if (showShareFolderPicker) {
                            FolderPickerDialog(
                                title = stringResource(R.string.share_pick_folder_title),
                                confirmLabel = stringResource(R.string.share_save_here),
                                viewModel = viewModel,
                                hasDevicePermission = hasAllFilesPermission(context),
                                onRequestDevicePermission = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        try {
                                            context.startActivity(
                                                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                                    data = Uri.parse("package:${context.packageName}")
                                                }
                                            )
                                        } catch (e: Exception) {
                                            try {
                                                context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                                            } catch (ex: Exception) {
                                                // No settings screen available; the app root stays browsable.
                                            }
                                        }
                                    }
                                },
                                onConfirm = { destDir ->
                                    viewModel.importSharedToFolder(shares, destDir)
                                    showShareFolderPicker = false
                                },
                                onDismiss = { showShareFolderPicker = false }
                            )
                        } else {
                            ShareDestinationDialog(
                                count = shares.size,
                                onSaveToVault = { viewModel.importSharedToVault(shares) },
                                onChooseFolder = { showShareFolderPicker = true },
                                onDismiss = { viewModel.clearPendingSharedImports() }
                            )
                        }
                    }

                    var activeTabIndex by rememberSaveable { mutableStateOf(1) } // Default to Files explorer tab
                    val isPickMode by viewModel.isPickMode.collectAsState()

                    LaunchedEffect(isPickMode) {
                        if (isPickMode) {
                            activeTabIndex = 1
                        }
                    }

                    LaunchedEffect(Unit) {
                        viewModel.pickedUris.collect { uris ->
                            if (uris.isNotEmpty()) {
                                val resultIntent = Intent().apply {
                                    if (uris.size == 1) {
                                        data = uris.first()
                                    } else {
                                        val clipData = android.content.ClipData.newUri(
                                            contentResolver,
                                            "Selected Files",
                                            uris.first()
                                        )
                                        for (i in 1 until uris.size) {
                                            clipData.addItem(android.content.ClipData.Item(uris[i]))
                                        }
                                        setClipData(clipData)
                                        putParcelableArrayListExtra(
                                            Intent.EXTRA_STREAM,
                                            ArrayList(uris)
                                        )
                                    }
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                setResult(RESULT_OK, resultIntent)
                                finish()
                            }
                        }
                    }

                    // Back from any non-main tab returns to the main Files tab.
                    // If in pick-mode, pressing Back exits pick mode and cancels.
                    BackHandler(enabled = isPickMode || activeTabIndex != 1) {
                        if (isPickMode) {
                            viewModel.exitPickMode()
                            setResult(RESULT_CANCELED)
                            finish()
                        } else {
                            viewModel.clearCategoryFilter()
                            activeTabIndex = 1
                        }
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            if (!isPickMode) {
                                NavigationBar(
                                    modifier = Modifier.testTag("app_bottom_nav_bar")
                                ) {
                                    NavigationBarItem(
                                        selected = activeTabIndex == 0,
                                        onClick = { activeTabIndex = 0 },
                                        icon = { Icon(Icons.Default.Storage, contentDescription = stringResource(R.string.cd_storage_stats)) },
                                        label = { Text(stringResource(R.string.nav_storage)) },
                                        modifier = Modifier.testTag("nav_storage_tab")
                                    )
                                    NavigationBarItem(
                                        selected = activeTabIndex == 1,
                                        onClick = {
                                            viewModel.clearCategoryFilter()
                                            activeTabIndex = 1
                                        },
                                        icon = { Icon(Icons.Default.Folder, contentDescription = stringResource(R.string.cd_file_explorer)) },
                                        label = { Text(stringResource(R.string.nav_files)) },
                                        modifier = Modifier.testTag("nav_files_tab")
                                    )
                                    NavigationBarItem(
                                        selected = activeTabIndex == 2,
                                        onClick = { activeTabIndex = 2 },
                                        icon = { Icon(Icons.Default.VpnKey, contentDescription = stringResource(R.string.cd_secure_vault)) },
                                        label = { Text(stringResource(R.string.nav_vault)) },
                                        modifier = Modifier.testTag("nav_vault_tab")
                                    )
                                    NavigationBarItem(
                                        selected = activeTabIndex == 3,
                                        onClick = { activeTabIndex = 3 },
                                        icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.cd_settings_options)) },
                                        label = { Text(stringResource(R.string.nav_settings)) },
                                        modifier = Modifier.testTag("nav_settings_tab")
                                    )
                                }
                            }
                        },
                        snackbarHost = { SnackbarHost(snackbarHostState) }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = innerPadding.calculateBottomPadding()) // Account for bottom navigation padding safely
                        ) {
                            when (activeTabIndex) {
                                0 -> StorageAnalyzerScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize(),
                                    onOpenVault = { activeTabIndex = 2 },
                                    onOpenFilesWithCategory = { category ->
                                        viewModel.openCategoryFilter(category)
                                        activeTabIndex = 1
                                    }
                                )
                                1 -> FileExplorerScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                                2 -> SecureVaultScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                                3 -> SettingsScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // A share can arrive while the app is already running (single-activity). Android delivers it
    // here instead of a fresh onCreate, so re-read it and hand the files to the same ViewModel.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val viewModel = ViewModelProvider(this)[StorageViewModel::class.java]
        handleIntent(intent, viewModel)
    }

    /**
     * Dispatches the incoming intent (view, share, or pick files) to the correct handler.
     */
    private fun handleIntent(intent: Intent?, viewModel: StorageViewModel) {
        if (intent == null) return
        val action = intent.action
        val dataUri = intent.data

        when (action) {
            Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE -> {
                handleShareIntent(intent, viewModel)
            }
            Intent.ACTION_GET_CONTENT, Intent.ACTION_OPEN_DOCUMENT -> {
                val allowMultiple = intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
                viewModel.startPickMode(allowMultiple)
            }
            Intent.ACTION_VIEW -> {
                if (dataUri != null) {
                    viewModel.previewExternalContentUri(this, dataUri)
                }
            }
        }
    }

    /**
     * Pulls any files shared into the app (ACTION_SEND / ACTION_SEND_MULTIPLE) out of [intent] and
     * hands them to the ViewModel as pending imports. Does nothing for a normal launch.
     */
    private fun handleShareIntent(intent: Intent?, viewModel: StorageViewModel) {
        if (intent == null) return
        val uris: List<Uri> = when (intent.action) {
            Intent.ACTION_SEND -> {
                val uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                if (uri != null) listOf(uri) else emptyList()
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                    ?: emptyList()
            }
            else -> emptyList()
        }
        if (uris.isEmpty()) return
        viewModel.setPendingSharedImports(uris.map { SharedImport(it, resolveDisplayName(it)) })
    }

    /**
     * Best-effort file name for a shared content [uri]: the provider's DISPLAY_NAME when available,
     * otherwise the last path segment, otherwise a generic fallback.
     */
    private fun resolveDisplayName(uri: Uri): String {
        try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val name = cursor.getString(idx)
                        if (!name.isNullOrBlank()) return name
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "shared_file"
    }
}

@Composable
fun AppLockScreen(
    viewModel: StorageViewModel,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val customPinValue by viewModel.customPin.collectAsState()
    var enteredPin by remember { mutableStateOf("") }
    var hasPinError by remember { mutableStateOf(false) }

    val activity = context as? FragmentActivity
    val hasBiometrics = remember(context) { BiometricHelper.isBiometricHardwareAvailable(context) }

    LaunchedEffect(Unit) {
        if (activity != null && hasBiometrics) {
            BiometricHelper.authenticate(
                activity = activity,
                title = context.getString(R.string.applock_biometric_title),
                subtitle = context.getString(R.string.applock_biometric_subtitle),
                onSuccess = {
                    onUnlocked()
                },
                onError = {
                    // Fail gracefully, user can use the Custom PIN fallback
                }
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize().testTag("app_lock_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = stringResource(R.string.cd_app_locked_shield),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.applock_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.applock_message),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = enteredPin,
                onValueChange = {
                    enteredPin = it
                    hasPinError = false
                },
                label = { Text(stringResource(R.string.applock_pin_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .testTag("app_lock_pin_input"),
                singleLine = true,
                isError = hasPinError,
                supportingText = {
                    if (hasPinError) {
                        Text(stringResource(R.string.applock_pin_error), color = MaterialTheme.colorScheme.error)
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(0.85f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasBiometrics && activity != null) {
                    IconButton(
                        onClick = {
                            BiometricHelper.authenticate(
                                activity = activity,
                                title = context.getString(R.string.applock_biometric_title),
                                subtitle = context.getString(R.string.applock_biometric_subtitle_short),
                                onSuccess = { onUnlocked() },
                                onError = {}
                            )
                        },
                        modifier = Modifier
                            .size(50.dp)
                            .testTag("app_lock_biometric_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = stringResource(R.string.cd_unlock_biometrics),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        if (enteredPin == customPinValue) {
                            onUnlocked()
                        } else {
                            hasPinError = true
                        }
                    },
                    modifier = Modifier
                        .height(50.dp)
                        .weight(1f)
                        .testTag("app_lock_verify_btn")
                ) {
                    Text(stringResource(R.string.applock_unlock_btn))
                }
            }
        }
    }
}

/**
 * Asks the user where to save files that were shared into the app: the encrypted vault, or a
 * folder they pick. [count] is how many files are being saved (drives singular/plural wording).
 */
@Composable
fun ShareDestinationDialog(
    count: Int,
    onSaveToVault: () -> Unit,
    onChooseFolder: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.share_dest_title)) },
        text = { Text(stringResource(R.string.share_dest_message, count)) },
        confirmButton = {
            TextButton(onClick = onChooseFolder) {
                Text(stringResource(R.string.share_dest_choose_folder))
            }
        },
        dismissButton = {
            TextButton(onClick = onSaveToVault) {
                Text(stringResource(R.string.share_dest_vault))
            }
        }
    )
}
