package com.github.tvbox.osc.official;

/** One retry per selected web source, with a latch for duplicate load failures. */
public final class OfficialLiveRetryState {
    public enum Action { NONE, RETRY, NEXT, EXHAUSTED }

    private boolean active;
    private boolean retried;

    public void reset() {
        active = false;
        retried = false;
    }

    public void onLoad() {
        active = true;
    }

    public void onReady() {
        if (active) retried = false;
    }

    public Action onError(int sourceIndex, int sourceCount) {
        if (!active) return Action.NONE;
        active = false;
        if (!retried) {
            retried = true;
            return Action.RETRY;
        }
        return sourceIndex >= 0 && sourceIndex + 1 < sourceCount
                ? Action.NEXT : Action.EXHAUSTED;
    }
}
