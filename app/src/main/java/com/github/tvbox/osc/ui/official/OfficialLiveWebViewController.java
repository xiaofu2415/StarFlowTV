package com.github.tvbox.osc.ui.official;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.http.SslError;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.WebChromeClient;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.official.OfficialLiveRequestTracker;
import com.github.tvbox.osc.official.LiveForegroundGate;
import com.github.tvbox.osc.official.OfficialLiveUrlPolicy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class OfficialLiveWebViewController {
    private static final String INTERNAL_BLANK_URL = "about:blank";
    private static final String FULLSCREEN_ASSET = "official_live_fullscreen.js";
    private static final String EXIT_WEB_FULLSCREEN_SCRIPT =
            "window.__starflowOfficialFullscreen&&window.__starflowOfficialFullscreen.exit();";

    public interface Listener {
        void onLoading(String url);

        void onReady();

        void onError(String message);
    }

    private final Context context;
    private final ViewGroup container;
    private final View loadingView;
    private final Listener listener;
    private final String enterWebFullscreenScript;
    private final OfficialLiveRequestTracker requestTracker = new OfficialLiveRequestTracker();
    private final LiveForegroundGate foregroundGate = new LiveForegroundGate();
    // Interception callbacks run off the UI thread and must see instance replacement.
    private volatile WebView webView;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private int normalSystemUiVisibility;
    private boolean webFullscreenActive;
    private boolean released;

    public OfficialLiveWebViewController(@NonNull Context context,
                                         @NonNull ViewGroup container,
                                         @NonNull Listener listener) {
        this.context = context;
        this.container = container;
        this.listener = listener;
        this.loadingView = container.findViewById(R.id.officialLiveWebLoading);
        this.enterWebFullscreenScript = readAsset(context, FULLSCREEN_ASSET);
        hideSurface();
    }

    public void load(String pageUrl) {
        foregroundGate.runWhenForeground(() -> loadNow(pageUrl));
    }

    private void loadNow(String pageUrl) {
        if (released) {
            listener.onError("央视直播页面已释放");
            return;
        }
        if (!OfficialLiveUrlPolicy.isAllowedNavigationUrl(pageUrl)) {
            stop();
            listener.onError("已拦截非央视官方直播页面");
            return;
        }

        // WebView callbacks have no reliable per-load token, even for identical URLs.
        // Invalidate and destroy A before creating B so stale callbacks cannot act on B.
        requestTracker.stop();
        hideCustomView();
        webFullscreenActive = false;
        WebView oldView = webView;
        webView = null;
        destroyWebView(oldView);
        ensureWebView();
        WebView view = webView;
        String requestedUrl = pageUrl.trim();
        requestTracker.begin(requestedUrl);
        container.setVisibility(View.VISIBLE);
        view.setVisibility(View.VISIBLE);
        view.onResume();
        setLoadingVisible(true);
        listener.onLoading(requestedUrl);
        // Listener code may synchronously stop, release, or start another load.
        if (view == webView && requestTracker.isActive()) view.loadUrl(requestedUrl);
    }

    public void onPause() {
        foregroundGate.onPause();
        if (webView != null) webView.onPause();
    }

    public void onResume() {
        foregroundGate.onResume();
        if (webView != null && requestTracker.isActive()) webView.onResume();
    }

    public void stop() {
        foregroundGate.clearPending();
        boolean hadActiveRequest = requestTracker.stop();
        hideCustomView();
        webFullscreenActive = false;
        if (webView != null) {
            webView.stopLoading();
            if (hadActiveRequest) webView.loadUrl(INTERNAL_BLANK_URL);
            webView.onPause();
            webView.setVisibility(View.GONE);
        }
        hideSurface();
    }

    public void release() {
        if (released) return;
        released = true;
        foregroundGate.onDestroy();
        requestTracker.stop();
        hideCustomView();
        webFullscreenActive = false;
        hideSurface();

        WebView view = webView;
        webView = null;
        destroyWebView(view);
    }

    public boolean isShowing() {
        return requestTracker.isActive() && !released && webView != null
                && container.getVisibility() == View.VISIBLE;
    }

    private void ensureWebView() {
        if (webView != null || released) return;
        webView = createWebView(context);
        container.addView(webView, 0, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @SuppressLint("SetJavaScriptEnabled")
    private WebView createWebView(Context context) {
        WebView view = new WebView(context);
        WebSettings settings = view.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        // CCTV's official player still emits legacy http:// media/API URLs.
        // shouldInterceptRequest below constrains those mixed resources to the
        // explicit official/CDN host allowlist; the page itself is HTTPS-only.
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        view.setBackgroundColor(Color.BLACK);
        view.setWebChromeClient(new OfficialWebChromeClient());
        // Keep D-pad/confirm events on the Activity's existing live-control path.
        view.setFocusable(false);
        view.setFocusableInTouchMode(false);
        view.setWebViewClient(new OfficialWebViewClient());
        return view;
    }

    /** Returns true when the Back key only needs to leave official video fullscreen. */
    public boolean handleBackPressed() {
        if (customView != null) {
            hideCustomView();
            exitWebFullscreen();
            return true;
        }
        return exitWebFullscreen();
    }

    private void enterWebFullscreen(WebView view) {
        if (enterWebFullscreenScript.isEmpty() || view != webView) return;
        webFullscreenActive = true;
        view.evaluateJavascript(enterWebFullscreenScript, null);
    }

    private boolean exitWebFullscreen() {
        if (!webFullscreenActive) return false;
        webFullscreenActive = false;
        WebView view = webView;
        if (view != null) view.evaluateJavascript(EXIT_WEB_FULLSCREEN_SCRIPT, null);
        return true;
    }

    private void showCustomView(View view, WebChromeClient.CustomViewCallback callback) {
        if (view == null || customView != null) {
            if (callback != null) callback.onCustomViewHidden();
            return;
        }
        if (!(context instanceof Activity)) {
            if (callback != null) callback.onCustomViewHidden();
            return;
        }
        View decorView = ((Activity) context).getWindow().getDecorView();
        if (!(decorView instanceof ViewGroup)) {
            if (callback != null) callback.onCustomViewHidden();
            return;
        }

        customView = view;
        customViewCallback = callback;
        normalSystemUiVisibility = decorView.getSystemUiVisibility();
        ViewParent parent = view.getParent();
        if (parent instanceof ViewGroup) ((ViewGroup) parent).removeView(view);
        view.setBackgroundColor(Color.BLACK);
        ViewGroup decorGroup = (ViewGroup) decorView;
        decorGroup.addView(view, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        decorView.setSystemUiVisibility(fullscreenSystemUiVisibility());
    }

    private void hideCustomView() {
        if (customView == null) return;
        View view = customView;
        WebChromeClient.CustomViewCallback callback = customViewCallback;
        customView = null;
        customViewCallback = null;
        ViewParent parent = view.getParent();
        if (parent instanceof ViewGroup) ((ViewGroup) parent).removeView(view);
        if (context instanceof Activity) {
            View decorView = ((Activity) context).getWindow().getDecorView();
            decorView.setSystemUiVisibility(normalSystemUiVisibility);
        }
        if (callback != null) callback.onCustomViewHidden();
    }

    private int fullscreenSystemUiVisibility() {
        return View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
    }

    private void hideSurface() {
        setLoadingVisible(false);
        container.setVisibility(View.GONE);
    }

    private void setLoadingVisible(boolean visible) {
        if (loadingView != null) {
            loadingView.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void fail(String message) {
        if (released || !requestTracker.isActive()) return;
        stop();
        listener.onError(message);
    }

    private boolean shouldBlockMainFrameNavigation(String url) {
        if (!requestTracker.isActive()) return !INTERNAL_BLANK_URL.equals(url);
        boolean blocked = !OfficialLiveUrlPolicy.isAllowedNavigationUrl(url);
        if (blocked) fail("已拦截非央视官方直播页面");
        return blocked;
    }

    private boolean isAllowedInterceptedRequest(String url, boolean mainFrame) {
        if (!requestTracker.isActive()) return INTERNAL_BLANK_URL.equals(url);
        if (mainFrame) {
            return OfficialLiveUrlPolicy.isAllowedNavigationUrl(url);
        }
        return OfficialLiveUrlPolicy.isAllowedResourceUrl(url);
    }

    private static WebResourceResponse emptyResponse() {
        return new WebResourceResponse(
                "text/plain",
                StandardCharsets.UTF_8.name(),
                new ByteArrayInputStream(new byte[0]));
    }

    private static String readAsset(Context context, String name) {
        try (InputStream input = context.getAssets().open(name);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            return "";
        }
    }

    private void destroyWebView(@Nullable WebView view) {
        if (view == null) return;
        view.stopLoading();
        view.onPause();
        view.clearHistory();
        view.removeAllViews();
        container.removeView(view);
        view.destroy();
    }

    private void destroyCrashedWebView(WebView view) {
        container.removeView(view);
        view.destroy();
    }

    private final class OfficialWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            if (view != webView) return true;
            String url = request.getUrl().toString();
            return request.isForMainFrame()
                    ? shouldBlockMainFrameNavigation(url)
                    : !isAllowedInterceptedRequest(url, false);
        }

        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (view != webView) return true;
            if (!requestTracker.isActive()) return !INTERNAL_BLANK_URL.equals(url);
            // API 23 cannot identify iframe navigation: only block/allow, never fail.
            return !OfficialLiveUrlPolicy.isAllowedNavigationUrl(url);
        }

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            if (view != webView) return emptyResponse();
            return isAllowedInterceptedRequest(
                    request.getUrl().toString(), request.isForMainFrame()) ? null : emptyResponse();
        }

        @Nullable
        @SuppressWarnings("deprecation")
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            if (view != webView) return emptyResponse();
            return isAllowedInterceptedRequest(url, false) ? null : emptyResponse();
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            if (view != webView) return;
            boolean transitioned = !requestTracker.matches(url);
            if (!requestTracker.transition(url)) return;
            setLoadingVisible(true);
            if (transitioned) listener.onLoading(url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (view != webView) return;
            if (!requestTracker.matches(url)) return;
            setLoadingVisible(false);
            enterWebFullscreen(view);
            listener.onReady();
        }

        @Override
        public void onReceivedError(WebView view,
                                    WebResourceRequest request,
                                    WebResourceError error) {
            if (view != webView) return;
            if (request.isForMainFrame()
                    && requestTracker.matches(request.getUrl().toString())) {
                fail("央视直播页面加载失败：" + error.getDescription());
            }
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            if (view != webView) {
                handler.cancel();
                return;
            }
            handler.cancel();
            if (requestTracker.matches(error.getUrl())) {
                fail("央视直播页面 SSL 校验失败");
            }
        }

        @Override
        public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
            if (view != webView) return true;
            boolean notifyListener = !released && requestTracker.isActive();
            requestTracker.stop();
            webView = null;
            hideSurface();
            destroyCrashedWebView(view);
            if (notifyListener) {
                listener.onError("央视直播页面渲染进程已退出");
            }
            return true;
        }
    }

    private final class OfficialWebChromeClient extends WebChromeClient {
        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            showCustomView(view, callback);
        }

        @SuppressWarnings("deprecation")
        @Override
        public void onShowCustomView(View view, int requestedOrientation, CustomViewCallback callback) {
            showCustomView(view, callback);
        }

        @Override
        public void onHideCustomView() {
            hideCustomView();
        }
    }
}
