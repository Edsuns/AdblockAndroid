package io.github.edsuns.adfilter

import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.anthonycr.mezzanine.FileStream
import com.anthonycr.mezzanine.MezzanineGenerator
import timber.log.Timber

/**
 * Created by Edsuns@qq.com on 2021/1/22.
 */
class ElementHiding internal constructor(private val detector: AbstractDetector) {

    @FileStream("src/main/js/element_hiding.js")
    interface EleHidingInjection {
        fun js(): String
    }

    private val hiddenFlag = randomAlphanumericString()

    private val eleHidingJS by lazy {
        var js = MezzanineGenerator.EleHidingInjection().js()
        js = js.replace(DEBUG_FLAG, if (BuildConfig.DEBUG) "" else "//")
        js = js.replace(JS_BRIDGE, JS_BRIDGE_NAME)
        js = js.replace(HIDDEN_FLAG, hiddenFlag)
        js
    }

    fun perform(webView: WebView?, url: String?) {
        webView?.evaluateJavascript(eleHidingJS, null)
        Timber.v("Evaluated element hiding Javascript for $url")
    }

    private fun randomAlphanumericString(): String {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z')
        val outputStrLength = (10..36).shuffled().first()

        return (1..outputStrLength)
            .map { kotlin.random.Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    @JavascriptInterface
    fun getEleHidingStyleSheet(documentUrl: String): String? {
        var selectors = detector.getElementHidingSelectors(documentUrl)
        var customSelectors = detector.getCustomElementHidingSelectors(documentUrl)
        if (selectors.isBlank() && customSelectors.isBlank()) {
            return null
        }
        if (selectors.isNotBlank()) {
            selectors += HIDING_CSS
        }
        if (customSelectors.isNotBlank()) {
            customSelectors += HIDING_CSS
        }
        return selectors + customSelectors
    }

    companion object {
        private const val DEBUG_FLAG = "{{DEBUG}}"
        private const val JS_BRIDGE = "{{BRIDGE}}"
        private const val HIDDEN_FLAG = "{{HIDDEN_FLAG}}"
        const val JS_BRIDGE_NAME = "getEleHidingStyleSheet"
        private const val HIDING_CSS = "{display: none !important; visibility: hidden !important;}"
    }
}