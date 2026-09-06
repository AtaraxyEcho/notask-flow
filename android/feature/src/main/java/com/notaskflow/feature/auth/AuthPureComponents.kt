package com.notaskflow.feature.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import com.notaskflow.core.common.LegalDocumentType
import com.notaskflow.core.common.LegalDocuments
import com.notaskflow.core.ui.theme.authpure.AuthPureColors
import com.notaskflow.feature.R

/** 全应用统一的密码最小长度（与后端 @Size(min = 8) 约束一致）。 */
internal const val AUTH_MIN_PASSWORD_LENGTH = 8

internal val AUTH_EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

private const val OTP_LENGTH = 6

/** 法律文档弹层的页面级控制器（认证页同时只存在一个实例）。 */
internal object AuthLegalController {
    var type by mutableStateOf(LegalDocumentType.TERMS)
    var visible by mutableStateOf(false)

    fun open(type: LegalDocumentType) {
        this.type = type
        visible = true
    }
}

// ---------- 页面骨架 ----------

@Composable
internal fun AuthPureScaffold(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    steps: (@Composable () -> Unit)? = null,
    errorBanner: String? = null,
    successBanner: String? = null,
    bottomContent: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(containerColor = AuthPureColors.Canvas) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            AuthPureTopBar(onBack = onBack)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                // 窄屏（<600dp）全幅铺满；宽屏（平板/横屏）呈现白卡片形态并与 Web 一致
                val isWide = maxWidth >= 600.dp
                val inner: @Composable ColumnScope.() -> Unit = {
                    steps?.invoke()
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = title,
                        fontSize = 24.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.2).sp,
                        color = AuthPureColors.Ink
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(text = subtitle, fontSize = 14.sp, lineHeight = 20.sp, color = AuthPureColors.Muted)
                    errorBanner?.let { banner ->
                        Spacer(Modifier.height(16.dp))
                        AuthBanner(text = banner, isError = true)
                    }
                    successBanner?.let { banner ->
                        Spacer(Modifier.height(16.dp))
                        AuthBanner(text = banner, isError = false)
                    }
                    Spacer(Modifier.height(24.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        content()
                    }
                    Spacer(Modifier.height(24.dp))
                    AuthDivider()
                    bottomContent()
                }
                if (isWide) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 440.dp)
                            .align(Alignment.Center),
                        shape = RoundedCornerShape(12.dp),
                        color = AuthPureColors.Surface,
                        border = BorderStroke(1.dp, AuthPureColors.Border)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 40.dp, vertical = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            inner()
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        inner()
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            AuthFooter()
            Spacer(Modifier.height(16.dp))
        }
    }

    LegalDocumentSheet(
        visible = AuthLegalController.visible,
        type = AuthLegalController.type,
        onDismiss = { AuthLegalController.visible = false }
    )
}

@Composable
private fun AuthPureTopBar(onBack: (() -> Unit)?) {
    val colors = AuthPureColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.auth_back),
                    tint = colors.Ink,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
        } else {
            Image(
                painter = painterResource(R.drawable.notask_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = stringResource(R.string.auth_app_name),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.1).sp,
            color = colors.Ink
        )
        Spacer(Modifier.weight(1f))
        AuthLanguageToggle()
    }
}

