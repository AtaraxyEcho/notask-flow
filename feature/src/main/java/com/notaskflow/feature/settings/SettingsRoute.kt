package com.notaskflow.feature.settings

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.notaskflow.domain.model.ManagedFileUpload
import com.notaskflow.domain.model.NotificationSettings
import com.notaskflow.domain.model.UserProfile

private data class SettingSection(
    val title: String,
    val items: List<SettingItem>
)

private data class SettingItem(
    val label: String,
    val icon: ImageVector,
    val subtitle: String? = null,
    val isDestructive: Boolean = false,
    val onClick: () -> Unit = {}
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    onSettingsChanged: () -> Unit = {},
    onLogout: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showProfileDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var showNotificationSheet by remember { mutableStateOf(false) }
    val avatarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { selectedUri ->
            readAvatarUpload(context, selectedUri)?.let(viewModel::uploadAvatar)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.load()
        viewModel.effect.collect { effect ->
            when (effect) {
                SettingsEffect.SettingsChanged -> onSettingsChanged()
            }
        }
    }

    val sections = listOf(
        SettingSection(
            title = "账户安全",
            items = listOf(
                SettingItem("编辑资料", Icons.Filled.Edit, "昵称；头像点击上方头像修改", onClick = { showProfileDialog = true }),
                SettingItem("修改邮箱", Icons.Filled.Email, "需要旧邮箱验证码确认", onClick = { showEmailDialog = true }),
                SettingItem("修改密码", Icons.Filled.Lock, "需要输入当前密码", onClick = { showPasswordDialog = true })
            )
        ),
        SettingSection(
            title = "偏好设置",
            items = listOf(
                SettingItem(
                    "主题与外观",
                    Icons.Filled.Palette,
                    uiState.notificationSettings?.themeLabel() ?: "跟随当前默认配置",
                    onClick = { showThemeSheet = true }
                ),
                SettingItem(
                    "通知偏好",
                    Icons.Filled.Notifications,
                    uiState.notificationSettings?.noticeLabel() ?: "任务、笔记、提及、系统通知",
                    onClick = { showNotificationSheet = true }
                )
            )
        ),
        SettingSection(
            title = "其他",
            items = listOf(
                SettingItem("退出登录", Icons.AutoMirrored.Filled.Logout, isDestructive = true, onClick = onLogout)
            )
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading && uiState.profile == null) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                ProfileHeader(
                    profile = uiState.profile,
                    avatarVersion = uiState.avatarVersion,
                    onClick = { showProfileDialog = true },
                    onAvatarClick = { avatarLauncher.launch("image/*") }
                )
                HorizontalDivider()
            }
            uiState.message?.let { message ->
                item {
                    SettingsMessage(text = message, isError = false, onDismiss = viewModel::dismissMessage)
                }
            }
            uiState.errorMessage?.let { message ->
                item {
                    SettingsMessage(text = message, isError = true, onDismiss = viewModel::dismissMessage)
                }
            }
            sections.forEach { section ->
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
                section.items.forEach { item ->
                    item {
                        SettingItemRow(item = item)
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Notask Flow v1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }
        }
    }

    if (showProfileDialog) {
        ProfileEditDialog(
            profile = uiState.profile,
            isSaving = uiState.isSaving,
            onDismiss = { showProfileDialog = false },
            onSubmit = { nickname ->
                viewModel.updateProfile(nickname)
                showProfileDialog = false
            }
        )
    }

    if (showPasswordDialog) {
        PasswordDialog(
            isSaving = uiState.isSaving,
            onDismiss = { showPasswordDialog = false },
            onSubmit = { oldPassword, newPassword ->
                viewModel.changePassword(oldPassword, newPassword)
                showPasswordDialog = false
            }
        )
    }

    if (showEmailDialog) {
        EmailChangeDialog(
            currentEmail = uiState.profile?.email.orEmpty(),
            isSaving = uiState.isSaving,
            isCodeSending = uiState.isEmailCodeSending,
            onDismiss = { showEmailDialog = false },
            onSendCode = viewModel::sendEmailChangeCode,
            onSubmit = { newEmail, code ->
                viewModel.changeEmail(newEmail, code)
                showEmailDialog = false
            }
        )
    }

    if (showThemeSheet) {
        ThemeSheet(
            settings = uiState.notificationSettings,
            isSaving = uiState.isSaving,
            onDismiss = { showThemeSheet = false },
            onUpdate = viewModel::updateNotificationSettings
        )
    }

    if (showNotificationSheet) {
        NotificationSheet(
            settings = uiState.notificationSettings,
            isSaving = uiState.isSaving,
            onDismiss = { showNotificationSheet = false },
            onUpdate = viewModel::updateNotificationSettings
        )
    }
}

