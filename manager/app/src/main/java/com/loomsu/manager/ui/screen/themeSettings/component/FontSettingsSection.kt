package com.loomsu.manager.ui.screen.themeSettings.component

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Check
import androidx.compose.material.icons.twotone.CloudDownload
import androidx.compose.material.icons.twotone.FontDownload
import androidx.compose.material.icons.twotone.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loomsu.manager.data.theme.ThemeRepository
import com.loomsu.manager.ui.component.LoomCard
import com.loomsu.manager.ui.component.LoomRow
import com.loomsu.manager.ui.component.LoomRowDivider
import com.loomsu.manager.ui.theme.AppFont
import com.loomsu.manager.ui.theme.AppFonts
import com.loomsu.manager.ui.theme.FontDownloader
import com.loomsu.manager.ui.theme.LoomColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.loomsu.manager.ui.theme.ThemeConfig
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * 应用字体：内置 10 套开源中文字体（jsDelivr 国内镜像），
 * 点一下就能下载并全局生效，进度条实时走百分比；也可以导入自己的 ttf/otf。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoomFontSection(modifier: Modifier = Modifier) {
    val themeConfig: ThemeConfig = koinInject()
    val repository: ThemeRepository = koinInject()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showSheet by remember { mutableStateOf(false) }
    // 字体 id -> 0f..1f，仅记录正在下载的项
    val progress = remember { mutableStateMapOf<String, Float>() }
    var error by remember { mutableStateOf<String?>(null) }
    var available by remember { mutableStateOf(AppFonts.catalog.map { it.id }) }

    val current = AppFonts.find(themeConfig.appFontId)
    val customInstalled = AppFonts.fontFile(context, AppFonts.CUSTOM_ID).exists()

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = runCatching { importFont(context, uri) }.getOrDefault(false)
            if (ok) {
                themeConfig.appFontId = AppFonts.CUSTOM_ID
                repository.saveAppFont(AppFonts.CUSTOM_ID)
                showSheet = false
                error = null
            } else {
                error = "这个文件不是有效的字体，换一个 ttf / otf 试试"
            }
        }
    }

    fun apply(font: AppFont) {
        themeConfig.appFontId = font.id
        repository.saveAppFont(font.id)
    }

    Column(modifier = modifier) {
        Text(
            text = "应用字体",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp,
            color = LoomColors.textTer,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        LoomCard {
            LoomRow(
                icon = Icons.TwoTone.TextFields,
                title = "当前字体",
                description = when {
                    current != null -> current.desc
                    themeConfig.appFontId == AppFonts.CUSTOM_ID && customInstalled -> "从本地导入的字体"
                    else -> "跟随系统字体渲染"
                },
                onClick = { showSheet = true },
                trailing = {
                    Text(
                        text = when {
                            current != null -> current.name
                            themeConfig.appFontId == AppFonts.CUSTOM_ID && customInstalled -> "自定义"
                            else -> "系统默认"
                        },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = LoomColors.textSec,
                        modifier = Modifier.padding(end = 6.dp),
                    )
                },
            )
            LoomRowDivider()
            LoomRow(
                icon = Icons.TwoTone.FontDownload,
                title = "导入自定义字体",
                description = "支持 ttf / otf，导入后立即全局生效",
                onClick = {
                    picker.launch(arrayOf("font/ttf", "font/otf", "application/octet-stream", "*/*"))
                },
            )
            LoomRowDivider()
            LoomRow(
                icon = Icons.TwoTone.CloudDownload,
                title = "内置字体库",
                description = "10 套开源中文字体 · 国内加速节点下载",
                onClick = { showSheet = true },
            )
        }
        error?.let {
            Text(
                text = it,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 6.dp, top = 8.dp),
            )
        }
    }

    if (showSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = LoomColors.canvasApp,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            ) {
                Text(
                    text = "应用字体",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = LoomColors.text,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "下载后自动应用 · 文件存在应用私有目录，不占系统空间",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = LoomColors.textMuted,
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 460.dp)) {
                    item {
                        FontTile(
                            name = "系统默认",
                            desc = "不做任何替换，跟随 ROM 字体",
                            selected = themeConfig.appFontId == AppFonts.SYSTEM_ID,
                            installed = true,
                            downloading = null,
                            onClick = {
                                themeConfig.appFontId = AppFonts.SYSTEM_ID
                                repository.saveAppFont(AppFonts.SYSTEM_ID)
                                showSheet = false
                            },
                        )
                    }
                    items(AppFonts.catalog, key = { it.id }) { font ->
                        val installed = available.contains(font.id) &&
                            AppFonts.isInstalled(context, font.id)
                        FontTile(
                            name = font.name,
                            desc = "${font.desc} · ${"%.1f".format(font.approxBytes / 1024f / 1024f)} MB",
                            selected = themeConfig.appFontId == font.id,
                            installed = installed,
                            downloading = progress[font.id],
                            onClick = {
                                error = null
                                when {
                                    themeConfig.appFontId == font.id -> showSheet = false
                                    installed -> {
                                        apply(font)
                                        showSheet = false
                                    }
                                    progress[font.id] != null -> Unit
                                    else -> scope.launch {
                                        progress[font.id] = 0f
                                        FontDownloader.download(context, font) { value ->
                                            progress[font.id] = value
                                        }.onSuccess {
                                            progress.remove(font.id)
                                            apply(font)
                                            showSheet = false
                                        }.onFailure {
                                            progress.remove(font.id)
                                            error = it.message ?: "下载失败，检查网络后重试"
                                            available = available - font.id
                                        }
                                    }
                                }
                            },
                        )
                    }
                    item {
                        FontTile(
                            name = "导入本地字体",
                            desc = "ttf / otf 文件，导入后可直接应用",
                            selected = themeConfig.appFontId == AppFonts.CUSTOM_ID && customInstalled,
                            installed = customInstalled,
                            downloading = null,
                            onClick = {
                                picker.launch(
                                    arrayOf("font/ttf", "font/otf", "application/octet-stream", "*/*"),
                                )
                            },
                        )
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun FontTile(
    name: String,
    desc: String,
    selected: Boolean,
    installed: Boolean,
    downloading: Float?,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    // 选中底是 primaryContainer：灰色主题下它是深灰，深色文字会糊成一片，
    // 所以按底色亮度决定前景，浅底用正文色、深底用白字。
    val selectedInk = if (LoomColors.brandSoft.luminance() < 0.5f) Color.White else LoomColors.text
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(shape)
            .background(if (selected) LoomColors.brandSoft else LoomColors.canvasSubtle)
            .clickable(enabled = downloading == null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = if (selected) selectedInk else LoomColors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = desc,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (selected) selectedInk.copy(alpha = 0.76f) else LoomColors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            when {
                downloading != null -> Text(
                    text = "${(downloading * 100).toInt()}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = if (selected) selectedInk else LoomColors.brand,
                )
                selected -> Icon(
                    imageVector = Icons.TwoTone.Check,
                    contentDescription = null,
                    tint = LoomColors.brand,
                    modifier = Modifier.size(18.dp),
                )
                else -> Text(
                    text = if (installed) "已下载" else "下载",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) selectedInk.copy(alpha = 0.76f) else LoomColors.textTer,
                )
            }
        }
        if (downloading != null) {
            Spacer(modifier = Modifier.height(7.dp))
            LinearProgressIndicator(
                progress = { downloading },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = LoomColors.brand,
                trackColor = LoomColors.line,
            )
        }
    }
}

/** 把用户选的 ttf/otf 复制到私有目录，成功返回 true。 */
private fun importFont(context: Context, uri: Uri): Boolean {
    val target = AppFonts.fontFile(context, AppFonts.CUSTOM_ID)
    val tmp = File(target.parentFile, "${AppFonts.CUSTOM_ID}.part")
    val input = context.contentResolver.openInputStream(uri) ?: return false
    input.use { source ->
        FileOutputStream(tmp).use { output -> source.copyTo(output) }
    }
    if (tmp.length() < 32 * 1024) {
        tmp.delete()
        return false
    }
    if (target.exists()) target.delete()
    if (!tmp.renameTo(target)) {
        tmp.copyTo(target, overwrite = true)
        tmp.delete()
    }
    return true
}
