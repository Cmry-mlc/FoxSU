package com.kuaimaosu.manager.ui.screen.kmroot

/**
 * 98260 引擎支持的机型清单。
 * 来源：Shark 实机验证过的 profile 表（偏移数据），机型↔内核对应关系取自其公开设备表。
 * 引擎在设备端读 uname -r 与这里的完整内核版本串自动精确匹配，无需手动选择机型。
 */
data class SupportedDevice(
    val name: String,      // 机型名 + 代号
    val system: String,     // 系统版本
    val kernels: List<String>, // 支持的完整内核版本串
)

data class DeviceGroup(val brand: String, val devices: List<SupportedDevice>)

object SupportedDevices {
    val GROUPS = listOf(
        DeviceGroup("一加 OnePlus", listOf(
            SupportedDevice("OnePlus 16 (PYB110)", "ColorOS 17", listOf(
                "6.18.21-android17-5-gf957accaf563-ab15926176-4k",
                "6.18.21-android17-5-g7fcbffcad3e1-ab16353812-4k")),
            SupportedDevice("OnePlus 15 (PLK110)", "ColorOS 17", listOf(
                "6.12.69-android16-6-g242e1a07f878-ab15865446-4k",
                "6.12.69-android16-6-g5fd39602326e-ab16372235-4k")),
            SupportedDevice("OnePlus 15T (PLZ110)", "ColorOS 17", listOf(
                "6.12.69-android16-6-g242e1a07f878-ab15865446-4k",
                "6.12.69-android16-6-g5fd39602326e-ab16372235-4k")),
            SupportedDevice("OnePlus Ace7 Pro (PMS110)", "ColorOS 17", listOf(
                "6.12.69-android16-6-g242e1a07f878-ab15865446-4k",
                "6.12.69-android16-6-g5fd39602326e-ab16372235-4k")),
            SupportedDevice("OnePlus Ace7 (PYF110)", "ColorOS 17", listOf(
                "6.12.69-android16-6-g242e1a07f878-ab15865446-4k",
                "6.12.69-android16-6-g5fd39602326e-ab16372235-4k")),
            SupportedDevice("OnePlus Ace6T (PLR110)", "ColorOS 17", listOf(
                "6.12.69-android16-6-g242e1a07f878-ab15865446-4k",
                "6.12.69-android16-6-g5fd39602326e-ab16372235-4k",
                "6.12.38-android16-5-g844001fb8721-ab14552068-4k")),
            SupportedDevice("OnePlus Ace6 Ultra (PMB110)", "ColorOS 17", listOf(
                "6.12.69-android16-6-g242e1a07f878-ab15865446-4k",
                "6.12.69-android16-6-g5fd39602326e-ab16372235-4k")),
            SupportedDevice("OnePlus Ace6 (PLQ110)", "ColorOS 17", listOf(
                "6.6.118-android15-8-g9bc34d5b0c79-abogki537459655-4k",
                "6.6.118-android15-8-gb1422e39c313-abogki552867473-4k")),
            SupportedDevice("OnePlus Ace5 Pro (PKR110)", "ColorOS 17", listOf(
                "6.6.118-android15-8-g9bc34d5b0c79-abogki537459655-4k")),
            SupportedDevice("OnePlus Pad3 Pro (OPD2513)", "ColorOS 16/17", listOf(
                "6.12.69-android16-6-g242e1a07f878-ab15865446-4k",
                "6.12.69-android16-6-g5fd39602326e-ab16372235-4k",
                "6.12.58-android16-6-g7704a1ae279b-ab15213644-4k")),
            SupportedDevice("OnePlus Pad2 Pro (OPD2413)", "ColorOS 17", listOf(
                "6.6.118-android15-8-gc4127a25dcf3-ab15863337-4k")),
        )),
        DeviceGroup("OPPO", listOf(
            SupportedDevice("Find X9 Ultra (PMA110)", "ColorOS 17", listOf(
                "6.12.69-android16-6-g242e1a07f878-ab15865446-4k",
                "6.12.69-android16-6-g5fd39602326e-ab16372235-4k")),
            SupportedDevice("Find X9 Pro (PLG110)", "ColorOS 17", listOf(
                "6.12.69-android16-6-g242e1a07f878-ab15865446-4k",
                "6.12.69-android16-6-g5fd39602326e-ab16372235-4k")),
            SupportedDevice("Find X9 (PLJ110)", "ColorOS 17", listOf(
                "6.12.69-android16-6-g242e1a07f878-ab15865446-4k",
                "6.12.69-android16-6-g5fd39602326e-ab16372235-4k")),
            SupportedDevice("Find X9s Pro (PME110EX)", "ColorOS 17", listOf(
                "6.12.69-android16-6-g242e1a07f878-ab15865446-4k",
                "6.12.69-android16-6-g5fd39602326e-ab16372235-4k")),
            SupportedDevice("Find X10 (PMX110)", "ColorOS 17", listOf(
                "6.12.69-android16-6-g242e1a07f878-ab15865446-4k",
                "6.12.69-android16-6-g5fd39602326e-ab16372235-4k")),
            SupportedDevice("Find X8 Ultra (PKJ110)", "ColorOS 17", listOf(
                "6.6.118-android15-8-g9bc34d5b0c79-abogki537459655-4k")),
            SupportedDevice("Find N5 (PKH110)", "ColorOS 17", listOf(
                "6.6.118-android15-8-gc4127a25dcf3-ab15863337-4k")),
            SupportedDevice("OPPO Pad Mini (OPD2515)", "ColorOS 16/17", listOf(
                "6.12.69-android16-6-g242e1a07f878-ab15865446-4k",
                "6.12.69-android16-6-g5fd39602326e-ab16372235-4k",
                "6.12.58-android16-6-g7704a1ae279b-ab15213644-4k")),
            SupportedDevice("OPPO Pad 5 Pro (OPD2511)", "ColorOS 17", listOf(
                "6.12.69-android16-6-g242e1a07f878-ab15865446-4k",
                "6.12.69-android16-6-g5fd39602326e-ab16372235-4k")),
            SupportedDevice("OPPO Pad4 Pro (OPD2409)", "ColorOS 17", listOf(
                "6.6.118-android15-8-gc4127a25dcf3-ab15863337-4k")),
            SupportedDevice("OPPO K13 Turbo Pro (PLE110)", "ColorOS 17", listOf(
                "6.6.118-android15-8-g9bc34d5b0c79-abogki537459655-4k")),
        )),
        DeviceGroup("realme", listOf(
            SupportedDevice("Realme GT8 Pro (RMX5200)", "ColorOS 17", listOf(
                "6.12.69-android16-6-g242e1a07f878-ab15865446-4k",
                "6.12.69-android16-6-g5fd39602326e-ab16372235-4k")),
            SupportedDevice("Realme GT8 (RMX6699)", "ColorOS 17", listOf(
                "6.6.118-android15-8-g9bc34d5b0c79-abogki537459655-4k")),
            SupportedDevice("Realme Neo8 (RMX8899)", "ColorOS 16/17", listOf(
                "6.12.38-android16-5-g844001fb8721-ab14552068-4k",
                "6.12.69-android16-6-g242e1a07f878-ab15865446-4k",
                "6.12.69-android16-6-g5fd39602326e-ab16372235-4k")),
        )),
        DeviceGroup("vivo / iQOO", listOf(
            SupportedDevice("vivo / iQOO 系列", "OriginOS 6/7", listOf(
                "6.12.58-android16-6-g690108013dcc-abogki576129896-4k",
                "6.12.58-android16-6-g0a092cc0037a-abogki521987229-4k",
                "6.12.58-android16-6-gdd6fd5c98918-abogki543627742-4k",
                "6.12.23-android16-5-g41f4cb1fa7c7-abogki465038634-4k",
                "6.6.127-android15-8-gd489d4ad2a70-abogki567717946-4k")),
        )),
        DeviceGroup("小米 Xiaomi", listOf(
            SupportedDevice("Xiaomi 18 Pro (M610BB)", "HyperOS 4 / Android 17", listOf(
                "6.18.21-android17-5-g1d099fcb35e0-abogki540753930-4k")),
            SupportedDevice("Xiaomi 18 Fold (lhasa)", "HyperOS 4 / Android 17", listOf(
                "6.18.21-android17-5-g805aa6adea19-abogki576585092-4k")),
        )),
    )

    val totalDevices: Int get() = GROUPS.sumOf { it.devices.size }
}
