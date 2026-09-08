# StarFlowTV 官方直播类目 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不改变现有直播遥控器和频道列表体验的前提下，为 StarFlowTV 增加央视官方直播类目，并构建 `1.4.0` `java32` APK。

**Architecture:** `LivePlayActivity` 继续负责频道列表、焦点、遥控器和状态提示；官方频道通过 `LiveChannelItem` 的播放模式元数据路由到现有 `VideoView` 或新的白名单 WebView。内置 `OfficialLiveCatalog` 提供 21 个央视官方页面作为离线安全回退，稳定直链字段为空时自动使用官方页面，不把 IPTV 源解析或远程脚本执行混入官方路径。

**Tech Stack:** Java 8、Android WebView、现有 Doikki `VideoView`、Gson/JUnit 4、现有 Android Gradle Plugin 与 `java32` ABI 构建。

**Spec:** `docs/superpowers/specs/2026-09-07-starflowtv-official-live-design.md`

## Global Constraints

- 保留现有 `LivePlayActivity`、上下换台、左右换线、OK 频道列表、菜单设置和返回逻辑。
- 官方入口必须是 HTTPS、`tv.cctv.com` 官方直播路径；不得加入社区中转地址。
- 不下载或执行远程任意 JavaScript/JAR，不绕过 DRM、登录、付费墙或地区限制。
- WebView 加载失败不能破坏现有 IPTV 播放和直播源缓存/回滚。
- 不覆盖工作区已有的手动拉取源、`LiveKeyMapper` key-up 和相关测试改动。
- 版本目标为 `versionName '1.4.0'`、`versionCode 6`；构建至少包含 `java32`。
- 每个实现任务先写失败测试，再写最小实现，再运行对应测试并提交独立 commit。

## File Map

- Create `app/src/main/java/com/github/tvbox/osc/official/OfficialLivePlaybackMode.java`: official source mode enum.
- Create `app/src/main/java/com/github/tvbox/osc/official/OfficialLiveChannel.java`: immutable catalog entry.
- Create `app/src/main/java/com/github/tvbox/osc/official/OfficialLiveUrlPolicy.java`: HTTPS/domain/path allowlist.
- Create `app/src/main/java/com/github/tvbox/osc/official/OfficialLiveCatalog.java`: built-in CCTV entries and `LiveChannelGroup` conversion.
- Modify `app/src/main/java/com/github/tvbox/osc/bean/LiveChannelItem.java`: aligned official metadata and source-mode accessors.
- Modify `app/src/main/java/com/github/tvbox/osc/api/ApiConfig.java`: append/restore the official group without duplicating it.
- Create `app/src/main/java/com/github/tvbox/osc/ui/official/OfficialLiveWebViewController.java`: lifecycle-safe WebView surface and navigation allowlist.
- Modify `app/src/main/res/layout/activity_live_play.xml`: add a hidden WebView container above the video surface.
- Modify `app/src/main/java/com/github/tvbox/osc/ui/activity/LivePlayActivity.java`: route official sources, preserve controls, and release WebView resources.
- Modify `app/build.gradle`: bump version to `1.4.0`/`6`.
- Create `docs/release-notes-v1.4.0.md`: explain official category and WebView tradeoffs.
- Modify `.github/workflows/production-signing.yml`: allow the protected signing job to target the new release ref without embedding a private key.
- Create/modify tests under `app/src/test/java/com/github/tvbox/osc/official/` and `app/src/test/java/com/github/tvbox/osc/config/`.

### Task 1: Official catalog model and URL policy

**Files:**
- Create: `app/src/main/java/com/github/tvbox/osc/official/OfficialLivePlaybackMode.java`
- Create: `app/src/main/java/com/github/tvbox/osc/official/OfficialLiveChannel.java`
- Create: `app/src/main/java/com/github/tvbox/osc/official/OfficialLiveUrlPolicy.java`
- Create: `app/src/main/java/com/github/tvbox/osc/official/OfficialLiveCatalog.java`
- Test: `app/src/test/java/com/github/tvbox/osc/official/OfficialLiveCatalogTest.java`
- Test: `app/src/test/java/com/github/tvbox/osc/official/OfficialLiveUrlPolicyTest.java`

