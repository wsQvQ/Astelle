#!/usr/bin/env python3
"""
Astelle 色板体检。

配色的问题用眼睛看常常吵不出结果（"我觉得发灰" / "我觉得还好"），
但下面这几件事是能算的：

  1. 文字色度 (Lab C) —— 偏色越重，和暖纸叠在一起越"糊"
  2. 正文对比度 (WCAG) —— 低于 4.5 就是看不清，不是风格问题
  3. 强调色族的色相是否一致 —— 浅底若不是从主色推导出来的，色相会漂
  4. 面与面的亮度差 (dL*) —— 低于 3 基本等于没分层

跑法：
    python tools/palette-audit.py

改色板之后跑一次，别只凭感觉。
"""

import math

# ---------------------------------------------------------------- 色彩数学

def _lin(c: float) -> float:
    c /= 255.0
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def rgb(h: str):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def lab(h: str):
    """返回 (L*, a*, b*, C*)。"""
    r, g, b = [_lin(v) for v in rgb(h)]
    x = 0.4124 * r + 0.3576 * g + 0.1805 * b
    y = 0.2126 * r + 0.7152 * g + 0.0722 * b
    z = 0.0193 * r + 0.1192 * g + 0.9505 * b

    def f(t):
        return t ** (1 / 3) if t > 0.008856 else 7.787 * t + 16 / 116

    fx, fy, fz = f(x / 0.95047), f(y), f(z / 1.08883)
    ll = 116 * fy - 16
    a = 500 * (fx - fy)
    bb = 200 * (fy - fz)
    return ll, a, bb, math.hypot(a, bb)


def hue(h: str) -> float:
    _, a, b, _ = lab(h)
    return math.degrees(math.atan2(b, a)) % 360


def lum(h: str) -> float:
    r, g, b = [_lin(v) for v in rgb(h)]
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def contrast(fg: str, bg: str) -> float:
    a, b = lum(fg), lum(bg)
    return (max(a, b) + 0.05) / (min(a, b) + 0.05)


def over(fg: str, alpha: float, bg: str) -> str:
    """把 fg 以 alpha 叠在 bg 上 —— 浅色底一律这样推导，别定死色值。"""
    f, b = rgb(fg), rgb(bg)
    return "#%02X%02X%02X" % tuple(round(f[i] * alpha + b[i] * (1 - alpha)) for i in range(3))


# ---------------------------------------------------------------- 被检色板

CURRENT = {
    "面": [("Paper 纸/卡片", "#FAF5EF"), ("PaperWarm", "#F3EBE0"),
           ("DrawerBg 抽屉底", "#EAE0D0"), ("Divider 分割", "#E8DDD0"),
           ("SurfaceFloat", "#FFFFFF")],
    "墨": [("Ink 主文字", "#2C2418"), ("InkSoft 次文字", "#4A4034"),
           ("Muted 弱文字", "#9C8B74"), ("Ghost 最弱", "#B8A992")],
    "强调": [("Accent", "#D4843A"), ("AccentMist 浅底", "#F7E8D4"),
             ("SavedText", "#A86428"), ("Danger", "#B54A4A")],
}

PROPOSED = {
    "面": [("Canvas 抽屉/页底", "#EDE6DB"), ("Surface 卡片/纸", "#F9F5EF"),
           ("Float 菜单/弹窗", "#FFFFFF"), ("Hairline 分割", "#DED4C5")],
    "墨": [("Ink 主文字", "#232320"), ("InkSoft 次文字", "#44443F"),
           ("Muted 弱文字", "#7E7C76"), ("Ghost 最弱", "#AFADA6")],
    "强调": [("Accent", "#B4651B"), ("AccentSoft 按压", "#E8A45C"),
             ("AccentWash 选中底", over("#B4651B", 0.08, "#F9F5EF")),
             ("Danger", "#A94242")],
}

# 参考值：花笺（Achilng/floral-notepaper）浅色主题
REF_FLORAL = {
    "面阶梯 dL*": [2.7, 3.4],
    "墨色度": [1.5, 3.3, 5.7, 5.4],
    "强调浅底 / 主色 色度比": 0.148,
}
# 参考值：granola.ai（同属"奶油纸 + 笔记"）
REF_GRANOLA = {"墨色度": [0.0, 5.0, 5.0], "浅底叠加": "accent @ 15% alpha"}

# ---------------------------------------------------------------- 检查项

SURFACE_BG = "#F9F5EF"          # 正文所在的面
MIN_BODY = 4.5                  # WCAG AA 正文
MIN_LARGE = 3.0                 # WCAG AA 大字号 / 非文本
MIN_SURFACE_STEP = 3.0          # 面与面至少差这么多才看得出


