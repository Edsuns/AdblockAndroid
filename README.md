# AdblockAndroid

[![](https://jitpack.io/v/Edsuns/AdblockAndroid.svg)](https://jitpack.io/#Edsuns/AdblockAndroid)

*English | [简体中文](README.zh-cn.md)*

A lightweight and efficient adblock engine library for Android, which has strong compatibility for filters like [EasyList](https://easylist.to/) and [AdGuard Filters](https://kb.adguard.com/en/general/how-to-create-your-own-ad-filters).

The native C++ code is based on [brave/ad-block](https://github.com/brave/ad-block). Beyond adapting it to Android platform, I fixed some fatal issues ([d85d341](https://github.com/Edsuns/AdblockAndroid/commit/d85d341692efbde551712f44b79ae590f4df64d5), [583f87a](https://github.com/Edsuns/AdblockAndroid/commit/583f87a2b193257aff797e3f6ba093e619700335)), made 40x better parsing performance ([ab18236](https://github.com/Edsuns/AdblockAndroid/commit/ab182369edcd2c86d6fbc3e9e2d85ca8ec82954e), [a0009c8](https://github.com/Edsuns/AdblockAndroid/commit/a0009c83857f435ea6c055a2b5fff6ec3ee88bdc)) and implemented some new features.

## Features

- All features from [brave/ad-block](https://github.com/brave/ad-block)
- Support for [element hiding](https://kb.adguard.com/en/general/how-to-create-your-own-ad-filters#cosmetic-elemhide-rules)
- Support for [Extended CSS selectors](https://kb.adguard.com/en/general/how-to-create-your-own-ad-filters#extended-css-selectors)
- Support for [CSS rules](https://kb.adguard.com/en/general/how-to-create-your-own-ad-filters#cosmetic-css-rules)
- Support for [Scriptlet rules](https://kb.adguard.com/en/general/how-to-create-your-own-ad-filters#scriptlets)
- Support for [checksum](https://hg.adblockplus.org/adblockplus/file/tip/validateChecksum.py)
- Support for background download and installation
- Custom filter subscriptions
- Custom rules

## Tech Stack

- Android
- Kotlin
- C++
- JavaScript
- JNI

## Demo

See [releases](https://github.com/Edsuns/AdblockAndroid/releases) and the code in `app/src`.

## Get Started

*Note: This development requires you to have fundamental Android WebView experience.*

*Note: A full application example can be found in `:app` module.*

### 1. Gradle Configuration

Add it in your root build.gradle at the end of repositories:

```groovy
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

Add the dependency:

```groovy
dependencies {
    implementation 'com.github.Edsuns.AdblockAndroid:ad-filter:1.0'
}
```

### 2. Coding

Add the code in your `Application` class

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Debug configuration.
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // Start adfilter.
        val filter = AdFilter.create(this)
    }
}
```

Add the code in your `WebChromeClient` class

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

Add the code in your `Activity` class

```kotlin
class MainActivity : AppCompatActivity(), WebViewClientListener {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val filter = AdFilter.get()
        val filterViewModel = filter.viewModel

        // Setup AdblockAndroid for your WebView.
        filter.setupWebView(binding.webView)

        // Add filter list subscriptions on first installation.
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
            // Clear cache when there are changes to the filter.
            // You need to refresh the page manually to make the changes take effect.
            binding.webView.clearCache(false)
        })
    }
}
```

**Congratulations, great success!**

## Thanks To

- [brave/ad-block](https://github.com/brave/ad-block)
- [github.com/AdguardTeam](https://github.com/AdguardTeam)
- [duckduckgo/Android](https://github.com/duckduckgo/Android)
