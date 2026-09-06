<<<<<<<<<<<<<<json接口示例>>>>>>>>>>>>>
# TVBox

=== Source Code - Editing the app default settings ===

    //searchable:搜索开关	        0:关闭 1:启用
    //filterable:首页可选	        0:否 1:是
    //playerType:播放器类型	        0:系统 1:IJK 2:EXO
    //采集接口类型		        0:xml 1:json 3:jar 4:remote
    //parses解析类型		        0:嗅探,自带播放器 1:解析,返回直链
    //直播参数说明                   ua:用户自定义ua epg:节目网址 logo:台标网址
    
    {
	"spider": "./your.jar",
	"wallpaper": "./api/img",
	"sites": [],
	"parses": [],
	"hosts": [
		"cache.ott.ystenlive.itv.cmvideo.cn=base-v4-free-mghy.e.cdn.chinamobile.com",
		"cache.ott.bestlive.itv.cmvideo.cn=ip"
	],
	"lives": [],
	"rules": [],
	"doh": [
		{
			"name": "騰訊",
			"url": "https://doh.pub/dns-query"
		},
		{
			"name": "阿里",
			"url": "https://dns.alidns.com/dns-query"
		},
		{
			"name": "360",
			"url": "https://doh.360.cn/dns-query"
		}
	]
    }

## StarFlowTV signed distribution

The current release candidate is `1.3.0` (`versionCode` 4). CI produces installable debug APKs
for TV validation plus release APKs for signing/packaging review in the same run. A release APK is
not OTA-ready until it is signed with the project production key and its certificate digest is
published in `latest.json`; no private key is committed here.

The app immediately uses the last activated local live configuration, then checks
`https://config.yuying.beauty/starflow/config/manifest.json` in the background. A new bundle is
activated only after HTTPS, Ed25519, SHA-256, and client schema verification. Three versions are
retained and a failed update cannot replace the active version.

APK updates use `https://update.yuying.beauty/starflow/update/latest.json`, select the device ABI,
and verify the signed manifest, APK digest, package name, and signing certificate before opening
the Android system installer. `latest.json` carries `minSupportedVersionCode`, `mandatory`,
`packageName`, `apkVariants`, `signingKeyId`, and the explicit `signingStatus`; a
`production-signing-pending` manifest is rejected and cannot trigger an OTA install. Defaults can
be changed at build time without committing secrets:

```bash
./gradlew :app:assembleJavaDebug \
  -PstarflowLiveUrl=https://config.example/starflow/config/live.txt \
  -PstarflowConfigManifestUrl=https://config.example/starflow/config/manifest.json \
  -PstarflowUpdateUrl=https://update.example/starflow/update/latest.json \
  -PstarflowSigningPublicKey='<base64 Ed25519 public key>' \
  -PstarflowSigningKeyId=starflow-production-1 \
  -PstarflowUpdateChannel=stable
```

The signing private key belongs in a CI secret store and never in this repository, the APK, or the
static VPS. The public app repository never contains private playlists or account credentials.