def dump(title: str, palette: dict) -> None:
    print(f"\n{title}")
    print("-" * 68)
    for group, items in palette.items():
        for name, h in items:
            ll, _, _, c = lab(h)
            print(f"  {group}  {name:<20}{h:<10} L*={ll:5.1f}  C={c:5.1f}  "
                  f"hue={hue(h):5.1f}")


def check_text(palette: dict) -> None:
    print("\n[1] 文字对比度（正文需 >= 4.5，占位/禁用 >= 3.0）")
    print("-" * 68)
    rules = {
        "Ink 主文字": MIN_BODY,
        "InkSoft 次文字": MIN_BODY,
        "Muted 弱文字": MIN_LARGE,
        "Ghost 最弱": MIN_LARGE,
    }
    for name, h in palette["墨"]:
        key = next((k for k in rules if name.startswith(k.split()[0])), None)
        need = rules.get(key, MIN_LARGE)
        c = contrast(h, SURFACE_BG)
        flag = "OK" if c >= need else "不达标"
        print(f"  {name:<20}对纸 {c:5.2f}   {flag}（需 {need}）")


def check_chroma(palette: dict) -> None:
    print("\n[2] 墨的色度（越接近参考的 0~6 越好；暖纸配冷墨才有对比）")
    print("-" * 68)
    print(f"  花笺参考：{' / '.join(f'{v:.1f}' for v in REF_FLORAL['墨色度'])}")
    print(f"  当前   ：{' / '.join(f'{lab(h)[3]:.1f}' for _, h in CURRENT['墨'])}")
    print(f"  提案   ：{' / '.join(f'{lab(h)[3]:.1f}' for _, h in palette['墨'])}")


def check_accent(palette: dict) -> None:
    print("\n[3] 强调浅底的浓度（花笺的浅底色度只有主色的 14.8%）")
    print("-" * 68)
    main = next(h for n, h in palette["强调"] if n == "Accent")
    mc = lab(main)[3]
    for name, h in palette["强调"]:
        if name == "Accent":
            continue
        c = lab(h)[3]
        print(f"  {name:<20}hue={hue(h):5.1f}  C={c:5.1f}   "
              f"与 Accent 差 {abs(hue(h) - hue(main)):4.1f} 度")
    print(f"\n  Accent 本身 C={mc:.1f}；浅底 C 落在主色的 10~25% 才是「带一点温度」")
    print("  ⚠ 低透明度时 Lab 色相被底色带偏是物理必然，别拿色相差当指标。")
    print("     浅底仍然应该用 `Accent.copy(alpha = 0.06~0.12)` 叠出来 —— 理由是")
    print("     一处改、处处一致，而不是为了让色相更准。")
    print("     反例：手挑的 AccentMist(#F7E8D4) 色相 80.9，和 PaperWarm(82.4) 几乎一样，")
    print("     它根本不是「橙的浅底」，是「深一点的纸」。")


def check_surfaces(palette: dict) -> None:
    print("\n[4] 面的层次（低于 3 等于没分层）")
    print("-" * 68)
    # Hairline 不是「面」，它是画在 Canvas 上的线，所以和 Canvas 比
    pairs = [("Canvas", "#EDE6DB", "Surface", "#F9F5EF"),
             ("Surface", "#F9F5EF", "Float", "#FFFFFF"),
             ("Canvas", "#EDE6DB", "Hairline", "#DED4C5")]
    for n1, h1, n2, h2 in pairs:
        d = abs(lab(h1)[0] - lab(h2)[0])
        flag = "OK" if d >= MIN_SURFACE_STEP else "分不开"
        print(f"  {n1:<10}-> {n2:<10}dL*={d:5.1f}   {flag}")
    print("\n  当前色板里 Divider(#E8DDD0) 与 DrawerBg(#EAE0D0) 只差 dL*=0.9，")
    print("  在抽屉底上画分割线等于没画。")


def main() -> None:
    dump("当前色板", CURRENT)
    dump("提案色板", PROPOSED)
    check_text(PROPOSED)
    check_chroma(PROPOSED)
    check_accent(PROPOSED)
    check_surfaces(PROPOSED)

    print("\n[5] 分类组头该多浓（花笺用 accent 叠 8%）")
    print("-" * 68)
    accent = "#B4651B"
    print(f"  现在：AccentMist 实色 #F7E8D4（C={lab('#F7E8D4')[3]:.1f}）≈ accent 叠 75%")
    for a in (0.06, 0.08, 0.12, 0.20):
        blended = over(accent, a, SURFACE_BG)
        print(f"  提案：accent 叠 {int(a * 100):>2}%  ->  {blended}  C={lab(blended)[3]:5.1f}")
    print("  → 组头用 6~8%，肉眼刚好觉得「这块带一点温度」，而不是「一块橙色」")


if __name__ == "__main__":
    main()
