# StarFlowTV

StarFlowTV 是面向 Android TV 的个人使用播放器：启动后直接进入直播，支持遥控器换台、多线路、自动换源与本地缓存；影视作为二级入口接入 TVBoxOS 点播能力。

> 当前稳定版本：**v1.4.7**（versionCode 13）。生产签名、在线配置和 OTA 发布链路均已启用。

## 当前状态

| 项目 | 状态 |
| --- | --- |
| applicationId | tv.starflow.player |
| versionName / versionCode | 1.4.7 / 13 |
| 代码分支 | main |
| 生产签名 | ready；keyId: starflow-production-2026-09-r2 |
| APK 架构 | armeabi-v7a / arm64-v8a / universal |
| 软件更新 | 设置页手动检查 + 直播页后台定时检查 |
| 官方直播 | CCTV 官方网页入口支持自动进入沉浸式全屏播放 |

## 功能

- 直播首页：上下换台，左右切换同频道线路，OK 打开频道列表。
- 多线路健康策略：首帧超时、播放中断、失败熔断、恢复探测和自动换源。
- 本地优先：启动先使用最后一个可用配置；远程配置在后台检查。
- 配置安全：HTTPS、Ed25519 签名、SHA-256、Schema 校验、原子替换、失败回滚，保留最近 3 个版本。
- 官方直播：保留普通直播线路，同时提供央视官方入口；官方网页播放器加载后自动尝试进入全屏/沉浸式播放。
- 官方源画质：直播设置中可选最高 / 自动 / 1080P / 720P / 流畅；指定档位不可用时向下回退，并短暂显示实际播放分辨率。
- 影视入口：通过 TVBoxOS 配置进入点播页面；直播仍是默认首页。
- APK 更新：按设备 ABI 选择 armeabi-v7a / arm64-v8a，无法匹配时回退 universal；拒绝降级、包名错误、签名证书错误和 SHA-256 错误。
- OTA 检查：设置页提供“检查更新”；进入直播页后约 5 秒首次检查，前台播放期间约每 30 分钟复查一次。
- OTA 反馈：手动检查会显示“已是最新版”“网络错误”“签名错误”“设备 ABI 不支持”等结果，不再静默失败。

## 在线配置与软件更新

客户端内置目标地址：

- 直播配置：https://config.yuying.beauty/starflow/config/manifest.json
- 软件更新：https://update.yuying.beauty/starflow/update/latest.json

在线配置包含 live.json、live.m3u、live.txt、tvbox-live.json、epg.xml、manifest.json、manifest.sig 和 checksums.sha256。当前生产 OTA 由 GitHub Actions 构建、生产密钥签名并发布到 1Panel 静态 HTTPS 目录，同时生成 latest.json / latest.sig / latest.sha256 和三种 ABI 的生产 APK。

在线更新会先校验 HTTPS、Ed25519、SHA-256、版本策略、包名和 APK 签名证书，再进入 Android 系统安装确认页；下载或校验失败不会破坏当前版本。

## v1.4.5 → v1.4.6 说明

v1.4.6 新增设置页“检查更新”、直播页周期检查，并修复 release 构建中 OTA 清单解析/生命周期处理。若电视仍停留在 v1.4.5 且没有“检查更新”入口、也收不到自动更新，请手动安装一次生产签名的 v1.4.6；从 v1.4.6 开始，后续版本即可继续走应用内 OTA 更新。

## 设备与安装

- TCL 65T8G Max 65 英寸：优先使用 arm64-v8a；若系统用户空间为 32 位则使用 armeabi-v7a。
- 小米电视 4X 55 英寸：根据系统 ABI 选择 armeabi-v7a 或 universal。
- 旧 debug 版不能直接覆盖生产签名版；生产版之间要求 applicationId 与签名证书保持一致。
- 若 Android 系统安装器/包验证器拒绝 ADB 安装，可将已签名 APK 复制到电视并从系统文件管理器发起安装；这不影响 OTA 文件本身的签名和校验状态。

## 构建与验证

Android CI 会构建三种 ABI 的 debug/release 产物；生产发布作业会重新签名、验证 APK 包名/versionCode/versionName、生成 SHA256SUMS、生成 Ed25519 OTA 清单，并在发布后从公网重新读取 latest.json、latest.sig、manifest.json、manifest.sig 进行逐字节比对，同时检查三个 APK 下载地址可访问。

官方直播自动全屏脚本与 OTA 更新策略均有独立测试，并纳入生产发布工作流。

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
