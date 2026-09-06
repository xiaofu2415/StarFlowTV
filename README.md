# StarFlowTV

StarFlowTV 是面向 Android TV 的个人使用播放器：启动后直接进入直播，支持遥控器换台、多线路、自动换源与本地缓存；“影视”作为二级入口接入 TVBoxOS 点播能力。

> 当前版本：**v1.3.0 Release Candidate**（versionCode 4）。RC 尚未合并到 `main`，也尚未发布 Stable Release。

## 当前状态

| 项目 | 状态 |
| --- | --- |
| applicationId | `tv.starflow.player` |
| versionName / versionCode | `1.3.0` / `4` |
| Android CI | [Run #28：成功](https://github.com/xiaofu2415/StarFlowTV/actions/runs/34067071801) |
| 代码分支 | `feat/formal-release-1.3.0-20260906` |
| PR | [#1](https://github.com/xiaofu2415/StarFlowTV/pull/1)，open，未合并 |
| 生产签名 | `production-signing-pending` |

## 功能

- 直播首页：上下换台，左右切换同频道线路，OK 打开频道列表。
- 多线路健康策略：首帧超时、播放中断、失败熔断、恢复探测和自动换源。
- 本地优先：启动先使用最后一个可用配置；远程配置在后台检查。
- 配置安全：HTTPS、Ed25519 签名、SHA-256、Schema 校验、原子替换、失败回滚，保留最近 3 个版本。
- 影视入口：通过 TVBoxOS 配置进入点播页面；直播仍是默认首页。
- APK 更新：按设备 ABI 选择 v7a / v8a，无法匹配时回退 universal；拒绝降级、包名错误、签名证书错误和 SHA-256 错误。

## 在线配置与软件更新

客户端内置的目标地址为：

- 直播配置：`https://config.yuying.beauty/starflow/config/manifest.json`
- 软件更新：`https://update.yuying.beauty/starflow/update/latest.json`

在线更新的实际流程是：客户端先加载本地最后可用版本，再后台读取 manifest；只有 HTTPS、Ed25519、SHA-256、Schema 和版本策略全部通过后才原子切换。配置或 APK 下载失败不会破坏当前版本。

**当前限制：** VPS 静态文件尚未由本仓库自动发布；`latest.json` 仍标记为 `production-signing-pending`，因此生产 APK 不会触发 OTA 安装。debug APK 可用于电视实机验收。不要把 GitHub Token、Cookie、账号凭据或私有直播地址写入 APK 或静态目录。

## 当前直播目录

正式候选目录来自私有维护仓库 [StarFlowTV-Sources](https://github.com/xiaofu2415/StarFlowTV-Sources) 的 `feat/community-source-formal-release-20260906` 分支，最近一次 Source Checks 的基线为：

| 指标 | 数量 |
| --- | ---: |
| 频道 | 171 |
| 线路 | 194 |
| FHD（1920×1080 及以上） | 185 |
| HD（1280×720） | 6 |
| 实测 4K（3840×2160） | 3 |
| 实测 8K | 0 |

频道分组：央视及国际 2、海外公开 6、卫视 4、央视频道 6、卫视频道 16、电影 19、数字 1、儿童 1、地方 76、纪录 6、解说 1、春晚 1、直播中国 32。逐条 URL、来源、地域范围、最近检测时间和 ffprobe 结果只保存在私有 Sources 仓库；频道名或 M3U 标签不会被当作 4K 证据。

## 设备与安装

- TCL 65T8G Max 65 英寸：优先使用 arm64-v8a。
- 小米电视 4X 55 英寸：根据系统 ABI 选择 armeabi-v7a 或 universal。
- [Android CI 构建产物](https://github.com/xiaofu2415/StarFlowTV/actions/runs/34067071801)包含三种 debug/release ABI 产物；在生产签名完成前，release APK 仅用于签名和打包检查。

## 构建与验证

```bash
./gradlew :app:testJava32DebugUnitTest
./gradlew :app:assembleJavaDebug :app:assembleJava32Debug :app:assembleJava64Debug
./gradlew :app:assembleJavaRelease :app:assembleJava32Release :app:assembleJava64Release
```

Source pipeline 会执行候选源审查、去重、网络检测、ffprobe 画质检测、排序、manifest 和 checksums 校验。最近一次 [Source Checks #25](https://github.com/xiaofu2415/StarFlowTV-Sources/actions/runs/34066946133) 成功。

## 分发架构

- GitHub Actions / Codex：搜集、清洗、检测、ffprobe、配置生成和 APK 构建。
- VPS / 1Panel：只提供 Nginx/静态 HTTPS 文件，不运行检测器、Worker、转码、直播代理、数据库或缓存服务。
- 生产私钥只放在受保护的 CI Secret 中；VPS 只接收公开验证信息、签名文件、APK 和校验文件。

## 相关链接

- [StarFlowTV-Sources（私有）](https://github.com/xiaofu2415/StarFlowTV-Sources)
- [Release Candidate PR #1](https://github.com/xiaofu2415/StarFlowTV/pull/1)
- [Android Actions](https://github.com/xiaofu2415/StarFlowTV/actions)
- [v1.3.0 RC 发布说明](docs/release-notes-v1.3.0-rc.md)
- [生产签名运行手册](docs/production-signing-v1.3.0.md)
- [手动生产签名 workflow](.github/workflows/production-signing.yml)

## 安全与使用边界

本项目不绕过 DRM、登录、付费墙或地区限制，不收集或提交凭据。直播地址可能受版权、地域、运营商和上游变更影响；Source 仓库中的 provenance、scope 和检测时间是使用前的依据。