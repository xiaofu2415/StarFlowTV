package com.github.tvbox.osc.official;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class OfficialLiveRetryStateTest {
    @Test public void retriesPageOnlyOnceThenExhaustsSingleSource() {
        OfficialLiveRetryState state = new OfficialLiveRetryState();
        state.onLoad();
        assertEquals(OfficialLiveRetryState.Action.RETRY, state.onError(0, 1));
        assertEquals(OfficialLiveRetryState.Action.NONE, state.onError(0, 1));
        state.onLoad();
        assertEquals(OfficialLiveRetryState.Action.EXHAUSTED, state.onError(0, 1));
        assertEquals(OfficialLiveRetryState.Action.NONE, state.onError(0, 1));
    }

    @Test public void advancesOnlyToAnExistingNextSourceWithoutWrapping() {
        OfficialLiveRetryState state = new OfficialLiveRetryState();
        state.onLoad();
        assertEquals(OfficialLiveRetryState.Action.RETRY, state.onError(0, 2));
        state.onLoad();
        assertEquals(OfficialLiveRetryState.Action.NEXT, state.onError(0, 2));
        state.reset();
        state.onLoad();
        assertEquals(OfficialLiveRetryState.Action.RETRY, state.onError(1, 2));
        state.onLoad();
        assertEquals(OfficialLiveRetryState.Action.EXHAUSTED, state.onError(1, 2));
    }

    @Test public void successAndNewSelectionRestoreOneRetry() {
        OfficialLiveRetryState state = new OfficialLiveRetryState();
        state.onLoad();
        assertEquals(OfficialLiveRetryState.Action.RETRY, state.onError(0, 1));
        state.onLoad();
        state.onReady();
        assertEquals(OfficialLiveRetryState.Action.RETRY, state.onError(0, 1));
        state.reset();
        state.onLoad();
        assertEquals(OfficialLiveRetryState.Action.RETRY, state.onError(0, 1));
    }

    @Test public void stoppedSelectionIgnoresErrorsAndLateReady() {
        OfficialLiveRetryState state = new OfficialLiveRetryState();
        state.onLoad();
        state.reset();
        state.onReady();
        assertEquals(OfficialLiveRetryState.Action.NONE, state.onError(0, 1));
    }
}