@Composable
private fun ProfileHeader(
    profile: UserProfile?,
    avatarVersion: Long,
    onClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(
            profile = profile,
            avatarVersion = avatarVersion,
            modifier = Modifier.clickable(onClick = onAvatarClick)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profile?.displayName() ?: "未登录用户",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = profile?.email ?: profile?.username.orEmpty().ifBlank { "暂无账号信息" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Avatar(
    profile: UserProfile?,
    avatarVersion: Long,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        val avatarUrl = profile?.avatarUrl
        var avatarLoadFailed by remember(avatarUrl, avatarVersion) { mutableStateOf(false) }
        if (avatarUrl.isNullOrBlank() || avatarLoadFailed) {
            Icon(
                Icons.Filled.Person,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        } else {
            val context = LocalContext.current
            val versionedAvatarUrl = remember(avatarUrl, avatarVersion) {
                avatarUrl.withAvatarVersion(avatarVersion)
            }
            val avatarModel = remember(context, avatarUrl, avatarVersion) {
                ImageRequest.Builder(context)
                    .data(versionedAvatarUrl)
                    .memoryCacheKey("avatar-$avatarUrl-$avatarVersion")
                    .diskCacheKey("avatar-$avatarUrl-$avatarVersion")
                    .build()
            }
            AsyncImage(
                model = avatarModel,
                contentDescription = "头像",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onSuccess = { avatarLoadFailed = false },
                onError = { avatarLoadFailed = true }
            )
        }
    }
}

@Composable
private fun SettingsMessage(text: String, isError: Boolean, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isError) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.primaryContainer
            )
            .clickable(onClick = onDismiss)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (isError) MaterialTheme.colorScheme.onErrorContainer
            else MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SettingItemRow(item: SettingItem) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = item.onClick)
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = if (item.isDestructive) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (item.isDestructive) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                )
                item.subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (!item.isDestructive) {
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 24.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun ProfileEditDialog(
    profile: UserProfile?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var nickname by remember(profile) { mutableStateOf(profile?.nickname.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑资料") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("昵称") },
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = profile?.email.orEmpty(),
                    onValueChange = { },
                    label = { Text("邮箱") },
                    leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = false,
                    supportingText = { Text("邮箱修改需要旧邮箱验证码确认，当前移动端先禁止直接修改") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(nickname) },
                enabled = !isSaving
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun EmailChangeDialog(
    currentEmail: String,
    isSaving: Boolean,
    isCodeSending: Boolean,
    onDismiss: () -> Unit,
    onSendCode: (String) -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var newEmail by remember(currentEmail) { mutableStateOf("") }
    var code by remember(currentEmail) { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改邮箱") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = currentEmail,
                    onValueChange = { },
                    label = { Text("当前邮箱") },
                    leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = false
                )
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = {
                        newEmail = it
                        localError = null
                    },
                    label = { Text("新邮箱") },
                    leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = {
                            code = it.filter(Char::isDigit).take(EMAIL_CODE_LENGTH)
                            localError = null
                        },
                        label = { Text("验证码") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    TextButton(
                        onClick = {
                            val checkedEmail = validateEmailChangeInput(currentEmail, newEmail)
                            if (checkedEmail == null) {
                                localError = "请输入有效且不同于当前邮箱的新邮箱"
                            } else {
                                onSendCode(checkedEmail)
                            }
                        },
                        enabled = !isCodeSending && !isSaving
                    ) {
                        Text(if (isCodeSending) "发送中" else "发送验证码")
                    }
                }
                Text(
                    text = "验证码会发送到当前旧邮箱，确认本人操作后才会修改。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                localError?.let { message ->
                    Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val checkedEmail = validateEmailChangeInput(currentEmail, newEmail)
                    when {
                        checkedEmail == null -> localError = "请输入有效且不同于当前邮箱的新邮箱"
                        code.length != EMAIL_CODE_LENGTH -> localError = "请输入 6 位验证码"
                        else -> onSubmit(checkedEmail, code)
                    }
                },
                enabled = !isSaving && !isCodeSending
            ) {
                Text("确认修改")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun PasswordDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改密码") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = {
                        oldPassword = it
                        localError = null
                    },
                    label = { Text("当前密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        localError = null
                    },
                    label = { Text("新密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        localError = null
                    },
                    label = { Text("确认新密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                localError?.let { message ->
                    Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        oldPassword.isBlank() -> localError = "请输入当前密码"
                        newPassword.length < MIN_PASSWORD_LENGTH -> localError = "新密码至少 8 位"
                        newPassword != confirmPassword -> localError = "两次输入的新密码不一致"
                        else -> onSubmit(oldPassword, newPassword)
                    }
                },
                enabled = !isSaving
            ) {
                Text("确认修改")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSheet(
    settings: NotificationSettings?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onUpdate: (NotificationSettings) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val current = settings ?: defaultNotificationSettings()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("主题与外观", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            ChoiceRow(
                title = "模式",
                options = listOf("light" to "浅色", "dark" to "深色", "system" to "跟随系统"),
                selected = current.themeMode,
                enabled = !isSaving,
                onSelect = { mode -> onUpdate(current.copy(themeMode = mode)) }
            )
            ChoiceRow(
                title = "个人主题",
                options = listOf("sunrise" to "朝阳", "forest" to "森林", "ocean" to "海洋", "midnight" to "暗夜"),
                selected = current.personalThemePreset,
                enabled = !isSaving,
                onSelect = { preset -> onUpdate(current.copy(personalThemePreset = preset)) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationSheet(
    settings: NotificationSettings?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onUpdate: (NotificationSettings) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val current = settings ?: defaultNotificationSettings()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("通知偏好", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            SwitchRow("任务通知", current.taskNoticeEnabled, isSaving) {
                onUpdate(current.copy(taskNoticeEnabled = it))
            }
            SwitchRow("笔记通知", current.noteNoticeEnabled, isSaving) {
                onUpdate(current.copy(noteNoticeEnabled = it))
            }
            SwitchRow("提及通知", current.mentionNoticeEnabled, isSaving) {
                onUpdate(current.copy(mentionNoticeEnabled = it))
            }
            SwitchRow("系统通知", current.systemNoticeEnabled, isSaving) {
                onUpdate(current.copy(systemNoticeEnabled = it))
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            SwitchRow("邮件通知", current.emailEnabled, isSaving) {
                onUpdate(current.copy(emailEnabled = it))
            }
            SwitchRow("免打扰", current.quietEnabled, isSaving) {
                onUpdate(current.copy(quietEnabled = it))
            }
            QuietTimeRow(current = current, enabled = !isSaving, onUpdate = onUpdate)
        }
    }
}

@Composable
private fun ChoiceRow(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    enabled: Boolean,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = selected == option.first,
                    onClick = { onSelect(option.first) },
                    enabled = enabled,
                    label = { Text(option.second) },
                    leadingIcon = if (selected == option.first) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        null
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = enabled,
                        selected = selected == option.first,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, isSaving: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = !isSaving)
    }
}

@Composable
private fun QuietTimeRow(
    current: NotificationSettings,
    enabled: Boolean,
    onUpdate: (NotificationSettings) -> Unit
) {
    var startTime by remember(current.quietStartTime) { mutableStateOf(current.quietStartTime ?: DEFAULT_QUIET_START) }
    var endTime by remember(current.quietEndTime) { mutableStateOf(current.quietEndTime ?: DEFAULT_QUIET_END) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = startTime,
                onValueChange = { startTime = it },
                enabled = enabled && current.quietEnabled,
                modifier = Modifier.weight(1f),
                label = { Text("开始") },
                singleLine = true
            )
            OutlinedTextField(
                value = endTime,
                onValueChange = { endTime = it },
                enabled = enabled && current.quietEnabled,
                modifier = Modifier.weight(1f),
                label = { Text("结束") },
                singleLine = true
            )
        }
        Button(
            onClick = {
                onUpdate(
                    current.copy(
                        quietStartTime = startTime,
                        quietEndTime = endTime
                    )
                )
            },
            enabled = enabled && current.quietEnabled,
            modifier = Modifier.widthIn(min = 120.dp)
        ) {
            Text("保存时段")
        }
    }
}

private fun UserProfile.displayName(): String {
    return nickname?.takeIf { it.isNotBlank() } ?: username
}

private fun readAvatarUpload(context: Context, uri: Uri): ManagedFileUpload? {
    val resolver = context.contentResolver
    val mimeType = resolver.getType(uri) ?: "image/*"
    val fileName = resolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex >= 0) {
            cursor.getString(nameIndex)
        } else {
            null
        }
    } ?: "avatar.jpg"
    val bytes = resolver.openInputStream(uri)?.use { inputStream ->
        inputStream.readBytes()
    } ?: return null
    return ManagedFileUpload(
        fileName = fileName,
        mimeType = mimeType,
        bytes = bytes
    )
}

private fun NotificationSettings.themeLabel(): String {
    return "${themeMode.labelOf(THEME_MODE_LABELS)} · ${personalThemePreset.labelOf(PRESET_LABELS)}"
}

private fun NotificationSettings.noticeLabel(): String {
    val enabledCount = listOf(
        taskNoticeEnabled,
        noteNoticeEnabled,
        mentionNoticeEnabled,
        systemNoticeEnabled
    ).count { it }
    return "$enabledCount 项站内通知已开启"
}

private fun String.labelOf(labels: Map<String, String>): String {
    return labels[this] ?: this
}

private fun defaultNotificationSettings(): NotificationSettings {
    return NotificationSettings(
        themeMode = "light",
        personalThemePreset = "sunrise",
        sidebarMode = "auto",
        taskNoticeEnabled = true,
        noteNoticeEnabled = true,
        mentionNoticeEnabled = true,
        systemNoticeEnabled = true,
        emailEnabled = false,
        taskEmailEnabled = false,
        todoEmailEnabled = false,
        mentionEmailEnabled = false,
        quietEnabled = false,
        quietStartTime = DEFAULT_QUIET_START,
        quietEndTime = DEFAULT_QUIET_END
    )
}

private fun validateEmailChangeInput(currentEmail: String, newEmail: String): String? {
    val normalizedNewEmail = newEmail.trim().lowercase()
    if (!EMAIL_PATTERN.matches(normalizedNewEmail)) {
        return null
    }
    if (normalizedNewEmail == currentEmail.trim().lowercase()) {
        return null
    }
    return normalizedNewEmail
}

private fun String.withAvatarVersion(version: Long): String {
    if (version <= 0L) {
        return this
    }
    val separator = if (contains("?")) "&" else "?"
    return "$this${separator}v=$version"
}

private val THEME_MODE_LABELS = mapOf(
    "light" to "浅色",
    "dark" to "深色",
    "system" to "跟随系统"
)

private val PRESET_LABELS = mapOf(
    "sunrise" to "朝阳",
    "forest" to "森林",
    "ocean" to "海洋",
    "midnight" to "暗夜"
)

private const val MIN_PASSWORD_LENGTH = 8
private const val EMAIL_CODE_LENGTH = 6
private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
private const val DEFAULT_QUIET_START = "22:00"
private const val DEFAULT_QUIET_END = "08:00"
