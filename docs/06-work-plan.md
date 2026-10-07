# 06 · 施工计划与注意事项

> 这份是**工作交接单**：待办、做法、踩过的坑。上下文压缩后照着它继续。
> 另一份是设计推理：[05-visual-direction.md](ui/05-visual-direction.md)。

---

## 0. 当前状态

| | |
|---|---|
| 版本 | **1.20.1**（`versionCode = 12001`） |
| 分支 | `main`，与 `https://github.com/wsQvQ/Astelle.git` 同步 |
| 测试 | **65 个单测全绿** |
| 设备 | 平板 `7b819b63`（旧手机是 `ea71b5ff`，已不用） |
| 远程 | `origin` 指向 `wsQvQ/Astelle`（用户名从 `wsq2024` 改过） |
| 推送 | 需要 xray 代理：repo 级 `http.proxy = http://127.0.0.1:10809` |

最近提交（旧→新）：
`b130117` 时间语义 → `b8b7153` 卡片间距 → `6774089` 版本号 → `7e126fb` + `713db1e` 菜单重做

---

## 1. 待办

### ✅ 已完成（这一轮）

| | 内容 | 提交 |
|---|---|---|
| ① | **时间语义**：`updatedAt` = 内容最后修改时间，元数据操作一律不碰它。查了六处（`togglePinned`/`toggleFavorite`/`moveToFolder`/`detachNotes`/`seedSampleNote`） | `b130117` |
| ⑥ | 卡片三行间距收紧（5→3、6→3、行内上下 11→9） | `b8b7153` |
| ⑦ | 二级菜单重做：纸白 + `Divider` 描边 + 圆角 14dp + 去 leadingIcon + 危险项单独隔一组 | `7e126fb` `713db1e` |
| ⑤ | **导出 Markdown + 1440px 图片**，`⋯` 菜单加「导出」二级；76 测试全绿，真机端到端（SAF 保存）验过 | `421fd97` |
| ⑧ | 菜单再调：**图标加回来**（用户：「有点单调」）、底色纯白→暖奶油（`SurfaceFloat`）、横向留白 12→24dp（用户：「左右太窄」）、菜单零件收进 `MenuChrome.kt` | 本轮 |

### 🔨 待做（按这个顺序）

**⑤ 导出 Markdown + 图片（1440px）** — ✅ 已完成，真机验过
- `MarkdownExport.toMarkdown(title, content)` + `Note.toMarkdown()`：`# 标题\n\n正文`，标题空则不写标题行；
  `baseName()` 出文件名（非法字符→下划线、清首尾点空格、洗完纯符号退回「笔记」）。14 条单测，含与 `MarkdownImport` 的**回环**
- SAF `CreateDocument("text/markdown")` / `CreateDocument("image/png")` → `ExportFileWriter` 落盘
  - ⚠️ `openOutputStream` 必须用 **`"wt"`**：默认 `"w"` 覆盖同名文件时不截断，短文本后面会拖上一版的尾巴
- 图片：`ExportImageCanvas` 屏幕外画布，**固定 1440px 宽**（写死 `Density(2f)` + 720dp，输出和设备无关）；
  标题 26sp SemiBold + 发丝线 + `MarkdownBody`（与编辑器预览同一份渲染配置，抽成了共用文件）
- 入口：`⋯` 菜单「导出」→ 二级（Markdown 文件 / 图片），沿用「移动到分类」那招：同一个 `DropdownMenu` 换内容
- ⚠️ 导出是**读操作**：全程只读内存 state，绝不落库、绝不碰 `updatedAt`

**④ 文件夹支持置顶**
- `folders` 表加 `isPinned` 列 → **DB version 4→5**
- ⚠️ `fallbackToDestructiveMigration()` 还在，升版本会清空数据。用户已确认**测试阶段无所谓**，
  但**发布前必须换真 `Migration`**
