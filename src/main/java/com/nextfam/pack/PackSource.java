package com.nextfam.pack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Satu resource pack (ZIP atau folder). Dibaca read-only, path divalidasi (anti path traversal). */
public final class PackSource {
    private static final long MAX_ENTRY = 64L * 1024 * 1024;

    public final String name;
    public final Path path;
    public final boolean zip;
    public volatile String error;
    public volatile boolean scanned;
    private volatile List<String> entries = List.of();
    private volatile Set<String> set = Set.of();
    private final Map<String, String> real = new HashMap<>();
    private ZipFile zf;

    public PackSource(Path p) {
        this.path = p.toAbsolutePath().normalize();
        this.zip = Files.isRegularFile(p);
        String n = p.getFileName().toString();
        this.name = zip && n.toLowerCase(Locale.ROOT).endsWith(".zip") ? n.substring(0, n.length() - 4) : n;
    }

    public static boolean safe(String n) {
        if (n == null || n.isEmpty() || n.length() > 240 || n.startsWith("/") || n.contains("..") || n.contains(":") || n.contains("\\")) return false;
        for (int i = 0; i < n.length(); i++) if (n.charAt(i) < 32) return false;
        return true;
    }

    public List<String> entries() { return entries; }
    public boolean has(String entry) { return set.contains(entry); }

    public void scan() {
        try {
            List<String> raw = new ArrayList<>();
            if (zip) {
                zf = new ZipFile(path.toFile());
                for (var en = zf.entries(); en.hasMoreElements(); ) {
                    ZipEntry e = en.nextElement();
                    if (e.isDirectory()) continue;
                    String n = e.getName().replace('\\', '/');
                    if (!safe(n)) continue;
                    raw.add(n);
                }
            } else {
                try (var st = Files.walk(path)) {
                    st.filter(Files::isRegularFile).filter(f -> !Files.isSymbolicLink(f)).forEach(f -> {
                        String n = path.relativize(f).toString().replace('\\', '/');
                        if (safe(n)) raw.add(n);
                    });
                }
            }
            // zip yang dibungkus satu folder induk: buang prefix
            String prefix = "";
            if (!raw.contains("pack.mcmeta")) {
                for (String n : raw) {
                    int s = n.indexOf('/');
                    if (s > 0 && n.substring(s + 1).equals("pack.mcmeta")) { prefix = n.substring(0, s + 1); break; }
                }
            }
            List<String> list = new ArrayList<>();
            Map<String, String> rm = new HashMap<>();
            for (String n : raw) {
                if (!prefix.isEmpty() && !n.startsWith(prefix)) continue;
                String k = prefix.isEmpty() ? n : n.substring(prefix.length());
                if (k.isEmpty() || !safe(k)) continue;
                list.add(k); rm.put(k, n);
            }
            Collections.sort(list);
            boolean valid = false;
            for (String k : list) if (k.equals("pack.mcmeta") || k.startsWith("assets/")) { valid = true; break; }
            synchronized (real) { real.clear(); real.putAll(rm); }
            entries = Collections.unmodifiableList(list);
            set = new HashSet<>(list);
            error = valid ? null : "Not a valid resource pack (no assets/ or pack.mcmeta)";
        } catch (Throwable t) {
            error = "Cannot read pack: " + t.getClass().getSimpleName();
        }
        scanned = true;
    }

    public synchronized byte[] read(String entry) throws IOException {
        if (!safe(entry) || !set.contains(entry)) throw new IOException("bad entry");
        if (zip) {
            String rn; synchronized (real) { rn = real.get(entry); }
            ZipEntry e = zf.getEntry(rn);
            if (e == null) throw new IOException("missing");
            if (e.getSize() > MAX_ENTRY) throw new IOException("too large");
            try (InputStream in = zf.getInputStream(e)) { return in.readNBytes((int) MAX_ENTRY); }
        }
        Path f = path.resolve(entry).normalize();
        if (!f.startsWith(path)) throw new IOException("outside pack");
        if (Files.size(f) > MAX_ENTRY) throw new IOException("too large");
        return Files.readAllBytes(f);
    }

    public synchronized void close() {
        try { if (zf != null) zf.close(); } catch (IOException ignored) {}
        zf = null;
    }
}
