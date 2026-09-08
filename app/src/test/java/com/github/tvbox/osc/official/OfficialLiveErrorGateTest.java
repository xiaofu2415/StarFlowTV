package com.github.tvbox.osc.official;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class OfficialLiveErrorGateTest {
    @Test public void pausedErrorWaitsForMatchingResumeAndIsConsumedOnce() {
        OfficialLiveErrorGate gate = new OfficialLiveErrorGate();
        Object channel = new Object();
        gate.onResume(channel, 0);
        gate.onPause();
        assertFalse(gate.shouldHandleError(channel, 0));
        assertFalse(gate.shouldHandleError(channel, 0));
        assertTrue(gate.onResume(channel, 0));
        assertFalse(gate.onResume(channel, 0));
    }

    @Test public void differentChannelOrSourceDiscardsPendingError() {
        OfficialLiveErrorGate gate = new OfficialLiveErrorGate();
        Object channel = new Object();
        assertFalse(gate.shouldHandleError(channel, 0));
        assertFalse(gate.onResume(new Object(), 0));
        assertFalse(gate.onResume(channel, 0));
        gate.onPause();
        assertFalse(gate.shouldHandleError(channel, 0));
        assertFalse(gate.onResume(channel, 1));
        assertFalse(gate.onResume(channel, 0));
    }

    @Test public void selectionOrRefreshClearsEvenSameChannelError() {
        OfficialLiveErrorGate gate = new OfficialLiveErrorGate();
        Object channel = new Object();
        assertFalse(gate.shouldHandleError(channel, 0));
        gate.clearPending();
        assertFalse(gate.onResume(channel, 0));
    }

    @Test public void destroyedGateDiscardsPendingAndNeverBecomesPlayableAgain() {
        OfficialLiveErrorGate gate = new OfficialLiveErrorGate();
        Object channel = new Object();
        assertFalse(gate.shouldHandleError(channel, 0));
        gate.onDestroy();
        assertFalse(gate.onResume(channel, 0));
        assertFalse(gate.shouldHandleError(channel, 0));
    }

    @Test public void foregroundHandlesImmediatelyButInitialStateDoesNot() {
        OfficialLiveErrorGate gate = new OfficialLiveErrorGate();
        Object channel = new Object();
        assertFalse(gate.shouldHandleError(channel, 0));
        gate.clearPending();
        assertFalse(gate.onResume(channel, 0));
        assertTrue(gate.shouldHandleError(channel, 0));
        assertFalse(gate.onResume(channel, 0));
    }
}