**Interfaces:**
- `OfficialLivePlaybackMode` exposes `NONE`, `DIRECT`, and `WEB`.
- `OfficialLiveChannel` exposes `getId()`, `getName()`, `getPageUrl()`, `getDirectUrl()`, and `preferredMode()`.
- `OfficialLiveUrlPolicy.isAllowedPageUrl(String)` accepts only HTTPS URLs whose host is `tv.cctv.com` and whose path starts with `/live/`.
- `OfficialLiveCatalog.builtIn()` returns an immutable list of 21 unique CCTV entries.

- [ ] **Step 1: Write the failing catalog tests**

```java
@Test public void builtInCatalogContainsTheTwentyOneCctvEntrances() {
    List<OfficialLiveChannel> channels = OfficialLiveCatalog.builtIn();
    assertEquals(21, channels.size());
    assertEquals("cctv-1", channels.get(0).getId());
    assertEquals("cctv-5plus", channels.get(17).getId());
    assertEquals("cctv-4-america", channels.get(20).getId());
    assertEquals(21, new HashSet<String>(ids(channels)).size());
    for (OfficialLiveChannel channel : channels) {
        assertTrue(OfficialLiveUrlPolicy.isAllowedPageUrl(channel.getPageUrl()));
    }
}

@Test public void emptyDirectUrlUsesOfficialWebPage() {
    OfficialLiveChannel channel = OfficialLiveCatalog.builtIn().get(0);
    assertEquals(OfficialLivePlaybackMode.WEB, channel.preferredMode());
}
```

- [ ] **Step 2: Write the failing URL-policy tests**

```java
@Test public void rejectsNonOfficialPageUrls() {
    assertFalse(OfficialLiveUrlPolicy.isAllowedPageUrl("http://tv.cctv.com/live/cctv1/"));
    assertFalse(OfficialLiveUrlPolicy.isAllowedPageUrl("https://evil.example/live/cctv1/"));
    assertFalse(OfficialLiveUrlPolicy.isAllowedPageUrl("https://tv.cctv.com/program/"));
    assertFalse(OfficialLiveUrlPolicy.isAllowedPageUrl("https://tv.cctv.com/live/cctv1/?token=secret"));
}

@Test public void acceptsCctvLivePages() {
    assertTrue(OfficialLiveUrlPolicy.isAllowedPageUrl("https://tv.cctv.com/live/cctv1/"));
    assertTrue(OfficialLiveUrlPolicy.isAllowedPageUrl("https://tv.cctv.com/live/cctveurope/index.shtml"));
}
```

- [ ] **Step 3: Run the focused tests and verify they fail**

Run:

```bash
./gradlew :app:testJava32DebugUnitTest --tests 'com.github.tvbox.osc.official.*'
```

Expected: compilation/test failure because the official package and interfaces do not exist yet.

- [ ] **Step 4: Implement the catalog and policy**

Add the entries in this order: CCTV-1 through CCTV-17, CCTV-5+, CCTV-4 Asia, CCTV-4 Europe, and CCTV-4 America. Use the verified page paths `/live/cctv1/` through `/live/cctv17/`, `/live/cctv5plus/`, `/live/cctv4/`, `/live/cctveurope/index.shtml`, and `/live/cctvamerica/`. Keep direct URLs empty in the built-in list so the first release always uses the official page rather than an unverified relay.

- [ ] **Step 5: Run the focused tests and verify they pass**

Run the same Gradle command. Expected: all catalog and URL-policy tests pass.

- [ ] **Step 6: Commit the model layer**

```bash
git add app/src/main/java/com/github/tvbox/osc/official app/src/test/java/com/github/tvbox/osc/official
git commit -m "feat: add official live catalog and URL policy"
```

### Task 2: Convert the catalog into live groups and source metadata

**Files:**
- Modify: `app/src/main/java/com/github/tvbox/osc/bean/LiveChannelItem.java`
- Modify: `app/src/main/java/com/github/tvbox/osc/api/ApiConfig.java`
- Modify: `app/src/main/java/com/github/tvbox/osc/official/OfficialLiveCatalog.java`
- Test: `app/src/test/java/com/github/tvbox/osc/official/OfficialLiveCatalogTest.java`

**Interfaces:**
- `LiveChannelItem.isOfficialLive()` returns true only when `officialChannelId` is non-empty and a page URL is present.
- `LiveChannelItem.getSourcePlaybackMode()` returns the mode aligned with `sourceIndex`; ordinary IPTV items return `NONE`.
- `OfficialLiveCatalog.toGroup(int groupIndex, int channelNumberStart)` returns a `LiveChannelGroup` named `官方直播` and assigns each item a channel index, channel number, one `央视官方页` URL, and `WEB` source mode.
- `ApiConfig.ensureOfficialLiveGroup()` appends the built-in group only when no group named `官方直播` exists.

