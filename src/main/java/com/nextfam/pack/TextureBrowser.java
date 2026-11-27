package com.nextfam.pack;

import java.util.*;

/** Kategori dan filter tekstur. */
public final class TextureBrowser {
    private TextureBrowser() {}
    public static final String[] CATS = {"All", "Blocks", "Items", "Armor", "Entity", "GUI", "Environment", "Particles", "Font", "Models", "Sounds", "Other"};

    public static String category(String path) {
        if (path.equals("pack.mcmeta") || path.equals("pack.png")) return "Other";
        String[] s = path.split("/");
        if (s.length < 3 || !s[0].equals("assets")) return "Other";
        String sec = s[2];
        if (sec.equals("textures") && s.length >= 4) {
            String t = s[3];
            switch (t) {
                case "block": return "Blocks";
                case "item": return "Items";
                case "entity":
                    if (s.length >= 5 && s[4].equals("equipment")) return "Armor";
                    return "Entity";
                case "models": return s.length >= 5 && s[4].equals("armor") ? "Armor" : "Other";
                case "gui": case "painting": case "mob_effect": return "GUI";
                case "environment": return "Environment";
                case "particle": return "Particles";
                case "font": return "Font";
                default: return "Other";
            }
        }
        if (sec.equals("font")) return "Font";
        if (sec.equals("models") || sec.equals("items") || sec.equals("blockstates") || sec.equals("equipment")) return "Models";
        if (sec.equals("sounds") || path.endsWith("/sounds.json")) return "Sounds";
        return "Other";
    }

    public static String shortName(String path) {
        int i = path.indexOf("textures/");
        if (i >= 0) return path.substring(i + 9);
        String[] s = path.split("/", 3);
        return s.length == 3 && s[0].equals("assets") ? s[2] : path;
    }

    public static List<String> filter(Map<String, List<PackSource>> index, String cat, String query) {
        String q = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
        List<String> out = new ArrayList<>();
        for (String p : index.keySet()) {
            if (!cat.equals("All") && !category(p).equals(cat)) continue;
            if (!q.isEmpty() && !p.toLowerCase(Locale.ROOT).contains(q)) continue;
            out.add(p);
        }
        Collections.sort(out);
        return out;
    }
}
