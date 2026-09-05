package com.github.tvbox.osc.live;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class FailoverPolicyTest {
    private final FailoverPolicy policy = new FailoverPolicy();

    @Test
    public void firstEightSecondStallRetriesCurrentLineOnce() {
        FailoverState state = new FailoverState(4);

        assertEquals(FailoverDecision.RETRY_CURRENT,
                policy.onEvent(PlaybackEvent.NO_DATA_8_SECONDS, state, 1_000L));
        assertEquals(FailoverDecision.TRY_NEXT,
                policy.onEvent(PlaybackEvent.NO_DATA_8_SECONDS, state, 2_000L));
    }

    @Test
    public void tenSecondNoFirstFrameRetriesThenFailsOver() {
        FailoverState state = new FailoverState(4);

        assertEquals(FailoverDecision.RETRY_CURRENT,
                policy.onEvent(PlaybackEvent.NO_FIRST_FRAME_10_SECONDS, state, 1_000L));
        assertEquals(FailoverDecision.TRY_NEXT,
                policy.onEvent(PlaybackEvent.NO_FIRST_FRAME_10_SECONDS, state, 2_000L));
    }

    @Test
    public void terminalHttpErrorsFailOverImmediately() {
        for (PlaybackEvent event : new PlaybackEvent[]{
                PlaybackEvent.HTTP_403, PlaybackEvent.HTTP_404, PlaybackEvent.HTTP_410}) {
            assertEquals(FailoverDecision.TRY_NEXT,
                    policy.onEvent(event, new FailoverState(4), 1_000L));
        }
    }

    @Test
    public void genericPlaybackErrorFailsOverImmediately() {
        assertEquals(FailoverDecision.TRY_NEXT,
                policy.onEvent(PlaybackEvent.PLAYBACK_ERROR, new FailoverState(4), 1_000L));
    }

    @Test
    public void threeFailuresInThirtyMinutesOpenCircuitForTwoHours() {
        FailoverState state = new FailoverState(4);
        policy.onEvent(PlaybackEvent.HTTP_404, state, 1_000L);
        policy.onEvent(PlaybackEvent.HTTP_404, state, 10 * 60_000L);
        policy.onEvent(PlaybackEvent.HTTP_404, state, 29 * 60_000L);

        assertTrue(state.isCircuitOpen(29 * 60_000L));
        assertEquals(149 * 60_000L, state.getCircuitOpenUntilMillis());
    }

    @Test
    public void threeSuccessfulProbesRestoreCircuitBrokenLine() {
        FailoverState state = new FailoverState(4);
        policy.onEvent(PlaybackEvent.HTTP_404, state, 1_000L);
        policy.onEvent(PlaybackEvent.HTTP_404, state, 2_000L);
        policy.onEvent(PlaybackEvent.HTTP_404, state, 3_000L);
        assertTrue(state.isCircuitOpen(4_000L));

        policy.onEvent(PlaybackEvent.PROBE_SUCCESS, state, 5_000L);
        policy.onEvent(PlaybackEvent.PROBE_SUCCESS, state, 6_000L);
        policy.onEvent(PlaybackEvent.PROBE_SUCCESS, state, 7_000L);

        assertFalse(state.isCircuitOpen(7_000L));
    }

    @Test
    public void antiFlapWindowLastsSixtySeconds() {
        FailoverState state = new FailoverState(4);
        state.recordSwitch(10_000L);

        assertTrue(state.isInsideAntiFlapWindow(69_999L));
        assertFalse(state.isInsideAntiFlapWindow(70_000L));
    }

    @Test
    public void userLockPreventsAutomaticFailover() {
        FailoverState state = new FailoverState(4);
        state.setUserLocked(true);

        assertEquals(FailoverDecision.STAY_LOCKED,
                policy.onEvent(PlaybackEvent.HTTP_404, state, 1_000L));
    }

    @Test
    public void fourAttemptCapRefreshesThenShowsExhausted() {
        FailoverState state = new FailoverState(8);
        assertEquals(FailoverDecision.TRY_NEXT, policy.onEvent(PlaybackEvent.HTTP_404, state, 1_000L));
        assertEquals(FailoverDecision.TRY_NEXT, policy.onEvent(PlaybackEvent.HTTP_404, state, 2_000L));
        assertEquals(FailoverDecision.TRY_NEXT, policy.onEvent(PlaybackEvent.HTTP_404, state, 3_000L));
        assertEquals(FailoverDecision.REFRESH_CONFIG, policy.onEvent(PlaybackEvent.HTTP_404, state, 4_000L));
        assertEquals(FailoverDecision.SHOW_EXHAUSTED, policy.onEvent(PlaybackEvent.HTTP_404, state, 5_000L));
    }
}
