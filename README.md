# StarFlowTV

StarFlowTV 是面向 Android TV 的个人使用播放器：启动后直接进入直播，支持遥控器换台、多线路、自动换源与本地缓存；影视作为二级入口接入 TVBoxOS 点播能力。

> 当前稳定版本：**v1.3.1**（versionCode 5）。PR #1 已合并到 main，生产签名和在线更新清单已就绪。

## 当前状态

| 项目 | 状态 |
| --- | --- |
| applicationId | tv.starflow.player |
| versionName / versionCode | 1.3.1 / 5 |
| 代码分支 | main |
| PR | #1 merged |
| 生产签名 | ready；keyId: starflow-production-2026-01 |
| 配置清单 | configVersion 5，Ed25519 + SHA-256 |

## 功能

- 直播首页：上下换台，左右切换同频道线路，OK 打开频道列表。
- 多线路健康策略：首帧超时、播放中断、失败熔断、恢复探测和自动换源。
- 本地优先：启动先使用最后一个可用配置；远程配置在后台检查。
- 配置安全：HTTPS、Ed25519 签名、SHA-256、Schema 校验、原子替换、失败回滚，保留最近 3 个版本。
- 影视入口：通过 TVBoxOS 配置进入点播页面；直播仍是默认首页。
- APK 更新：按设备 ABI 选择 armeabi-v7a / arm64-v8a，无法匹配时回退 universal；拒绝降级、包名错误、签名证书错误和 SHA-256 错误。

## 在线配置与软件更新

客户端内置目标地址：

- 直播配置：https://config.yuying.beauty/starflow/config/manifest.json
- 软件更新：https://update.yuying.beauty/starflow/update/latest.json

当前已部署的在线配置包含 live.json、live.m3u、live.txt、tvbox-live.json、epg.xml、manifest.json、manifest.sig 和 checksums.sha256。当前 OTA 清单为 v1.3.1 / versionCode 5，包含 armeabi-v7a、arm64-v8a 和 universal 三个生产签名 APK。

在线更新会先校验 HTTPS、Ed25519、SHA-256、Schema 和版本策略，再原子切换；下载或校验失败不会破坏当前版本。

## 设备与安装

- TCL 65T8G Max 65 英寸：优先使用 arm64-v8a；若系统为 32 位则使用 armeabi-v7a。
- 小米电视 4X 55 英寸：根据系统 ABI 选择 armeabi-v7a 或 universal。
- 旧的 debug 版不能覆盖生产签名版。若电视上仍安装 1.3.0 debug，请先卸载旧应用，再安装生产签名的 v1.3.1 APK；后续版本即可通过 OTA 升级。

## 构建与验证

Android CI 会构建三种 ABI 的 debug/release 产物；生产发布作业会重新签名、验证证书、生成 SHA256SUMS 和 OTA 清单。

Sources pipeline 会执行候选源审查、去重、网络检测、ffprobe 画质检测、排序、manifest 和 checksums 校验。逐条 URL、来源、地域范围和检测记录只保存在私有 StarFlowTV-Sources 仓库。

## 分发架构

- GitHub Actions / Codex：搜集、清洗、检测、ffprobe、配置生成和 APK 构建。
- VPS / 1Panel：只提供 Nginx/静态 HTTPS 文件，不运行检测器、Worker、转码、直播代理、数据库或缓存服务。
- 生产私钥只放在受保护的 CI Secret 中；VPS 只接收公开验证信息、签名文件、APK 和校验文件。

## 相关链接

- StarFlowTV-Sources（私有）：https://github.com/xiaofu2415/StarFlowTV-Sources
- Android Actions：https://github.com/xiaofu2415/StarFlowTV/actions
- Releases：https://github.com/xiaofu2415/StarFlowTV/releases

## 安全与使用边界

本项目不绕过 DRM、登录、付费墙或地区限制，不收集或提交凭据。直播地址可能受版权、地域、运营商和上游变更影响；使用前请以 Sources 仓库中的 provenance、scope 和检测时间为依据。