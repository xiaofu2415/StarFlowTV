package tv.starflow.player;

import com.github.tvbox.osc.BuildConfig;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class BuildIdentityTest {
    @Test
    public void packageNameIsStarFlow() {
        assertEquals("tv.starflow.player", BuildConfig.APPLICATION_ID);
    }

    @Test
    public void releaseIdentityIsVersion142() {
        assertEquals("1.4.2", BuildConfig.VERSION_NAME);
        assertEquals(8, BuildConfig.VERSION_CODE);
    }
}

