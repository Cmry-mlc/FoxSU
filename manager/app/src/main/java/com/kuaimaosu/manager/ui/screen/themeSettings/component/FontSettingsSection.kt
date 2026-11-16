package com.kuaimaosu.manager.ui.screen.themeSettings.component

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Add
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuaimaosu.manager.data.theme.ThemeRepository
import com.kuaimaosu.manager.ui.component.KuaiMaoCard
import com.kuaimaosu.manager.ui.component.KuaiMaoRow
import com.kuaimaosu.manager.ui.component.KuaiMaoRowDivider
import com.kuaimaosu.manager.ui.theme.AppFont
import com.kuaimaosu.manager.ui.theme.AppFonts
import com.kuaimaosu.manager.ui.theme.FontDownloader
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import com.kuaimaosu.manager.ui.theme.ThemeConfig
import com.kuaimaosu.manager.ui.theme.loadLocalFontFamily
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * 应用字体：内置 10 套开源中文字体，点一下就能下载并全局生效。
 *
 * 选择器是带字体样本的双列卡片：每张卡用「这套字体自己」写一个「永」和「Aa」，
 * 没下载的退回系统字体显示名字，下载完立刻就能看到真实字形再决定换不换。
 * 之前是一列灰条列表，只有名字、没有字形，十套字体长得一模一样。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KuaiMaoFontSection(modifier: Modifier = Modifier) {
    val themeConfig: ThemeConfig = koinInject()
    val repository: ThemeRepository = koinInject()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showSheet by remember { mutableStateOf(false) }
    // 字体 id -> 0f..1f，仅记录正在下载的项
    val progress = remember { mutableStateMapOf<String, Float>() }
    var error by remember { mutableStateOf<String?>(null) }

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

    fun applyFont(id: String) {
        themeConfig.appFontId = id
        repository.saveAppFont(id)
    }

    fun startDownload(font: AppFont) {
        if (progress[font.id] != null) return
        error = null
        scope.launch {
            progress[font.id] = 0f
            FontDownloader.download(context, font) { value ->
                progress[font.id] = value
            }.onSuccess {
                progress.remove(font.id)
                applyFont(font.id)
                showSheet = false
            }.onFailure {
                progress.remove(font.id)
                error = it.message ?: "下载失败，换个网络再试"
            }
        }
    }

    Column(modifier = modifier) {
        Text(
            text = "应用字体",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp,
            color = KuaiMaoColors.textTer,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        KuaiMaoCard {
            KuaiMaoRow(
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
                        color = KuaiMaoColors.textSec,
                        modifier = Modifier.padding(end = 6.dp),
                    )
                },
            )
            KuaiMaoRowDivider()
            KuaiMaoRow(
                icon = Icons.TwoTone.FontDownload,
                title = "导入自定义字体",
                description = "支持 ttf / otf，导入后立即全局生效",
                onClick = {
                    picker.launch(arrayOf("font/ttf", "font/otf", "application/octet-stream", "*/*"))
                },
            )
            KuaiMaoRowDivider()
            KuaiMaoRow(
                icon = Icons.TwoTone.CloudDownload,
                title = "内置字体库",
                description = "10 套开源中文字体 · 多节点自动切换",
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
            containerColor = KuaiMaoColors.canvasApp,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "选择字体",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            color = KuaiMaoColors.text,
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "卡片里就是这套字体的样子 · 下载后自动应用",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = KuaiMaoColors.textMuted,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))

                error?.let {
                    Text(
                        text = it,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 2.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 540.dp)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item(key = AppFonts.SYSTEM_ID) {
                        FontPreviewCard(
                            sample = "永字 Aa",
                            family = FontFamily.Default,
                            name = "系统默认",
                            status = "跟随 ROM 字体",
                            selected = themeConfig.appFontId == AppFonts.SYSTEM_ID,
                            downloading = null,
                            onClick = {
                                applyFont(AppFonts.SYSTEM_ID)
                                showSheet = false
                            },
                        )
                    }
                    items(AppFonts.catalog, key = { it.id }) { font ->
                        val installed = AppFonts.isInstalled(context, font.id)
                        FontPreviewCard(
                            sample = "永字 Aa",
                            family = remember(font.id, themeConfig.appFontId) {
                                loadLocalFontFamily(context, font.id)
                            } ?: FontFamily.Default,
                            name = font.name,
                            status = when {
                                progress[font.id] != null -> "下载中 ${((progress[font.id] ?: 0f) * 100).toInt()}%"
                                installed -> "已下载 · ${"%.1f".format(font.approxBytes / 1024f / 1024f)} MB"
                                else -> "${"%.1f".format(font.approxBytes / 1024f / 1024f)} MB · 点击下载"
                            },
                            selected = themeConfig.appFontId == font.id,
                            downloading = progress[font.id],
                            onClick = {
                                when {
                                    themeConfig.appFontId == font.id -> showSheet = false
                                    installed -> {
                                        applyFont(font.id)
                                        showSheet = false
                                    }
                                    progress[font.id] != null -> Unit
                                    else -> startDownload(font)
                                }
                            },
                        )
                    }
                    item(key = AppFonts.CUSTOM_ID) {
                        ImportCard(
                            installed = customInstalled,
                            selected = themeConfig.appFontId == AppFonts.CUSTOM_ID && customInstalled,
                            onClick = {
                                picker.launch(
                                    arrayOf("font/ttf", "font/otf", "application/octet-stream", "*/*"),
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

/** 字体卡：上方字形样本，下方名称与状态。 */
@Composable
private fun FontPreviewCard(
    sample: String,
    family: FontFamily,
    name: String,
    status: String,
    selected: Boolean,
    downloading: Float?,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val accent = KuaiMaoColors.brand
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (selected) KuaiMaoColors.brandSoft.copy(alpha = 0.55f) else KuaiMaoColors.canvasSubtle,
            )
            .border(
                width = if (selected) 1.8.dp else 0.8.dp,
                color = if (selected) accent else KuaiMaoColors.line,
                shape = shape,
            )
            .clickable(enabled = downloading == null, onClick = onClick)
            .padding(10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (selected) accent.copy(alpha = 0.10f) else KuaiMaoColors.canvasCard,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = sample,
                fontFamily = family,
                fontSize = 23.sp,
                fontWeight = FontWeight.Medium,
                color = KuaiMaoColors.text,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                textAlign = TextAlign.Center,
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .size(15.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.TwoTone.Check,
                        contentDescription = null,
                        tint = if (accent.luminance() < 0.55f) Color.White else Color(0xFF101318),
                        modifier = Modifier.size(10.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = KuaiMaoColors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = status,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (downloading != null) accent else KuaiMaoColors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )

        if (downloading != null) {
            Spacer(modifier = Modifier.height(7.dp))
            LinearProgressIndicator(
                progress = { downloading },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = accent,
                trackColor = KuaiMaoColors.line,
            )
        }
    }
}

/** 末尾的「导入本地字体」卡：虚线感的浅底 + 加号。 */
@Composable
private fun ImportCard(
    installed: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(KuaiMaoColors.canvasSubtle)
            .border(
                width = 1.dp,
                color = if (selected) KuaiMaoColors.brand else KuaiMaoColors.line,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(KuaiMaoColors.canvasCard),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.TwoTone.Add,
                    contentDescription = null,
                    tint = KuaiMaoColors.textTer,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "ttf / otf",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = KuaiMaoColors.textMuted,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "导入本地字体",
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = KuaiMaoColors.text,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = if (installed) "已导入 · 点此替换" else "用自己的字体文件",
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = KuaiMaoColors.textMuted,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
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
