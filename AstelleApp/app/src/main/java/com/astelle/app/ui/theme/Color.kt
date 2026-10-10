package com.astelle.app.ui.theme

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * 纸感品牌色板 —— 与 `docs/ui/01-home-screen.md` §1 的设计 Token 表一一对应，
 * 不跟随系统动态取色（除非设置里**显式**打开「动态取色」，见 [setDynamicPalette]）。
 *
 * **全项目只此一份，不要在别处再抄一遍。**
 *
 * ## 三级取值顺序（10-10 莫奈接口）
 *
 * 1. **动态覆盖槽**（Monet / Material You）—— 只在设置里开了才非空
 * 2. **明暗双列** —— 跟着 [setPaletteDark] 的开关
 * 3. 推导类浅底从当前 [Paper] + [Accent] 现算，三级自动跟随
 *
 * 这样：**现有色板 = 默认品牌色，永不被覆盖**；动态取色是可关的客人。
 */

/* ==================== 桥：动态覆盖 + 明暗开关 ==================== */

private val paletteDark = mutableStateOf(false)

internal fun setPaletteDark(dark: Boolean) {
    paletteDark.value = dark
}

/**
 * 动态取色槽（莫奈）。null = 关（默认）= 品牌色。
 * `AstelleTheme` 在设置开了「动态取色」且 API 31+ 时塞入映射后的色板。
 */
private val dynamicOverride = mutableStateOf<AstelleColors?>(null)

internal fun setDynamicPalette(colors: AstelleColors?) {
    dynamicOverride.value = colors
}

/* ---------- 面：三层 + 一档发丝线 ---------- */

/** 纸 / 卡片 / 分类容器体 / 编辑器纸。所有可书写、可阅读的面 */
val Paper: Color
    get() = dynamicOverride.value?.paper
        ?: if (paletteDark.value) Color(0xFF201C17) else Color(0xFFF9F5EF)

/** 按下态、输入框底。比纸深一档，只用来表示「被按住了」 */
val PaperWarm: Color
    get() = dynamicOverride.value?.paperWarm
        ?: if (paletteDark.value) Color(0xFF191510) else Color(0xFFF2ECE2)

/** 页面底 / 抽屉底。比纸深一档，卡片靠这层温度差浮起来 */
val DrawerBg: Color
    get() = dynamicOverride.value?.let { lerp(it.paper, Color(0xFF000000), 0.055f) }
        ?: if (paletteDark.value) Color(0xFF14100C) else Color(0xFFEDE6DB)

/** 发丝分割线。对抽屉底的亮度差 6.2，看得见 */
val Divider: Color
    get() = dynamicOverride.value?.divider
        ?: if (paletteDark.value) Color(0xFF3B342A) else Color(0xFFDED4C5)

/* ---------- 墨：中性偏暖，色度压到花笺 / granola 的量级 ---------- */

/** 主文字。色度 2.1，和暖纸拉开色相 */
val Ink: Color
    get() = dynamicOverride.value?.ink
        ?: if (paletteDark.value) Color(0xFFF4EDE2) else Color(0xFF232320)

/** 次文字：卡片标题、次级正文 */
val InkSoft: Color
    get() = dynamicOverride.value?.inkSoft
        ?: if (paletteDark.value) Color(0xFFDAD1C3) else Color(0xFF44443F)

/** 弱文字：日期、字数这类元信息。**不承载正文** */
val Muted: Color
    get() = dynamicOverride.value?.muted
        ?: if (paletteDark.value) Color(0xFFA2988A) else Color(0xFF7E7C76)

/** 占位符、禁用态。对比度只有 2.1，**不承载任何信息** */
val Ghost: Color
    get() = dynamicOverride.value?.ghost
        ?: if (paletteDark.value) Color(0xFF776E62) else Color(0xFFAFADA6)

/* ---------- 强调：一个橙，三种浓度 ---------- */

/**
 * 主强调。比 v1 的 #D4843A 深一档 —— 用在文字上才有对比度，
 * 也更像「印章/印刷」而不是「荧光笔」。暗底上提亮一档保证可读。
 */
val Accent: Color
    get() = dynamicOverride.value?.accent
        ?: if (paletteDark.value) Color(0xFFE0913F) else Color(0xFFB4651B)

/**
 * 主强调的浅底 = [Accent] 叠 8% 到 [Paper] 上。**不要手挑这个值** ——
 * 用 alpha 推导的理由是**颜色只有一个来源**：以后调 [Accent]（或动态取色换了它），
 * 所有浅底自动跟着走。
 */
val AccentMist: Color
    get() = dynamicOverride.value?.accentMist ?: lerp(Paper, Accent, 0.08f)

/**
 * 按压闪光：比 [AccentMist] 再暖一档的浅橙。
 *
 * ⚠️ 动画淡出的透明态必须用 `PressGlow.copy(alpha = 0f)`，**不能用 `Color.Transparent`** ——
 * 后者是透明**黑**，颜色插值会穿过灰色中间帧（实测翻车点）。
 */
val PressGlow: Color
    get() = lerp(Paper, Accent, 0.13f)

/**
 * 分类容器的组头 = [Accent] 叠 7% 到 [Paper] 上，体 = 叠 3%。
 * **头和体必须同一个色相**，只差浓淡；边界靠 `Divider` 描边，不靠填色撞色。
 */
val FolderHead: Color
    get() = lerp(Paper, Accent, 0.07f)
val FolderBody: Color
    get() = lerp(Paper, Accent, 0.03f)

/** 按压态的次级强调 */
val AccentSoft: Color
    get() = dynamicOverride.value?.let { lerp(it.accent, Color(0xFFFFFFFF), 0.35f) }
        ?: if (paletteDark.value) Color(0xFFEDB06A) else Color(0xFFE8A45C)

/**
 * 浮层：菜单、弹窗。[Accent] 叠 4.2% 到 [Paper] 上的暖奶油底。
 * 浓度档位记着别再猜：12% 被否 → 0.08 偏重 → 0.06 被夸 → **0.035**（用户点名再浅一档）→ 0.042 定稿。
 * **边界全靠 `Divider` 描边 + 投影**（边界靠描边，不靠填色撞色）。
 */
val SurfaceFloat: Color
    get() = dynamicOverride.value?.surfaceFloat ?: lerp(Paper, Accent, 0.042f)

/* ---------- 危险 ---------- */

/** 仅用于删除。深一档，用在文字上才读得清 */
val Danger: Color
    get() = dynamicOverride.value?.danger
        ?: if (paletteDark.value) Color(0xFFE08080) else Color(0xFFA94242)

/** 危险操作的浅底，同 [AccentMist] 的推导方式（Danger 叠 8% 到 Paper） */
val DangerBg: Color
    get() = dynamicOverride.value?.dangerBg
        ?: if (paletteDark.value) Color(0xFF3A2422) else Color(0xFFF6ECEC)

/* ---------- 保存胶囊「已保存」态的文字色 ----------
 * 比 [Accent] 再深一档：它落在 AccentMist 上，需要更高的对比度 */
val SavedText: Color
    get() = dynamicOverride.value?.let { lerp(it.accent, Color(0xFF000000), 0.25f) }
        ?: if (paletteDark.value) Color(0xFFD9A05B) else Color(0xFF8F5212)
