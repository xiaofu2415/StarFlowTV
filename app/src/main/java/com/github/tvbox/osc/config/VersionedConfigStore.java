package com.github.tvbox.osc.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class VersionedConfigStore {
    private static final String ACTIVE = "active-version";

    private VersionedConfigStore() {
    }

    public static synchronized boolean activate(File root, int version, byte[] liveJson, byte[] liveTxt) {
        File staging = new File(root, ".staging-" + version);
        try {
            ClientLiveConfig parsed = ClientLiveConfig.parseAndValidate(new String(liveJson, StandardCharsets.UTF_8));
            if (!parsed.valid || parsed.configVersion != version || liveTxt == null || liveTxt.length == 0) return false;
            if (!root.isDirectory() && !root.mkdirs()) return false;
            deleteTree(staging);
            if (!staging.mkdir()) return false;
            write(new File(staging, "live.json"), liveJson);
            write(new File(staging, "live.txt"), liveTxt);
            File target = new File(root, "v-" + version);
            if (target.exists() || !staging.renameTo(target)) return false;
            File marker = new File(root, ACTIVE + ".next");
            write(marker, (version + "\n").getBytes(StandardCharsets.US_ASCII));
            File active = new File(root, ACTIVE);
            File backup = new File(root, ACTIVE + ".previous");
            if (backup.exists() && !backup.delete()) return false;
            if (active.exists() && !active.renameTo(backup)) return false;
            if (!marker.renameTo(active)) {
                if (backup.exists()) backup.renameTo(active);
                deleteTree(target);
                return false;
            }
            if (backup.exists()) backup.delete();
            prune(root, 3);
            return true;
        } catch (Exception ignored) {
            deleteTree(staging);
            return false;
        }
    }

    public static int activeVersion(File root) {
        File marker = new File(root, ACTIVE);
        try (FileInputStream input = new FileInputStream(marker)) {
            byte[] bytes = new byte[(int) marker.length()];
            int offset = 0;
            while (offset < bytes.length) {
                int read = input.read(bytes, offset, bytes.length - offset);
                if (read < 0) break;
                offset += read;
            }
            String value = new String(bytes, 0, offset, StandardCharsets.US_ASCII).trim();
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return 0;
        }
    }

    public static File activeLiveTxt(File root) {
        int version = activeVersion(root);
        return version <= 0 ? null : new File(new File(root, "v-" + version), "live.txt");
    }

    private static void write(File file, byte[] content) throws Exception {
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(content);
            output.getFD().sync();
        }
    }

    private static void prune(File root, int keep) {
        File[] entries = root.listFiles(file -> file.isDirectory() && file.getName().matches("v-[0-9]+"));
        if (entries == null || entries.length <= keep) return;
        List<File> versions = new ArrayList<>();
        Collections.addAll(versions, entries);
        versions.sort(Comparator.comparingInt(VersionedConfigStore::version));
        for (int i = 0; i < versions.size() - keep; i++) deleteTree(versions.get(i));
    }

    private static int version(File file) {
        try { return Integer.parseInt(file.getName().substring(2)); } catch (Exception ignored) { return 0; }
    }

    private static void deleteTree(File file) {
        if (file == null || !file.exists()) return;
        File[] children = file.listFiles();
        if (children != null) for (File child : children) deleteTree(child);
        file.delete();
    }
}
