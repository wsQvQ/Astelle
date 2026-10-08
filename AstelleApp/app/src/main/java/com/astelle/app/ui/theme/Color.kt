package com.astelle.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * 纸感品牌色板 —— 与 `docs/ui/01-home-screen.md` §1 的设计 Token 表一一对应，
 * 不跟随系统动态取色。
 *
 * **全项目只此一份，不要在别处再抄一遍。** 之前 HomeScreen.kt 自己重抄了
 * 13 个同名常量，导致调品牌色要改两处、漏一处就两边不一致。
 *
 * ## 这一版怎么来的
 *
 * v1 的问题是「暖到糊」：纸是暖的、墨也是暖的，连强调色的浅底都是暖米色，
 * 全屏同一色相、同一明度区间，眼睛找不到任何一个确定的东西。
 *
 * v2 的三条改动（数字与完整推理见 `docs/ui/05-visual-direction.md`）：
 *
 * 1. **墨改成中性**（纸保持暖）。主文字色度 9.6 → 2.1。暖纸配中性墨才有
 *    印刷感的清爽；暖纸配暖墨就是一锅粥。
 * 2. **强调色加深一档，浅底浓度降下来。** [AccentMist] 从「一块实色」改成
 *    「主色叠 8%」，于是它自动贴着 [Accent] 走。
 * 3. **面收成三层**，分割线要真的看得见 —— v1 的 Divider 与抽屉底只差 0.9
 *    亮度，等于没画。
 *
 * 改完请跑 `python tools/palette-audit.py`。配色靠眼睛吵不出结果。
 */

/* ---------- 面：三层 + 一档发丝线 ---------- */

/** 纸 / 卡片 / 分类容器体 / 编辑器纸。所有可书写、可阅读的面 */
val Paper = Color(0xFFF9F5EF)

/** 按下态、输入框底。比纸深一档，只用来表示「被按住了」 */
val PaperWarm = Color(0xFFF2ECE2)

/** 页面底 / 抽屉底。比纸深一档，卡片靠这层温度差浮起来 */
val DrawerBg = Color(0xFFEDE6DB)

/** 发丝分割线。对抽屉底的亮度差 6.2，看得见 */
val Divider = Color(0xFFDED4C5)

/* ---------- 墨：中性偏暖，色度压到花笺 / granola 的量级 ---------- */

/** 主文字。色度 2.1，和暖纸拉开色相 */
val Ink = Color(0xFF232320)

/** 次文字：卡片标题、次级正文 */
val InkSoft = Color(0xFF44443F)

/** 弱文字：日期、字数这类元信息。**不承载正文** */
val Muted = Color(0xFF7E7C76)

/** 占位符、禁用态。对比度只有 2.1，**不承载任何信息** */
val Ghost = Color(0xFFAFADA6)

/* ---------- 强调：一个橙，三种浓度 ---------- */

/**
 * 主强调。比 v1 的 #D4843A 深一档 —— 用在文字上才有对比度，
 * 也更像「印章/印刷」而不是「荧光笔」。
 */
val Accent = Color(0xFFB4651B)

/**
 * 主强调的浅底 = [Accent] 叠 8% 到 [Paper] 上。
 *
 * **不要手挑这个值。** v1 的 #F7E8D4 就是手挑出来的：色相 80.9、亮度 92.7，
 * 和 PaperWarm 几乎重合，根本不是「橙的浅底」，是「深一点的纸」。
 * 用 alpha 推导的理由是**颜色只有一个来源**：以后调 [Accent]，所有浅底
 * 自动跟着走。（低透明度下 Lab 色相被底色带偏是物理必然，别拿色相当指标。）
 */
val AccentMist = lerp(Paper, Accent, 0.08f)

/**
 * 按压闪光（用户 2026-10-08：「好看一点但比较浅」）：比 [AccentMist] 再暖一档的浅橙。
 *
 * ⚠️ 动画淡出的透明态必须用 `PressGlow.copy(alpha = 0f)`，**不能用 `Color.Transparent`** ——
 * 后者是透明**黑**，颜色插值会穿过灰色中间帧，用户看到的就是「闪一下灰的」（实测翻车点）。
 */
val PressGlow = lerp(Paper, Accent, 0.13f)

/**
 * 分类容器的组头 = [Accent] 叠 7% 到 [Paper] 上，体 = 叠 3%。
 *
 * 同样从 [Accent] 推导（`lerp(paper, accent, a)` 就是「accent 以 alpha a 叠在
 * paper 上」），不手挑十六进制。
 *
 * **头和体必须同一个色相**，只差浓淡。分头是橙、体是另一种白，两块贴在一起
 * 就是「两张皮」，怎么调都不像一个东西。
 *
 * 但**光靠填色救不了「没有边界」**：先前用 10% 的时候，组头的亮度正好和
 * 抽屉底（L*≈91.6）撞上，这块区域压根没被画出来。真正把它立起来的是
 * `Divider` 那圈描边 —— 花笺的 `border border-bamboo/15` 一直是我漏掉的那条。
 */
val FolderHead = lerp(Paper, Accent, 0.07f)
val FolderBody = lerp(Paper, Accent, 0.03f)

/** 按压态的次级强调 */
val AccentSoft = Color(0xFFE8A45C)

/**
 * 浮层：菜单、弹窗。[Accent] 叠 6% 到 [Paper] 上的暖奶油底。
 *
 * 这里曾经是纯白 `#FFFFFF`，理由是「纯白浮在暖纸上，靠温度差建立层次」。
 * 用户看后的原话是「白色还是觉得好丑」——纯白在这套暖纸色板里确实像个异物：
 * 它的温度比纸面还冷，浮层看着像系统控件不像纸。改成暖底后，浮层和纸面
 * 同一个色相，靠略深 + `Divider` 描边 + 投影立边界（和分类容器同一个道理：
 * **边界靠描边，不靠填色撞色**）。
 *
 * 浓度一路试下来的档位，记着别再从头猜：12% 被否（「有点太深」）→
 * 0.08 仍偏重 → 0.06 被夸「非常接近我喜欢的颜色了」→ **0.035**（用户点名再浅一档）。
 * 这个浓度下浮层和纸面差别很小，**边界全靠 `Divider` 描边 + 投影**——
 * 正是分类容器那条「边界靠描边，不靠填色撞色」的路子。要调只改这个系数。
 */
val SurfaceFloat = lerp(Paper, Accent, 0.042f)

/* ---------- 危险 ---------- */

/** 仅用于删除。深一档，用在文字上才读得清 */
val Danger = Color(0xFFA94242)

/** 危险操作的浅底，同 [AccentMist] 的推导方式（Danger 叠 8% 到 Paper） */
val DangerBg = Color(0xFFF6ECEC)

/* ---------- 保存胶囊「已保存」态的文字色 ----------
 * 比 [Accent] 再深一档：它落在 AccentMist 上，需要更高的对比度 */
val SavedText = Color(0xFF8F5212)