- 长按文件夹菜单加「置顶」；排序 `isPinned DESC, sortOrder ASC`
- 规则（用户已确认）：置顶的文件夹**整屏最前**（在「全部」视图和「文件夹」视图都一样）

**② 图钉/星标记 + 修「根本不显示」的 bug**
- 用户说收藏星**根本不显示**。静态链路查过是通的（SQL `isFavorite` → `NoteSummaryRow` → 卡片
  `if (note.isFavorite)`），所以要么是那个 12dp 图标画不出来，要么收藏没落库。**平板真机复现**
- 图钉/星都放**卡片内部**（用户确认），简约为主
- ⚠️ 置顶/收藏**不该改 `updatedAt`**（①已修），所以标记变化不会让卡片跳位

**③ 文件夹视图 +「📁 新建」按钮**
- 筛选行从「全部/置顶/收藏」改成 **「全部 / 文件夹 / 收藏」**
- 「文件夹」视图（用户定的方案）：**只显示文件夹标题行、不显示文章**；点某个文件夹 → 展开它的文章
- 「置顶」chip 删掉（用户认为没必要）
- 右上角 ＋ 改成：**文件夹图标 +「新建」两字，无底色**（用户给了图）
  ⚠️ 它是**新建文件夹**，不是新建笔记。要和顶栏的 ✎ 区分开
- 侧栏「新建笔记」缺位：现在只能关抽屉点顶栏 ✎

**⑨ 预览模式能复制文字**（用户：「预览模式下可以复制就好了，这个可以计划一下」）
- ⚠️ 歧义待确认：**复制整篇还是选中片段？** 复制的是**渲染后的纯文本**还是 **Markdown 源码**？
  默认按「整篇 + 渲染后纯文本」起步（最小可用），选中片段是后续增强
- 现状：`MarkdownText` 是 AndroidView 包的 TextView，Compose 的 `SelectionContainer` **选不中它**
- 两条路：
  - a. 让底层 TextView 可选（`setTextIsSelectable(true)`）→ 原生选中手柄 + 复制；
    要先确认 compose-markdown 有没有开这个口子，没有就得换库或自定义 factory
  - b. 预览模式加「复制」动作（`⋯` 菜单或顶栏），把正文放进剪贴板 —— 简单可靠，先做这个

---

## 2. 三个待讨论的点

### A. 二级菜单「太淡、突兀、位置奇怪」

三个可能，建议从 B 试起：

| | 做法 |
|---|---|
| A | 底色 `#FFFFFF` → `Paper #F9F5EF`（更纸） |
| **B** | **底色不动，投影加深**（`0 4 16 rgba(0,0,0,.10)`）。"淡"多半是**缺投影**不是缺颜色 |
| C | 描边加深一档（`Divider` → `Muted` 30%） |

**位置奇怪**：现在 `DropdownMenu` 锚在卡片外层 `Box` 的左上角，不是长按时的手指位置。
花笺是自绘 `Popup` + `useViewportPopupPosition`，菜单出现在**触点**上。
→ 建议改成按触点定位（`DropdownMenu` 的 `offset` 参数，或换 `Popup` + `PopupPositionProvider`）。

### B. 能不能预留接口接安卓莫奈（Material You 动态取色）？

**能，而且不用重构底层。** 原因：`ui/theme/Color.kt` 已经是**唯一色板**，
全项目只从它取色（这是之前踩过坑之后定下的规矩）。

要做的事很集中，基本只改 `Theme.kt`：
1. `AstelleTheme(darkTheme, dynamicColor = false, content)` 加个开关
2. 开着时用 `androidx.compose.material3.dynamicLightColorScheme(context)` / `dynamicDarkColorScheme(context)`
   生成 `ColorScheme`，再把 Monet 的角色映射到 `AstelleColors`：
   `primary→Accent`、`surface→Paper`、`onSurface→Ink`、`outline→Ghost`、`error→Danger`…
3. `PaperWarm` / `Divider` / `SavedText` / `DangerBg` 这几个 Monet 没有直接对应的，
   从上面几个角色派生（`lerp` 即可）

