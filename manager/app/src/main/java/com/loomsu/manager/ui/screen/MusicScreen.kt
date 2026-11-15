package com.loomsu.manager.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.KeyboardArrowLeft
import androidx.compose.material.icons.twotone.Close
import androidx.compose.material.icons.twotone.MusicNote
import androidx.compose.material.icons.twotone.Pause
import androidx.compose.material.icons.twotone.PlayArrow
import androidx.compose.material.icons.twotone.Search
import androidx.compose.material.icons.twotone.SkipNext
import androidx.compose.material.icons.twotone.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.loomsu.manager.data.music.LoomMusicApi
import com.loomsu.manager.data.music.LoomMusicPlayer
import com.loomsu.manager.data.music.MusicState
import com.loomsu.manager.data.music.MusicTrack
import com.loomsu.manager.ui.component.LoomCard
import com.loomsu.manager.ui.component.LoomRowDivider
import com.loomsu.manager.ui.navigation.LocalNavigator
import com.loomsu.manager.ui.theme.LoomColors
import com.loomsu.manager.ui.theme.backdropInk
import com.loomsu.manager.ui.theme.renderBackgroundBlur
import kotlinx.coroutines.launch

/**
 * 音乐页。
 *
 * 在线搜索走 Audius 的公开接口（无需 Key），点一首就直接流式播放；
 * 播放状态放在全局的 [LoomMusicPlayer] 里，所以退出页面、回到主页，
 * 迷你播放条还在，歌也不会断。
 */
@Composable
fun MusicScreen(onBack: () -> Unit = {}) {
    val navigator = LocalNavigator.current
    val state by LoomMusicPlayer.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<MusicTrack>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var section by remember { mutableStateOf("热门") }

    fun runSearch(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        loading = true
        error = null
        section = "「$trimmed」的结果"
        scope.launch {
            LoomMusicApi.search(trimmed, 24)
                .onSuccess { list ->
                    results = list
                    if (list.isEmpty()) error = "没有找到可播放的曲目，换个关键词试试。"
                }
                .onFailure { error = it.message ?: "搜索失败，检查一下网络。" }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        if (results.isNotEmpty()) return@LaunchedEffect
        loading = true
        LoomMusicApi.trending(20)
            .onSuccess { if (it.isNotEmpty()) results = it }
            .onFailure { error = it.message ?: "无法连接音乐服务。" }
        loading = false
        section = "热门"
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { _ ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                MusicHeader(
                    onBack = {
                        if (navigator.backStack.size > 1) navigator.pop() else onBack()
                    },
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            item {
                MusicSearchField(
                    query = query,
                    onQueryChange = { query = it },
                    onSearch = { runSearch(query) },
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (state.hasTrack) {
                item {
                    NowPlayingCard(state = state)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = section,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = backdropInk(),
                        modifier = Modifier.weight(1f),
                    )
                    if (loading) {
                        Text(
                            text = "加载中…",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = LoomColors.textMuted,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                LoomCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)) {
                    when {
                        error != null && results.isEmpty() -> MusicEmptyState(error!!)
                        results.isEmpty() && !loading -> MusicEmptyState("搜索一首歌开始播放。")
                        else -> {
                            results.forEachIndexed { index, track ->
                                if (index > 0) LoomRowDivider()
                                MusicRow(
                                    track = track,
                                    isCurrent = state.track?.id == track.id,
                                    onClick = { LoomMusicPlayer.playQueue(results, index) },
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(28.dp)) }
        }
    }
}

@Composable
private fun MusicHeader(onBack: () -> Unit) {
    Column {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(LoomColors.canvasCard)
                .renderBackgroundBlur(tintColor = LoomColors.canvasCardSolid)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.TwoTone.KeyboardArrowLeft,
                contentDescription = "返回",
                tint = LoomColors.textBody,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "音乐",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp,
            color = backdropInk(),
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = "在线搜索 · 后台播放 · 可以让助手帮你放",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = LoomColors.textMuted,
        )
    }
}

@Composable
private fun MusicSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(LoomColors.canvasField)
            .renderBackgroundBlur(tintColor = LoomColors.canvasFieldSolid)
            .padding(start = 13.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.TwoTone.Search,
            contentDescription = null,
            tint = LoomColors.textMuted,
            modifier = Modifier.size(17.dp),
        )
        Spacer(modifier = Modifier.width(9.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "搜索歌曲或歌手",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = LoomColors.textMuted,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = LoomColors.textBody,
                ),
                cursorBrush = SolidColor(LoomColors.brand),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable { onQueryChange("") },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.TwoTone.Close,
                    contentDescription = null,
                    tint = LoomColors.textMuted,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

@Composable
private fun NowPlayingCard(state: MusicState) {
    val track = state.track ?: return
    LoomCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MusicArtwork(track = track, size = 62.dp, corner = 16.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = LoomColors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = track.artist,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = LoomColors.textSec,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (state.loading) "加载中…" else state.error ?: formatTime(state.positionMs),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state.error != null) LoomColors.danger else LoomColors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Slider(
            value = state.progress,
            onValueChange = { fraction ->
                if (state.durationMs > 0) {
                    LoomMusicPlayer.seekTo((state.durationMs * fraction).toLong())
                }
            },
            colors = SliderDefaults.colors(
                thumbColor = LoomColors.brand,
                activeTrackColor = LoomColors.brand,
                inactiveTrackColor = LoomColors.line,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MusicControlButton(
                icon = Icons.TwoTone.SkipPrevious,
                description = "上一首",
                onClick = { LoomMusicPlayer.previous() },
            )
            Spacer(modifier = Modifier.width(18.dp))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(LoomColors.brand)
                    .clickable { LoomMusicPlayer.toggle() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (state.playing) Icons.TwoTone.Pause else Icons.TwoTone.PlayArrow,
                    contentDescription = if (state.playing) "暂停" else "播放",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(18.dp))
            MusicControlButton(
                icon = Icons.TwoTone.SkipNext,
                description = "下一首",
                onClick = { LoomMusicPlayer.next() },
            )
        }
    }
}

@Composable
private fun MusicControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(LoomColors.canvasField)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = LoomColors.textBody,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun MusicRow(
    track: MusicTrack,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MusicArtwork(track = track, size = 42.dp, corner = 12.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isCurrent) LoomColors.brand else LoomColors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = track.artist,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                color = LoomColors.textTer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = track.durationText,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = LoomColors.textMuted,
        )
    }
}

@Composable
private fun MusicArtwork(track: MusicTrack, size: androidx.compose.ui.unit.Dp, corner: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(LoomColors.iconContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (!track.artwork.isNullOrBlank()) {
            AsyncImage(
                model = track.artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.TwoTone.MusicNote,
                contentDescription = null,
                tint = LoomColors.textMuted,
                modifier = Modifier.size(size * 0.42f),
            )
        }
    }
}

@Composable
private fun MusicEmptyState(text: String) {
    Text(
        text = text,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Medium,
        color = LoomColors.textMuted,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 26.dp),
    )
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}
