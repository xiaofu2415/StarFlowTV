# StarFlowTV 1.4.0 official-live verification

Date: 2026-09-08
Scope: source-range inspection of `b1c1ce9..251fc99` before this report's
documentation commit (`3141d5c`)
Status: **BLOCKED/PENDING — external CI and release evidence**

This report distinguishes source-level evidence from build, signed-artifact,
and live-service evidence.  It does not contain a fabricated APK checksum,
certificate digest, signature result, or endpoint result.

## Local execution status

The intended local command is:

```bash
./gradlew :app:testJava32DebugUnitTest \
  :app:assembleJava32Debug \
  :app:assembleJavaDebug \
  :app:assembleJava64Debug \
  --stacktrace
```

It was not re-run for this change because the recorded local wrapper bootstrap
blocker still applies and no complete Gradle 7.5 distribution is cached.  The
wrapper declares `https://services.gradle.org/distributions/gradle-7.5-bin.zip`;
the documented prior attempt in `docs/verification/upstream-baseline.md` stopped
before Gradle configuration with `java.net.SocketException: Network is
unreachable`.  The cache contains only an incomplete `gradle-7.5-bin.zip.part`,
and there is no system Gradle.  This is an environment/network blocker, not
evidence of either a passing or failing Android build.

The environment has a Java runtime (`OpenJDK 17.0.20`) but no `javac` launcher.
That does not mean the JDK compiler is unavailable: prior task evidence used
`java com.sun.tools.javac.Main`. Task 3 recorded a 10-method pure-Java runner
pass for URL-policy/request-tracker checks; Task 4 recorded an initial 19-method
catalog/policy/tracker/retry pass and a separate 9-method error-gate/lifecycle
state pass. Task 6 did **not** re-run those harnesses, so they are historical
focused-check evidence rather than a Task 6 test result. They do not replace a
Gradle/JUnit/Android compilation pass.

No debug or release APK is present beneath `app/build/outputs/apk`. `aapt` and
`apksigner` are not on `PATH`, and no executable copy was discovered under the
configured Android SDK location. They were therefore not invoked against a
nonexistent APK.

## Source-level checks completed

The following range checks completed successfully against the checked-out source
**before the report commit `3141d5c`**:

```bash
git status --short
git diff --check b1c1ce9..HEAD
git diff --stat b1c1ce9..HEAD
git log --oneline b1c1ce9..HEAD
```

- The worktree was clean before adding this report, and `git diff --check
  b1c1ce9..HEAD` reported no whitespace errors.
- The manual refresh baseline remains present: `RemoteConfigManager`,
  `LiveKeyMapper`, and their respective tests exist both at `b1c1ce9` and in
  this checkout. `LivePlayActivity` still contains the manual-refresh dialog
  flow (`startManualLiveConfigRefresh`).
- The range adds the `official` catalog/policy/state classes, the official
  WebView controller, its layout surface, activity integration, focused unit
  tests, version/release notes, and the production-signing workflow update.
- `app/build.gradle` declares `applicationId 'tv.starflow.player'`,
  `versionName '1.4.0'`, and `versionCode 6`. The identity test source asserts
  these values, but it has **not** been run by Gradle in this environment.
- Static, non-Gradle structural assertions verified that the official URL policy
  requires HTTPS, exact `tv.cctv.com` live-page navigation, and rejects query
  strings; the WebView configuration disables file/content access and mixed
  content; replacement/release paths destroy WebViews; and the activity forwards
  pause, resume, and destroy lifecycle events. This is source inspection only,
  not a WebView/device execution result.
- The source declares allowlisted resource hosts under `cctv.com` and
  `cctvpic.com`; main-frame navigation is restricted to the official live-page
  policy. This establishes the implemented policy, not the availability or
  behavior of remote CCTV content.

## Advertising scan (scope and result)

The Gradle declaration files were scanned for common SDK coordinate/name markers
(`admob`, Google/Facebook ads, AppLovin, ironSource, Unity Ads, Vungle,
Chartboost, StartApp, InMobi, Pangle/OpenAdSDK, AdColony, and MoPub). No matching
declaration was returned. The manifest was also scanned for common advertising
permissions (`AD_ID` and Android AdServices permissions); no matching permission
was returned.