@Composable
private fun AuthFooter() {
    val colors = AuthPureColors
    val year = remember { Calendar.getInstance().get(Calendar.YEAR) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.auth_copyright, year),
            fontSize = 12.sp,
            color = colors.Muted
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            TextButton(
                onClick = { AuthLegalController.open(LegalDocumentType.TERMS) },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(stringResource(R.string.auth_terms_title), fontSize = 12.sp, color = colors.Muted)
            }
            TextButton(
                onClick = { AuthLegalController.open(LegalDocumentType.PRIVACY) },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(stringResource(R.string.auth_privacy_title), fontSize = 12.sp, color = colors.Muted)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LegalDocumentSheet(
    visible: Boolean,
    type: LegalDocumentType,
    onDismiss: () -> Unit
) {
    if (!visible) {
        return
    }
    val document = LegalDocuments.get(type)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = document.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = document.meta,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            document.sections.forEach { section ->
                Text(
                    text = section.heading,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                section.paragraphs.forEach { paragraph ->
                    Text(
                        text = paragraph,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                }
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

// ---------- 排版小件 ----------

@Composable
internal fun AuthFieldLabel(
    label: String,
    required: Boolean = false,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    val colors = AuthPureColors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = colors.Ink
        )
        if (required) {
            Text(text = "*", color = colors.Error, modifier = Modifier.padding(start = 2.dp))
        }
        Spacer(Modifier.weight(1f))
        trailing?.invoke(this)
    }
}

@Composable
internal fun AuthFieldError(text: String) {
    val colors = AuthPureColors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = colors.Error,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colors.Error)
    }
}

@Composable
internal fun AuthHelperText(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = AuthPureColors.Muted
    )
}

// ---------- 输入框 ----------

@Composable
internal fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    helperText: String? = null,
    required: Boolean = false,
    labelTrailing: (@Composable RowScope.() -> Unit)? = null,
    leadingIcon: ImageVector? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val colors = AuthPureColors
    Column(modifier = modifier.fillMaxWidth()) {
        AuthFieldLabel(label = label, required = required, trailing = labelTrailing)
        Spacer(Modifier.height(6.dp))
        var focused by remember { mutableStateOf(false) }
        val borderColor = when {
            errorMessage != null -> colors.Error
            focused -> colors.Ink
            else -> colors.Border
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .onFocusChanged { focused = it.isFocused },
            textStyle = TextStyle(fontSize = 14.sp, color = colors.Ink),
            cursorBrush = SolidColor(colors.Ink),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.Surface)
                        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    leadingIcon?.let { icon ->
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = colors.Muted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                fontSize = 14.sp,
                                color = colors.Placeholder,
                                maxLines = 1
                            )
                        }
                        innerTextField()
                    }
                    trailingContent?.invoke()
                }
            }
        )
        when {
            errorMessage != null -> {
                Spacer(Modifier.height(6.dp))
                AuthFieldError(text = errorMessage)
            }
            helperText != null -> {
                Spacer(Modifier.height(6.dp))
                AuthHelperText(text = helperText)
            }
        }
    }
}

// ---------- 六位分离验证码 ----------

@Composable
internal fun AuthOtpRow(
    code: String,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AuthPureColors
    val focusRequesters = remember { List(OTP_LENGTH) { FocusRequester() } }
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(OTP_LENGTH) { index ->
            val cellValue = code.getOrNull(index)?.toString().orEmpty()
            var focused by remember { mutableStateOf(false) }
            BasicTextField(
                value = cellValue,
                onValueChange = { raw ->
                    handleOtpInput(code, onCodeChange, index, raw, focusRequesters)
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 54.dp)
                    .focusRequester(focusRequesters[index])
                    .onFocusChanged { focused = it.isFocused }
                    .onPreviewKeyEvent { event ->
                        val backspace = event.type == KeyEventType.KeyDown &&
                            event.key == Key.Backspace
                        if (backspace && cellValue.isEmpty() && index > 0 && code.isNotEmpty()) {
                            onCodeChange(code.dropLast(1))
                            focusRequesters[index - 1].requestFocus()
                            true
                        } else {
                            false
                        }
                    },
                textStyle = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = colors.Ink,
                    fontFeatureSettings = "tnum"
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                cursorBrush = SolidColor(Color.Transparent),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.Surface)
                            .border(
                                width = 1.dp,
                                color = if (focused) colors.Ink else colors.Border,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        innerTextField()
                    }
                }
            )
        }
    }
}

private fun handleOtpInput(
    code: String,
    onCodeChange: (String) -> Unit,
    index: Int,
    raw: String,
    focusRequesters: List<FocusRequester>
) {
    val digits = raw.filter(Char::isDigit)
    when {
        digits.isEmpty() -> {
            if (index < code.length) {
                onCodeChange(code.removeRange(index, index + 1))
            }
        }
        digits.length > 1 -> {
            val fillCount = digits.length.coerceAtMost(OTP_LENGTH - index)
            val merged = buildString {
                append(code.take(index))
                append(digits.take(fillCount))
                append(code.drop(index + fillCount))
            }.take(OTP_LENGTH)
            onCodeChange(merged)
            focusRequesters[(index + fillCount).coerceAtMost(OTP_LENGTH - 1)].requestFocus()
        }
        else -> {
            val next = when {
                index < code.length -> code.replaceRange(index, index + 1, digits)
                else -> code + digits
            }.take(OTP_LENGTH)
            onCodeChange(next)
            if (index < OTP_LENGTH - 1) {
                focusRequesters[index + 1].requestFocus()
            }
        }
    }
}

// ---------- 按钮 ----------

