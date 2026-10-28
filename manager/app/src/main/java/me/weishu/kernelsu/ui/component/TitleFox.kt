package me.weishu.kernelsu.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R

/**
 * 标题栏统一狐狸徽章：所有皮肤共用同一规格 ——
 * 圆形背景 46dp + 全局彩色狐狸 40dp，仅圆底颜色随皮肤微调。
 */
@Composable
fun FoxTitleBadge(
    modifier: Modifier = Modifier,
    circleSize: Dp = 46.dp,
    foxSize: Dp = 40.dp,
    circleColor: Color,
    border: Color? = null,
    shadowElevation: Dp = 0.dp,
    shadowColor: Color = Color(0x38000000),
    showDot: Boolean = false,
    dotColor: Color = Color(0xFF6EF08A),
    dotBorder: Color = Color.White,
) {
    Box(modifier = modifier.size(circleSize)) {
        Box(
            modifier = Modifier
                .size(circleSize)
                .then(
                    if (shadowElevation > 0.dp) {
                        Modifier.shadow(
                            shadowElevation,
                            CircleShape,
                            ambientColor = shadowColor,
                            spotColor = shadowColor,
                        )
                    } else Modifier
                )
                .clip(CircleShape)
                .background(circleColor)
                .then(
                    if (border != null) {
                        Modifier.border(1.dp, border, CircleShape)
                    } else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_fox_global),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(foxSize),
            )
        }
        if (showDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(dotColor)
                    .border(2.dp, dotBorder, CircleShape),
            )
        }
    }
}