- [ ] **Step 1: Add failing metadata tests**

```java
@Test public void catalogGroupProvidesNavigableLiveItems() {
    LiveChannelGroup group = OfficialLiveCatalog.toGroup(4, 12);
    assertEquals("官方直播", group.getGroupName());
    assertEquals(21, group.getLiveChannels().size());
    LiveChannelItem first = group.getLiveChannels().get(0);
    assertTrue(first.isOfficialLive());
    assertEquals(13, first.getChannelNum());
    assertEquals(OfficialLivePlaybackMode.WEB, first.getSourcePlaybackMode());
    assertEquals("央视官方页", first.getSourceName());
}
```

- [ ] **Step 2: Run the test and verify it fails**

Run:

```bash
./gradlew :app:testJava32DebugUnitTest --tests 'com.github.tvbox.osc.official.OfficialLiveCatalogTest.catalogGroupProvidesNavigableLiveItems'
```

Expected: compilation failure for missing official metadata methods.

- [ ] **Step 3: Implement aligned source metadata**

Add `officialChannelId`, `officialPageUrl`, `officialDirectUrl`, and `ArrayList<OfficialLivePlaybackMode> channelSourceModes` to `LiveChannelItem`. Make `setChannelUrls` preserve existing ordinary behavior and make `getSourcePlaybackMode` safely return `NONE` for legacy items or out-of-range indices. Extend `OfficialLiveCatalog.toGroup` to populate all aligned arrays.

- [ ] **Step 4: Add the built-in group to `ApiConfig`**

Call `ensureOfficialLiveGroup()` at the end of `loadLives` and from `getChannelGroupList()`. Recalculate the next group/channel numbers from the existing list, append rather than prepend, and skip insertion when a remote configuration already supplies a group named `官方直播`. This keeps existing group indices stable and lets an empty/corrupt remote configuration still expose the built-in official group.

- [ ] **Step 5: Run all config/navigation tests**

```bash
./gradlew :app:testJava32DebugUnitTest --tests 'com.github.tvbox.osc.official.*' --tests 'com.github.tvbox.osc.config.*' --tests 'com.github.tvbox.osc.navigation.*'
```

Expected: PASS, including the pre-existing manual-refresh and key-up tests.

- [ ] **Step 6: Commit the group integration**

```bash
git add app/src/main/java/com/github/tvbox/osc/bean/LiveChannelItem.java app/src/main/java/com/github/tvbox/osc/api/ApiConfig.java app/src/main/java/com/github/tvbox/osc/official/OfficialLiveCatalog.java app/src/test/java/com/github/tvbox/osc/official
git commit -m "feat: expose official live channels in live groups"
```

### Task 3: Add the official WebView surface

**Files:**
- Create: `app/src/main/java/com/github/tvbox/osc/ui/official/OfficialLiveWebViewController.java`
- Modify: `app/src/main/res/layout/activity_live_play.xml`
- Test: `app/src/test/java/com/github/tvbox/osc/official/OfficialLiveUrlPolicyTest.java`

**Interfaces:**
- Constructor: `OfficialLiveWebViewController(Context context, ViewGroup container, Listener listener)`.
- Methods: `load(String pageUrl)`, `onPause()`, `onResume()`, `stop()`, `release()`, and `isShowing()`.
- Listener callbacks: `onLoading(String url)`, `onReady()`, and `onError(String message)`.

- [ ] **Step 1: Extend URL-policy tests for resource/navigation decisions**

```java
@Test public void officialResourceHostsRemainAllowlisted() {
    assertTrue(OfficialLiveUrlPolicy.isAllowedNavigationUrl("https://tv.cctv.com/live/cctv1/"));
    assertTrue(OfficialLiveUrlPolicy.isAllowedResourceHost("p2.img.cctvpic.com"));
    assertTrue(OfficialLiveUrlPolicy.isAllowedResourceHost("live.cctv.com"));
    assertFalse(OfficialLiveUrlPolicy.isAllowedNavigationUrl("https://example.com/redirect"));
}
```

- [ ] **Step 2: Run the test and verify it fails**

```bash
./gradlew :app:testJava32DebugUnitTest --tests 'com.github.tvbox.osc.official.OfficialLiveUrlPolicyTest.officialResourceHostsRemainAllowlisted'
```

