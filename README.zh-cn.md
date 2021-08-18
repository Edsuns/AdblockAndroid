# AdblockAndroid

[![](https://jitpack.io/v/Edsuns/AdblockAndroid.svg)](https://jitpack.io/#Edsuns/AdblockAndroid)

*[English](README.md) | 简体中文*

一个轻量级、高性能、为安卓量身打造的 adblock 过滤引擎依赖库，支持 [EasyList](https://easylist.to/) 和 [AdGuard Filters](https://kb.adguard.com/en/general/how-to-create-your-own-ad-filters) 的过滤规则。

底层的算法实现基于 [brave/ad-block](https://github.com/brave/ad-block)，在将其适配到安卓平台之外，我还修复了几个致命的错误（[d85d341](https://github.com/Edsuns/AdblockAndroid/commit/d85d341692efbde551712f44b79ae590f4df64d5)，[583f87a](https://github.com/Edsuns/AdblockAndroid/commit/583f87a2b193257aff797e3f6ba093e619700335)）、使安装规则的性能提升了40倍（[ab18236](https://github.com/Edsuns/AdblockAndroid/commit/ab182369edcd2c86d6fbc3e9e2d85ca8ec82954e)，[a0009c8](https://github.com/Edsuns/AdblockAndroid/commit/a0009c83857f435ea6c055a2b5fff6ec3ee88bdc)），并实现了许多新的特性。

## 特性

- 所有来自 [brave/ad-block](https://github.com/brave/ad-block) 的特性
- 支持 [element hiding](https://kb.adguard.com/en/general/how-to-create-your-own-ad-filters#cosmetic-elemhide-rules)
- 支持 [Extended CSS selectors](https://kb.adguard.com/en/general/how-to-create-your-own-ad-filters#extended-css-selectors)
- 支持 [CSS rules](https://kb.adguard.com/en/general/how-to-create-your-own-ad-filters#cosmetic-css-rules)
- 支持 [Scriptlet rules](https://kb.adguard.com/en/general/how-to-create-your-own-ad-filters#scriptlets)
- 支持 [checksum](https://hg.adblockplus.org/adblockplus/file/tip/validateChecksum.py)
- 支持在后台下载和安装
- 支持订阅过滤列表
- 支持自定义过滤规则

## 技术栈

- Android
- Kotlin
- C++
- JavaScript
- JNI

## 示例

请查看 [releases](https://github.com/Edsuns/AdblockAndroid/releases) 和 `app/src`里的代码。

## 快速上手

*注意：本教程要求阅读者有基本的WebView开发经验*

*注意：完整的示例应用可查看 `:app` 模块*

### 1. Gradle 配置

在项目根目录的 build.gradle 添加仓库地址：

```groovy
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

在具体模块添加依赖：

```groovy
dependencies {
    implementation 'com.github.Edsuns.AdblockAndroid:ad-filter:1.0'
}
```

### 2. 代码

添加以下代码到 `Application` 类

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Debug配置
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // 启动过滤器
        val filter = AdFilter.create(this)
    }
}
```

添加以下代码到 `WebChromeClient` 类

```kotlin
class WebClient(private val webViewClientListener: WebViewClientListener) : WebViewClient() {

    private val filter = AdFilter.get()

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        val result = filter.shouldIntercept(view!!, request!!)
        return result.resourceResponse
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        filter.performScript(view, url)
    }
}
```

添加以下代码到 `Activity` 类

```kotlin
class MainActivity : AppCompatActivity(), WebViewClientListener {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val filter = AdFilter.get()
        val filterViewModel = filter.viewModel

        // 为WebView安装过滤器
        filter.setupWebView(binding.webView)

        // 在首次安装时添加订阅并下载
        if (!filter.hasInstallation) {
            val map = mapOf(
                "AdGuard Base" to "https://filters.adtidy.org/extension/chromium/filters/2.txt",
                "EasyPrivacy Lite" to "https://filters.adtidy.org/extension/chromium/filters/118_optimized.txt",
                "AdGuard Tracking Protection" to "https://filters.adtidy.org/extension/chromium/filters/3.txt",
                "AdGuard Annoyances" to "https://filters.adtidy.org/extension/chromium/filters/14.txt",
                "AdGuard Chinese" to "https://filters.adtidy.org/extension/chromium/filters/224.txt",
                "NoCoin Filter List" to "https://filters.adtidy.org/extension/chromium/filters/242.txt"
            )
            for ((key, value) in map) {
                val subscription = filterViewModel.addFilter(key, value)
                filterViewModel.download(subscription.id)
            }
        }

        filterViewModel.onDirty.observe(this, {
            // 在过滤器有变更时清除缓存，需要手动刷新网页使变更生效
            binding.webView.clearCache(false)
        })
    }
}
```

**大功告成！**

## 致谢

- [brave/ad-block](https://github.com/brave/ad-block)
- [github.com/AdguardTeam](https://github.com/AdguardTeam)
- [duckduckgo/Android](https://github.com/duckduckgo/Android)