This is a source/declaration scan only. Dependency resolution and APK inspection
did not run, so it is not proof that a produced APK contains no advertising SDK.
The source also contains an M3U8 filtering list with ad-server-like host strings;
that is not, by itself, an SDK declaration.

Commands and scan scope:

```bash
# Repository Gradle declaration files and the version catalog if present.
rg -n -i '(admob|google.*ads|facebook.*ads|applovin|ironsource|unityads|vungle|chartboost|startapp|inmobi|pangle|openadsdk|adcolony|mopub)' \
  --glob '*gradle' --glob 'gradle/libs.versions.toml' .

# All source manifests (not a merged manifest).
rg -n -i 'com\.google\.android\.gms\.permission\.AD_ID|android\.permission\.ACCESS_ADSERVICES_ATTRIBUTION|android\.permission\.ACCESS_ADSERVICES_AD_ID|android\.permission\.ACCESS_ADSERVICES_TOPICS|android\.permission\.AD_SERVICES_CONFIG|advertising' \
  app/src/*/AndroidManifest.xml
```

Both commands returned no matching Gradle declaration or source-manifest
permission. Generated manifests, resolved transitive dependencies, and APK
contents were outside this scan because Android/Gradle execution is blocked.

## Artifact and release gates

### Final repair-wave checks (2026-09-08)

The final source repair wave starts from `9646e3d`. A replacement minimal Java
test runner compiled the real catalog/model/state/lease classes with
`java com.sun.tools.javac.Main`, using only temporary JUnit annotation/assertion
and unused Gson-model dependency stubs. **32 focused test methods passed**.
These cover the A/B/C request lease, duplicate completion isolation, foreground
refresh deferral and destroy discard, same-name official-ID selection, offline
proxy fallback, all 21 exact catalog entries, forged/empty same-name groups,
URL policy, request tracking, error gating, and one-retry exhaustion.

Tests were added before implementation. New helper APIs initially failed to
compile because they were absent; that is not presented as a runtime regression
failure. A subsequent deliberate mutation check restored four defective
behaviors (unowned lease release, immediate background execution, name-only
selection, and name-only catalog trust): **6 assertions failed in 13 tests**.
The mutations were removed and the complete 32-test focused suite passed again.

The Android-dependent `ApiConfigOfficialLiveGroupTest` additions are committed
but were **not executed** by this substitute runner. Six integration Java files
were parsed using the JDK syntax parser with no syntax errors; that check does
not resolve Android types or compile the Activity/WebView integration. Manual
inspection confirms failure fallback applies the prepared directory directly
without calling `initLiveChannelList`/remote loading again, and only the acquired
lease's listener releases the refresh lock. This is source inspection, not an
Android runtime test. Gradle, emulator/TV playback, WebView load counts, Toast
display, and Hawk persistence across a process death remain required CI/device
checks. No APK/signing/OTA evidence was created by this repair wave.

Before production signing, verify actual repository branch rules and signing
Environment protections. An external GitHub check on 2026-09-08 reported main
at `48adafb`, `protected=false`, and branch protection `enabled=false`.
The workflow's main-ancestor check does **not** establish that main is protected;
Environment protections remain unconfirmed. This repair wave changes no
repository permissions or external protection settings.

| Evidence | Status | Reason / required evidence |
| --- | --- | --- |
| Gradle unit tests | **PENDING** | Gradle 7.5 download is network-blocked locally; no test execution output. |
| Debug APKs | **PENDING** | No local `assembleJava{,32,64}Debug` run and no APK outputs. |
| Debug APK package/versionName/versionCode/minSdk identity | **PENDING** | No APK exists to inspect with `aapt dump badging`. |
| Each of three signed APKs: package/versionName/versionCode/minSdk | **PENDING** | No signed APKs or `aapt` records exist. This is a distinct release gate. |
| APK SHA-256 | **PENDING** | No APK exists; no checksum has been invented. |
| APK signing and certificate metadata | **PENDING** | Protected keystore/workflow has not run; no `apksigner` result or certificate output. |
| Production signing | **PENDING** | Do not dispatch until the exact ref is merged/eligible and CI evidence is available. Its current `aapt` check only greps package name; it does not automatically assert versionName, versionCode, or minSdk. |
| Update endpoint / OTA readiness | **PENDING** | No request was made to `latest.json`, signatures, certificate metadata, or APK URLs. |

