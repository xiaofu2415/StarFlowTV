package com.github.tvbox.osc.update;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class UpdateCheckResultTest {
    @Test public void describesLatestVersion() {
        assertEquals("当前已是最新版本", UpdateCheckResult.message(
                UpdateCheckResult.Status.NO_UPDATE, null));
    }

    @Test public void describesAvailableVersion() {
        assertEquals("发现新版本 1.4.6", UpdateCheckResult.message(
                UpdateCheckResult.Status.UPDATE_AVAILABLE, "1.4.6"));
    }

    @Test public void describesVerificationFailure() {
        assertEquals("更新签名校验失败，请稍后重试", UpdateCheckResult.message(
                UpdateCheckResult.Status.SIGNATURE_INVALID, null));
    }
}
