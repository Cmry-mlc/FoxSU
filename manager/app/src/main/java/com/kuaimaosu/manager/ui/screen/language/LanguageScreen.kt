package com.kuaimaosu.manager.ui.screen.language

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kuaimaosu.manager.R
import com.kuaimaosu.manager.data.AppSettingsRepository
import com.kuaimaosu.manager.ui.component.KuaiMaoBackCapsule
import com.kuaimaosu.manager.ui.component.KuaiMaoCard
import com.kuaimaosu.manager.ui.component.KuaiMaoPageTitle
import com.kuaimaosu.manager.ui.component.KuaiMaoRowDivider
import com.kuaimaosu.manager.ui.navigation.LocalNavigator
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import com.kuaimaosu.manager.ui.theme.backdropInk
import com.kuaimaosu.manager.ui.theme.blurSource
import com.kuaimaosu.manager.ui.util.adaptiveScaffoldWindowInsets
import com.kuaimaosu.manager.ui.viewmodel.SettingsUiAction
import com.kuaimaosu.manager.ui.viewmodel.SettingsViewModel
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * 应用语言。
 *
 * 之前这一项直接把用户丢进系统的「应用语言」页（ACTION_APP_LOCALE_SETTINGS）：
 * 黑底大 logo、系统自己的单选圈、一个点了没用的搜索按钮，跟管理器其余页面完全两套皮肤，
 * 而且系统页只能在 Android 13+ 用，低版本上还会退化成跳系统设置首页。
 *
 * 现在改成 App 内自建的页面，语言清单、选中态、分组标题都走 KuaiMao 的设计语言；
 * 切换语言同时写两处：
 * - Android 13+ 走框架的 LocaleManager（配合 res/xml/locales_config.xml，系统据此重建 Activity）；
 * - 13 以下写 app_locale 偏好，由 MainActivity.attachBaseContext 在重建时套用。
 */
