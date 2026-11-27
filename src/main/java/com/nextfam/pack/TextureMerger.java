package com.nextfam.pack;

import java.util.*;

/** Pilihan tekstur (path -> pack sumber). Tidak pernah mengubah pack asli. */
public final class TextureMerger {
    public final Map<String, PackSource> picks = Collections.synchronizedMap(new LinkedHashMap<>());

    public void pick(String path, PackSource src) { picks.put(path, src); }
    public void unpick(String path) { picks.remove(path); }
    public void clear() { picks.clear(); }
    public int count() { return picks.size(); }

    /** Pilih semua file kategori dari satu pack. */
    public int fillFromPack(Map<String, List<PackSource>> index, String cat, PackSource pack) {
        int n = 0;
        for (var e : index.entrySet()) {
            if (!cat.equals("All") && !TextureBrowser.category(e.getKey()).equals(cat)) continue;
            if (e.getValue().contains(pack)) { picks.put(e.getKey(), pack); n++; }
        }
        return n;
    }
}
