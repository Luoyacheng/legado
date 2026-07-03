![icon_android](https://gitee.com/lyc486/yuedu/raw/master/icon_android.png)
<a href="https://jb.gg/OpenSourceSupport" target="_blank">
<img width="24" height="24" src="https://resources.jetbrains.com/storage/products/company/brand/logos/jb_beam.svg?_gl=1*135yekd*_ga*OTY4Mjg4NDYzLjE2Mzk0NTE3MzQ.*_ga_9J976DJZ68*MTY2OTE2MzM5Ny4xMy4wLjE2NjkxNjMzOTcuNjAuMC4w&_ga=2.257292110.451256242.1669085120-968288463.1639451734" alt="idea"/>
</a>

<div align="center">
<img width="125" height="125" src="https://gitee.com/lyc486/legado/raw/main/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="legado"/>
<br>
阅读Sigma
<br>
阅读Sigma继承自<a href="https://github.com/gedoor/legado" target="_blank">Legado</a>，延续开源阅读项目，在其基础上新增更多功能。
</div>

## 版本说明
- 测试版(beta)：包名与Legado原版相同，可覆盖更新，版本更新频繁
- 正式版(plus)：新的共存包名，安装后是一个新软件，不会覆盖Legado原版，每到一个稳定阶段进行一次更新
#### 找不到下载地址可以去这里 [下载软件](https://gitee.com/lyc486/legado/releases)

[![](https://img.shields.io/badge/-Contents:-696969.svg)](#contents) [![](https://img.shields.io/badge/-Function-F5F5F5.svg)](#Function-主要功能-) [![](https://img.shields.io/badge/-Community-F5F5F5.svg)](#Community-交流社区-) [![](https://img.shields.io/badge/-API-F5F5F5.svg)](#API-) [![](https://img.shields.io/badge/-Other-F5F5F5.svg)](#Other-其他-) [![](https://img.shields.io/badge/-Grateful-F5F5F5.svg)](#Grateful-感谢-) [![](https://img.shields.io/badge/-Interface-F5F5F5.svg)](#Interface-界面-)

>新用户？
>
>软件不提供内容，需要您自己手动添加，例如导入书源等。  
>看看 [帮助文档](https://www.yuque.com/legado/wiki)，也许里面就有你要的答案。

# Function-主要功能 [![](https://img.shields.io/badge/-Function-F5F5F5.svg)](#Function-主要功能-)

<details><summary>原版</summary>
1.自定义书源，自己设置规则，抓取网页数据，规则简单易懂，软件内有规则说明。<br>
2.列表书架，网格书架自由切换。<br>
3.书源规则支持搜索及发现，所有找书看书功能全部自定义，找书更方便。<br>
4.订阅内容,可以订阅想看的任何内容,看你想看<br>
5.支持替换净化，去除广告替换内容很方便。<br>
6.支持本地TXT、EPUB阅读，手动浏览，智能扫描。<br>
7.支持高度自定义阅读界面，切换字体、颜色、背景、行距、段距、加粗、简繁转换等。<br>
8.支持多种翻页模式，覆盖、仿真、滑动、滚动等。<br>
9.软件开源，持续优化，无广告。
</details>

<details><summary>延续</summary>
1.带歌词的音频播放功能。<br>
2.带弹幕的视频播放功能。<br>
3.更方便的书源编辑。<br>
4.更丰富的书源功能实现。<br>
5.更完善的订阅源功能。<br>
</details>

<a href="#readme">
    <img src="https://img.shields.io/badge/-返回顶部-orange.svg" alt="#" align="right">
</a>

# Community-交流社区 [![](https://img.shields.io/badge/-Community-F5F5F5.svg)](#Community-交流社区-)

#### Telegram
[![Telegram-channel](https://img.shields.io/badge/Σ_Telegram-%E9%A2%91%E9%81%93-blue)](https://t.me/readsigma)

#### WeChat
[![WeChat-channel](https://img.shields.io/badge/Σ_%e5%be%ae%e4%bf%a1-%e5%85%ac%e4%bc%97%e5%8f%b7-green)](https://mp.weixin.qq.com/s/f54f7yP9HQi6P5Wky8wE1A)  
<img src="https://open.weixin.qq.com/qr/code?username=legado_plus" width="100">

#### Discord
[![Discord](https://img.shields.io/discord/560731361414086666?color=%235865f2&label=Discord)](https://discord.gg/VtUfRyzRXn)

#### Other
https://www.yuque.com/legado/wiki/community

<a href="#readme">
    <img src="https://img.shields.io/badge/-返回顶部-orange.svg" alt="#" align="right">
</a>

# API [![](https://img.shields.io/badge/-API-F5F5F5.svg)](#API-)
* 阅读3.0 提供了2种方式的API：`Web方式`和`Content Provider方式`。您可以在[这里](api.md)根据需要自行调用。 
* 可通过url唤起阅读进行一键导入,url格式: legado://import/{path}?src={url}
* path类型: bookSource,rssSource,replaceRule,textTocRule,httpTTS,theme,readConfig,dictRule,[addToBookshelf](/app/src/main/java/io/legado/app/ui/association/AddToBookshelfDialog.kt)
* path类型解释: 书源,订阅源,替换规则,本地txt小说目录规则,在线朗读引擎,主题,阅读排版,添加到书架

<a href="#readme">
    <img src="https://img.shields.io/badge/-返回顶部-orange.svg" alt="#" align="right">
</a>

# Other-其他 [![](https://img.shields.io/badge/-Other-F5F5F5.svg)](#Other-其他-)
##### 免责声明

阅读依赖系统webview提供网页访问功能，通过用户自定义的第三方网页书源返回阅读内容，阅读对其返回内容概不负责，亦不承担任何法律责任。任何第三方网页书源均系他人制作或提供，非软件作者，阅读对其合法性概不负责，亦不承担任何法律责任。第三方网页书源提供的试读，不代表阅读赞成其内容或立场。您应该对用搜索到的结果自行承担风险。

任何单位或个人认为第三方网页书源内容可能涉嫌侵犯其信息网络传播权，应该及时向阅读提出书权力通知，并提供身份证明、权属证明及详细侵权情况证明。阅读在收到上述法律文件后，将会依法尽快屏蔽相关内容。

##### 阅读3.0
* [书源规则](https://mgz0227.github.io/The-tutorial-of-Legado/)
* [更新日志](/app/src/main/assets/updateLog.md)
* [帮助文档](/app/src/main/assets/web/help/md/appHelp.md)

<a href="#readme">
    <img src="https://img.shields.io/badge/-返回顶部-orange.svg" alt="#" align="right">
</a>

# Grateful-感谢 [![](https://img.shields.io/badge/-Grateful-F5F5F5.svg)](#Grateful-感谢-)
> * org.jsoup:jsoup
> * cn.wanghaomiao:JsoupXpath
> * com.jayway.jsonpath:json-path
> * com.github.gedoor:rhino-android
> * com.squareup.okhttp3:okhttp
> * com.github.bumptech.glide:glide
> * org.nanohttpd:nanohttpd
> * org.nanohttpd:nanohttpd-websocket
> * cn.bingoogolapple:bga-qrcode-zxing
> * com.jaredrummler:colorpicker
> * org.apache.commons:commons-text
> * io.noties.markwon:core
> * io.noties.markwon:image-glide
> * com.hankcs:hanlp
> * com.positiondev.epublib:epublib-core
> * com.github.Moriafly:LyricViewX
> * io.github.rosemoe:editor
<a href="#readme">
    <img src="https://img.shields.io/badge/-返回顶部-orange.svg" alt="#" align="right">
</a>

# Interface-界面 [![](https://img.shields.io/badge/-Interface-F5F5F5.svg)](#Interface-界面-)
<img src="https://channel.qpic.cn/psc?/channel/NaDwC23LjvXqrn3RH.9z8bdVVU8X7gejuELokmv2DGfJwQpP2UFcwHqqFK4nzfgNUTL6Wij4QqsJySkoPfQuCB1l5MstXCXWjmH5Mm6PGkU!/b=&bo=UQRICVEESAkWADA!&ek=1&tl=1" width="270">  <img src="https://channel.qpic.cn/psc?/channel/NaDwC23LjvXqrn3RH.9z8epqbe*Bzro*ybEEVmlLpuD9E14zFMjtRx2JeZJdVzNYeSZqCm5Kv0MruAumEe0tJ77n*agIEzJIaSN9VHAen2I!/b=&bo=UQRICVEESAkWADA!&ek=1&tl=1" width="270">  
<img src="https://channel.qpic.cn/psc?/channel/NaDwC23LjvXqrn3RH.9z8SQggDiubhwWyktI5v48VluvK1vwdY2OV9RauO04TcXb4425kVcBiAz2DhpfnrDZKnSpwp8SLnDtKOxtLm1yP9I!/b=&bo=UQRICVEESAkWADA!&ek=1&tl=1" width="270">  <img src="https://channel.qpic.cn/psc?/channel/NaDwC23LjvXqrn3RH.9z8Tx.7oqZyCeAdzSVQa6ZgtXKE*EaUCdEJ3zm2ynIrgem10TqY8aKvSbG3v5FNNHtwSFyN5QfLW*Zd14gUetfsC8!/b=&bo=UgRKCVIESgkWADA!&ek=1&tl=1" width="270">

<a href="#readme">
    <img src="https://img.shields.io/badge/-返回顶部-orange.svg" alt="#" align="right">
</a>