**三个要注意的**：
- ⚠️ **莫奈只在 Android 12+（API 31）生效**，我们 minSdk 26 —— 26~30 必须有回退（用品牌色板）
- ⚠️ **设计稿明确写了「不跟随系统动态取色」**。开莫奈等于放弃品牌识别度，
  所以它应该是**设置页里的一个开关**，默认关
- ⚠️ `AstelleIcons` 把 `SolidColor(Muted)` 之类**烤进了矢量路径**，但 `Icon(tint = ...)` 会覆盖，
  所以不会出问题；只要别用不带 tint 的 `Icon(...)` 就行

**工作量**：一晚。难度**低**，因为颜色已经收口了。

### C. 卡片选中的色条 + 折叠动画
已完成（600ms 展开、250ms 折叠），但用户没实机验收过。

---

## 3. ⚠️ 注意事项（踩过的坑，别重蹈）

1. **换包必须 `adb uninstall` + `install`**，别只 `install -r`。
   `install -r` 之后 `am force-stop` 有时不生效，截图会是**旧包**的界面 —— 我为此误判过两次。
2. **临时验证代码一定要撤干净再装**。我用过 `DrawerValue.Open`、临时种子数据去截图，
   忘了装回正式包，用户拿到「一打开就是侧边栏」的测试包。撤完必须 `grep TEMP-VERIFY` 确认。
3. **不要用 PowerShell 的 `Get-Content`/`Set-Content` 改源文件** —— 会毁掉 UTF-8，中文全乱。
   改代码一律用 edit 工具。PowerShell 只用来跑 gradle / adb / git。
4. **没跑过、没看过，就别说结论。** 我有一次说「渲染出来了，看下来 G 最稳」，
   其实脚本根本没跑，结论是想象的。后来真跑了一遍碰巧一致，但那是运气。
5. **配色别靠感觉吵**，跑 `python tools/palette-audit.py`（对比度 / 墨的色度 / 浅底浓度 / 面的层次差）。
   我在配色上猜错四版，用户连着说「越来越丑」。
6. **`updatedAt` = 内容最后修改时间**。置顶、收藏、移动分类、删分类，全都不许碰它。
   碰了它笔记会凭空跳到最前，看着像被置顶了。
7. **`fallbackToDestructiveMigration()` 还在**：数据库版本号一提升，本地笔记全没。
   测试阶段用户认可，**发布前必须换真 `Migration`**。
8. **长按是「藏起来」的入口**，必须有触觉反馈（`HapticFeedbackType.LongPress`）。
9. **M3 的 `DropdownMenu` 在 material3 1.3 有 `border` 参数**，不用自绘 Popup。
10. **PowerShell 跑 git 推送**：成功标志是输出里出现 `-> main`（`git push origin main` 显示
    `4525aca..421fd97  main -> main`，裸 `git push` 才显示 `HEAD -> main`）。别只匹配 `HEAD`，
    错误信息里也有它；也别只匹配 `HEAD -> main`，用 `git push origin main` 时看不到这串。
    推送失败多半是 xray 没起来，等代理回来重试。
11. **`read_image` 前文件得先存在**：`present` 工具用绝对路径，工作目录下的临时 PNG
    偶尔会被清掉，生成完立刻 present。
12. **用户说话有歧义要指出**，他明确要求过（自认提示词写得不好）。
    这一轮靠这条避免了一次返工（「文件夹里的文章没有置顶选项」我最初理解错）。
13. **验证流程**：改完 → `gradlew :app:testDebugUnitTest :app:assembleDebug` →
    `adb uninstall` + `install` → `am force-stop` + `am start` → `screencap` → 检查 logcat 无 FATAL。
    黑屏（<40KB）就重截，重试 3 次。
14. **构建命令**：
    ```
    $env:JAVA_HOME='E:\Android\android-studio-2025.1.3.7-windows\android-studio\jbr'
    .\gradlew :app:testDebugUnitTest :app:assembleDebug --offline --console=plain
    ```
    adb 在 `$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe`。
    跑 gradle / adb 的 PowerShell 任务**必须显式传 `workdir`**，否则落在会话目录里找不到 gradlew。