@Composable
internal fun AuthPrimaryButton(
    text: String,
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AuthPureColors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        interactionSource = interactionSource,
        modifier = modifier.fillMaxWidth().heightIn(min = 44.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (pressed) colors.InkActive else colors.Ink,
            contentColor = Color.White,
            disabledContainerColor = colors.DisabledBg,
            disabledContentColor = colors.Placeholder
        ),
        border = if (enabled && !loading) {
            null
        } else {
            BorderStroke(1.dp, colors.Border)
        }
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
        } else {
            Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
internal fun AuthSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = AuthPureColors
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 44.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = colors.Ink,
            disabledContentColor = colors.Placeholder
        )
    ) {
        Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

// ---------- 复选框 ----------

@Composable
internal fun AuthCheckboxRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val colors = AuthPureColors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (checked) colors.Ink else colors.Surface)
                .border(
                    width = 1.dp,
                    color = if (checked) colors.Ink else colors.BorderHover,
                    shape = RoundedCornerShape(4.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        content()
    }
}

// ---------- 密码强度条 ----------

/** 密码强度评分：长度满 8 位、字母数字混合、含特殊符号、长度满 12 位各计 1 分，返回 0-4。 */
internal fun passwordStrengthLevel(password: String): Int {
    if (password.isBlank()) {
        return 0
    }
    var score = 0
    if (password.length >= AUTH_MIN_PASSWORD_LENGTH) {
        score += 1
    }
    if (password.any { it.isDigit() } && password.any { it.isLetter() }) {
        score += 1
    }
    if (password.any { !it.isLetterOrDigit() }) {
        score += 1
    }
    if (password.length >= 12) {
        score += 1
    }
    return score
}

@Composable
internal fun AuthStrengthBar(password: String, modifier: Modifier = Modifier) {
    val colors = AuthPureColors
    val level = passwordStrengthLevel(password)
    val filledColor = when (level) {
        1 -> colors.Strength1
        2 -> colors.Strength2
        3 -> colors.Strength3
        else -> colors.Strength4
    }
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        when {
                            index < level -> filledColor
                            else -> colors.Border
                        }
                    )
            )
        }
    }
}

// ---------- 横幅 ----------

@Composable
internal fun AuthBanner(text: String, isError: Boolean, modifier: Modifier = Modifier) {
    val colors = AuthPureColors
    val containerColor = if (isError) colors.ErrorSurface else colors.SuccessSurface
    val borderColor = if (isError) colors.ErrorBorder else colors.SuccessBorder
    val contentColor = if (isError) colors.ErrorText else colors.SuccessText
    val icon = if (isError) Icons.Filled.Error else Icons.Filled.CheckCircle
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isError) colors.Error else colors.Success,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 18.sp,
                color = contentColor
            )
        }
    }
}

// ---------- 分隔线与步骤条 ----------

@Composable
internal fun AuthDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AuthPureColors.Border)
    )
}

/**
 * 单行式引导文案：前缀 + 若干内联链接（如"没有账号？立即注册"）。
 * 单个 Text 承载全部内容，换行后仍然整体居中对齐，链接词独立响应点击。
 */
internal data class AuthLink(
    val label: String,
    val onClick: (() -> Unit)? = null,
    val muted: Boolean = false
)

/**
 * 单行式引导文案：前缀 + 若干内联链接（如"没有账号？立即注册"）。
 * 单个 Text 承载全部内容，换行后仍整体居中对齐；链接词独立响应点击。
 */
@Composable
internal fun AuthLinkLine(
    prefix: String,
    links: List<AuthLink>,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyMedium.copy(color = AuthPureColors.Secondary),
        text = buildAnnotatedString {
            append(prefix)
            append(" ")
            links.forEach { link ->
                if (link.onClick != null) {
                    withLink(
                        LinkAnnotation.Clickable(
                            tag = link.label,
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = AuthPureColors.Ink,
                                    fontWeight = FontWeight.SemiBold
                                )
                            ),
                            linkInteractionListener = { link.onClick?.invoke() }
                        )
                    ) {
                        append(link.label)
                    }
                } else {
                    withStyle(
                        SpanStyle(
                            color = if (link.muted) AuthPureColors.Placeholder else AuthPureColors.Secondary
                        )
                    ) {
                        append(link.label)
                    }
                }
                append(" ")
            }
        }
    )
}

@Composable
internal fun AuthSteps(currentStep: Int, labels: List<String>, modifier: Modifier = Modifier) {
    val colors = AuthPureColors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        labels.forEachIndexed { index, label ->
            val active = (index + 1) == currentStep
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (active) colors.Ink else colors.DisabledBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (active) Color.White else colors.Muted
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    color = if (active) colors.Ink else colors.Muted
                )
            }
            if (index < labels.lastIndex) {
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .weight(1f, fill = false)
                        .widthIn(min = 12.dp)
                        .height(1.dp)
                        .background(colors.Border)
                )
                Spacer(Modifier.width(8.dp))
            }
        }
    }
}
