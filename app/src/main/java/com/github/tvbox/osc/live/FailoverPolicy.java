package com.github.tvbox.osc.live;

public final class FailoverPolicy {
    public FailoverDecision onEvent(PlaybackEvent event, FailoverState state, long nowMillis) {
        if (event == PlaybackEvent.PLAYBACK_STARTED) {
            state.recordPlaybackStarted();
            return FailoverDecision.NONE;
        }
        if (event == PlaybackEvent.PROBE_SUCCESS) {
            state.recordSuccessfulProbe();
            return FailoverDecision.NONE;
        }
        if (state.isUserLocked()) {
            return FailoverDecision.STAY_LOCKED;
        }

        state.recordFailure(nowMillis);
        if ((event == PlaybackEvent.NO_DATA_8_SECONDS
                || event == PlaybackEvent.NO_FIRST_FRAME_10_SECONDS)
                && state.useStallRetry()) {
            return FailoverDecision.RETRY_CURRENT;
        }

        int cap = state.getAttemptCap();
        if (cap == 0) {
            return FailoverDecision.SHOW_EXHAUSTED;
        }
        if (state.incrementAttempts() >= cap) {
            if (!state.isRefreshRequested()) {
                state.markRefreshRequested();
                return FailoverDecision.REFRESH_CONFIG;
            }
            return FailoverDecision.SHOW_EXHAUSTED;
        }
        state.recordSwitch(nowMillis);
        return FailoverDecision.TRY_NEXT;
    }
}
