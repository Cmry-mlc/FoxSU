package com.loomsu.manager.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Close
import androidx.compose.material.icons.twotone.MusicNote
import androidx.compose.material.icons.twotone.Pause
import androidx.compose.material.icons.twotone.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.loomsu.manager.data.music.LoomMusicPlayer
import com.loomsu.manager.ui.navigation.LocalNavigator
import com.loomsu.manager.ui.navigation.Route
import com.loomsu.manager.ui.theme.LoomColors
import com.loomsu.manager.ui.theme.renderBackgroundBlur

/**
 * 全局迷你播放条。
 *
 * 挂在导航容器最外层，所以不管在主页、设置还是模块页，只要有一首歌在放
 * 它就在。主页有底栏，抬高到胶囊上方；二级页没有底栏，直接贴底。
 * 音乐页本身不显示——那一页有完整的播放卡。
 */
@Composable
fun MiniPlayerHost() {
    val state by LoomMusicPlayer.state.collectAsStateWithLifecycle()
    if (!state.hasTrack) return

    val navigator = LocalNavigator.current
    val current = navigator.backStack.lastOrNull()
    if (current is Route.Music) return
    val aboveBottomBar = current is Route.Main

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = if (aboveBottomBar) 92.dp else 16.dp)
                .height(58.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(LoomColors.canvasCard)
                .renderBackgroundBlur(tintColor = LoomColors.canvasCardSolid)
                .clickable {
                    if (navigator.current() !is Route.Music) navigator.push(Route.Music)
                }
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(LoomColors.iconContainer),
                contentAlignment = Alignment.Center,
            ) {
                val artwork = state.track?.artwork
                if (!artwork.isNullOrBlank()) {
                    AsyncImage(
                        model = artwork,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.TwoTone.MusicNote,
                        contentDescription = null,
                        tint = LoomColors.textMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.track?.title.orEmpty(),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = LoomColors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = state.error ?: (state.track?.artist ?: "正在播放"),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (state.error != null) LoomColors.danger else LoomColors.textTer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(LoomColors.brand)
                    .clickable { LoomMusicPlayer.toggle() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (state.playing) Icons.TwoTone.Pause else Icons.TwoTone.PlayArrow,
                    contentDescription = if (state.playing) "暂停" else "播放",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }

            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable { LoomMusicPlayer.stop() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.TwoTone.Close,
                    contentDescription = "关闭播放条",
                    tint = LoomColors.textMuted,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}
