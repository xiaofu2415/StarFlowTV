# TVBoxOS upstream baseline verification

Date: 2026-09-05

## Baseline

- Repository: `https://github.com/q215613905/TVBoxOS.git`
- Commit: `ccc25f67ce35c6699529d493177c737be3ad4ba4`
- Branch: `feat/starflowtv-foundation`
- Java runtime: OpenJDK available in the execution environment
- Gradle wrapper: 7.5

## Command

```bash
./gradlew :app:assembleJava32Debug :app:assembleJava64Debug --stacktrace
```

## Result

The first attempt stopped before Gradle startup because the upstream `gradlew`
file was committed with mode `100644`. Its mode was corrected to `100755`.

The second attempt reached the wrapper bootstrap and stopped before project
configuration because the environment could not connect to
`https://services.gradle.org/distributions/gradle-7.5-bin.zip`:

```text
java.net.SocketException: Network is unreachable
```

The local Gradle cache contains only an incomplete
`gradle-7.5-bin.zip.part`; no system Gradle or complete Gradle 7.5 distribution
is installed. Therefore this result does not indicate an Android source or
dependency failure. No product source changes were made before recording the
baseline.

GitHub Actions run `33964653758` successfully downloaded Gradle 7.5 and then
failed while configuring project `:pyramid` because a clean runner did not
contain the untracked root `local.properties` file:

```text
A problem occurred configuring project ':pyramid'.
> Failed to notify project evaluation listener.
   > local.properties (No such file or directory)
```

The workflow must generate `local.properties` from `ANDROID_SDK_ROOT` before
Gradle configuration. This file remains untracked and contains no credential.

GitHub Actions run `33964764599` generated that file from the runner's
`ANDROID_SDK_ROOT`, built both requested variants, and uploaded their APKs and
reports successfully:

```text
:app:assembleJava32Debug   success
:app:assembleJava64Debug   success
```

Baseline verification is complete. Future code changes are checked by
`.github/workflows/baseline.yml`.
