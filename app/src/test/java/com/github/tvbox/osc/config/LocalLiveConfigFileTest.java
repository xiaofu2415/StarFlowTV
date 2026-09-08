package com.github.tvbox.osc.config;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class LocalLiveConfigFileTest {
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void resolvesInternalFileUrlToTheActualFile() throws Exception {
        File live = temporary.newFile("live.txt");
        File resolved = LocalLiveConfigFile.resolve(live.toURI().toString());
        assertEquals(live.getAbsolutePath(), resolved.getAbsolutePath());
    }

    @Test
    public void doesNotTreatRemoteUrlAsLocalFile() {
        assertNull(LocalLiveConfigFile.resolve(
                "https://config.yuying.beauty/starflow/config/live.txt"));
    }
}
