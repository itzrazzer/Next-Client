package com.nextfam.pack;

import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/** Daftar pack, status aktif, urutan prioritas (indeks 0 = prioritas tertinggi) dan index gabungan path. */
public final class PackManager {
    public final List<PackSource> packs = new CopyOnWriteArrayList<>();
    public final Set<PackSource> active = Collections.synchronizedSet(new HashSet<>());
    public volatile Map<String, List<PackSource>> index = Map.of();
    public volatile int indexVersion = 0;
    public volatile String status = "";
    public volatile boolean busy;

    public static Path packsDir() { return FabricLoader.getInstance().getGameDir().resolve("resourcepacks"); }

    /** Membaca semua zip / folder di .minecraft/resourcepacks (di thread terpisah). */
    public void scanFolder() {
        if (busy) return;
        busy = true; status = "Scanning resourcepacks folder...";
        Thread t = new Thread(() -> {
            try {
                Files.createDirectories(packsDir());
                try (var st = Files.list(packsDir())) {
                    st.sorted().forEach(p -> {
                        String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                        boolean ok = Files.isDirectory(p) || n.endsWith(".zip");
                        if (!ok || n.startsWith("nextfam_merged_")) { if (!ok) return; }
                        Path abs = p.toAbsolutePath().normalize();
                        for (PackSource ps : packs) if (ps.path.equals(abs)) return;
                        PackSource ps = new PackSource(p);
                        ps.scan();
                        packs.add(ps);
                    });
                }
                status = packs.size() + " pack(s) found";
            } catch (Throwable e) { status = "Scan failed: " + e.getMessage(); }
            busy = false;
            rebuild();
        }, "NextFam-PackScan");
        t.setDaemon(true); t.start();
    }

    /** Import dari path ZIP / folder (disalin dari clipboard). */
    public String importPath(String raw) {
        if (raw == null) return "Clipboard is empty";
        String s = raw.trim();
        if (s.length() > 1 && (s.startsWith("\"") && s.endsWith("\""))) s = s.substring(1, s.length() - 1);
        if (s.isEmpty()) return "Clipboard is empty";
        final Path p;
        try { p = Paths.get(s).toAbsolutePath().normalize(); } catch (Exception e) { return "Clipboard does not contain a valid path"; }
        if (!Files.exists(p)) return "File not found: " + p.getFileName();
        if (!Files.isDirectory(p) && !p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip")) return "Only .zip files or pack folders are supported";
        for (PackSource ps : packs) if (ps.path.equals(p)) return "Already imported: " + ps.name;
        Thread t = new Thread(() -> {
            PackSource ps = new PackSource(p);
            ps.scan();
            packs.add(ps);
            if (ps.error == null) active.add(ps);
            status = ps.error == null ? "Imported " + ps.name + " (" + ps.entries().size() + " files)" : ps.name + ": " + ps.error;
            rebuild();
        }, "NextFam-PackImport");
        t.setDaemon(true); t.start();
        return "Importing " + p.getFileName() + "...";
    }

    public void toggle(PackSource p) { if (!active.remove(p)) { if (p.error == null) active.add(p); } rebuild(); }

    public synchronized void move(int i, int dir) {
        int j = i + dir;
        if (i < 0 || j < 0 || i >= packs.size() || j >= packs.size()) return;
        PackSource a = packs.get(i), b = packs.get(j);
        packs.set(i, b); packs.set(j, a);
        rebuild();
    }

    /** Membangun index path -> daftar pack (urut prioritas), di thread terpisah. */
    public void rebuild() {
        List<PackSource> order = new ArrayList<>();
        for (PackSource p : packs) if (active.contains(p) && p.error == null) order.add(p);
        Thread t = new Thread(() -> {
            Map<String, List<PackSource>> m = new HashMap<>();
            for (PackSource p : order) for (String e : p.entries()) m.computeIfAbsent(e, k -> new ArrayList<>(2)).add(p);
            index = m; indexVersion++;
        }, "NextFam-PackIndex");
        t.setDaemon(true); t.start();
    }

    public void closeAll() { for (PackSource p : packs) p.close(); }
}
