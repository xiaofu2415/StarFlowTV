package com.github.tvbox.osc.live;

public enum FailoverDecision {
    RETRY_CURRENT,
    TRY_NEXT,
    REFRESH_CONFIG,
    STAY_LOCKED,
    SHOW_EXHAUSTED,
    NONE
}
