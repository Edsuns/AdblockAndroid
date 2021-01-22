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

    private var progress: Int = 0

    fun perform(webView: WebView?, newProgress: Int) {
        if (newProgress > progress || newProgress == 10) {
            webView?.let {
                it.evaluateJavascript(eleHidingJS, null)
                progress = 100
                Timber.v("Evaluated element hiding Javascript when progress $newProgress")
            }
        }
        if (newProgress < 30) {
            progress = newProgress
        }
    }

    private fun randomAlphanumericString(): String {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z')
        val outputStrLength = (8..36).shuffled().first()

        return (1..outputStrLength)
            .map { kotlin.random.Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    @JavascriptInterface
    fun getEleHidingStyleSheet(documentUrl: String): String? {
        val selectors = detector.getElementHidingSelectors(documentUrl)
        if (selectors.isBlank()) {
            return null
        }
        return selectors + HIDING_CSS
    }

    companion object {
        private const val DEBUG_FLAG = "{{DEBUG}}"
        private const val JS_BRIDGE = "{{BRIDGE}}"
        private const val HIDDEN_FLAG = "{{HIDDEN_FLAG}}"
        const val JS_BRIDGE_NAME = "getEleHidingStyleSheet"
        private const val HIDING_CSS = "{display: none !important; visibility: hidden !important;}"
    }
}