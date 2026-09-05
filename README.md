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

## StarFlowTV private distribution endpoint

The application starts with the live configuration at
`https://tv.yuying.beauty/live/live.txt` and checks
`https://tv.yuying.beauty/app/update.json` for a newer APK. A user's saved live URL always wins.
Both defaults can be changed at build time without committing credentials:

```bash
./gradlew :app:assembleJavaDebug \
  -PstarflowLiveUrl=https://tv.example/live/live.txt \
  -PstarflowUpdateUrl=https://tv.example/app/update.json
```

Update manifests and APK downloads must use credential-free HTTPS on the same origin. The APK is
installed only after its declared byte length and SHA-256 digest are verified. The public app
repository never contains private playlists or a GitHub token.
