package com.astelle.app.data.seed

/**
 * 测试期的示例笔记。
 *
 * 固定 ID，走 upsert —— 每次构建都会用这里的最新内容覆盖它，
 * 不会产生重复笔记，方便在真机上随时验收 Markdown 渲染效果。
 * 只在 debug 构建中写入（见 HomeViewModel.seedSampleNote）。
 */
object SampleNote {

    /** 固定 ID：保证反复覆盖同一条，而不是越塞越多 */
    const val ID = "astelle-sample-note"

    const val TITLE = "Markdown 速查 · 示例笔记"

    val CONTENT: String = """
# Astelle 支持 Markdown

这篇既是示例，也是一份速查表。
切到顶栏右上角的「预览」，就能看到它的渲染结果。

## 标题

一个井号是一级标题，两个是二级，最多支持六级。

## 强调

**粗体**用两个星号，*斜体*用一个，~~删除线~~用两个波浪号。
行内代码用反引号包起来，比如 `println("hi")`。

## 列表

- 无序列表用减号
- 第二项
- 第三项

1. 有序列表用数字加点
2. 第二项

## 任务清单

- [ ] 还没做的事
- [x] 已经做完的事

## 引用

> 纸感米色打底，暖橙只用来表达状态。
> —— Astelle 的设计原则之一

## 代码块

```kotlin
@Composable
fun AstelleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content,
    )
}
```

## 表格

| 元素 | 写法 | 说明 |
|---|---|---|
| 标题 | `#` | 最多六级 |
| 粗体 | `**文字**` | 两个星号 |
| 链接 | `[名字](网址)` | 可点击 |

## 链接

[Astelle 的仓库](https://github.com/wsq2024/Astelle)

---

## 一个和标准 Markdown 不同的地方

在正文里按一次回车就会换行，不会被合并成一段。
笔记应用该是这个手感 —— 你写下的换行，就是你想要的换行。
""".trimIndent()
}
