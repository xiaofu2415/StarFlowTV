# StarFlowTV Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a live-first Android TV application based on TVBoxOS, plus a companion source pipeline that validates, ranks, publishes, and safely updates multi-line live-channel configurations.

**Architecture:** `StarFlowTV` retains the TVBoxOS VOD engine but routes startup directly to a redesigned live experience. Pure Java domain services own line scoring, failover, and update decisions so they can be unit-tested independently of activities. A sibling Python project, `StarFlowTV-Sources`, probes candidate streams, publishes signed/versioned artifacts, and never stores private credentials in public outputs.

**Tech Stack:** Android/Java 8, Gradle, JUnit 4, Robolectric where Android APIs are required, OkHttp, Gson, Python 3.11, pytest, ffprobe/FFmpeg, JSON Schema, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-09-05-starflowtv-design.md`

## Global Constraints

- Product name: `StarFlowTV`; Chinese display name: `星流电视`.
- Android application ID: `tv.starflow.player`.
- Minimum Android version: Android 6.0 / API 23.
- Build outputs: `armeabi-v7a`, `arm64-v8a`, and universal APKs.
- Startup opens live TV; VOD remains available through a secondary entry.
- Live and VOD configuration lifecycles remain isolated.
- No live catch-up or time-shift implementation.
- Do not bypass DRM, authentication, paywalls, or geographic controls.
- Never commit tokens, cookies, account credentials, or private IPTV addresses.
- Preserve TVBoxOS AGPL-3.0 license and upstream attribution.
- Upstream baseline: `q215613905/TVBoxOS@ccc25f67ce35c6699529d493177c737be3ad4ba4`.

---

## Milestone 1 — Player Foundation and Live-First Shell

### Task 1: Import and Verify the Upstream Baseline

**Files:**
- Modify: `.git/config`
- Preserve: `LICENSE`
- Create: `UPSTREAM.md`
- Create: `docs/verification/upstream-baseline.md`

**Interfaces:**
- Consumes: TVBoxOS commit `ccc25f67ce35c6699529d493177c737be3ad4ba4`.
- Produces: a buildable repository with `upstream` remote and recorded baseline.

- [ ] **Step 1: Add and fetch the exact upstream**

```bash
git remote add upstream https://github.com/q215613905/TVBoxOS.git
git fetch upstream ccc25f67ce35c6699529d493177c737be3ad4ba4
git merge --allow-unrelated-histories --no-edit ccc25f67ce35c6699529d493177c737be3ad4ba4
```

- [ ] **Step 2: Record provenance**

Create `UPSTREAM.md` with the upstream URL, baseline SHA, import date, AGPL-3.0 notice, and a rule that upstream syncs occur on dedicated `sync/upstream-*` branches.

- [ ] **Step 3: Run the baseline build**

```bash
./gradlew :app:assembleJava32Debug :app:assembleJava64Debug --stacktrace
```

Expected: both tasks finish with `BUILD SUCCESSFUL`; if the unmodified baseline fails, record the complete failing task and environment details in `docs/verification/upstream-baseline.md` before making product changes.

- [ ] **Step 4: Commit**

```bash
git add LICENSE UPSTREAM.md docs/verification/upstream-baseline.md
git commit -m "chore: import TVBoxOS upstream baseline"
```

### Task 2: Apply StarFlowTV Identity and Build Matrix

**Files:**
- Modify: `app/build.gradle`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/xml/file_paths.xml`
- Create: `app/src/test/java/tv/starflow/player/BuildIdentityTest.java`

**Interfaces:**
- Consumes: upstream Android application.
- Produces: application ID `tv.starflow.player`, display name `星流电视`, API 23 floor, named ABI artifacts.

- [ ] **Step 1: Write the identity test**

```java
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public final class BuildIdentityTest {
    @Test public void packageNameIsStarFlow() {
        assertEquals("tv.starflow.player", BuildConfig.APPLICATION_ID);
    }
}
```

- [ ] **Step 2: Run it and verify the old identity fails**

```bash
./gradlew :app:testJava32DebugUnitTest --tests '*BuildIdentityTest'
```

Expected: FAIL because the upstream application ID is `com.github.tvbox.osc.jun`.

- [ ] **Step 3: Update build identity**

Set `applicationId 'tv.starflow.player'`, set every flavor to `minSdkVersion 23`, keep v7a/v8a filters, and emit filenames `StarFlowTV-<version>-<flavor>-<buildType>.apk`. Change `app_name` to `星流电视` and keep FileProvider authority based on `${applicationId}`.

