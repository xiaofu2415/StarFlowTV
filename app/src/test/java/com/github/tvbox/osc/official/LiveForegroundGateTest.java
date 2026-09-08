package com.github.tvbox.osc.official;

import org.junit.Test;
import static org.junit.Assert.*;

public class LiveForegroundGateTest {
    @Test public void pausedRefreshWaitsForResumeAndOnlyRunsLatestResult() {
        LiveForegroundGate gate = new LiveForegroundGate();
        int[] plays = {0};
        gate.onResume();
        gate.onPause();
        gate.runWhenForeground(() -> plays[0] += 1);
        gate.runWhenForeground(() -> plays[0] += 10);
        assertEquals(0, plays[0]);
        gate.onResume();
        assertEquals(10, plays[0]);
        gate.onResume();
        assertEquals(10, plays[0]);
    }

    @Test public void destroyDiscardsPendingAndFutureLoads() {
        LiveForegroundGate gate = new LiveForegroundGate();
        int[] plays = {0};
        gate.runWhenForeground(() -> plays[0]++);
        gate.onDestroy();
        gate.onResume();
        gate.runWhenForeground(() -> plays[0]++);
        assertEquals(0, plays[0]);
    }
}
