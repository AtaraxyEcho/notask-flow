package com.notaskflow.feature.auth

import android.content.Context
import android.content.res.Configuration
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import java.util.Locale

/** 认证页语言控制器（仅作用于认证流程子树，独立于应用主框架）。 */
internal object AuthLocaleController {
    private const val PREFS = "auth_locale_prefs"
    private const val KEY_LOCALE = "locale"
    const val CHINESE = "zh-CN"
    const val ENGLISH = "en"

    var tag by mutableStateOf(ENGLISH)
        private set

    private var initialized = false

    /** 首次进入时读取持久化选择；无记录则跟随系统语言。 */
    fun init(context: Context) {
        if (initialized) {
            return
        }
        initialized = true
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LOCALE, null)
        tag = stored
            ?: if (context.resources.configuration.locales[0].language.startsWith("en")) ENGLISH else CHINESE
    }

    fun toggle(context: Context) {
        tag = if (tag == CHINESE) ENGLISH else CHINESE
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LOCALE, tag)
            .apply()
    }
}

/**
 * 为认证子树提供所选语言的资源配置；
 * 其余页面不经过此 Provider，不受语言切换影响。
 */
@Composable
fun AuthLocaleProvider(content: @Composable () -> Unit) {
    val context = LocalContext.current
    remember { AuthLocaleController.init(context) }
    val configuration = LocalConfiguration.current
    val localizedConfiguration = remember(AuthLocaleController.tag) {
        Configuration(configuration).apply {
            setLocale(Locale.forLanguageTag(AuthLocaleController.tag))
        }
    }
    val localizedContext = remember(localizedConfiguration) {
        context.createConfigurationContext(localizedConfiguration)
    }
    androidx.compose.runtime.CompositionLocalProvider(
        LocalConfiguration provides localizedConfiguration,
        LocalContext provides localizedContext,
        content = content
    )
}

/** 顶栏右侧的中英切换按钮：显示将要切换到的语言。 */
@Composable
internal fun AuthLanguageToggle() {
    val context = LocalContext.current
    TextButton(
        onClick = { AuthLocaleController.toggle(context) },
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
    ) {
        Text(
            text = if (AuthLocaleController.tag == AuthLocaleController.CHINESE) "EN" else "中文",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