- [ ] **Step 4: Run identity and assembly checks**

```bash
./gradlew :app:testJava32DebugUnitTest :app:assembleJava32Debug :app:assembleJava64Debug
```

Expected: PASS and both APKs use the StarFlowTV filename prefix.

- [ ] **Step 5: Commit**

```bash
git add app/build.gradle app/src/main/AndroidManifest.xml app/src/main/res/values/strings.xml app/src/main/res/xml/file_paths.xml app/src/test
git commit -m "feat: apply StarFlowTV identity and ABI builds"
```

### Task 3: Add a Testable Startup Route Policy

**Files:**
- Create: `app/src/main/java/com/github/tvbox/osc/navigation/StartDestination.java`
- Create: `app/src/main/java/com/github/tvbox/osc/navigation/StartRoutePolicy.java`
- Create: `app/src/test/java/com/github/tvbox/osc/navigation/StartRoutePolicyTest.java`
- Modify: `app/src/main/java/com/github/tvbox/osc/ui/activity/HomeActivity.java`

**Interfaces:**
- Produces: `StartDestination resolve(boolean hasLiveConfig, boolean recoveryMode)` returning `LIVE`, `HOME`, or `SETTINGS`.
- Consumers: `HomeActivity` startup routing.

- [ ] **Step 1: Write failing route tests**

```java
public final class StartRoutePolicyTest {
    private final StartRoutePolicy policy = new StartRoutePolicy();

    @Test public void liveConfigStartsLive() {
        assertEquals(StartDestination.LIVE, policy.resolve(true, false));
    }

    @Test public void missingLiveConfigOpensSettings() {
        assertEquals(StartDestination.SETTINGS, policy.resolve(false, false));
    }

    @Test public void recoveryModeKeepsHomeAvailable() {
        assertEquals(StartDestination.HOME, policy.resolve(true, true));
    }
}
```

- [ ] **Step 2: Verify failure**

```bash
./gradlew :app:testJava32DebugUnitTest --tests '*StartRoutePolicyTest'
```

Expected: FAIL because the policy types do not exist.

- [ ] **Step 3: Implement the pure route policy**

Implement the enum and `resolve` method exactly as exercised by the tests. In `HomeActivity`, apply the policy after configuration initialization, launch `LivePlayActivity` for `LIVE`, retain the current home UI for `HOME`, and open settings for `SETTINGS`.

- [ ] **Step 4: Verify tests and build**

```bash
./gradlew :app:testJava32DebugUnitTest --tests '*StartRoutePolicyTest' :app:assembleJava32Debug
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/github/tvbox/osc/navigation app/src/main/java/com/github/tvbox/osc/ui/activity/HomeActivity.java app/src/test/java/com/github/tvbox/osc/navigation
git commit -m "feat: start directly in live TV"
```

### Task 4: Create the Live-First Navigation Shell

**Files:**
- Modify: `app/src/main/res/layout/activity_live_play.xml`
- Modify: `app/src/main/res/layout/player_live_control_view.xml`
- Modify: `app/src/main/java/com/github/tvbox/osc/ui/activity/LivePlayActivity.java`
- Create: `app/src/main/java/com/github/tvbox/osc/navigation/LiveKeyAction.java`
- Create: `app/src/main/java/com/github/tvbox/osc/navigation/LiveKeyMapper.java`
- Create: `app/src/test/java/com/github/tvbox/osc/navigation/LiveKeyMapperTest.java`

**Interfaces:**
- Produces: `LiveKeyAction map(int keyCode, boolean menuVisible)`.
- Actions: `CHANNEL_PREVIOUS`, `CHANNEL_NEXT`, `LINE_PREVIOUS`, `LINE_NEXT`, `OPEN_CHANNELS`, `OPEN_SETTINGS`, `OPEN_VOD`, `BACK`, `NONE`.

- [ ] **Step 1: Write key-mapping tests**

Cover DPAD up/down, left/right, center/OK, menu, back, and a long-press/menu action for `OPEN_VOD`. Assert that visible menus receive navigation instead of channel changes.

- [ ] **Step 2: Verify failure**

```bash
./gradlew :app:testJava32DebugUnitTest --tests '*LiveKeyMapperTest'
```

- [ ] **Step 3: Implement mapper and wire the activity**

Keep key interpretation in `LiveKeyMapper`; make `LivePlayActivity` translate actions into its existing channel/source operations. Add a secondary “影视” entry that returns to the existing TVBox home/VOD experience without reinitializing live configuration.

