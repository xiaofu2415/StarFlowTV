# StarFlowTV v1.3.0 生产签名运行手册

本文只描述生产签名的安全接线，不包含任何私钥。当前状态仍为 `production-signing-pending`，没有配置真实密钥时不得把 APK 或配置标为 OTA 可安装。

## 需要配置的 GitHub Actions Secrets

在仓库的受保护环境或 Secrets 中配置以下值；不要把值写入代码、Issue、PR、构建日志或 VPS：

- `STARFLOW_ANDROID_KEYSTORE_B64`：Android production keystore 的 base64 内容。
- `STARFLOW_ANDROID_KEYSTORE_PASSWORD`：keystore 密码。
- `STARFLOW_ANDROID_KEY_ALIAS`：发布 key alias。
- `STARFLOW_ANDROID_KEY_PASSWORD`：发布 key 密码。
- `STARFLOW_SIGNING_KEY_ID`：公开的签名 key id，例如 `starflow-production-1`。
- `STARFLOW_ED25519_PRIVATE_KEY_B64`：仅用于在受保护环境生成 `manifest.sig` / `latest.sig`；若未配置，配置签名同样保持 pending。

Android keystore 应在线下生成并保存离线备份。Actions 只在受保护分支手动触发 `.github/workflows/production-signing.yml`；workflow 会用 `apksigner` 验证包名、版本、证书和 SHA-256，并上传签名 APK 与公开证书摘要。

## 完成条件

1. 三个 ABI APK 都通过 `apksigner verify`。
2. `aapt`/`apkanalyzer` 显示 `tv.starflow.player`、`versionName 1.3.0`、`versionCode 4`。
3. 三个 APK 的 SHA-256 写入 release bundle 和 `latest.json`。
4. `latest.json.signingStatus` 不再是 `production-signing-pending`，且 certificate digest 与签名 APK 一致。
5. `manifest.sig` 与 `latest.sig` 使用对应 Ed25519 私钥生成，客户端内置公钥和 `signingKeyId` 一致。
6. 在未完成以上条件前，客户端必须拒绝 OTA 安装，只允许手动安装 debug APK 做实机验收。

## 本地检查命令（不上传私钥）

```bash
apksigner verify --verbose StarFlowTV-1.3.0-armeabi-v7a.apk
apksigner verify --verbose StarFlowTV-1.3.0-arm64-v8a.apk
apksigner verify --verbose StarFlowTV-1.3.0-universal.apk
sha256sum StarFlowTV-1.3.0-*.apk
```

证书摘要只记录 `apksigner verify --print-certs` 输出中的 SHA-256；不记录 keystore、密码或私钥。

## 为什么当前仍不能宣布完成

当前仓库和 CI 没有可读取的 production keystore、Ed25519 私钥及证书摘要。使用 debug 证书或随机摘要会导致升级校验失败，也会破坏已安装版本的签名连续性，因此本项目不会这样做。