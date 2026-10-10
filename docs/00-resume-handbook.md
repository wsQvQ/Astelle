# 00 · 恢复工作手册（压缩后第一口吃这份）

> 目的：上下文被压缩/重置后，**5 分钟内**摸清项目、环境、全部的坑，直接进入工作状态。
> 生成：2026-10-10 深夜（`4cf08ca`）。配套：`07-master-plan.md`（总账）、
> `06-work-plan.md`（古代坑册 29 条）、`ui/06~10`（各专项设计稿）。

## 1. 项目 10 秒背景

**Astelle** = Android 纸感 Markdown 笔记（Kotlin 2.0.21 / Compose BOM 2024.10 / Hilt / Room / 单 :app 模块）。
产品口径（**别改坏**）：打开就写 · 颜色只表达状态 · 无 hover · 边界靠描边 · 浅底从 Accent lerp ·
`updatedAt`=内容最后修改（置顶/收藏/移动分类都不许碰）· 置顶只属单篇和文件夹 ·
**软件不改系统里的任何东西**（删图只删 app 副本）。
用户风格：快速迭代、真机验证（「没跑过没看过就别说结论」）、**有歧义必须指出**、诚实报错、
每轮 commit+push（断电教训）、汇报用表格别啰嗦。

## 2. 环境与命令

- 构建：`$env:JAVA_HOME='E:\Android\android-studio-2025.1.3.7-windows\android-studio\jbr'; .\gradlew :app:testDebugUnitTest :app:assembleDebug --offline --console=plain`
  ⚠️ **必须传 `workdir` = `E:\AIstudio\Astelle\AstelleApp`**（漏过两次）
- **构建离线**：禁新依赖；要新库先查 `$env:USERPROFILE\.gradle\caches\modules-2` 有没有货
- 真机：平板 `7b819b63`（USB）/ 手机 `ea71b5ff`；adb = `$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe`
  ⚠️ **换包必须 uninstall + install**（`install -r` 会静默跑旧码）
- git：代理已配（`http.proxy=127.0.0.1:10809`，仓库级）；push 成功标志 = 输出含 `-> main`
- 搜索：**web_search 工具没配 DeepSeek key 用不了**；用 Exa 直连（pwsh）：
  `Invoke-RestMethod -Uri https://api.exa.ai/search -Method Post -Headers @{Authorization="Bearer <key>"}` …
  ⚠️ key 不进 git；建议用户轮换（曾贴在对话里）
- 读写规矩：**read 工具读、edit/write 改**；bash/gpt 系工具不走文件守卫；
  `git revert`/脚本改过的文件会触发 FS_STALE —— 重读再改即可

## 3. ⚠️ 坑总账（古代 29 条见 06 号，这里是**本场新增**的，都是真摔过的）

### 渲染 / 颜色
1. **透明黑渐变穿灰**：动画淡出的透明态必须 `色.copy(alpha = 0f)`，用 `Color.Transparent`
   = 透明**黑**，插值过灰色中间帧（用户看到「闪一下灰的」）
2. **token 桥不许拆**：`Color.kt` 的静态 token 是**三级计算属性**
   （动态覆盖槽 → 明暗双列 → lerp 推导），全 app 一百多处调用点因此零改动换肤；
   别改回 `val` 常量、别手挑十六进制（浅底一律 `lerp(Paper, Accent, x)`）
3. **中文行盒基线偏移**：固定高度胶囊里的小字会「视觉下沉/上浮」——
   带 `TextStyle(lineHeightStyle = CenteredLineHeight)`（`Trim.Both + Center`，定义在 `ui/theme/Type.kt`）
4. 菜单/浮层浓度别猜：`SurfaceFloat = lerp(Paper, Accent, 0.042f)` 是试出来的定稿

### 布局 / 手势
5. **Row 权重挤压**：不加 weight 的胶囊「先量先吃」，窄屏把固定 48dp 的球挤成 0 宽（球消失）——
   给柔性元素 `weight(1f, fill = false)`，固定元素先拿尺寸
6. **MIUI 的 ime inset 收起后不归零**：`imePadding` 一律 `if (imeVisible) Modifier.imePadding() else Modifier`
   （`WindowInsets.isImeVisible` 才可信，还要 `@OptIn(ExperimentalLayoutApi)`）
7. **悬浮工具栏 = Box 覆盖层**：矩形带要透明就让文字真的从下面过；
   「文末余量」垫进**滚动内容**（末尾 Spacer），布局 padding 做不到两者兼得
8. **裸 BasicTextField 外部滚动会「顶飞」**：聚焦时把整个布局当 bringIntoView 矩形滚到底 ——
   光标初始化到 `TextRange(0)`（文首）就是修它；别改回文末
9. **窄带要吃横滑**：工具栏这类窄带给 `detectHorizontalDragGestures { change, _ -> change.consume() }`，
   否则滑它时误触开抽屉；胶囊能滚时它先消费，外层自动退场
10. 弹层「点外面收起」和按钮点击是同一根手指的竞态：再点=收回，用「刚被收起」250ms 闸门挡连击

### 图片 / 渲染管线
11. **Markwon 自愈 = `setText(getText())` 整体重排**：图片 Coil 异步就绪后才重排 ——
    导出抓图前必须等 `AsyncDrawableSpan.drawable.hasResult()` + 2 帧（`MarkdownBody.onImageDrawables` 出口）
12. **compose-markdown 的 fork 烤了 `CoilImagesPlugin`**：`MarkdownText(imageLoader=…)` 即接通；
    本地图片先把相对路径改写成 `file://`（`ImageLinks.resolveForDisplay`，只改显示不落库）