- [ ] **Step 4: Verify tests and manual focus behavior**

```bash
./gradlew :app:testJava32DebugUnitTest --tests '*LiveKeyMapperTest' :app:assembleJava32Debug
```

Use an emulator or device to verify every entry is reachable without touch input.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/layout/activity_live_play.xml app/src/main/res/layout/player_live_control_view.xml app/src/main/java/com/github/tvbox/osc/ui/activity/LivePlayActivity.java app/src/main/java/com/github/tvbox/osc/navigation app/src/test/java/com/github/tvbox/osc/navigation
git commit -m "feat: add live-first remote navigation"
```

---

## Milestone 2 — Source Pipeline and Failover

### Task 5: Scaffold StarFlowTV-Sources with a Strict Schema

**Files (sibling repository `../StarFlowTV-Sources`):**
- Create: `pyproject.toml`
- Create: `schemas/channel.schema.json`
- Create: `src/starflow_sources/models.py`
- Create: `src/starflow_sources/validate.py`
- Create: `tests/fixtures/channels.valid.json`
- Create: `tests/fixtures/channels.invalid-secret.json`
- Create: `tests/test_validate.py`

**Interfaces:**
- Produces: `validate_catalog(path: Path) -> ValidationReport`.
- `ValidationReport` fields: `valid: bool`, `errors: list[str]`, `channel_count: int`, `line_count: int`.

- [ ] **Step 1: Write validator tests**

Tests must accept a catalog with channel IDs, groups, logos, EPG IDs, and multiple lines; reject duplicate IDs, missing provenance, literal cookies, token-like query values, and unsupported schemes.

- [ ] **Step 2: Verify failure**

```bash
python -m pytest tests/test_validate.py -q
```

- [ ] **Step 3: Implement schema and validation**

Allow `https`, `http`, `rtsp`, `rtp`, and `udp`. Require each line to declare `scope` as `public`, `geo_limited`, `isp_only`, or `tokenized`; reject secrets from publishable catalogs.

- [ ] **Step 4: Verify**

```bash
python -m pytest tests/test_validate.py -q
```

- [ ] **Step 5: Commit in the sources repository**

```bash
git add pyproject.toml schemas src tests
git commit -m "feat: add strict live source schema"
```

### Task 6: Implement ffprobe-Based Quality Detection

**Files:**
- Create: `../StarFlowTV-Sources/src/starflow_sources/probe.py`
- Create: `../StarFlowTV-Sources/src/starflow_sources/quality.py`
- Create: `../StarFlowTV-Sources/tests/fixtures/ffprobe-2160p.json`
- Create: `../StarFlowTV-Sources/tests/fixtures/ffprobe-1080p.json`
- Create: `../StarFlowTV-Sources/tests/test_quality.py`

**Interfaces:**
- Produces: `probe_stream(line: StreamLine, timeout_seconds: int = 15) -> ProbeResult`.
- Produces: `classify_resolution(width: int, height: int) -> str` returning `8K`, `4K`, `FHD`, `HD`, or `SD`.
- `ProbeResult` includes dimensions, codec, frame rate, bitrate, HDR transfer, audio codec, first-byte time, first-frame time, and error category.

- [ ] **Step 1: Write classification and parser tests**

Assert 7680×4320=`8K`, 3840×2160=`4K`, 1920×1080=`FHD`, 1280×720=`HD`, and 960×540=`SD`. Assert the actual media stream overrides an HLS variant label.

- [ ] **Step 2: Verify failure**

```bash
python -m pytest tests/test_quality.py -q
```

- [ ] **Step 3: Implement bounded subprocess probing**

Invoke `ffprobe` as an argument array, never through a shell. Enforce timeout, cap captured output, parse JSON, redact credentials from diagnostics, and map timeout/DNS/HTTP/decode failures to stable error categories.

- [ ] **Step 4: Verify unit tests and one opt-in network smoke test**

```bash
python -m pytest tests/test_quality.py -q
STARFLOW_NETWORK_TESTS=1 python -m pytest tests/test_network_probe.py -q
```

The network test must skip unless `STARFLOW_NETWORK_TESTS=1` is present.

- [ ] **Step 5: Commit**

```bash
git add src/starflow_sources/probe.py src/starflow_sources/quality.py tests
git commit -m "feat: verify actual stream quality with ffprobe"
```

### Task 7: Generate Ranked M3U, TXT, JSON, and TVBox Artifacts

**Files:**
- Create: `../StarFlowTV-Sources/src/starflow_sources/ranking.py`
- Create: `../StarFlowTV-Sources/src/starflow_sources/render.py`
- Create: `../StarFlowTV-Sources/src/starflow_sources/cli.py`
- Create: `../StarFlowTV-Sources/tests/golden/live.m3u`
- Create: `../StarFlowTV-Sources/tests/golden/live.json`
- Create: `../StarFlowTV-Sources/tests/test_render.py`

**Interfaces:**
- Produces: `score_line(metrics: LineMetrics, device: DeviceProfile) -> int`.
- Produces: `rank_lines(lines: list[StreamLine], device: DeviceProfile) -> list[StreamLine]`.
- Produces: `render_all(catalog: Catalog, output_dir: Path) -> list[Path]`.

- [ ] **Step 1: Write score and golden-output tests**

Assert weights: availability 35, stability 25, first-frame 15, compatibility 15, media quality 10. Assert an unstable 4K line does not outrank a qualified 1080p line, while a 4K line meeting every gate is selected first.

- [ ] **Step 2: Verify failure**

```bash
python -m pytest tests/test_render.py -q
```

- [ ] **Step 3: Implement deterministic ranking and rendering**

Sort channels by configured group/order and lines by qualification, quality tier, score, and stable line ID. Generate `live.m3u`, `live.txt`, `live.json`, `tvbox-live.json`, `manifest.json`, and `checksums.sha256` without timestamps inside content-addressed files.

- [ ] **Step 4: Verify reproducibility**

```bash
python -m pytest -q
tmp_a=$(mktemp -d)
tmp_b=$(mktemp -d)
python -m starflow_sources.cli build --catalog tests/fixtures/channels.valid.json --out "$tmp_a"
python -m starflow_sources.cli build --catalog tests/fixtures/channels.valid.json --out "$tmp_b"
diff -ru "$tmp_a" "$tmp_b"
```

Expected: tests pass and `diff` has no output.

- [ ] **Step 5: Commit**

```bash
git add src tests
git commit -m "feat: generate ranked live configuration artifacts"
```

### Task 8: Add the Android Line Selection and Failover State Machine

**Files:**
- Create: `app/src/main/java/com/github/tvbox/osc/live/LineHealth.java`
- Create: `app/src/main/java/com/github/tvbox/osc/live/LineSelector.java`
- Create: `app/src/main/java/com/github/tvbox/osc/live/FailoverPolicy.java`
- Create: `app/src/main/java/com/github/tvbox/osc/live/FailoverState.java`
- Create: `app/src/test/java/com/github/tvbox/osc/live/LineSelectorTest.java`
- Create: `app/src/test/java/com/github/tvbox/osc/live/FailoverPolicyTest.java`
- Modify: `app/src/main/java/com/github/tvbox/osc/ui/activity/LivePlayActivity.java`

**Interfaces:**
- Produces: `List<LiveChannelItem> rank(List<LiveChannelItem> lines, DeviceCapabilities device, Instant now)`.
- Produces: `FailoverDecision onEvent(PlaybackEvent event, FailoverState state)`.
- `FailoverDecision`: `RETRY_CURRENT`, `TRY_NEXT`, `REFRESH_CONFIG`, `STAY_LOCKED`, `SHOW_EXHAUSTED`, `NONE`.

- [ ] **Step 1: Write line ranking and state-machine tests**

Cover 8-second no-data, 10-second no-first-frame, one retry after an 8-second stall, repeat stall within 5 minutes, immediate 403/404/410 failover, 3 failures in 30 minutes causing a 2-hour circuit break, 3 successful probes restoring a line, 60-second anti-flap window, user lock, and 4-line attempt cap.

- [ ] **Step 2: Verify failure**

```bash
./gradlew :app:testJava32DebugUnitTest --tests 'com.github.tvbox.osc.live.*'
```

- [ ] **Step 3: Implement pure Java domain logic**

Use injected `Clock` values; do not read wall-clock time inside selection logic. Persist device-specific successful line IDs through the existing Hawk storage layer under keys prefixed `starflow_live_`.

- [ ] **Step 4: Wire playback events**

Translate existing player callbacks in `LivePlayActivity` to `PlaybackEvent`s. Keep UI effects in the activity and decisions in `FailoverPolicy`. Trigger a source refresh only after every eligible line is exhausted.

- [ ] **Step 5: Verify**

```bash
./gradlew :app:testJava32DebugUnitTest --tests 'com.github.tvbox.osc.live.*' :app:assembleJava32Debug :app:assembleJava64Debug
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/github/tvbox/osc/live app/src/main/java/com/github/tvbox/osc/ui/activity/LivePlayActivity.java app/src/test/java/com/github/tvbox/osc/live
git commit -m "feat: add deterministic live line failover"
```

---

## Milestone 3 — Safe Updates, Releases, and Device Acceptance

### Task 9: Implement Signed Live Configuration Updates

**Files:**
- Create: `app/src/main/java/com/github/tvbox/osc/update/LiveConfigManifest.java`
- Create: `app/src/main/java/com/github/tvbox/osc/update/ManifestVerifier.java`
- Create: `app/src/main/java/com/github/tvbox/osc/update/LiveConfigStore.java`
- Create: `app/src/main/java/com/github/tvbox/osc/update/LiveConfigUpdater.java`
- Create: `app/src/test/java/com/github/tvbox/osc/update/ManifestVerifierTest.java`
- Create: `app/src/test/java/com/github/tvbox/osc/update/LiveConfigUpdaterTest.java`

**Interfaces:**
- Produces: `VerificationResult verify(byte[] manifest, byte[] signature, PublicKey key)`.
- Produces: `UpdateResult refresh(UpdateReason reason)` where reasons are `SCHEDULED`, `MANUAL`, and `ALL_LINES_FAILED`.
- `LiveConfigStore` operations: `stage`, `validate`, `activate`, `rollback`, `listVersions`.

- [ ] **Step 1: Write verification and rollback tests**

Use fixed Ed25519 test keys. Cover valid signature, tampered manifest, checksum mismatch, empty catalog, lower version, interrupted staging, failed core-channel sample, atomic activation, and preservation of the last three versions.

- [ ] **Step 2: Verify failure**

```bash
./gradlew :app:testJava32DebugUnitTest --tests 'com.github.tvbox.osc.update.*'
```

- [ ] **Step 3: Implement updater and storage**

Use OkHttp with bounded timeouts. Attempt endpoints in manifest order, write into a staging directory, verify before activation, and swap directories atomically. Startup must read the active local version without waiting for network.

- [ ] **Step 4: Connect scheduled and emergency refreshes**

Check at most once every 6 hours during normal startup. `ALL_LINES_FAILED` bypasses the interval once per hour. Add manual update and rollback entries to the existing settings UI.

- [ ] **Step 5: Verify**

```bash
./gradlew :app:testJava32DebugUnitTest --tests 'com.github.tvbox.osc.update.*' :app:assembleJava32Debug
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/github/tvbox/osc/update app/src/test/java/com/github/tvbox/osc/update app/src/main/java/com/github/tvbox/osc/ui/activity/SettingActivity.java app/src/main/res
git commit -m "feat: update live configuration with verification and rollback"
```

### Task 10: Add APK Release Update Checks

**Files:**
- Create: `app/src/main/java/com/github/tvbox/osc/update/AppRelease.java`
- Create: `app/src/main/java/com/github/tvbox/osc/update/AppUpdatePolicy.java`
- Create: `app/src/main/java/com/github/tvbox/osc/update/AppUpdateDownloader.java`
- Create: `app/src/test/java/com/github/tvbox/osc/update/AppUpdatePolicyTest.java`
- Modify: `app/src/main/java/com/github/tvbox/osc/ui/dialog/AboutDialog.java`
- Modify: `app/src/main/res/layout/dialog_about.xml`

**Interfaces:**
- Produces: `Optional<AppRelease> select(UpdateChannel channel, int currentVersionCode, Abi abi, List<AppRelease> releases)`.
- Produces: `DownloadResult downloadAndVerify(AppRelease release, Path destination)`.

- [ ] **Step 1: Write selection tests**

Cover stable/beta channels, no downgrade, ABI preference, universal fallback, checksum mismatch, and unchanged current version.

- [ ] **Step 2: Verify failure**

```bash
./gradlew :app:testJava32DebugUnitTest --tests '*AppUpdatePolicyTest'
```

- [ ] **Step 3: Implement update selection and verified download**

Check no more than once per 24 hours unless manually requested. Use FileProvider to open the Android package installer only after checksum/signature verification. Never request silent-install privileges.

- [ ] **Step 4: Verify**

```bash
./gradlew :app:testJava32DebugUnitTest --tests '*AppUpdatePolicyTest' :app:assembleJava32Debug :app:assembleJava64Debug
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/github/tvbox/osc/update app/src/test/java/com/github/tvbox/osc/update app/src/main/java/com/github/tvbox/osc/ui/dialog/AboutDialog.java app/src/main/res/layout/dialog_about.xml
git commit -m "feat: add verified APK update checks"
```

### Task 11: Add CI for Sources and APK Releases

**Files:**
- Create: `.github/workflows/android-ci.yml`
- Create: `.github/workflows/release.yml`
- Create: `../StarFlowTV-Sources/.github/workflows/source-check.yml`
- Create: `../StarFlowTV-Sources/.github/workflows/publish.yml`
- Create: `../StarFlowTV-Sources/scripts/redact_check.py`
- Create: `../StarFlowTV-Sources/tests/test_redact_check.py`

**Interfaces:**
- Produces: signed source artifacts and checksumed APK assets.
- Consumes: signing keys stored only as repository secrets.

- [ ] **Step 1: Write secret-scanner tests**

Test rejection of cookies, authorization headers, credential-like query values, private RFC1918 hosts in public catalogs, and accepted placeholder fixtures.

- [ ] **Step 2: Verify failure, then implement scanner**

```bash
cd ../StarFlowTV-Sources
python -m pytest tests/test_redact_check.py -q
```

- [ ] **Step 3: Add source workflows**

Run validation and unit tests on every pull request. Schedule public probes every 6 hours. Build deterministic artifacts, scan them, sign the manifest, and publish only when all required checks pass.

- [ ] **Step 4: Add Android workflows**

On pull requests, run unit tests and assemble v7a/v8a variants. On version tags, create universal and ABI-specific release assets, generate SHA-256 files, and publish the release manifest.

- [ ] **Step 5: Validate workflow syntax and local tests**

```bash
cd ../StarFlowTV-Sources && python -m pytest -q
cd ../StarFlowTV && ./gradlew test assembleJava32Debug assembleJava64Debug
```

- [ ] **Step 6: Commit each repository separately**

```bash
git add .github scripts tests
git commit -m "ci: validate and publish live source artifacts"
```

```bash
git add .github
git commit -m "ci: build and publish StarFlowTV releases"
```

### Task 12: Complete End-to-End and Device Acceptance

**Files:**
- Create: `docs/testing/device-acceptance.md`
- Create: `docs/testing/release-checklist.md`
- Create: `docs/configuration/live-sources.md`
- Create: `docs/configuration/tvbox.md`
- Create: `docs/troubleshooting.md`
- Modify: `README.md`

**Interfaces:**
- Produces: repeatable release evidence for TCL 65T8G Max and Xiaomi TV 4X 55.

- [ ] **Step 1: Run all automated verification**

```bash
./gradlew clean test assembleJava32Debug assembleJava64Debug
cd ../StarFlowTV-Sources && python -m pytest -q
```

- [ ] **Step 2: Verify configuration failure modes**

Test offline startup, corrupt manifest, wrong signature, empty catalog, interrupted download, unavailable primary endpoint, exhausted lines, manual rollback, and preservation of VOD configuration.

- [ ] **Step 3: Test TCL 65T8G Max**

Record cold start ≤5 seconds, normal first frame ≤4 seconds, line switch ≤4 seconds, 4-hour continuous playback, 30-minute HEVC 4K playback, suspend/resume, network loss/recovery, and APK upgrade retention.

- [ ] **Step 4: Test Xiaomi TV 4X 55**

Record cold start ≤8 seconds, normal first frame ≤6 seconds, line switch ≤6 seconds, 4-hour continuous playback, HEVC 4K capability result, automatic 1080p fallback, suspend/resume, network loss/recovery, and APK upgrade retention.

- [ ] **Step 5: Complete release documentation**

Document installation by USB and ADB, live-source URL setup, TVBox config import, update channels, rollback, redacted diagnostics, and known device limitations. Do not claim a metric passed unless the corresponding evidence is recorded.

- [ ] **Step 6: Commit**

```bash
git add README.md docs
git commit -m "docs: add configuration and device acceptance evidence"
```

## Final Verification Gate

Run:

```bash
git diff --check
./gradlew clean test assembleJava32Debug assembleJava64Debug
cd ../StarFlowTV-Sources && python -m pytest -q
```

Confirm:

- Both working trees are clean.
- No secret scanner findings remain.
- APK names, application ID, and display name are correct.
- Live startup remains functional when VOD initialization fails.
- Configuration and APK update signatures reject tampering.
- Device acceptance evidence exists for both target televisions.
- GitHub remotes point to repositories owned by the user before any push.
