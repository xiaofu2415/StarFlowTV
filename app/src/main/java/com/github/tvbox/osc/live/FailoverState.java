package com.github.tvbox.osc.live;

import java.util.ArrayDeque;
import java.util.Deque;

public final class FailoverState {
    static final long FAILURE_WINDOW_MILLIS = 30 * 60_000L;
    static final long CIRCUIT_BREAK_MILLIS = 2 * 60 * 60_000L;
    static final long ANTI_FLAP_MILLIS = 60_000L;

    private final int lineCount;
    private final Deque<Long> failures = new ArrayDeque<>();
    private boolean stallRetryUsed;
    private boolean userLocked;
    private boolean refreshRequested;
    private int attemptedLines;
    private int successfulProbes;
    private long circuitOpenUntilMillis;
    private long lastSwitchMillis = Long.MIN_VALUE;

    public FailoverState(int lineCount) {
        this.lineCount = Math.max(0, lineCount);
    }

    public void setUserLocked(boolean userLocked) {
        this.userLocked = userLocked;
    }

    public boolean isUserLocked() {
        return userLocked;
    }

    boolean useStallRetry() {
        if (stallRetryUsed) return false;
        stallRetryUsed = true;
        return true;
    }

    void recordPlaybackStarted() {
        stallRetryUsed = false;
        attemptedLines = 0;
        refreshRequested = false;
    }

    void recordFailure(long nowMillis) {
        successfulProbes = 0;
        long cutoff = nowMillis - FAILURE_WINDOW_MILLIS;
        while (!failures.isEmpty() && failures.peekFirst() < cutoff) {
            failures.removeFirst();
        }
        failures.addLast(nowMillis);
        if (failures.size() >= 3) {
            circuitOpenUntilMillis = nowMillis + CIRCUIT_BREAK_MILLIS;
        }
    }

    void recordSuccessfulProbe() {
        successfulProbes++;
        if (successfulProbes >= 3) {
            circuitOpenUntilMillis = 0L;
            failures.clear();
            successfulProbes = 0;
        }
    }

    int incrementAttempts() {
        return ++attemptedLines;
    }

    int getAttemptCap() {
        return Math.min(4, lineCount);
    }

    boolean isRefreshRequested() {
        return refreshRequested;
    }

    void markRefreshRequested() {
        refreshRequested = true;
    }

    public void recordSwitch(long nowMillis) {
        lastSwitchMillis = nowMillis;
        stallRetryUsed = false;
    }

    public boolean isInsideAntiFlapWindow(long nowMillis) {
        return lastSwitchMillis != Long.MIN_VALUE
                && nowMillis >= lastSwitchMillis
                && nowMillis - lastSwitchMillis < ANTI_FLAP_MILLIS;
    }

    public boolean isCircuitOpen(long nowMillis) {
        return circuitOpenUntilMillis > nowMillis;
    }

    public long getCircuitOpenUntilMillis() {
        return circuitOpenUntilMillis;
    }
}
