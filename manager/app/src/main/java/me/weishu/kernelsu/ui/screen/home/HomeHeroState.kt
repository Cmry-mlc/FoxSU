package me.weishu.kernelsu.ui.screen.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.KernelVersion

/**
 * 全局刷写状态追踪器。
 *
 * 刷写页（FlashScreen / FlashEffect）在真实执行 boot-patch 时回写状态，
 * 主页 Hero 大卡片据此展示真实的「刷写中 / 已刷写待重启 / 刷写失败」，
 * 而不是一个永远不变的静态文案。
 */
enum class FlashPhase {
    IDLE,
    FLASHING,
    SUCCESS,
    FAILED,
}

object FlashTracker {
    var phase by mutableStateOf(FlashPhase.IDLE)
        private set

    var detail by mutableStateOf("")
        private set

    fun begin(detail: String = "") {
        phase = FlashPhase.FLASHING
        this.detail = detail
    }

    fun succeed(detail: String = "") {
        phase = FlashPhase.SUCCESS
        this.detail = detail
    }

    fun fail(detail: String = "") {
        phase = FlashPhase.FAILED
        this.detail = detail
    }

    fun reset() {
        phase = FlashPhase.IDLE
        detail = ""
    }
}

/**
 * Hero 大卡片的统一状态机。
 *
 * 所有主题（Fresh / Miuix / Editorial / Glass / Nocturne）共用同一套判定，
 * 保证同一台设备在不同主题下显示的状态语义完全一致。
 */
enum class HeroPhase {
    /** 未安装，且当前内核可刷写 */
    NOT_INSTALLED,

    /** 正在刷写 */
    FLASHING,

    /** 已刷写成功，等待重启生效 */
    PENDING_REBOOT,

    /** 已安装且一切正常 */
    WORKING,

    /** 已安装但存在告警 */
    ATTENTION,

    /** 刷写失败 */
    FLASH_FAILED,

    /** 当前内核不支持 */
    UNSUPPORTED,
}

data class HeroState(
    val phase: HeroPhase,
    val installed: Boolean,
    /** LKM / GKI；非 GKI 内核时为 null（不能瞎写 GKI） */
    val modeLabel: String?,
    val jailbreakMode: Boolean,
    val safeMode: Boolean,
    val customLkm: Boolean,
    val versionName: String,
    val hasUpdate: Boolean,
    val latestVersionCode: Int,
    /** 越狱模式下刷写会变砖，因此禁用刷写入口 */
    val canFlash: Boolean,
    val flashFailed: Boolean,
)

/**
 * 由真实运行态推导 Hero 状态。
 *
 * @param flashPhase 全局刷写阶段（由 [FlashTracker] 提供）
 */
fun HomeUiState.heroState(
    flashPhase: FlashPhase = FlashTracker.phase,
): HeroState {
    val issues = freshIssues()
    val phase = when {
        flashPhase == FlashPhase.FLASHING -> HeroPhase.FLASHING
        // 刷写失败但旧版本仍在运行 → 只是需要关注；只有没装上才算彻底未工作
        flashPhase == FlashPhase.FAILED && ksuVersion == null -> HeroPhase.FLASH_FAILED
        flashPhase == FlashPhase.SUCCESS && ksuVersion == null -> HeroPhase.PENDING_REBOOT
        ksuVersion == null ->
            if (kernelVersion.isGKI()) HeroPhase.NOT_INSTALLED else HeroPhase.UNSUPPORTED
        issues.isNotEmpty() -> HeroPhase.ATTENTION
        else -> HeroPhase.WORKING
    }

    return HeroState(
        phase = phase,
        installed = ksuVersion != null,
        // lkmMode 为 null 表示当前内核不是 GKI，此时不能显示 GKI
        modeLabel = when (lkmMode) {
            true -> "LKM"
            false -> "GKI"
            null -> null
        },
        jailbreakMode = isLateLoadMode,
        safeMode = isSafeMode,
        customLkm = showCustomLkmBadge,
        versionName = BuildConfig.VERSION_NAME,
        hasUpdate = hasUpdate,
        latestVersionCode = latestVersionInfo.versionCode,
        canFlash = !isLateLoadMode,
        flashFailed = flashPhase == FlashPhase.FAILED,
    )
}

/** 供各主题复用的真实内核版本展示值。 */
fun HomeUiState.displayKernelVersion(): String =
    systemInfo.kernelVersion.ifBlank { kernelVersion.toString() }

/** 供各主题复用的真实管理器版本展示值（跟随应用版本自动更新）。 */
fun HomeUiState.displayManagerVersion(): String = BuildConfig.VERSION_NAME
