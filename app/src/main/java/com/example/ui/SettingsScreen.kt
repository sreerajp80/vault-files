package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.ui.theme.TileBorderDark
import com.example.ui.theme.TileBorderLight
import com.example.utils.BiometricHelper
import `in`.sreerajp.vault_files.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: StorageViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Core parameters collected from VM
    val isPhoneLockDeleteEnabled by viewModel.phoneLockDeleteEnabled.collectAsState()
    val customPinValue by viewModel.customPin.collectAsState()
    val protectedFolders by viewModel.securedFolders.collectAsState()

    val themePreference by viewModel.themePreference.collectAsState()
    val isShowHiddenOn by viewModel.showHiddenItems.collectAsState()
    val isPasswordAppOn by viewModel.passwordProtectApp.collectAsState()
    val isPasswordHiddenOn by viewModel.passwordProtectHidden.collectAsState()

    var showPinSetupDialog by remember { mutableStateOf(false) }
    var expandedThemeMenu by remember { mutableStateOf(false) }
    var activeActionPendingValidation by remember { mutableStateOf<SettingsPendingAction?>(null) }

    // Hub navigation: settings | display | security | about
    var settingsPage by rememberSaveable { mutableStateOf("settings") }

    // Effective dark mode (correct even when the theme is forced opposite to the system),
    // used to pick a clearly-visible tile/pill border for both palettes.
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val tileBorder = BorderStroke(1.dp, if (isDark) TileBorderDark else TileBorderLight)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (settingsPage == "about") {
            AboutScreen(onBack = { settingsPage = "settings" }, modifier = Modifier.fillMaxSize())
        } else {
            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = statusBarTop + 12.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (settingsPage) {
                    // -------------------------------------------------- HUB --------------------------------------------------
                    "settings" -> {
                        item {
                            Column {
                                Text(
                                    stringResource(R.string.settings_title),
                                    fontSize = 27.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    stringResource(R.string.settings_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        item { SettingsSectionLabel(stringResource(R.string.settings_section_preferences), Modifier.padding(top = 6.dp)) }

                        item {
                            SettingsTile(
                                icon = Icons.Default.Tune,
                                title = stringResource(R.string.settings_display_title),
                                subtitle = stringResource(R.string.settings_display_subtitle),
                                border = tileBorder,
                                onClick = { settingsPage = "display" },
                                trailing = { ChevronTrailing() }
                            )
                        }
                        item {
                            SettingsTile(
                                icon = Icons.Default.Shield,
                                title = stringResource(R.string.settings_security_title),
                                subtitle = stringResource(R.string.settings_security_subtitle),
                                border = tileBorder,
                                onClick = { settingsPage = "security" },
                                trailing = { ChevronTrailing() }
                            )
                        }

                        item { SettingsSectionLabel(stringResource(R.string.settings_section_help), Modifier.padding(top = 12.dp)) }

                        item {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = tileBorder,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                    HelpEntry(
                                        question = stringResource(R.string.help_compression_q),
                                        answer = stringResource(R.string.help_compression_a)
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                    HelpEntry(
                                        question = stringResource(R.string.help_securing_q),
                                        answer = stringResource(R.string.help_securing_a)
                                    )
                                }
                            }
                        }

                        item {
                            SettingsTile(
                                icon = Icons.Default.Info,
                                title = stringResource(R.string.settings_about_title),
                                subtitle = stringResource(R.string.settings_about_subtitle),
                                border = tileBorder,
                                onClick = { settingsPage = "about" },
                                modifier = Modifier.testTag("about_row"),
                                trailing = { ChevronTrailing() }
                            )
                        }
                    }

                    // -------------------------------------------------- DISPLAY --------------------------------------------------
                    "display" -> {
                        item {
                            SettingsSubPageHeader(
                                title = stringResource(R.string.settings_display_title),
                                onBack = { settingsPage = "settings" },
                                border = tileBorder
                            )
                        }

                        // Theme Mode Selector
                        item {
                            SettingsTile(
                                icon = when (themePreference) {
                                    "light" -> Icons.Default.LightMode
                                    "dark" -> Icons.Default.DarkMode
                                    else -> Icons.Default.Palette
                                },
                                title = stringResource(R.string.theme_title),
                                subtitle = when (themePreference) {
                                    "light" -> stringResource(R.string.theme_subtitle_light)
                                    "dark" -> stringResource(R.string.theme_subtitle_dark)
                                    else -> stringResource(R.string.theme_subtitle_system)
                                },
                                border = tileBorder,
                                trailing = {
                                    Box {
                                        Button(
                                            onClick = { expandedThemeMenu = true },
                                            modifier = Modifier.testTag("theme_selector_btn")
                                        ) {
                                            Text(stringResource(R.string.action_change))
                                        }
                                        DropdownMenu(
                                            expanded = expandedThemeMenu,
                                            onDismissRequest = { expandedThemeMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.theme_opt_system)) },
                                                onClick = {
                                                    viewModel.updateThemePreference("system")
                                                    expandedThemeMenu = false
                                                },
                                                modifier = Modifier.testTag("theme_opt_system")
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.theme_opt_light)) },
                                                onClick = {
                                                    viewModel.updateThemePreference("light")
                                                    expandedThemeMenu = false
                                                },
                                                modifier = Modifier.testTag("theme_opt_light")
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.theme_opt_dark)) },
                                                onClick = {
                                                    viewModel.updateThemePreference("dark")
                                                    expandedThemeMenu = false
                                                },
                                                modifier = Modifier.testTag("theme_opt_dark")
                                            )
                                        }
                                    }
                                }
                            )
                        }

                        // Show Hidden Items toggle
                        item {
                            SettingsTile(
                                icon = Icons.Default.Visibility,
                                title = stringResource(R.string.show_hidden_title),
                                subtitle = stringResource(R.string.show_hidden_subtitle),
                                border = tileBorder,
                                trailing = {
                                    Switch(
                                        checked = isShowHiddenOn,
                                        onCheckedChange = { newValue ->
                                            if (newValue && isPasswordHiddenOn && customPinValue != null) {
                                                activeActionPendingValidation = SettingsPendingAction(
                                                    title = context.getString(R.string.hidden_verify_title),
                                                    subtitle = context.getString(R.string.hidden_verify_subtitle),
                                                    onValidated = {
                                                        viewModel.updateShowHiddenItemsSetting(true)
                                                    }
                                                )
                                            } else {
                                                viewModel.updateShowHiddenItemsSetting(newValue)
                                            }
                                        },
                                        modifier = Modifier.testTag("show_hidden_switch")
                                    )
                                }
                            )
                        }
                    }

                    // -------------------------------------------------- SECURITY --------------------------------------------------
                    "security" -> {
                        item {
                            SettingsSubPageHeader(
                                title = stringResource(R.string.settings_security_title),
                                onBack = { settingsPage = "settings" },
                                border = tileBorder
                            )
                        }

                        // App PIN passcode
                        item {
                            SettingsTile(
                                icon = Icons.Default.Grid3x3,
                                title = stringResource(R.string.app_passcode_title),
                                subtitle = if (customPinValue == null) stringResource(R.string.app_passcode_none) else stringResource(R.string.app_passcode_active),
                                subtitleColor = if (customPinValue == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                                subtitleBold = customPinValue == null,
                                border = tileBorder,
                                trailing = {
                                    Button(
                                        onClick = { showPinSetupDialog = true },
                                        modifier = Modifier.testTag("setup_pin_btn")
                                    ) {
                                        Text(if (customPinValue == null) stringResource(R.string.action_setup) else stringResource(R.string.action_modify))
                                    }
                                }
                            )
                        }

                        // Password Protect Whole App
                        item {
                            SettingsTile(
                                icon = Icons.Default.VpnKey,
                                title = stringResource(R.string.protect_app_title),
                                subtitle = stringResource(R.string.protect_app_subtitle),
                                border = tileBorder,
                                trailing = {
                                    Switch(
                                        checked = isPasswordAppOn,
                                        onCheckedChange = { newValue ->
                                            if (newValue) {
                                                if (customPinValue == null) {
                                                    showPinSetupDialog = true
                                                    viewModel.dispatchMessage(context.getString(R.string.msg_pin_required_protection))
                                                } else {
                                                    viewModel.updatePasswordProtectAppSetting(true)
                                                }
                                            } else {
                                                if (customPinValue != null) {
                                                    activeActionPendingValidation = SettingsPendingAction(
                                                        title = context.getString(R.string.disable_app_lock_title),
                                                        subtitle = context.getString(R.string.disable_app_lock_subtitle),
                                                        onValidated = {
                                                            viewModel.updatePasswordProtectAppSetting(false)
                                                        }
                                                    )
                                                } else {
                                                    viewModel.updatePasswordProtectAppSetting(false)
                                                }
                                            }
                                        },
                                        modifier = Modifier.testTag("app_lock_switch")
                                    )
                                }
                            )
                        }

                        // Password Protect Hidden Items
                        item {
                            SettingsTile(
                                icon = Icons.Default.NoEncryption,
                                title = stringResource(R.string.protect_hidden_title),
                                subtitle = stringResource(R.string.protect_hidden_subtitle),
                                border = tileBorder,
                                trailing = {
                                    Switch(
                                        checked = isPasswordHiddenOn,
                                        onCheckedChange = { newValue ->
                                            if (newValue) {
                                                if (customPinValue == null) {
                                                    showPinSetupDialog = true
                                                    viewModel.dispatchMessage(context.getString(R.string.msg_pin_required_protection))
                                                } else {
                                                    viewModel.updatePasswordProtectHiddenSetting(true)
                                                }
                                            } else {
                                                if (customPinValue != null) {
                                                    activeActionPendingValidation = SettingsPendingAction(
                                                        title = context.getString(R.string.disable_hidden_lock_title),
                                                        subtitle = context.getString(R.string.disable_hidden_lock_subtitle),
                                                        onValidated = {
                                                            viewModel.updatePasswordProtectHiddenSetting(false)
                                                            viewModel.markHiddenUnlockedState(false)
                                                        }
                                                    )
                                                } else {
                                                    viewModel.updatePasswordProtectHiddenSetting(false)
                                                    viewModel.markHiddenUnlockedState(false)
                                                }
                                            }
                                        },
                                        modifier = Modifier.testTag("password_hidden_switch")
                                    )
                                }
                            )
                        }

                        // Password Protect Deletion & Move
                        item {
                            SettingsTile(
                                icon = Icons.Default.Lock,
                                title = stringResource(R.string.protect_delete_title),
                                subtitle = stringResource(R.string.protect_delete_subtitle),
                                border = tileBorder,
                                trailing = {
                                    Switch(
                                        checked = isPhoneLockDeleteEnabled,
                                        onCheckedChange = { newValue ->
                                            if (newValue) {
                                                if (customPinValue == null) {
                                                    showPinSetupDialog = true
                                                    viewModel.dispatchMessage(context.getString(R.string.msg_pin_required_protection))
                                                } else {
                                                    viewModel.updateDeletePhoneLockSetting(true)
                                                }
                                            } else {
                                                if (customPinValue != null) {
                                                    activeActionPendingValidation = SettingsPendingAction(
                                                        title = context.getString(R.string.disable_actions_lock_title),
                                                        subtitle = context.getString(R.string.disable_actions_lock_subtitle),
                                                        onValidated = {
                                                            viewModel.updateDeletePhoneLockSetting(false)
                                                        }
                                                    )
                                                } else {
                                                    viewModel.updateDeletePhoneLockSetting(false)
                                                }
                                            }
                                        },
                                        modifier = Modifier.testTag("delete_move_lock_switch")
                                    )
                                }
                            )
                        }

                        item { SettingsSectionLabel(stringResource(R.string.shielded_ledger_label), Modifier.padding(top = 8.dp)) }

                        if (protectedFolders.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            stringResource(R.string.shielded_empty_title),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            stringResource(R.string.shielded_empty_subtitle),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            items(protectedFolders, key = { it.path }) { folder ->
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = tileBorder,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Shield,
                                                contentDescription = stringResource(R.string.cd_shield_icon),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = folder.path.substringAfterLast(java.io.File.separator),
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 15.sp
                                            )
                                        }
                                        IconButton(onClick = {
                                            viewModel.toggleFolderShield(
                                                com.example.data.FileItem(
                                                    name = folder.path.substringAfterLast(java.io.File.separator),
                                                    absolutePath = folder.path,
                                                    file = java.io.File(folder.path),
                                                    isDirectory = true,
                                                    size = 0,
                                                    isSecured = true,
                                                    category = "Folder"
                                                )
                                            )
                                        }) {
                                            Icon(Icons.Default.LockOpen, contentDescription = stringResource(R.string.cd_remove_shield), tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Custom App PIN Setup Dialog ---
        if (showPinSetupDialog) {
            var pinVal by remember { mutableStateOf("") }
            var pinValConfirm by remember { mutableStateOf("") }
            var isPinError by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showPinSetupDialog = false },
                title = { Text(stringResource(R.string.pin_setup_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            stringResource(R.string.pin_setup_message),
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = pinVal,
                            onValueChange = {
                                pinVal = it
                                isPinError = false
                            },
                            label = { Text(stringResource(R.string.pin_setup_enter_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth().testTag("setup_pin_field_1"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = pinValConfirm,
                            onValueChange = {
                                pinValConfirm = it
                                isPinError = false
                            },
                            label = { Text(stringResource(R.string.pin_setup_confirm_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth().testTag("setup_pin_field_2"),
                            singleLine = true,
                            isError = isPinError,
                            supportingText = {
                                if (isPinError) {
                                    Text(stringResource(R.string.pin_setup_error), color = Color.Red)
                                }
                            }
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (pinVal.isNotBlank() && pinVal == pinValConfirm && pinVal.length >= 4) {
                                viewModel.createOrUpdatePin(pinVal)
                                showPinSetupDialog = false
                            } else {
                                isPinError = true
                            }
                        },
                        modifier = Modifier.testTag("pin_setup_save_btn")
                    ) {
                        Text(stringResource(R.string.action_save_pin))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPinSetupDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        // --- Active settings verification triggers ---
        activeActionPendingValidation?.let { pendingAction ->
            val activity = context as? FragmentActivity
            val hasBiometrics = remember(context) { BiometricHelper.isBiometricHardwareAvailable(context) }

            if (activity != null && hasBiometrics) {
                LaunchedEffect(pendingAction) {
                    BiometricHelper.authenticate(
                        activity = activity,
                        title = pendingAction.title,
                        subtitle = pendingAction.subtitle,
                        onSuccess = {
                            pendingAction.onValidated()
                            activeActionPendingValidation = null
                        },
                        onError = { err ->
                            if (customPinValue == null) {
                                viewModel.dispatchMessage(context.getString(R.string.msg_configure_pin_settings))
                                activeActionPendingValidation = null
                            }
                        }
                    )
                }
            }

            if (customPinValue != null) {
                var enteredPin by remember { mutableStateOf("") }
                var hasPinError by remember { mutableStateOf(false) }

                AlertDialog(
                    onDismissRequest = { activeActionPendingValidation = null },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.security_verification_title))
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = pendingAction.subtitle,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            OutlinedTextField(
                                value = enteredPin,
                                onValueChange = {
                                    enteredPin = it
                                    hasPinError = false
                                },
                                label = { Text(stringResource(R.string.auth_enter_app_pin)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth().testTag("auth_pin_input_settings"),
                                singleLine = true,
                                isError = hasPinError,
                                supportingText = {
                                    if (hasPinError) {
                                        Text(stringResource(R.string.auth_incorrect_passcode), color = Color.Red)
                                    }
                                }
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (enteredPin == customPinValue) {
                                    pendingAction.onValidated()
                                    activeActionPendingValidation = null
                                } else {
                                    hasPinError = true
                                }
                            },
                            modifier = Modifier.testTag("auth_pin_confirm_btn_settings")
                        ) {
                            Text(stringResource(R.string.action_verify))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { activeActionPendingValidation = null }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                )
            } else if (activity == null || !hasBiometrics) {
                AlertDialog(
                    onDismissRequest = { activeActionPendingValidation = null },
                    title = { Text(stringResource(R.string.credentials_required_title)) },
                    text = {
                        Text(stringResource(R.string.credentials_required_message))
                    },
                    confirmButton = {
                        Button(onClick = { activeActionPendingValidation = null }) {
                            Text(stringResource(R.string.action_ok))
                        }
                    }
                )
            }
        }
    }
}

// ----------------------------------------------------------------------------------------------
// Reusable Settings building blocks
// ----------------------------------------------------------------------------------------------

@Composable
internal fun SettingsSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        color = MaterialTheme.colorScheme.secondary,
        modifier = modifier.padding(start = 6.dp)
    )
}

@Composable
internal fun SettingsIconChip(icon: ImageVector, size: Dp = 44.dp) {
    Surface(
        shape = RoundedCornerShape(13.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        modifier = Modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ChevronTrailing() {
    Icon(
        imageVector = Icons.Default.ChevronRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.secondary
    )
}

@Composable
private fun SettingsTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    border: BorderStroke,
    modifier: Modifier = Modifier,
    subtitleColor: Color = MaterialTheme.colorScheme.secondary,
    subtitleBold: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SettingsIconChip(icon)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor,
                    fontWeight = if (subtitleBold) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            trailing?.invoke()
        }
    }

    if (onClick != null) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = border,
            onClick = onClick,
            modifier = modifier.fillMaxWidth()
        ) { content() }
    } else {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = border,
            modifier = modifier.fillMaxWidth()
        ) { content() }
    }
}

@Composable
internal fun SettingsSubPageHeader(
    title: String,
    onBack: () -> Unit,
    border: BorderStroke,
    backTestTag: String? = null,
) {
    Row(
        modifier = Modifier.padding(bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = border,
            onClick = onBack,
            modifier = (if (backTestTag != null) Modifier.testTag(backTestTag) else Modifier).size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HelpEntry(question: String, answer: String) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = question,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = answer,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 5.dp)
        )
    }
}

data class SettingsPendingAction(
    val title: String,
    val subtitle: String,
    val onValidated: () -> Unit
)
