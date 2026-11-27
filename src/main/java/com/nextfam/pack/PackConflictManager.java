package com.nextfam.pack;

import java.util.*;

/** Mendeteksi file yang ada di lebih dari satu pack dan memilih berdasarkan prioritas. */
public final class PackConflictManager {
    private PackConflictManager() {}

    public static boolean isConflict(List<PackSource> l) { return l != null && l.size() > 1; }

    public static int countConflicts(Map<String, List<PackSource>> index) {
        int n = 0;
        for (var l : index.values()) if (l.size() > 1) n++;
        return n;
    }

    /** Untuk semua file (dalam kategori) yang belum dipilih: pakai pack dengan prioritas tertinggi. */
    public static int autoResolve(Map<String, List<PackSource>> index, TextureMerger m, String cat, boolean onlyConflicts) {
        int n = 0;
        for (var e : index.entrySet()) {
            if (!cat.equals("All") && !TextureBrowser.category(e.getKey()).equals(cat)) continue;
            if (onlyConflicts && e.getValue().size() < 2) continue;
            if (m.picks.containsKey(e.getKey())) continue;
            m.pick(e.getKey(), e.getValue().get(0));       // list sudah urut prioritas
            n++;
        }
        return n;
    }
}
