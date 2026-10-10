# 10 · 设置页 RikkaHub 化 + 预测返回动画（计划稿，未实现）

> 用户 2026-10-10：「设置页可以直接抄袭 rikkahub 的设置 UI 风格，包括设置标题的动态处理、
> 每个板块/组件/按钮的 UI、返回按钮常驻（比如那个 ∨ 换个好看的 UI）；
> 大幅优化预测返回手势的动画，参考 rikkahub」。
> 研究结论来自 Exa 检索（Compose 官方 app-bars 文档 + Predictive Back 两篇专文）；
> RikkaHub 本体是 Material3 标准配方，下面的方案与其同源、且**零新依赖**（离线构建安全）。

## 0. 本场已顺手修掉的（不需要计划）
- ✅ 状态栏底色割裂：背景色挪到 inset **外面**（inset padding 区露父级底色的锅）
- ✅ 设置页右上「侧栏」钮：删除（设置页里没有侧栏的事）

## 1. 设置页 UI 升级（照 RikkaHub 气质）

### 1a. 「设置」大标题的动态处理（上滑收缩）
- `LargeTopAppBar` + `TopAppBarDefaults.exitUntilCollapsedScrollBehavior(scrollState)`：
  上滑时大标题「设置」34sp 平滑收缩为小标题（18sp）钉在顶栏，返回钮**常驻不滚走**
- 底色透明（页面 `paperWarm` 透上来），收缩后才落一层 `paper` + 发丝线 —— 层次像 RikkaHub
- 标题字也套 `CenteredLineHeight`（别再犯中文基线的病）

### 1b. 组件升级
- **下拉胶囊**：`∨` 字符换 `Icons.Outlined.ExpandMore`（箭头图标，点开时旋转 180°，150ms 缓动）
- **开关**：`开/关` 文字胶囊换 **M3 `Switch`**（单色 thumb/track，用 Accent/AccentMist 上色）
- **板块小标**：13sp Medium `Accent` 不变，行距略收（24→20dp）
- **卡片行**：圆角 16 不变；按压整行淡入 `PaperWarm`（现在只有部分行有点击反馈）

### 1c. 返回常驻
- 返回钮进 `TopAppBar` 的 `navigationIcon` 槽（和 1a 一起天然常驻）；物理返回/手势返回照旧

## 2. 预测返回动画（Predictive Back）

现状：manifest `enableOnBackInvokedCallback=true` 已开，但**动画是系统的默认缩放**（预览窗）。
升级路径（Compose 1.7 的 `PredictiveBackHandler`，零依赖）：

```kotlin
PredictiveBackHandler(enabled = true) { progress: Flow<BackEventCompat> ->
    // 跟手阶段：收集 progress → 内容层 graphicsLayer
    //   scaleX/Y = 1 - progress * 0.05f（轻微收缩，RikkaHub 那种「内容让路」感）
    //   translationX = progress * 24.dp（跟手右移一点点）
    //   cornerRadius 可选：progress * 18.dp（圆角化，像 iOS）
    progress.collect { /* 更新一个 mutableFloatStateOf */ }
    // 完成：popBackStack / finish
}
```
- 页面级套一层 `graphicsLayer` 跟手；取消时 `animate` 回原位（150ms）
- 首页不做（没得退）；设置/日记/计划三个子页套上
- ⚠️ 系统手势导航模式下才吃得到 progress（三键导航=直接退，无动画——正常）

## 3. 实施顺序（新场次/压缩后跑，约一晚）
1. 设置页 1a（大标题收缩）+ 1c（返回常驻）—— 一次 TopAppBar 手术
2. 1b 组件升级（箭头/开关/按压反馈）
3. PredictiveBackHandler 包三个子页 + 手感调参（收缩 5%、位移 24dp、圆角 18dp 起步）
4. 真机：上滑标题收缩 ✓ 返回钮常驻 ✓ 三键/手势两种导航 ✓ 暗色/动态取色下不穿帮 ✓
