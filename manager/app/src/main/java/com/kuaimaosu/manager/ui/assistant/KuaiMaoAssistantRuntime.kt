package com.kuaimaosu.manager.ui.assistant

import android.content.Context
import android.media.AudioManager
import com.kuaimaosu.manager.ui.navigation.Navigator
import com.kuaimaosu.manager.ui.navigation.Route
import com.kuaimaosu.manager.ui.theme.BackgroundManager
import com.kuaimaosu.manager.ui.theme.BottomBarStyle
import com.kuaimaosu.manager.ui.theme.CapsuleEffect
import com.kuaimaosu.manager.ui.theme.CardConfig
import com.kuaimaosu.manager.ui.theme.ThemeConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 助手能直接打开的一级 / 二级页面。
 *
 * 之前助手只有几条写死的跳转，用户说「打开动态管理器」它只会回一句
 * 「请手动打开」。这里把 App 里所有能到达的页面编成索引，本地先匹配、
 * 远端模型也能通过 [pageIds] 精确选择。
 */
data class AppDestination(
    val id: String,
    val label: String,
    val keywords: List<String>,
    val route: Route,
)

object AppDestinations {
    val all: List<AppDestination> = listOf(
        AppDestination("home", "主页", listOf("主页", "首页", "home"), Route.Main),
        AppDestination("superuser", "超级用户", listOf("超级用户", "授权", "root", "su 列表"), Route.SuperUser),
        AppDestination("module", "模块", listOf("模块", "module", "已安装"), Route.Module),
        AppDestination("module_repo", "模块仓库", listOf("模块仓库", "仓库", "在线模块", "repo"), Route.ModuleRepo),
        AppDestination("settings", "设置", listOf("设置", "settings"), Route.Settings),
        AppDestination(
            "theme",
            "主题设置",
            listOf("主题", "外观", "壁纸", "背景", "字体", "配色", "主题色", "质感", "底栏设置"),
            Route.ThemeSettings,
        ),
        AppDestination("notifications", "通知", listOf("通知", "消息", "铃铛"), Route.Notifications),
        AppDestination("system_info", "系统信息", listOf("系统信息", "内核信息", "设备信息", "内核版本"), Route.SystemInfo),
        AppDestination("dynamic_manager", "动态管理器", listOf("动态管理器", "动态", "管理器配置"), Route.DynamicManager),
        AppDestination("umount", "卸载模块", listOf("卸载", "umount"), Route.UmountManager),
        AppDestination("susfs", "SuSFS 配置", listOf("susfs", "隐藏"), Route.SuSFSConfig),
        AppDestination("log", "日志", listOf("日志", "log", "sulog"), Route.Sulog),
        AppDestination("about", "关于", listOf("关于", "版本", "开源许可"), Route.About),
        AppDestination("ai_settings", "AI 设置", listOf("ai 设置", "模型设置", "api"), Route.SettingsAi),
    )

    fun find(query: String): AppDestination? {
        val q = query.lowercase()
        // 先精确命中名称 / 关键词，再退回包含匹配，避免「主题」把「主题设置」让位给别的页面
        all.firstOrNull { dest -> dest.keywords.any { q == it } }?.let { return it }
        return all
            .flatMap { dest -> dest.keywords.map { it to dest } }
            .filter { q.contains(it.first) }
            .maxByOrNull { it.first.length }
            ?.second
    }

    fun byId(id: String): AppDestination? = all.firstOrNull { it.id == id }
}

sealed interface AssistantAction {
    data class OpenPage(val pageId: String) : AssistantAction
    data class SetCapsuleEffect(val effect: CapsuleEffect) : AssistantAction
    data class SetBottomBarStyle(val style: BottomBarStyle) : AssistantAction
    data class SetBlur(val enabled: Boolean) : AssistantAction
    data class SetBackgroundDim(val percent: Int) : AssistantAction
    data class SetCardAlpha(val percent: Int) : AssistantAction
    data class SetVolume(val percent: Int) : AssistantAction
}

/**
 * 本地指令解析。
 *
 * 不需要联网就能处理「把底栏换成胶囊」「打开动态管理器」这类
 * 明确指令——先本地执行，没命中才交给模型。这样即使没配 API Key，
 * 助手也是能干活的状态，而不是一个只会聊天的输入框。
 */
object AssistantIntentParser {
    fun parse(raw: String): AssistantAction? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        val q = text.lowercase()

        // 1) 音量
        if (q.contains("音量")) {
            val value = Regex("(\\d{1,3})").find(q)?.groupValues?.get(1)?.toIntOrNull()
            if (value != null) return AssistantAction.SetVolume(value.coerceIn(0, 100))
        }

        // 3) 底栏样式（先于「打开主题设置」，用户要的是改，不是跳转）
        parseBottomBar(text)?.let { return it }

