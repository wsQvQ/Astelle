# Astelle

> 打开就能写字的 Android 笔记应用。纸感米色 + 暖橙强调，不跟随系统动态取色。

---

## 现状

**开发中** · 最近一次提交 `2026-09-30` · 可编译（`assembleDebug` BUILD SUCCESSFUL）

| 模块 | 状态 |
|---|---|
| 首页编辑器 | ✅ 完成 |
| 侧边栏抽屉 | ✅ 完成 |
| 日记页 | ⬜ 占位 |
| 计划页 | ⬜ 占位 |
| 设置页 | ⬜ 占位 |

完整缺口清单（含优先级）见 **[docs/04-dev-gaps.md](docs/04-dev-gaps.md)**。

### 已实现

- **打开即写**：冷启动直接进编辑器，无首页列表
- **自动保存**：标题/正文变更去抖 500ms 落库，保存胶囊三态（未保存 / 保存中 / 已保存）
- **侧边栏抽屉**：约 4/5 屏宽，搜索、筛选（全部 / 置顶 / 收藏）、笔记卡片
- **笔记 CRUD**：新建、置顶、收藏、删除（带二次确认）、长按菜单
- **系统适配**：状态栏 / 导航栏 / 输入法避让，预测式返回（抽屉开着时先关抽屉）

### 已知缺口（P0）

- **撤销 / 重做**：顶栏按钮目前是死的，未接编辑历史栈
- **Markdown 预览**：目前是纯文本渲染（依赖 `compose-markdown` 已声明但未使用）
- **导入 Markdown**：菜单项为空壳
- **分类 / 文件夹**：仅对话框外壳，无数据表

---

## 技术栈

| | |
|---|---|
| 语言 | Kotlin 2.0.21 |
| UI | Jetpack Compose · Material 3（BOM 2024.10.00） |
| 架构 | MVVM / UDF，单模块 `:app` |
| 依赖注入 | Hilt 2.52（KSP） |
| 数据库 | Room 2.6.1（`astelle.db`，version 3） |
| 导航 | Navigation Compose 2.8.4 |
| 构建 | AGP 8.13.2 · Gradle 8.13 · KSP |

---

## 构建

需要 **JDK 17+**（Android Studio 自带的 JBR 即可）和 **Android SDK**（`compileSdk 36`）。

```bash
cd AstelleApp
./gradlew :app:assembleDebug
```

产物：`AstelleApp/app/build/outputs/apk/debug/app-debug.apk`

> `local.properties` 中的 `sdk.dir` 需指向你本机的 Android SDK，该文件不入版本库。

---

## ⚠️ 数据库迁移警告

`DatabaseModule` 使用了 `fallbackToDestructiveMigration()`，且数据库当前为 `version = 3`。

**任何版本号提升都会清空本地全部笔记。** 在实现「分类 / 文件夹」（需要升至 v4）之前，务必先完成备份或导出功能。

---

## 设计资料

视觉以可点原型为准，代码实现应与之一致。

| 资源 | 说明 |
|---|---|
| [`index.html`](index.html) | **可点样式原型**（3 套首页方案 + 侧边栏）· 权威视觉 |
| [docs/ui/01-home-screen.md](docs/ui/01-home-screen.md) | 首页 + 抽屉完整规格（设计 Token、动效、摆位） |
| [docs/ui/02-plans-page-discussion.md](docs/ui/02-plans-page-discussion.md) | 计划页讨论（含 SVG） |
| [docs/ui/03-diary-page-discussion.md](docs/ui/03-diary-page-discussion.md) | 日记页讨论（含 SVG） |

---

## 目录结构

```
Astelle/
├── AstelleApp/              # Android 工程
│   └── app/src/main/java/com/astelle/app/
│       ├── data/            # Room 实体 / DAO / Repository 实现
│       ├── domain/          # 模型 + Repository 接口
│       ├── di/              # Hilt 模块
│       └── ui/              # home / plans / diary / settings / theme
├── docs/                    # 设计规格与开发缺口
├── index.html               # 可点原型
└── 图标/                    # 应用图标素材
```
