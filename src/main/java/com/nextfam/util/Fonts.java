package com.nextfam.util;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.util.HashMap;
import java.util.Map;

/** Font GUI NextFam (Poppins). Text di-cache agar tidak membuat objek baru tiap frame. */
public final class Fonts {
    private Fonts() {}
    private static final Style MED = Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("nextfam", "gui")));
    private static final Style BOLD = Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("nextfam", "gui_bold")));
    private static final Map<String, Text> CM = new HashMap<>(), CB = new HashMap<>();

    public static Text m(String s) {
        Text t = CM.get(s);
        if (t == null) { if (CM.size() > 4000) CM.clear(); t = Text.literal(s).setStyle(MED); CM.put(s, t); }
        return t;
    }
    public static Text b(String s) {
        Text t = CB.get(s);
        if (t == null) { if (CB.size() > 2000) CB.clear(); t = Text.literal(s).setStyle(BOLD); CB.put(s, t); }
        return t;
    }
    public static int w(TextRenderer tr, String s) { return tr.getWidth(m(s)); }
    public static int wb(TextRenderer tr, String s) { return tr.getWidth(b(s)); }
    public static String trim(TextRenderer tr, String s, int max) {
        if (w(tr, s) <= max) return s;
        String e = "...";
        int n = s.length();
        while (n > 0 && w(tr, s.substring(0, n) + e) > max) n--;
        return s.substring(0, Math.max(0, n)) + e;
    }
}