13. `Bitmap.compress` 拒收硬件位图；`GraphicsLayer.toImageBitmap()` 出的是硬件位图 ——
    一律 `bitmap.copy(RGB_565)` 转软件；长图**分片抓**（`MAX_SLICE_PX=2048`）躲 GPU 纹理上限
14. `decorationBox`/子层在**无界高度**里禁用 `fillMaxSize`（炸约束）—— 用 `fillMaxWidth`

### 工程 / 流程
15. **import 前缀陷阱**：`…theme.Ink` 是 `…theme.InkSoft` 的前缀、`Folder` 是 `FolderOpen` 的前缀 ——
    edit 的 old_string 撞前缀会「matched 2 times」，换更长锚
16. **改枚举要同步测试假件**（`Fakes.kt` 的 Fake 仓库补方法），单测会咬人
17. **每轮干完立刻 commit+push**（断电/压缩随时来）；文档是恢复锚，随手更新 07 号
18. uiautomator dump **不含弹层窗口**；判断菜单开没开看截图文件大小差更靠谱
19. 中文别走命令行参数（PowerShell→Python 换码必坏）；dump 的 `\u` 转义别拿中文 grep
20. `runCatching{}.getOrNull()` 别吞根因（猜错过两轮）；`openOutputStream` 要 `"wt"`

### 2026-10-11 补充（10 号计划场）
21. **PowerShell `>` 重定向毁二进制**：`screencap` 一律 `shell screencap -p /sdcard/x.png` + `pull`，
    `exec-out … > file` 出来的 PNG 是坏的
22. **MIUI 合成点击偶发吞点**：`input tap` 不稳时换 `input touchscreen tap X Y`
    或 `input motionevent DOWN` + 80ms + `UP`（真实触摸时序）
23. **uiautomator dump 抓的是「当前活跃窗口」**：弹层开着时 dump 就是弹层那棵小树
    （~4KB vs 全页 ~12KB）——这反而是菜单开没开的**硬证据**（修正古代 18 号的经验）
24. **PredictiveBackHandler 常开会吃掉弹层的返回**（菜单开着按返回直接退页）——
    弹层场景配 `BackHandler(enabled = menuOpen) { … }`，注册晚于它 = dispatcher 优先级更高
25. **读截图认真点**：菜单浮层看漏三轮、白折腾一轮输入排查；
    截图字节量 +20KB 级跳变 = 弹层开了的指纹，先信尺寸差再细看图
26. `animate(a, b, tween(...))` 编译炸别慌——Float 重载第三参是**初速**不是动画规格，
    用具名 `animationSpec =`；`View.drawToBitmap` 在 **`androidx.core.view`**（不是 core.graphics）
27. **预测返回想显示「返回后的页面」= 用快照别用实时组合**（NavBackdrops 栈）：
    导航时 `drawToBitmap` 拍一张压栈、返回弹出，逐帧只是一次 blit；
    逐帧 `shadowElevation` 才是卡顿元凶，已拆
28. navigation-compose 2.8 的 `composable(enterTransition=…)` 要的是**lambda**
    （`AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?`），
    直接传 EnterTransition 值编译炸；返回落地要不闪 = popEnter 只淡入（与快照同像素接管）+ popExit 顺手势方向滑出
29. **12 号终局已换架构**：首页永生 + 子页浮层（`AstelleRoot`/`OverlayLayer`），
    NavHost/快照全退役；刚体常量在 **`Spring.StiffnessXxx`** 对象里（不是顶层）；
    预测返回 1:1 跟手用 `BackEventCompat.touchX`（右缘镜像 `width - touchX`）
30. 手势**取消**时回弹必须丢外层 scope（job 被连坐）；**提交**路径协程活着，
    收尾动画可就地做——两种路径待遇不同，别写反

## 4. 当前状态快照（2026-10-10 深夜，`4cf08ca`）

**已完工**：导出/表格/工具栏 v2/图片管道/悬浮工具栏/P0 文末/常驻侧栏/文件夹置顶/筛选行/
密度开关/图钉星修复/深色全亮（token 桥）/花笺三栏/阅读进度条/设置页 v1（四开关：颜色模式、
动态取色、卡片密度、压缩档位）。121 测全绿。
**在逃 bug**：抽屉误触（等真手复现）· 超长图压测 · 状态高亮归档（`archive/format-highlight-20261008`）。
**待用户设计**：侧栏顶栏（他自己画）。

## 5. 下一步（按序）

1. ~~**设置页 RikkaHub 化 + 预测返回动画**~~ ✅ **2026-10-11 完工**（细案 `ui/10`）：
   LargeTopAppBar 34→18sp 收缩+返回常驻+发丝线、ExpandMore 旋转箭头、M3 Switch、
   整行点击+按压反馈、`PredictiveBackPage`（收缩5%/位移24dp/圆角18dp/退后压暗18%）包三个子页；
   修 bug：菜单开着返回先关菜单。**验收中（用户亲手）**，121 测绿
2. **发布套件**：签名/proguard/1.21.0/README 截图/长图压测（一次性）
3. ⑪ v3（拍照/音频/网址卡片）、讨论 A/C —— 挂起不阻塞

## 6. 协作经验（用户会怎么对你）

- 报告用**表格 + 短句**；每轮结尾给「验收清单 + 装包路径」
- 他给的截图/参考图是**规格**，先复述理解再动手；猜错了一次他会让你「别自己猜，发图给你」
- 涉及审美：拿不准就问（他喜欢拍板），但能自己定的别烦他（「你觉得好看就行」）
- 断电/压缩随时发生：**干完就推**；恢复后先读本手册 → 07 总账 → 相关 ui/ 细案 → 一句「按 X 号计划开工」
