# 源规则帮助

* [阅读3.0(Legado)规则说明](https://mgz0227.github.io/The-tutorial-of-Legado/)　
* [书源帮助文档](https://mgz0227.github.io/The-tutorial-of-Legado/Rule/source.html)　
* [订阅源帮助文档](https://mgz0227.github.io/The-tutorial-of-Legado/Rule/rss.html)　
* 辅助键盘❓中可插入URL参数模板,打开帮助,js教程,正则教程,选择文件
* 规则标志, {{......}}内使用规则必须有明显的规则标志,没有规则标志当作js执行
```
@@ 默认规则,直接写时可以省略@@
@XPath: xpath规则,直接写时以//开头可省略@XPath
@Json: json规则,直接写时以$.开头可省略@Json
: regex规则,不可省略,只可以用在书籍列表和目录列表
```
* jsLib
> 注入JavaScript到RhinoJs引擎中，支持两种格式，可实现[函数共用](https://github.com/gedoor/legado/wiki/JavaScript%E5%87%BD%E6%95%B0%E5%85%B1%E7%94%A8)　

> `JavaScript Code` 直接填写JavaScript片段  
> `{"example":"https://www.example.com/js/example.js", ...}` 自动复用已经下载的js文件

> 注意此处定义的函数可能会被多个线程同时调用，在函数里的全局变量内容将会共享使用，对其进行修改可能会出现竞争问题
> 函数内声明全局变量必须使用var

* 并发率
> 并发限制，单位ms，可填写两种格式

> `1000` 访问间隔1s  
> `20/60000` 60s内访问次数20  
> `source.putConcurrent(str: String)` 更改并发率(书源的并发率规则不为空时才生效)

* 书源类型: 文件
> 对于类似知轩藏书提供文件整合下载的网站，可以在书源详情的下载URL规则获取文件链接

> 通过截取下载链接或文件响应头头获取文件信息，获取失败会自动拼接`书名` `作者`和下载链接的`UrlOption`的`type`字段

> 压缩文件解压缓存会在下次启动后自动清理，不会占用额外空间  

* 书源类型: 音频
> 将正文获得的字符串作为音频链接，返回序列化后的链接数组会将多个链接拼接成一条音频。

* CookieJar
> 启用后会自动保存每次返回头中的Set-Cookie中的值，适用于验证码图片一类需要session的网站

* 登录UI
> 不使用内置webView登录网站，需要使用`登录URL`规则实现登录逻辑，可使用`登录检查JS`检查登录结果  
> 版本20221113重要更改：按钮支持调用`登录URL`规则里面的函数，必须实现`login`函数  
> 版本20260131：文本输入类型支持`action`键，在用户完成输入后执行js函数，用来判断用户输入内容并进行提示  
文本类输入需用户主动打勾保存，或调用java.upLoginData更新
```
//所有按钮类型："text"、"password"、"button"、"toggle"、"select"
规则填写示范
[
    {
        "name": "telephone",
        "type": "text",
        "default": "123"
    },
    {
        "name": "password",
        "type": "password",
        "action": "checkPassword()"
    },
    {
        "name": "注册",
        "type": "button",
        "action": "http://www.yooike.com/xiaoshuo/#/register?title=%E6%B3%A8%E5%86%8C"
    },
    {
        "name": "获取验证码",
        "type": "button",
        "action": "getVerificationCode()",
        "style": {
            "layout_flexGrow": 0,
            "layout_flexShrink": 1,
            "layout_alignSelf": "auto",
            "layout_flexBasisPercent": -1,
            "layout_wrapBefore": false
        }
    },
    {
        "name": "评论开关",
        "type": "toggle",
        "chars": ["❎", "☑️"],
        "default": "☑️"
    },
    {
        "name": "显示书名",
        "viewName": "book?.name||'未获取到书名'",
        "type": "button"
    },
    {
        "name": "选择排序",
        "viewName": "'排序按钮别名'",
        "type": "select",
        "chars": ["月票", "人气"],
        "default": "人气",
        "style": {
            "layout_flexGrow": 0,
            "layout_flexBasisPercent": -1,
            "layout_justifySelf": "flex_end"
        }
    }
]
```
* 登录URL
> 可填写登录链接或者实现登录UI的登录逻辑的JavaScript  
变量`isLongClick`为true时表示为按钮长按点击
```
示范填写
function login() {
    java.log("模拟登录请求");
    java.log(source.getLoginInfoMap());
}
function getVerificationCode() {
    java.log("登录UI按钮：获取到手机号码"+result.get("telephone"))
}

登录按钮函数获取登录信息
result.get("telephone")
login函数获取登录信息
source.getLoginInfo()
source.getLoginInfoMap().get("telephone")
source登录相关方法,可在js内通过source.调用,可以参考阿里云语音登录
login()
getHeaderMap(hasLoginHeader: Boolean = false)
getLoginHeader(): String?
getLoginHeaderMap(): Map<String, String>?
putLoginHeader(header: String)
removeLoginHeader()
setVariable(variable: String?)
getVariable(): String?
AnalyzeUrl相关函数,js中通过java.调用
initUrl() //重新解析url,可以用于登录检测js登录后重新解析url重新访问
getHeaderMap().putAll(source.getHeaderMap(true)) //重新设置登录头
getStrResponse( jsStr: String? = null, sourceRegex: String? = null) //返回访问结果,文本类型,书源内部重新登录后可调用此方法重新返回结果
getResponse(): Response //返回访问结果,网络朗读引擎采用的是这个,调用登录后在调用这方法可以重新访问,参考阿里云登录检测
```

* 发现url格式
> 对比登录ui，name换成了title，url用来打开发现页面，其余相同  
> 额外的变量[infoMap](https://github.com/Luoyacheng/legado-E/blob/main/app/src/main/java/io/legado/app/utils/InfoMap.kt)可读取按钮的切换值
```js
//读取值
var input = infoMap["关键词"];
//修改值
infoMap["关键词"]="系统";
//替换infoMap
infoMap.set({"键":"值"});
//保存infoMap
infoMap.save();
```
```
//所有按钮类型："url"、"text"、"button"、"toggle"、"select"
规则填写示范
[
  {
    "title": "xxx",
    "url": "",
    "style": {
      "layout_flexGrow": 0,
      "layout_flexShrink": 1,
      "layout_alignSelf": "auto",
      "layout_flexBasisPercent": -1,
      "layout_wrapBefore": false
    }
  },
  {
    "title": "关键词",
    "type": "text"
  }
]
```

* 请求头,支持http代理,socks4 socks5代理设置
> 注意请求头的key是区分大小写的  
> 正确格式 User-Agent Referer  
> 错误格式 user-agent referer
```
socks5代理    不支持需要验证的socks5代理
{ "proxy":"socks5://127.0.0.1:1080" }
http代理
{ "proxy":"http://127.0.0.1:1080" }
支持http代理服务器验证
{ "proxy":"http://127.0.0.1:1080@用户名@密码" }
注意:这些请求头是无意义的,会被忽略掉
```

* url添加js参数,解析url时执行,可在访问url时处理url,例
```
https://www.baidu.com,{"js":"java.headerMap.put('xxx', 'yyy')"}
https://www.baidu.com,{"js":"java.url=java.url+'yyyy'"}
```

* url添加bodyJs参数,对访问结果进行二次js处理,例
```
https://www.baidu.com,{"bodyJs":"if(result)'这里的文本作为访问返回的响应体body'else result"}
```

* url添加dnsIp参数,解析url时执行,强制指定链接访问的ip地址,例
```
https://dns.google,{"dnsIp":"8.8.8.8"}
```

* 增加js方法，用于重定向拦截
  * `java.get(urlStr: String, headers: Map<String, String>)`
  * `java.post(urlStr: String, body: String, headers: Map<String, String>)`
* 对于搜索重定向的源，可以使用此方法获得重定向后的url
```
(()=>{
  if(page==1){
    let url='https://www.yooread.net/e/search/index.php,'+JSON.stringify({
    "method":"POST",
    "body":"show=title&tempid=1&keyboard="+key
    });
    return source.put('surl',String(java.connect(url).raw().request().url()));
  } else {
    return source.get('surl')+'&page='+(page-1)
  }
})()
或者
(()=>{
  let base='https://www.yooread.net/e/search/';
  if(page==1){
    let url=base+'index.php';
    let body='show=title&tempid=1&keyboard='+key;
    return base+source.put('surl',java.post(url,body,{}).header("Location"));
  } else {
    return base+source.get('surl')+'&page='+(page-1);
  }
})()
```

* 图片链接支持修改headers
```
let options = {
"headers": {"User-Agent": "xxxx","Referrer":baseUrl,"Cookie":"aaa=vbbb;"}
};
'<img src="'+src+","+JSON.stringify(options)+'">'
```

* 字体解析使用
> 使用方法,在正文替换规则中使用,原理根据f1字体的字形数据到f2中查找字形对应的编码
```
<js>
(function(){
  var b64=String(src).match(/ttf;base64,([^\)]+)/);
  if(b64){
    var f1 = java.queryTTF(b64[1]);
    var f2 = java.queryTTF("https://alanskycn.gitee.io/teachme/assets/font/Source Han Sans CN Regular.ttf");
    // return java.replaceFont(result, f1, f2);
    return java.replaceFont(result, f1, f2, true); // 过滤掉f1中不存在的字形
  }
  return result;
})()
</js>
```

* 副文规则
> 书籍为文本时，获取的内容会拼接到正文后面。  
> 书籍为音频时，获取的内容作为歌词显示。  
> 书籍为视频时，获取的内容作为弹幕文本来加载。  

* 购买操作
> 可直接填写链接或者JavaScript，如果执行结果是网络链接将会自动打开浏览器,js返回true自动刷新目录和当前章节

* 回调操作
> 先启用事件监听按钮，然后软件触发事件时会执行回调规则的js代码。  
可空字符串变量`result`的值为事件对应内容。  
字符串变量`event`的值对应事件名称，目前的事件有
```js
"clickBookName" //点击详情页书名
"longClickBookName" //长按详情页书名
"clickAuthor" //点击详情页作者
"longClickAuthor" //长按详情页作者
"clickCustomButton" //点击书源自定义按钮
"longClickCustomButton" //长按书源自定义按钮（只存在小说的正文界面）
"clickShareBook" //点击详情页分享按钮
"clickClearCache" //点击详情页清理缓存按钮
"clickCopyBookUrl" //点击详情页拷贝书籍URl按钮
"clickCopyTocUrl" //点击详情页拷贝目录URl按钮
"clickCopyPlayUrl" //音频、视频界面点击拷贝播放URL按钮
"clickBookLabel" //点击详情页标签
"longClickBookLabel" //长按详情页标签
//上面的事件回调执行结果返回true会消费事件，原本的软件操作不会再执行

//下面的事件无法被回调结果消费
"addBookShelf" //添加到书架
"delBookShelf" //移除书架
"saveRead" //保存阅读进度
"startRead" //开始阅读
"endRead" //结束阅读
"startShelfRefresh" //开始书架刷新
"endShelfRefresh" //结束书架刷新
```

* 图片解密
> 适用于图片需要二次解密的情况，直接填写JavaScript，返回解密后的`ByteArray`  
> 部分变量说明：java（仅支持[js扩展类](https://github.com/luoyacheng/legado-E/blob/master/app/src/main/java/io/legado/app/help/JsExtensions.kt)），result为待解密图片的`ByteArray`，src为图片链接

```js
java.createSymmetricCrypto("AES/CBC/PKCS5Padding", key, iv).decrypt(result)
```

```js
function decodeImage(data, key) {
  var input = new Packages.java.io.ByteArrayInputStream(data)
  var out = new Packages.java.io.ByteArrayOutputStream()
  var byte
  while ((byte = input.read()) != -1) {
    out.write(byte ^ key)
  }
  return out.toByteArray()
}

decodeImage(result, key)
```

* 封面解密
> 同图片解密 其中result为待解密封面的`inputStream`

```js
java.createSymmetricCrypto("AES/CBC/PKCS5Padding", key, iv).decrypt(result)
```

```js
function decodeImage(data, key) {
  var out = new Packages.java.io.ByteArrayOutputStream()
  var byte
  while ((byte = data.read()) != -1) {
    out.write(byte ^ key)
  }
  return out.toByteArray()
}

decodeImage(result, key)
```

* 网页JS
> `window.close()` 关闭浏览器界面  
> `screen.orientation.lock()` 全屏后可控制屏幕方向  
> lock参数"landscape"->横屏且受重力控制正反、"landscape-primary"->正向横屏、"landscape-secondary"->反向横屏  

> 本地html中的额外支持的js函数  

> 异步执行阅读函数，并返回字符串结果
```js
window.run("java.toast('执行成功');'成功'")
.then(r=>alert(r))
.catch(e=>alert("执行出错:"+e));
```

* 图片链接控制样式
> 在书源正文  
> 图片链接含有"click"键时，图片被点击就会执行  
> 点击图片执行js键为兼容性保留,需要用户主动开启兼容设置，或者手动刷新图片  
```js
//建议使用
var url = `https://www.baidu.com/img/flexible/logo/pc/result.png,{"click": "java.toast('这是'+book.name+'正文的图被点击了');"}`;
result = `<img src = "${url}">`;
```
```js
//不建议使用
var url = `https://www.baidu.com/img/flexible/logo/pc/result.png,{"js": "if (book) java.toast('这是'+book.name+'正文的图被点击了');result", "style": "TEXT"}`;
result = `<img src = "${url}">`;
```

> "width"键值控制图片宽度  
> 键值为数字时为像素宽度，带`%`时为最大宽度百分比  

> "style"键值控制单个图片的样式  
> 目前支持"text"、"full"、"single"、"left"、"right"  
> 在书源正文样式为大写"TEXT"时，占1.5个字符位(text样式宽度与汉字保持一致，不受width控制)  

```
<img src = "https://du.com/result.png,{'style': 'center','width':'50%'}">
<img src = "https://du.com/result.png,{'style': 'right','width':'300'}">
<img src = "data:image/svg+xml;base64,QQ,{'style': 'left','width':'100%'}">
```

* 详情页html
> 书籍详情页支持轻度显示html字符串样式（同字典规则）  
> 获取到的简介字符串需要用`<usehtml></usehtml>`包裹起来才能识别  
> 按钮文本需要含有@onclick:执行内容才能被识别  
```xml
<usehtml>
<p style="text-align:end">右对齐文本</p>
<button>点我@onclick:java.toast("Hello World")</button>
</usehtml>
```
> 支持Markdown语法，需要用`<md></md>`包裹起来  
> 支持使用浏览器渲染，需要用`<useweb></useweb>`包裹起来  