Expected: FAIL because the resource/navigation policy methods do not exist.

- [ ] **Step 3: Implement the controller and layout**

Add a `FrameLayout` named `officialLiveWebContainer` above `mVideoView`, initially `gone`, with a black background and a centered progress indicator/text. The controller creates one system `WebView`, enables JavaScript/DOM storage/media playback, disables file access and multiple windows, never calls `addJavascriptInterface`, and never downloads remote scripts. `WebViewClient` cancels SSL errors and blocks top-level navigation outside the official allowlist while allowing official CCTV/CDN resource hosts.

- [ ] **Step 4: Build resources and run unit tests**

```bash
./gradlew :app:testJava32DebugUnitTest :app:processJava32DebugResources
```

Expected: PASS and successful Android resource processing.

- [ ] **Step 5: Commit the WebView surface**

```bash
git add app/src/main/java/com/github/tvbox/osc/ui/official/OfficialLiveWebViewController.java app/src/main/res/layout/activity_live_play.xml app/src/main/java/com/github/tvbox/osc/official/OfficialLiveUrlPolicy.java app/src/test/java/com/github/tvbox/osc/official/OfficialLiveUrlPolicyTest.java
git commit -m "feat: add allowlisted official live web surface"
```

### Task 4: Route official channels without changing live controls

**Files:**
- Modify: `app/src/main/java/com/github/tvbox/osc/ui/activity/LivePlayActivity.java`
- Modify: `app/src/main/java/com/github/tvbox/osc/ui/official/OfficialLiveWebViewController.java`
- Test: `app/src/test/java/com/github/tvbox/osc/official/OfficialLiveCatalogTest.java`

**Interfaces:**
- Add private `playOfficialChannel(LiveChannelItem item, boolean changeSource)` in `LivePlayActivity`.
- Add private `isOfficialItem(LiveChannelItem item)` and `releaseOfficialWebSurface()` helpers.
- `OfficialLiveWebViewController` reports WebView load failure through its listener; Activity retries the current page once, then uses the aligned next source if one exists.

- [ ] **Step 1: Add a failing source-selection test**

```java
@Test public void builtInOfficialItemsStartInWebModeAndExposeOneSource() {
    LiveChannelItem item = OfficialLiveCatalog.toGroup(0, 0).getLiveChannels().get(0);
    assertEquals(1, item.getSourceNum());
    assertEquals(OfficialLivePlaybackMode.WEB, item.getSourcePlaybackMode());
    assertTrue(item.getUrl().startsWith("https://tv.cctv.com/live/"));
}
```

- [ ] **Step 2: Run the test and verify it fails if routing metadata is incomplete**

```bash
./gradlew :app:testJava32DebugUnitTest --tests 'com.github.tvbox.osc.official.OfficialLiveCatalogTest.builtInOfficialItemsStartInWebModeAndExposeOneSource'
```

Expected: FAIL until the source-mode alignment and official URL are fully wired.

- [ ] **Step 3: Route the playback branch**

In `playChannel`, after the current item is selected and before ordinary `VideoView` setup, branch on `getSourcePlaybackMode()`. `DIRECT` keeps the current player setup. `WEB` releases/hides the video surface, hides the switch snapshot, and calls the controller with `getUrl()`. On an ordinary IPTV item, release/hide the WebView and execute the existing code unchanged.

- [ ] **Step 4: Preserve lifecycle and remote-key behavior**

Initialize the controller in `init()`, forward `onPause`/`onResume`/`onDestroy`, and keep `dispatchKeyEvent`, `handleLiveKeyAction`, `showChannelList`, `showSettingGroup`, `playNext`, `playPrevious`, `playPreSource`, and `playNextSource` as the single control path. For official WebView channels, disable the video-only seek/play controls and leave channel-list/menu controls active.

- [ ] **Step 5: Handle official errors and EPG safely**

On `onError`, retry the current official page once; if the item has another aligned source, call `nextSource()` and `playChannel`; otherwise show the existing exhausted-source toast. Keep EPG requests and bottom channel information functional; when no EPG exists, show the existing empty state and `1/1` source display.

- [ ] **Step 6: Run focused and full unit tests**

```bash
./gradlew :app:testJava32DebugUnitTest
```

Expected: PASS with no regressions in failover, config refresh, key mapping, build identity, and official catalog tests.

