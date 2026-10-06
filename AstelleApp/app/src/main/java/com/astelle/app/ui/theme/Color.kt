package com.astelle.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 纸感品牌色板 —— 与 `docs/ui/01-home-screen.md` §1 的设计 Token 表一一对应，
 * 不跟随系统动态取色。
 *
 * **全项目只此一份，不要在别处再抄一遍。** 之前 HomeScreen.kt 自己重抄了
 * 13 个同名常量，导致调品牌色要改两处、漏一处就两边不一致。
 */
val Paper = Color(0xFFFAF5EF)
val PaperWarm = Color(0xFFF3EBE0)

/** 抽屉底色：比编辑器纸底深一档，两者要能拉开 */
val DrawerBg = Color(0xFFEAE0D0)

val Divider = Color(0xFFE8DDD0)
val Ink = Color(0xFF2C2418)
val InkSoft = Color(0xFF4A4034)
val Muted = Color(0xFF9C8B74)
val Ghost = Color(0xFFB8A992)
val Accent = Color(0xFFD4843A)
val AccentMist = Color(0xFFF7E8D4)
val SurfaceFloat = Color(0xFFFFFFFF)

/** 保存胶囊「已保存」态的文字色 */
val SavedText = Color(0xFFA86428)

val DangerBg = Color(0xFFFBEAEA)
val Danger = Color(0xFFB54A4A)
