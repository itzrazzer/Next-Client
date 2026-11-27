package com.nextfam.pack;

import net.minecraft.client.MinecraftClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Menulis pack gabungan ke ZIP (di thread terpisah) dan menerapkannya ke Minecraft. */
public final class PackExporter {
    private PackExporter() {}
    public static volatile String status = "";
    public static volatile boolean running;

    public static String sanitize(String n) {
        String s = n == null ? "" : n.trim().replaceAll("[^A-Za-z0-9 _.\\-]", "");
        if (s.isEmpty()) s = "NextFam Custom Pack";
        return s.length() > 60 ? s.substring(0, 60) : s;
    }

    public static void export(String name, Map<String, PackSource> picks, boolean apply) {
        if (running) { status = "Export already running..."; return; }
        if (picks.isEmpty()) { status = "Nothing selected: pick textures or press MERGE first"; return; }
        final Map<String, PackSource> snap;
        synchronized (picks) { snap = new LinkedHashMap<>(picks); }
        final String clean = sanitize(name);
        running = true; status = "Exporting " + snap.size() + " files...";
        Thread t = new Thread(() -> {
            Path tmp = null;
            try {
                Path dir = PackManager.packsDir(); Files.createDirectories(dir);
                Path target = dir.resolve(clean + ".zip"); int k = 1;
                while (Files.exists(target)) target = dir.resolve(clean + "-" + (k++) + ".zip");
                tmp = dir.resolve(target.getFileName() + ".tmp");
                int done = 0, skipped = 0;
                Set<String> written = new HashSet<>();
                try (ZipOutputStream z = new ZipOutputStream(Files.newOutputStream(tmp))) {
                    put(z, "pack.mcmeta", mcmeta(clean).getBytes(StandardCharsets.UTF_8));
                    written.add("pack.mcmeta");
                    PackSource iconSrc = null;
                    for (var e : snap.entrySet()) {
                        String p = e.getKey(); PackSource src = e.getValue();
                        if (!PackSource.safe(p) || p.equals("pack.mcmeta") || !written.add(p)) continue;
                        try {
                            put(z, p, src.read(p)); done++;
                            String mm = p + ".mcmeta";
                            if (src.has(mm) && PackSource.safe(mm) && written.add(mm)) put(z, mm, src.read(mm));
                            if (iconSrc == null && src.has("pack.png")) iconSrc = src;
                        } catch (Throwable ex) { skipped++; }
                        if ((done & 63) == 0) status = "Exporting... " + done + "/" + snap.size();
                    }
                    if (iconSrc != null && written.add("pack.png")) { try { put(z, "pack.png", iconSrc.read("pack.png")); } catch (Throwable ignored) {} }
                }
                try { Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE); }
                catch (Exception ex) { Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING); }
                final Path fin = target;
                status = "Saved " + fin.getFileName() + " (" + done + " files" + (skipped > 0 ? ", " + skipped + " skipped" : "") + ")";
                if (apply) MinecraftClient.getInstance().execute(() -> apply(fin.getFileName().toString()));
            } catch (Throwable ex) {
                status = "Export failed: " + ex.getClass().getSimpleName() + (ex.getMessage() == null ? "" : " - " + ex.getMessage());
                try { if (tmp != null) Files.deleteIfExists(tmp); } catch (Exception ignored) {}
            }
            running = false;
        }, "NextFam-PackExport");
        t.setDaemon(true); t.start();
    }

    private static void put(ZipOutputStream z, String name, byte[] data) throws java.io.IOException {
        z.putNextEntry(new ZipEntry(name)); z.write(data); z.closeEntry();
    }

    private static String mcmeta(String name) {
        String desc = (name + " - merged with NextFam Pack Studio").replace("\\", "").replace("\"", "'");
        return "{\n  \"pack\": {\n    \"description\": \"" + desc + "\",\n    \"pack_format\": 75,\n    \"min_format\": 75,\n    \"max_format\": 75,\n"
             + "    \"supported_formats\": { \"min_inclusive\": 75, \"max_inclusive\": 75 }\n  }\n}\n";
    }

    /** Mengaktifkan pack di Minecraft dan reload sekali (dijalankan di main thread). */
    public static void apply(String fileName) {
        try {
            var mc = MinecraftClient.getInstance();
            var mgr = mc.getResourcePackManager();
            mgr.scanPacks();
            String id = "file/" + fileName;
            List<String> en = new ArrayList<>(mgr.getEnabledIds());
            en.remove(id); en.add(id);                     // paling akhir = prioritas tertinggi
            mgr.setEnabledProfiles(en);
            mc.options.refreshResourcePacks(mgr);
            status = "Applied " + fileName + " - reloading resources...";
            mc.reloadResources();
        } catch (Throwable t) {
            status = "Saved, but could not apply automatically (" + t.getClass().getSimpleName() + "). Enable it in Options > Resource Packs.";
        }
    }
}
