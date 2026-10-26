package com.notaskflow.core.common

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * 全局语言控制器，作用于整个应用（含认证页与主界面）。
 *
 * 语言选择持久化到 SharedPreferences，登录后与登录前保持一致，
 * 解决此前仅认证页生效、主界面硬编码中文的问题。
 */
object AppLocaleController {

    private const val PREFS = "app_locale_prefs"
    private const val KEY_LOCALE = "locale"

    const val CHINESE = "zh-CN"
    const val ENGLISH = "en"

    var tag by mutableStateOf(CHINESE)
        private set

    private var initialized = false

    /**
     * 初始化语言标记，读取持久化选择；无记录则跟随系统语言。
     *
     * @param context 上下文
     */
    fun init(context: Context) {
        if (initialized) {
            return
        }
        initialized = true
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LOCALE, null)
        tag = stored
            ?: if (context.resources.configuration.locales[0].language.startsWith("en")) ENGLISH else CHINESE
    }

    /**
     * 切换语言并持久化。
     *
     * @param context 上下文
     */
    fun toggle(context: Context) {
        setLocale(context, if (tag == CHINESE) ENGLISH else CHINESE)
    }

    /**
     * 指定语言并持久化。
     *
     * @param context 上下文
     * @param localeTag 语言标记
     */
    fun setLocale(context: Context, localeTag: String) {
        tag = localeTag
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LOCALE, localeTag)
            .apply()
    }

    /**
     * 判断当前是否为英文。
     *
     * @return true 表示英文
     */
    fun isEnglish(): Boolean = tag == ENGLISH
}

/**
 * 包装上下文并重写资源配置，使子树按所选语言取字符串。
 *
 * base 保持 Activity 本身（而非 createConfigurationContext 的 ContextImpl），
 * 以保证 Hilt/导航可沿 Context 链找到 Activity。
 */
private class AppLocaleContextWrapper(
    base: Context,
    private val localizedResources: Resources
) : android.content.ContextWrapper(base) {

    override fun getResources(): Resources = localizedResources

    companion object {

        /**
         * 构造本地化上下文包装。
         *
         * @param context 原始上下文
         * @param tag 语言标记
         * @return 本地化上下文
         */
        fun wrap(context: Context, tag: String): AppLocaleContextWrapper {
            val config = Configuration(context.resources.configuration).apply {
                setLocale(Locale.forLanguageTag(tag))
            }
            val localizedContext = context.createConfigurationContext(config)
            return AppLocaleContextWrapper(context, localizedContext.resources)
        }
    }
}

/**
 * 为整个应用子树提供所选语言的资源配置。
 *
 * @param content 应用内容
 */
@Composable
fun AppLocaleProvider(content: @Composable () -> Unit) {
    val context = LocalContext.current
    remember { AppLocaleController.init(context) }
    val localizedContext = remember(AppLocaleController.tag) {
        AppLocaleContextWrapper.wrap(context, AppLocaleController.tag)
    }
    CompositionLocalProvider(LocalContext provides localizedContext, content = content)
}
