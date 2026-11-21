package com.kuaimaosu.manager.ui.screen.kuaimao

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.CheckCircle
import androidx.compose.material.icons.twotone.Devices
import androidx.compose.material.icons.twotone.ExpandLess
import androidx.compose.material.icons.twotone.ExpandMore
import androidx.compose.material.icons.twotone.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kuaimaosu.manager.ui.theme.KuaiMaoColors
import com.kuaimaosu.manager.ui.theme.KuaiMaoTones

/**
 * 「支持机型」二级菜单（底部弹层）。
 * 按品牌分组列出所有已验证机型与对应内核版本，可搜索；当前设备内核会自动高亮并标注是否匹配。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportedDevicesSheet(
    currentKernel: String,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val q = query.trim()

    // 当前内核是否命中了清单里的某个版本串
    val matchedKernel = remember(currentKernel) {
        if (currentKernel.isBlank()) null
        else SupportedDevices.GROUPS
            .flatMap { it.devices }
            .flatMap { it.kernels }
            .firstOrNull { it == currentKernel }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            // 标题
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(KuaiMaoTones.violet.container),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.TwoTone.Devices, null, tint = KuaiMaoTones.violet.content, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("支持机型", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = KuaiMaoColors.text)
                    Text(
                        "共 ${SupportedDevices.totalDevices} 款 · 引擎按内核版本自动精确匹配",
                        fontSize = 11.sp,
                        color = KuaiMaoColors.textMuted,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // 当前内核匹配状态
            if (currentKernel.isNotBlank()) {
                val ok = matchedKernel != null
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (ok) KuaiMaoTones.green.container else KuaiMaoTones.amber.container)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (ok) Icons.TwoTone.CheckCircle else Icons.TwoTone.Devices,
                        null,
                        tint = if (ok) KuaiMaoTones.green.content else KuaiMaoTones.amber.content,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (ok) "当前内核：已匹配支持清单" else "当前内核：不在精确清单内",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = KuaiMaoColors.textBody,
                        )
                        Text(
                            currentKernel,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = KuaiMaoColors.textMuted,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // 搜索框
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("搜索机型 / 代号 / 内核", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.TwoTone.Search, null, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = KuaiMaoColors.brand,
                    unfocusedBorderColor = KuaiMaoColors.line,
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            )

            Spacer(Modifier.height(10.dp))

            // 分组机型列表（item 之间留间距，避免高亮卡片贴在一起）
            LazyColumn(
                modifier = Modifier.fillMaxWidth().height(420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SupportedDevices.GROUPS.forEach { group ->
                    val devices = group.devices.filter { d ->
                        q.isBlank() || d.name.contains(q, true) ||
                            d.system.contains(q, true) || d.kernels.any { it.contains(q, true) }
                    }
                    if (devices.isEmpty()) return@forEach

                    item {
                        Text(
                            group.brand,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = KuaiMaoColors.brand,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                    devices.forEach { device ->
                        item { DeviceRow(device, currentKernel) }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun DeviceRow(device: SupportedDevice, currentKernel: String) {
    var expanded by remember { mutableStateOf(false) }
    val hasMatch = device.kernels.any { it == currentKernel }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (hasMatch) KuaiMaoTones.green.container.copy(alpha = 0.35f) else Color.Transparent)
            .clickable { expanded = !expanded }
            .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(device.name, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = KuaiMaoColors.textBody)
                Spacer(Modifier.height(2.dp))
                Text(device.system, fontSize = 10.sp, color = KuaiMaoColors.textMuted)
            }
            if (hasMatch) {
                Icon(
                    Icons.TwoTone.CheckCircle, "当前设备",
                    tint = KuaiMaoTones.green.content,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
            }
            Icon(
                if (expanded) Icons.TwoTone.ExpandLess else Icons.TwoTone.ExpandMore,
                null,
                tint = KuaiMaoColors.textMuted,
                modifier = Modifier.size(18.dp),
            )
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            device.kernels.forEach { k ->
                val isCur = k == currentKernel
                Text(
                    k,
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (isCur) KuaiMaoTones.green.content else KuaiMaoColors.textMuted,
                    modifier = Modifier.padding(start = 4.dp).padding(vertical = 2.dp),
                )
            }
        }
    }
}