- [ ] **Step 7: Commit the playback integration**

```bash
git add app/src/main/java/com/github/tvbox/osc/ui/activity/LivePlayActivity.java app/src/main/java/com/github/tvbox/osc/ui/official/OfficialLiveWebViewController.java app/src/test/java/com/github/tvbox/osc/official
git commit -m "feat: route official live playback through existing live UI"
```

### Task 5: Version, release documentation, and protected build target

**Files:**
- Modify: `app/build.gradle`
- Create: `docs/release-notes-v1.4.0.md`
- Modify: `.github/workflows/production-signing.yml`
- Test: `app/src/test/java/tv/starflow/player/BuildIdentityTest.java`

**Interfaces:**
- APK metadata becomes `versionName '1.4.0'` and `versionCode 6`.
- The signing workflow accepts an explicit protected release ref, while secrets remain GitHub Environment secrets and no keystore enters git.

- [ ] **Step 1: Add a failing build-version assertion**

Add a `BuildIdentityTest` assertion for `BuildConfig.VERSION_NAME` equal to `1.4.0` and `BuildConfig.VERSION_CODE` equal to `6`, then run:

```bash
./gradlew :app:testJava32DebugUnitTest --tests 'tv.starflow.player.BuildIdentityTest'
```

Expected: FAIL while the module still reports `1.3.1`/`5`.

- [ ] **Step 2: Bump the version and document behavior**

Update `app/build.gradle`, add release notes covering the official category, 21 CCTV pages, hybrid fallback behavior, WebView limitations, manual source refresh, and the fixed key-up handling. Do not add private URLs, tokens, or signing material.

- [ ] **Step 3: Generalize the protected signing ref**

Add a required `workflow_dispatch` input named `release_ref` with default `main`; use it for checkout and for the expected remote ref comparison. Keep all signing secret checks, `apksigner` verification, package-name verification, public certificate output, and SHA256 generation unchanged.

- [ ] **Step 4: Run identity and full tests**

```bash
./gradlew :app:testJava32DebugUnitTest
```

Expected: PASS and version assertions report `1.4.0`/`6`.

- [ ] **Step 5: Commit release metadata**

```bash
git add app/build.gradle docs/release-notes-v1.4.0.md .github/workflows/production-signing.yml app/src/test/java/tv/starflow/player/BuildIdentityTest.java
git commit -m "release: prepare StarFlowTV 1.4.0 official live category"
```

### Task 6: Build, verify, and push the release candidate

**Files/artifacts:**
- Build outputs: `app/build/outputs/apk/java32/debug/StarFlowTV-1.4.0-java32-debug.apk` and the Java release APKs.
- Checksums: `app/build/outputs/apk/SHA256SUMS`.
- Verification report: `docs/verification/starflowtv-1.4.0-official-live.md`.

- [ ] **Step 1: Run the complete local verification**

```bash
./gradlew :app:testJava32DebugUnitTest \
  :app:assembleJava32Debug \
  :app:assembleJavaDebug \
  :app:assembleJava64Debug \
  --stacktrace
```

Expected: all unit tests pass and all three debug APKs exist with the `1.4.0` filenames.

- [ ] **Step 2: Verify APK identity and checksums**

Run `sha256sum` for every produced APK; use Android build-tools `aapt dump badging` to verify package `tv.starflow.player`, version `1.4.0`, and SDK floor; use `apksigner verify` when the protected signing artifact is available. Record commands and outputs in the verification report without recording secrets.

- [ ] **Step 3: Inspect the diff and protect existing work**

Run `git status --short`, `git diff --check`, and `git diff --stat`. Confirm the earlier manual-refresh dialog, `RemoteConfigManager` changes, `LiveKeyMapper` changes, and their tests remain present. Do not use reset/checkout commands that discard them.

- [ ] **Step 4: Push the feature branch and trigger CI**

Push the committed branch to `origin`, open or update the pull request targeting `main`, and wait for `:app:testJava32DebugUnitTest` plus Java ABI builds. If terminal push is unavailable, use the authenticated GitHub browser session; do not paste tokens into the repository or chat.

- [ ] **Step 5: Publish only after evidence**

When CI passes, trigger the protected signing workflow for the exact release ref, verify all three certificate outputs and `SHA256SUMS`, then publish the release candidate artifacts. Do not claim online APK update readiness until `latest.json`, signatures, certificate metadata, and the APK URLs return successfully from the configured update endpoint.

