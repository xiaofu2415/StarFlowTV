package com.github.tvbox.osc.live;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public final class LineSelectorTest {
    private final LineSelector selector = new LineSelector();

    @Test
    public void compatibleHealthyLineRanksBeforeBroken4kLine() {
        LineHealth unstable4k = new LineHealth("4k", 0, 98, "hevc", 2160, 10_000L, 0L);
        LineHealth stable1080p = new LineHealth("fhd", 1, 90, "h264", 1080, 0L, 20_000L);
        DeviceCapabilities device = new DeviceCapabilities(2160, Arrays.asList("h264", "hevc"));

        List<LineHealth> ranked = selector.rank(
                Arrays.asList(unstable4k, stable1080p), device, 20_000L);

        assertEquals("fhd", ranked.get(0).getLineId());
    }

    @Test
    public void circuitBrokenAndUnsupportedLinesRankLast() {
        LineHealth openCircuit = new LineHealth("open", 0, 99, "h264", 1080, 50_000L, 0L);
        LineHealth unsupported = new LineHealth("av1", 1, 95, "av1", 2160, 0L, 0L);
        LineHealth eligible = new LineHealth("eligible", 2, 80, "h264", 1080, 0L, 10_000L);
        DeviceCapabilities device = new DeviceCapabilities(2160, Arrays.asList("h264", "hevc"));

        List<LineHealth> ranked = selector.rank(
                Arrays.asList(openCircuit, unsupported, eligible), device, 20_000L);

        assertEquals("eligible", ranked.get(0).getLineId());
        assertEquals("open", ranked.get(2).getLineId());
    }
}
