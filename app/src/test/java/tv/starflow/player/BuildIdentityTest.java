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
    public void releaseIdentityIsVersion143() {
        assertEquals("1.4.3", BuildConfig.VERSION_NAME);
        assertEquals(9, BuildConfig.VERSION_CODE);
    }
}
