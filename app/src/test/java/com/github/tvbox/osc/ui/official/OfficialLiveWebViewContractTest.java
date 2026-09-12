package com.github.tvbox.osc.ui.official;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public final class OfficialLiveWebViewContractTest {
    @Test public void controllerSupportsHtml5VideoAndCustomFullscreen() throws IOException {
        Path source = Paths.get("app/src/main/java/com/github/tvbox/osc/ui/official/OfficialLiveWebViewController.java");
        String text = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);

        assertTrue(text.contains("setWebChromeClient"));
        assertTrue(text.contains("MIXED_CONTENT_ALWAYS_ALLOW"));
        assertTrue(text.contains("onShowCustomView"));
        assertTrue(text.contains("onHideCustomView"));
        assertTrue(text.contains("CustomViewCallback"));

        Path activity = Paths.get("app/src/main/java/com/github/tvbox/osc/ui/activity/LivePlayActivity.java");
        String activityText = new String(Files.readAllBytes(activity), StandardCharsets.UTF_8);
        assertTrue(activityText.contains("officialLiveController.handleBackPressed()"));
    }
}
