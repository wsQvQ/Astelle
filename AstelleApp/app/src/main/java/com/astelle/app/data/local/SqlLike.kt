package com.astelle.app.data.local

/**
 * 把用户输入变成可以安全塞进 `LIKE` 的模式。
 *
 * 不处理的话，搜 `%` 会匹配全部笔记、搜 `_` 会变成「任意一个字符」——
 * 用户输入的是字面量，不是通配符。
 *
 * 反斜杠必须**第一个**换掉：否则后面补进去的 `\` 会被自己再转义一遍。
 * 配套的 SQL 要写 `ESCAPE '\'`，两者缺一不可。
 */
internal fun escapeLikePattern(raw: String): String = raw
    .replace("\\", "\\\\")
    .replace("%", "\\%")
    .replace("_", "\\_")
