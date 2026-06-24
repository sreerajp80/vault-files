package com.example

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.example.ui.FileExplorerScreen
import com.example.ui.SecureVaultScreen
import com.example.ui.SettingsScreen
import com.example.ui.StorageAnalyzerScreen
import com.example.ui.StorageViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.BiometricHelper
import `in`.sreerajp.vault_files.R
import kotlinx.coroutines.flow.collectLatest

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize the centralized StorageViewModel
        val viewModel = ViewModelProvider(this)[StorageViewModel::class.java]

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
                    var activeTabIndex by rememberSaveable { mutableStateOf(1) } // Default to Files explorer tab

                    // Back from any non-main tab returns to the main Files tab rather than closing
                    // the app. Screen-level handlers (Files folder nav, Settings sub-pages) are
                    // composed deeper and take priority, so this only fires once a tab has no inner
                    // navigation left to pop. The Files tab itself leaves this disabled, so back at
                    // its root falls through and closes the app.
                    BackHandler(enabled = activeTabIndex != 1) {
                        viewModel.clearCategoryFilter()
                        activeTabIndex = 1
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
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
                                        // Tapping the Files tab directly always returns to normal
                                        // browsing, clearing any category filter carried over from
                                        // a Storage Analysis tile tap.
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
