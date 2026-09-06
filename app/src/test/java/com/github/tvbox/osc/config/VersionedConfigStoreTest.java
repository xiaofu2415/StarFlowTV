package com.github.tvbox.osc.config;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class VersionedConfigStoreTest {
    @Rule public final TemporaryFolder temporary = new TemporaryFolder();

    private byte[] live(int version) {
        return ("{\"schemaVersion\":1,\"configVersion\":" + version +
                ",\"generatedAt\":\"2026-09-06T03:30:00Z\",\"channels\":[{" +
                "\"id\":\"one\",\"name\":\"One\",\"group\":\"Test\",\"sources\":[{" +
                "\"id\":\"a\",\"url\":\"https://live.example/" + version +
                ".m3u8\",\"priority\":1,\"protocol\":\"https\"}]}]}").getBytes(StandardCharsets.UTF_8);
    }

    @Test public void atomicallyActivatesAndKeepsNewestThreeVersions() throws Exception {
        File root = temporary.newFolder("config");
        for (int version = 1; version <= 4; version++) {
            assertTrue(VersionedConfigStore.activate(root, version, live(version),
                    ("One,https://live.example/" + version + ".m3u8\n").getBytes(StandardCharsets.UTF_8)));
        }
        assertEquals(4, VersionedConfigStore.activeVersion(root));
        assertTrue(VersionedConfigStore.activeLiveTxt(root).getName().equals("live.txt"));
        assertFalse(new File(root, "v-1").exists());
        assertTrue(new File(root, "v-2").isDirectory());
    }

    @Test public void invalidReplacementCannotDamageCurrentConfig() throws Exception {
        File root = temporary.newFolder("rollback");
        assertTrue(VersionedConfigStore.activate(root, 1, live(1), "good\n".getBytes(StandardCharsets.UTF_8)));
        assertFalse(VersionedConfigStore.activate(root, 2, "{}".getBytes(StandardCharsets.UTF_8),
                "bad\n".getBytes(StandardCharsets.UTF_8)));
        assertEquals(1, VersionedConfigStore.activeVersion(root));
    }
}
