package me.weishu.kernelsu.ui.component.material

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.ui.component.freshCardColor
import me.weishu.kernelsu.ui.component.rememberCardContentColor
import me.weishu.kernelsu.ui.component.rememberCardSurface
import me.weishu.kernelsu.ui.theme.isFreshStyle

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TonalCard(
    modifier: Modifier = Modifier,
    containerColor: Color = rememberCardSurface(
        if (isFreshStyle()) freshCardColor() else MaterialTheme.colorScheme.surfaceBright
    ),
    contentColor: Color = rememberCardContentColor(contentColorFor(containerColor)),
    shape: Shape = if (isFreshStyle()) RoundedCornerShape(22.dp) else MaterialTheme.shapes.large,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit,
) {
    val colors = CardDefaults.cardColors(
        containerColor = containerColor,
        contentColor = contentColor,
    )
    when {
        onLongClick != null -> Card(
            modifier = modifier
                .clip(shape)
                .combinedClickable(
                    enabled = enabled,
                    onClick = onClick ?: {},
                    onLongClick = onLongClick,
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                ),
            colors = colors,
            shape = shape,
        ) { content() }

        onClick != null -> Card(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = colors,
            shape = shape,
            interactionSource = interactionSource,
        ) { content() }

        else -> Card(
            modifier = modifier,
            colors = colors,
            shape = shape,
        ) { content() }
    }
}
