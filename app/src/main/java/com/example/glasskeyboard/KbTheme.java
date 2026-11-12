package com.example.glasskeyboard;

import android.graphics.Color;

/** Colour palettes for the glass keyboard. */
public final class KbTheme {
    public final String name;
    public final int bgTop, bgMid, bgBottom;      // vertical gradient
    public final int blobA, blobB, blobC;         // soft light blobs
    public final int keyTint;                     // base tint of glass keys (RGB, alpha added in view)
    public final int text;
    public final int accent;                      // enter key / highlights
    public final boolean light;

    private KbTheme(String name, int top, int mid, int bottom, int a, int b, int c,
                    int keyTint, int text, int accent, boolean light) {
        this.name = name;
        this.bgTop = top; this.bgMid = mid; this.bgBottom = bottom;
        this.blobA = a; this.blobB = b; this.blobC = c;
        this.keyTint = keyTint; this.text = text; this.accent = accent;
        this.light = light;
    }

    public static final KbTheme[] ALL = new KbTheme[] {
        // 0 - matches the reference: violet glass over a dark base
        new KbTheme("Aurora",
            Color.parseColor("#7A3FB8"), Color.parseColor("#2C2140"), Color.parseColor("#17131F"),
            Color.parseColor("#D77CFF"), Color.parseColor("#8E3FE0"), Color.parseColor("#F4E9FF"),
            Color.WHITE, Color.WHITE, Color.parseColor("#B65CFF"), false),
        new KbTheme("Ocean",
            Color.parseColor("#1B7FA8"), Color.parseColor("#12304A"), Color.parseColor("#0A1624"),
            Color.parseColor("#4FD1FF"), Color.parseColor("#2A6BFF"), Color.parseColor("#C9F3FF"),
            Color.WHITE, Color.WHITE, Color.parseColor("#2FB6FF"), false),
        new KbTheme("Sunset",
            Color.parseColor("#D8506B"), Color.parseColor("#4A2243"), Color.parseColor("#1D1224"),
            Color.parseColor("#FF9A5C"), Color.parseColor("#FF4F8B"), Color.parseColor("#FFE3C9"),
            Color.WHITE, Color.WHITE, Color.parseColor("#FF6F7D"), false),
        new KbTheme("Emerald",
            Color.parseColor("#1E9E76"), Color.parseColor("#123B34"), Color.parseColor("#0A1816"),
            Color.parseColor("#5CF0B8"), Color.parseColor("#18B58A"), Color.parseColor("#D2FFEF"),
            Color.WHITE, Color.WHITE, Color.parseColor("#2EE0A0"), false),
        new KbTheme("Midnight",
            Color.parseColor("#2B2B33"), Color.parseColor("#17171C"), Color.parseColor("#0C0C10"),
            Color.parseColor("#6B6B80"), Color.parseColor("#4A4A5C"), Color.parseColor("#B8B8C8"),
            Color.WHITE, Color.WHITE, Color.parseColor("#8A8AFF"), false),
        new KbTheme("Frost",
            Color.parseColor("#E6DDFB"), Color.parseColor("#D9DFF7"), Color.parseColor("#CBD3EA"),
            Color.parseColor("#FFFFFF"), Color.parseColor("#B79CFF"), Color.parseColor("#8FD3FF"),
            Color.parseColor("#FFFFFF"), Color.parseColor("#1D1B2A"), Color.parseColor("#7A4DFF"), true),
    };

    public static KbTheme get(int i) {
        return ALL[Math.max(0, Math.min(ALL.length - 1, i))];
    }
}