        // 4) 质感 / 模糊 / 暗度
        parseEffect(q)?.let { return it }

        // 5) 页面跳转
        AppDestinations.find(q)?.let { return AssistantAction.OpenPage(it.id) }
        return null
    }

    private fun parseBottomBar(text: String): AssistantAction? {
        val q = text.lowercase()
        val mentionsBar = listOf("底栏", "导航栏", "底部的栏", "底栏样式").any { q.contains(it) }
        if (!mentionsBar) return null
        return when {
            listOf("胶囊", "浮岛", "悬浮").any { q.contains(it) } ->
                AssistantAction.SetBottomBarStyle(BottomBarStyle.CAPSULE)
            listOf("传统", "经典", "贴底", "普通").any { q.contains(it) } ->
                AssistantAction.SetBottomBarStyle(BottomBarStyle.TRADITIONAL)
            listOf("侧边", "侧栏", "竖排").any { q.contains(it) } ->
                AssistantAction.SetBottomBarStyle(BottomBarStyle.RAIL)
            else -> null
        }
    }

    private fun parseEffect(q: String): AssistantAction? = when {
        q.contains("液态玻璃") -> AssistantAction.SetCapsuleEffect(CapsuleEffect.LIQUID_GLASS)
        q.contains("磨砂") -> AssistantAction.SetCapsuleEffect(CapsuleEffect.FROSTED)
        q.contains("半透明") -> AssistantAction.SetCapsuleEffect(CapsuleEffect.TRANSLUCENT)
        (q.contains("模糊") && (q.contains("关") || q.contains("取消") || q.contains("禁用"))) ->
            AssistantAction.SetBlur(false)
        (q.contains("模糊") && (q.contains("开") || q.contains("启用"))) ->
            AssistantAction.SetBlur(true)
        q.contains("暗度") || q.contains("变暗") || q.contains("压暗") -> {
            val value = Regex("(\\d{1,3})").find(q)?.groupValues?.get(1)?.toIntOrNull()
            value?.let { AssistantAction.SetBackgroundDim(it.coerceIn(0, 100)) }
        }
        else -> null
    }
}

/**
 * 助手的执行端。
 *
 * 只是一层「把导航 / 主题配置 / 播放器接起来」的胶水，自身不持有状态，
 * 由 NavContainer 在 Compose 树里注入真实对象。所有操作都作用在同一个
 * 全局实例上，所以助手改完底栏，用户切回首页立刻能看到。
 */
object KuaiMaoAssistantRuntime {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    var navigator: Navigator? = null
    var themeConfig: ThemeConfig? = null
    var cardConfig: CardConfig? = null
    var backgroundManager: BackgroundManager? = null
    var appContext: Context? = null

    fun execute(action: AssistantAction): String = when (action) {
        is AssistantAction.OpenPage -> openPage(action.pageId)
        is AssistantAction.SetCapsuleEffect -> {
            backgroundManager?.saveCapsuleEffect(action.effect)
            "已把胶囊质感切换为「${action.effect.label}」。${
                if (action.effect == CapsuleEffect.TRANSLUCENT) "这一档不模糊，只留一层薄膜。" else ""
            }"
        }

        is AssistantAction.SetBottomBarStyle -> {
            backgroundManager?.saveBottomBarStyle(action.style)
            "底栏已切换为${when (action.style) {
                BottomBarStyle.CAPSULE -> "胶囊样式"
                BottomBarStyle.TRADITIONAL -> "传统样式"
                BottomBarStyle.RAIL -> "侧边栏样式"
            }}。"
        }

        is AssistantAction.SetBlur -> {
            backgroundManager?.saveEnableBlur(action.enabled)
            if (action.enabled) "已开启背景模糊。" else "已关闭背景模糊。"
        }

        is AssistantAction.SetBackgroundDim -> {
            backgroundManager?.saveBackgroundDim(action.percent / 100f)
            "背景暗度已设为 ${action.percent}%。"
        }

        is AssistantAction.SetCardAlpha -> {
            cardConfig?.let {
                it.updateAlpha(action.percent / 100f)
                it.save()
            }
            "卡片透明度已设为 ${action.percent}%。"
        }

        is AssistantAction.SetVolume -> {
            val result = runCatching {
                val context = appContext ?: error("没有上下文")
                val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val max = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                manager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    (max * action.percent / 100f).toInt().coerceIn(0, max),
                    0,
                )
            }
            if (result.isSuccess) "媒体音量已调到 ${action.percent}%。" else "调整音量失败。"
        }
    }

    private fun openPage(pageId: String): String {
        val destination = AppDestinations.byId(pageId)
            ?: return "没有找到这个页面。"
        val nav = navigator ?: return "导航还没准备好，请稍后再试。"
        nav.push(destination.route)
        return "已打开${destination.label}。"
    }
}