The default source configuration names
`https://update.yuying.beauty/starflow/update/latest.json`; its reachability and
the validity of its release data are not verified by this report.

## Required CI, signing, and device evidence

`baseline.yml` does not list `feat/official-live-1.4.0` under its `push.branches`
(`main`, `feat/starflowtv-foundation`, and `feat/distribution-protocol-v1` only),
and it ignores `docs/**` changes for both push and pull-request events. A plain
push of this feature branch, including the documentation-only commit, will not
automatically run the baseline workflow. Create or update a pull request targeting
`main` that contains the non-documentation official-live diff, or explicitly use
`workflow_dispatch`; then inspect and record the actual run URL, commit SHA, and
artifact/test outcome. Do not treat workflow configuration as a completed run.

Run the baseline workflow or equivalent trusted runner command:

```bash
printf 'sdk.dir=%s\n' "$ANDROID_SDK_ROOT" > local.properties
./gradlew :app:testJava32DebugUnitTest \
  :app:assembleJavaDebug :app:assembleJava32Debug :app:assembleJava64Debug \
  --stacktrace
```

For each produced debug APK, record only actual output from:

```bash
aapt dump badging app/build/outputs/apk/java32/debug/StarFlowTV-1.4.0-java32-debug.apk
aapt dump badging app/build/outputs/apk/java/debug/StarFlowTV-1.4.0-java-debug.apk
aapt dump badging app/build/outputs/apk/java64/debug/StarFlowTV-1.4.0-java64-debug.apk
sha256sum app/build/outputs/apk/*/debug/StarFlowTV-1.4.0-*-debug.apk
```

After CI is green and the release ref is eligible, dispatch
`Production signing (manual)` with `release_ref=main` or an eligible `v*` tag.
The workflow itself gates the ref against `origin/main`, requires all signing
secrets, builds the three release ABIs, runs `zipalign`, runs
`apksigner verify --verbose`, emits `apksigner verify --print-certs`, performs a
package-name-only `aapt` grep, and generates `SHA256SUMS`. It does **not**
currently assert versionName, versionCode, or minSdk. Retain those workflow
artifacts and certificate files as signing evidence, then run and record this
additional pre-publication gate for **each** signed APK:

```bash
for apk in signed-apks/StarFlowTV-1.4.0-*.apk; do
  echo "== $apk =="
  aapt dump badging "$apk" | tee "$apk.badging.txt"
  aapt dump badging "$apk" | grep -E "package: name='tv\.starflow\.player'.*versionCode='6'.*versionName='1\.4\.0'"
  aapt dump badging "$apk" | grep -E "^sdkVersion:'[0-9]+'"
  apksigner verify --verbose "$apk"
  apksigner verify --print-certs "$apk"
done
sha256sum signed-apks/StarFlowTV-1.4.0-*.apk
```

The saved badging output must identify the universal, `armeabi-v7a`, and
`arm64-v8a` outputs separately and record the actual package, versionName,
versionCode, and minSdk. Compare the recorded minSdk to the approved release
floor before publication. Do not put passwords or private keys in this report.

Device acceptance must cover the official live group (21 catalog items), allowed
official-page load, rejected external main-frame navigation, one retry/fallback
path, D-pad/live controls while the WebView is visible, background/foreground,
channel switch, and activity destruction. Record device/Android System WebView
versions, network condition, and actual pass/fail observations.

Finally, fetch the configured update manifest and its adjacent signatures, check
the signed manifest/certificate metadata against the signing artifact, and fetch
every published APK URL over HTTPS. OTA readiness remains blocked until all of
those requests succeed and the client acceptance policy validates them.