@Composable
fun LanguageScreen() {
    val navigator = LocalNavigator.current
    val viewModel = koinViewModel<SettingsViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsRepository: AppSettingsRepository = koinInject()
    val activity = context as? android.app.Activity
    val scope = rememberCoroutineScope()

    val systemLocale = rememberSystemLocale()
    val appLocale = rememberAppLocale()
    val currentLocale = state.currentAppLocale
    val currentTag = currentLocale?.let(::localeToTag) ?: TAG_SYSTEM

    val options = remember(systemLocale, appLocale) {
        buildLanguageOptions(systemLocale, appLocale)
    }

    // 当前生效语言的名字：跟随系统时显示系统语言，否则显示已选语言的自称。
    val currentName = currentLocale?.let { languageTitle(localeToTag(it), it) }
        ?: languageTitle(localeToTag(systemLocale), systemLocale)

    Scaffold(
        contentWindowInsets = adaptiveScaffoldWindowInsets(),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(modifier = Modifier.height(6.dp))
                KuaiMaoBackCapsule(onClick = { navigator.pop() })
                Spacer(modifier = Modifier.height(14.dp))
                KuaiMaoPageTitle(
                    title = stringResource(R.string.settings_language),
                    subtitle = "选择后立即生效，不需要重启",
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .blurSource(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 32.dp,
            ),
        ) {
            item {
                // 顶部先回答「现在是什么语言」，省得用户在长列表里找选中的那条。
                KuaiMaoCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(KuaiMaoColors.brand.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.TwoTone.Translate,
                                contentDescription = null,
                                tint = KuaiMaoColors.brand,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "当前语言",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = KuaiMaoColors.textMuted,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currentName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = KuaiMaoColors.textBody,
                            )
                        }
                        if (currentLocale == null) {
                            PillTag(text = "跟随系统")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "选择语言",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = backdropInk(),
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${options.size} 种",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = KuaiMaoColors.textMuted,
                    )
                }
            }

            item {
                KuaiMaoCard(contentPadding = PaddingValues(horizontal = 16.dp)) {
                    options.forEachIndexed { index, option ->
                        LanguageRow(
                            option = option,
                            selected = option.tag == currentTag,
                            onClick = {
                                if (option.tag == currentTag) {
                                    navigator.pop()
                                    return@LanguageRow
                                }
                                viewModel.dispatch(SettingsUiAction.SetLanguage(option.tag))
                                applyAppLocale(context, option.tag)
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                    // 13 以下没有 LocaleManager，只能靠重建 Activity 让
                                    // attachBaseContext 重新套用 app_locale。
                                    // 这里自己等落盘再重建：若走 ViewModel 的事件流，
                                    // 语言页拿到的可能是另一个 VM 实例，事件没人收。
                                    scope.launch {
                                        settingsRepository.putString("app_locale", option.tag)
                                        delay(250)
                                        activity?.recreate()
                                    }
                                }
                            },
                        )
                        if (index != options.lastIndex) {
                            KuaiMaoRowDivider()
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            item {
                Text(
                    text = "语言由系统按应用单独记录。选「跟随系统」时会跟随系统语言变化；" +
                        "选择具体语言后，只有本应用会切换，系统其余部分不受影响。",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 15.sp,
                    color = KuaiMaoColors.textMuted,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun PillTag(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(KuaiMaoColors.brand.copy(alpha = 0.10f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = KuaiMaoColors.brand,
        )
    }
}

/**
 * 语言行：左侧语言代码方块 + 自称 / 译名两行，右侧自绘单选圈。
 *
 * 用方块而不是国旗：国旗既不准（一种语言对应多国）、又有版权与政治风险，
 * 一个字母/汉字反而一眼能认出是哪种语言。
 */
@Composable
private fun LanguageRow(
    option: LanguageOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (selected) {
                        KuaiMaoColors.brand.copy(alpha = 0.14f)
                    } else {
                        KuaiMaoColors.canvasFieldSolid
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = option.badge,
                fontSize = if (option.badge.length > 1) 11.sp else 15.sp,
                fontWeight = FontWeight.Black,
                color = if (selected) KuaiMaoColors.brand else KuaiMaoColors.textSec,
            )
        }
        Spacer(modifier = Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = option.title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = KuaiMaoColors.textBody,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = option.subtitle,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = KuaiMaoColors.textMuted,
                maxLines = 1,
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        RadioMark(selected = selected)
    }
}

/** 单选圈：选中是实心圆 + 反色内点，未选是空心描边。 */
@Composable
private fun RadioMark(selected: Boolean) {
    val accent = KuaiMaoColors.brand
    // 夜间品牌色接近白，内点必须换成深色，否则白点压在白圆上。
    val dotColor = if (accent.luminance() < 0.55f) Color.White else Color(0xFF101318)
    Box(
        modifier = Modifier
            .size(21.dp)
            .clip(CircleShape)
            .then(
                if (selected) {
                    Modifier.background(accent)
                } else {
                    Modifier.border(1.5.dp, KuaiMaoColors.lineField, CircleShape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 语言清单
// ---------------------------------------------------------------------------

private const val TAG_SYSTEM = "system"

/** 一行语言：tag 用于落盘与系统调用，标题用母语自称，副标题用当前界面语言的译名。 */
private data class LanguageOption(
    val tag: String,
    val locale: Locale?,
    val title: String,
    val subtitle: String,
    val badge: String,
)

/**
 * 内置语言清单。
 *
 * 只列**安装包里真的带资源**的语言：build.gradle.kts 里
 * `androidResources.localeFilters` 只保留 en / zh-rCN / zh-rHK / zh-rTW，
 * 其余几十种翻译在打 release 包时已被裁掉。清单里多写一门语言，
 * 用户切过去只会看到英文界面，比不列更让人困惑。
 *
 * 顺序固定：简体 → 繁体（台灣）→ 繁体（香港）→ English。
 * 这份清单要和 localeFilters 保持一致，改一处就要改另一处。
 */
private val SUPPORTED_LANGUAGE_TAGS = listOf(
    "zh_CN", "zh_TW", "zh_HK", "en",
)

@Composable
private fun rememberSystemLocale(): Locale = androidx.compose.runtime.remember {
    val locales = android.content.res.Resources.getSystem().configuration.locales
    if (locales.isEmpty) Locale.getDefault() else locales[0]
}

/** 应用当前生效的语言（可能是 per-app 覆盖后的结果），用来决定译名用哪种语言显示。 */
@Composable
private fun rememberAppLocale(): Locale {
    val context = LocalContext.current
    return androidx.compose.runtime.remember(context) {
        val locales = context.resources.configuration.locales
        if (locales.isEmpty) Locale.getDefault() else locales[0]
    }
}

/**
 * 常见语言的标题覆盖。
 *
 * Locale.getDisplayName(locale) 走 ICU 名称，中文会输出「中文（中国）」这种，
 * 和系统语言页里的「简体中文 / 繁體中文（台灣）」不是一套说法。这几种常用语言直接写死，
 * 其余继续用 ICU 名称（阿拉伯语、越南语之类本来也没歧义）。
 */
private val LANGUAGE_TITLE_OVERRIDES = mapOf(
    "zh_CN" to "简体中文",
    "zh_TW" to "繁體中文（台灣）",
    "zh_HK" to "繁體中文（香港）",
    "en" to "English",
)

private fun languageTitle(tag: String, locale: Locale): String =
    LANGUAGE_TITLE_OVERRIDES[tag] ?: locale.getDisplayName(locale)

private fun buildLanguageOptions(systemLocale: Locale, uiLocale: Locale): List<LanguageOption> {
    val systemOption = LanguageOption(
        tag = TAG_SYSTEM,
        locale = null,
        title = "跟随系统",
        subtitle = languageTitle(localeToTag(systemLocale), systemLocale),
        badge = "系",
    )
    val others = SUPPORTED_LANGUAGE_TAGS.mapNotNull { tag ->
        val locale = parseTag(tag) ?: return@mapNotNull null
        LanguageOption(
            tag = tag,
            locale = locale,
            title = languageTitle(tag, locale),
            subtitle = locale.getDisplayName(uiLocale),
            badge = localeBadge(locale),
        )
    }.distinctBy { it.title }
    return listOf(systemOption) + others
}

/** 方块里的字：中文用单字，其余用两位语言码。 */
private fun localeBadge(locale: Locale): String = when {
    locale.language == "zh" && locale.country == "CN" -> "简"
    locale.language == "zh" && locale.country == "TW" -> "繁"
    locale.language == "zh" && locale.country == "HK" -> "港"
    locale.language == "zh" -> "中"
    else -> locale.language.uppercase(Locale.ROOT).take(2)
}

private fun parseTag(tag: String): Locale? = try {
    if (tag.contains("_")) {
        val (language, region) = tag.split("_")
        Locale.Builder().setLanguage(language).setRegion(region).build()
    } else {
        Locale.Builder().setLanguage(tag).build()
    }
} catch (_: Exception) {
    null
}

private fun localeToTag(locale: Locale): String =
    if (locale.country.isEmpty()) locale.language else "${locale.language}_${locale.country}"

/**
 * 把选择写进系统的「按应用语言」。
 *
 * Android 13 起语言不再由应用自己维护，必须交给 LocaleManager，
 * 否则重建后资源还是旧语言。空列表 = 跟随系统。
 */
@SuppressLint("NewApi")
private fun applyAppLocale(context: Context, tag: String) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    try {
        val manager = context.getSystemService(Context.LOCALE_SERVICE) as? android.app.LocaleManager
            ?: return
        manager.applicationLocales = if (tag == TAG_SYSTEM) {
            LocaleList.getEmptyLocaleList()
        } else {
            LocaleList.forLanguageTags(tag.replace('_', '-'))
        }
    } catch (_: Exception) {
        // 个别 ROM 没有把 LocaleService 暴露出来，落盘值仍会在下次启动时生效。
    }
}