15. **`GraphicsLayer.toImageBitmap()` 是 suspend，而且要求 `record` 先发生**（库文档明说）。
    挂载画布后**等两帧**再抓：第一帧排版+绘制（record），第二帧时它才落定；只等一帧抓到的是上一帧甚至空图。
    另外 `GraphicsLayer.record {}` 是 **`DrawScope` 扩展**（得写在 `drawWithContent {}` 里），
    `Layout` 在 `androidx.compose.ui.layout`，都不在 foundation 里。
16. **屏幕外画布不能给孩子任何尺寸约束**。普通 Box / `requiredSize` 会把孩子压成宿主的尺寸，
    导出图高度直接塌掉（真机实测 **1440×2px**）。`OffscreenCanvasHost` 用自定义 `Layout`
    以**无界约束**量孩子、自身报 1×1px（不报 0×0 —— 零面积节点会不会被跳过绘制是实现细节，赌不起），
    再 `clipToBounds` 裁住。宽度对了不代表高度对了，看图要两个维度都看。
17. **导出图片 = 固定 1440px**，靠写死 `Density(2f)` + 720dp 实现，设备密度与系统字体缩放都不参与
    （同一份笔记在任何设备上导出的图一模一样）。改宽度只改 `EXPORT_PAGE_WIDTH`。
    正文标题 26sp 是量过的：正文 32px，页标题 52px，正文里的 `# 一级标题`（Markwon 2em≈64px）仍略高 —— 
    不必追它，那只是个别笔记里才有的结构。
18. **`GraphicsLayer.toImageBitmap()` 吐出来的是 HARDWARE 配置位图**（compose 内部
    `Bitmap.createBitmap(Picture)` 造的），而 `Bitmap.compress()` 要锁像素，硬件位图锁不了、直接抛异常
    —— 真机上「md 导得出、图导不出」就是栽在这。`ExportFileWriter.softwareCopy()` 先拷成 ARGB_8888 再压。
    这条查了很久：**md 能写不代表 PNG 能压**，两条路只共享 `openOutputStream` 那一段。
19. **菜单样式别再从头猜，看用户给的档位**：底色 `SurfaceFloat = lerp(Paper, Accent, x)` 的 x 试过
    12%（太深）→ 0.08 → 0.06（「非常接近」）→ **0.035（现值）**；横向留白 24dp；图标**要**（用户说纯文字「单调」，
    把 `713db1e` 那次「去 leadingIcon」的结论推翻了）。零件统一走 `ui/components/MenuChrome.kt`。
20. **真机 UI 驱动**：`adb shell input tap/swipe/keyevent` 现在**可用**（之前那次 INJECT_EVENTS 是锁屏下的误判）。
    找控件别手算坐标 —— `uiautomator dump` 拿 `bounds`，或用脚本按中心点点。
    ⚠️ **中文别走命令行参数**：PowerShell → Python 一路换码，匹配必然失败；用 `\u` 转义输出。
    平板息屏时 Compose 不出帧（`withFrameNanos` 会一直等），先 `input keyevent KEYCODE_WAKEUP` + 上滑解锁。

---

## 4. 产品口径（别改坏了）

- **打开就能写**：冷启动直接进编辑器，无首页列表
- **颜色只表达状态**，不做装饰；工具栏图标一律安静
- **手机没有 hover**：hover 一律改写为 press / selected
- 分类组的边界**靠描边**，不靠填色（填色解决不了「没有边界」）
- 强调色的浅底**从主色用 alpha 推导**（`lerp(Paper, Accent, x)`），不手挑十六进制
- **语义**：`updatedAt` = 内容最后修改时间；`isPinned` 只作用于**单个文章 + 文件夹**，
  **文件夹里的文章没有置顶选项**（用户明确的）
