# 11 · 液态玻璃材质调研（Kyant0/AndroidLiquidGlass）＋ 预留接口说明

> 10-11 用户：「提前了解 github 开源的 Kyant0/AndroidLiquidGlass 玻璃效果，
> 设置预留二级菜单的材质切换接口」。本文 = 调研结论 + 本轮已落地的预留件 + 接入路线图。

## 1. 库是什么

- **Kyant0/AndroidLiquidGlass**（文档名 **Backdrop**）：`github.com/Kyant0/AndroidLiquidGlass`
  3095★ · Apache-2.0 · Compose Multiplatform（2.0 起 KMP，含 iOS 示例）
- 玻璃 = **实时背景采样 + AGSL 折射着色器**（边缘透镜、色散、高光、深度阴影全可调）
- **不含高层组件**：只给 `BackdropEffectScope` / `runtimeShaderEffect` 这类画笔，
  按钮/开关/Tab 得自己拼（仓库带 LiquidButton/LiquidToggle/LiquidSlider 示例）

## 2. ⚠️ 接入硬门槛（为什么本轮只留接口不动工）

| 门槛 | 现状 | 差距 |
|---|---|---|
| Kotlin | 我们 2.0.21 | 库 2.0.1 已随 Compose 1.12 / Kotlin 2.3 构建，**metadata 不向下兼容** |
| Compose | BOM 2024.10（1.7.x） | 需 1.12 一带（大版本升级，全 UI 回归） |
| 依赖 | **构建离线** | 缓存里没有它（Maven Central `io.github.kyant0:*`），要联网入库 |
| GPU | 每帧背景采样+折射 shader | 中低端/MIUI 会掉帧；只适合**小控件**（按钮/开关/浮条），别整页玻璃 |
| 系统 | AGSL `RuntimeShader` = **API 33+** | 低版本必须备降级（见下） |

## 3. 值得抄的降级策略（参照 styropyr0/PrismalAGSL，MIT）

三档管线自动选：API 25-30 采样+磨砂层 → 31-32 `RenderEffect` 模糊 → 33+ 全 AGSL 折射。
接入时照这个分层，**老机器永不比现在难看**（产品口径：颜色只表达状态、不穿帮）。

## 4. 本轮已预留的接口（10-11 用户澄清后定稿：**一个开关**）

- `data/settings/SettingsStore.kt`：`glassMenus: Boolean` 持久化开关 ——
  管的是**二级菜单/浮层**（下拉菜单、⋯ 菜单这类）的玻璃质感材质
- 设置页「通用设置 → **玻璃菜单**」开关行（M3 Switch，与动态取色同款）
- ⚠️ 渲染层还没上（下面的版本墙）：**现在拨开关不改变外观是预期行为**，
  玻璃真身接入时 MenuChrome 的浮层容器按此开关分叉材质管线
- ~~材质二级页方案~~ 已按用户意见作废（"只是设置页的一个选项/开关而已"）

## 5. 将来接入路线（供下一场直接开工）

1. Kotlin 2.0→2.3 + Compose 1.7→1.12（独立一轮，纯升级+全回归）
2. 联网入库 `io.github.kyant0:backdrop`（拉进 Gradle 缓存，之后离线可用）
3. 材质管线分叉点 = `MenuChrome` 浮层容器（读 `glassMenus` 开关）：
   先只给**二级菜单/浮层**上玻璃（用户指定的范围），按钮/工具条看效果再议
4. API<33 降级：磨砂（现有 SurfaceFloat 语言）；帧率红线 60fps，掉帧自动回纸感
