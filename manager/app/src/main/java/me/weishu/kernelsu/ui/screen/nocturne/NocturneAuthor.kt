package me.weishu.kernelsu.ui.screen.nocturne

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.R

/** Nocturne 作者页：返回 + acard r24 渐变描边 + 头像虚线环 + 引言 + stat3 + GitHub 按钮。 */
@Composable
fun NocturneAuthorScreen(onBack: () -> Unit, onOpenUrl: (String) -> Unit) {
    val t = ncTokens()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = NcDimens.pageX,
            end = NcDimens.pageX,
            top = ncStatusTop() + 20.dp,
            bottom = 40.dp,
        ),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NcRoundButton(iconRes = R.drawable.ic_d_back, onClick = onBack)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.author_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = t.text1,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
        item {
            // acard：外层 1dp 渐变描边 + 内层 r23
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(NcRadius.hero))
                    .background(
                        Brush.linearGradient(
                            listOf(t.violet.copy(alpha = 0.55f), t.mint.copy(alpha = 0.35f), t.violet.copy(alpha = 0.55f))
                        )
                    )
                    .padding(1.dp)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(NcRadius.hero - 1.dp))
                        .background(t.surface1)
                        .padding(18.dp)
                ) {
                    // 左下光晕
                    Box(
                        Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = (-40).dp, y = 40.dp)
                            .size(170.dp)
                            .background(
                                Brush.radialGradient(listOf(t.violet.copy(alpha = 0.14f), Color.Transparent))
                            )
                    )
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // 66dp 头像（首字母 + violet 渐变）+ 外虚线环
                            Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                                Canvas(Modifier.size(78.dp)) {
                                    drawCircle(
                                        color = t.violet.copy(alpha = 0.55f),
                                        radius = size.minDimension / 2 - 1.dp.toPx(),
                                        style = Stroke(
                                            width = 1.2.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
                                        ),
                                    )
                                }
                                Box(
                                    Modifier
                                        .size(NcDimens.authorAvatar)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(t.violet, t.violetDeep))),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "F",
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                    )
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.author_name),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = t.text1,
                                )
                                Text(
                                    text = stringResource(R.string.author_role),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp,
                                    color = t.violetText,
                                    modifier = Modifier.padding(top = 5.dp),
                                )
                                Text(
                                    text = "v${BuildConfig.VERSION_NAME}",
                                    fontSize = 9.5.sp,
                                    color = t.text3,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                        // 引言块：左 2dp violet 竖线
                        Row(
                            Modifier
                                .padding(top = 16.dp)
                                .height(IntrinsicSize.Min)
                        ) {
                            Box(
                                Modifier
                                    .width(2.dp)
                                    .fillMaxHeight()
                                    .background(t.violet.copy(alpha = 0.55f))
                            )
                            Text(
                                text = stringResource(R.string.author_desc),
                                fontSize = 10.5.sp,
                                lineHeight = 18.sp,
                                color = t.text2,
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        // stat3 三列数据条
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(NcRadius.card))
                                .background(t.surface2)
                                .border(1.dp, t.line, RoundedCornerShape(NcRadius.card))
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            NcStat("项目", "FoxSu", t.violet, Modifier.weight(1f))
                            Box(Modifier.width(1.dp).height(28.dp).background(t.line))
                            NcStat("内核", "KernelSU", t.mint, Modifier.weight(1f))
                            Box(Modifier.width(1.dp).height(28.dp).background(t.line))
                            NcStat("版本", BuildConfig.VERSION_NAME, t.text1, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
        item {
            // GitHub 按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(NcRadius.panel))
                    .background(t.surface1)
                    .border(1.dp, t.line, RoundedCornerShape(NcRadius.panel))
                    .clickable { onOpenUrl("https://github.com/Cmry-mlc/FoxSu") }
                    .padding(horizontal = 15.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NcIconBox(iconRes = R.drawable.ic_d_link, accent = t.violet)
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.author_project),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = t.text1,
                    )
                    Text(
                        text = stringResource(R.string.author_project_value),
                        fontSize = 10.sp,
                        color = t.text2,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                NcChevron()
            }
        }
        item { Spacer(Modifier.height(10.dp)) }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(NcRadius.panel))
                    .background(t.surface1)
                    .border(1.dp, t.line, RoundedCornerShape(NcRadius.panel))
                    .clickable { onOpenUrl("https://github.com/tiann/KernelSU") }
                    .padding(horizontal = 15.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NcIconBox(iconRes = R.drawable.ic_d_book, accent = t.mint)
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.author_upstream),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = t.text1,
                    )
                    Text(
                        text = stringResource(R.string.author_upstream_value),
                        fontSize = 10.sp,
                        color = t.text2,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                NcChevron()
            }
        }
    }
}

@Composable
private fun NcStat(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            maxLines = 1,
        )
        Text(
            text = label,
            fontSize = 9.sp,
            color = t_text3(),
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun t_text3(): Color = ncTokens().text3
