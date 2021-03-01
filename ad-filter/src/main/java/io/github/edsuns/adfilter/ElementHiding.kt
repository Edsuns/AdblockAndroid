package io.github.edsuns.adfilter

import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.anthonycr.mezzanine.FileStream
import com.anthonycr.mezzanine.MezzanineGenerator
import timber.log.Timber
import java.net.MalformedURLException
import java.net.URL

/**
 * Created by Edsuns@qq.com on 2021/1/22.
 */
class ElementHiding internal constructor(private val detector: AbstractDetector) {

    @FileStream("src/main/js/elemhide_blocked.js")
    interface ElemhideBlockedInjection {
        fun js(): String
    }

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

    private val elemhideBlockedJs by lazy {
        var js = MezzanineGenerator.ElemhideBlockedInjection().js()
        js = js.replace(DEBUG_FLAG, if (BuildConfig.DEBUG) "" else "//")
        js
    }

    internal fun elemhideBlockedResource(webView: WebView?, resourceUrl: String?) {
        var filenameWithQuery: String
        try {
            filenameWithQuery = extractPathWithQuery(resourceUrl)
            if (filenameWithQuery.startsWith("/")) {
                filenameWithQuery = filenameWithQuery.substring(1)
            }
        } catch (e: MalformedURLException) {
            Timber.e("Failed to parse URI for blocked resource:$resourceUrl. Skipping element hiding")
            return
        }
        Timber.d("Trying to elemhide visible blocked resource with url `$resourceUrl` and path `$filenameWithQuery`")

        // It finds all the elements with source URLs ending with ... and then compare full paths.
        // We do this trick because the paths in JS (code) can be relative and in DOM tree they are absolute.
        val selectorBuilder = StringBuilder()
            .append("[src$='").append(filenameWithQuery)
            .append("'], [srcset$='")
            .append(filenameWithQuery)
            .append("']")

        // all UI views including AdblockWebView can be touched from UI thread only
        webView?.post {
            val scriptBuilder = StringBuilder(elemhideBlockedJs)
                .append("\n\n")
                .append("elemhideForSelector(\"")
                .append(resourceUrl)// 1st argument

            scriptBuilder.append("\", \"")
            scriptBuilder.append(escapeJavaScriptString(selectorBuilder.toString()))// 2nd argument

            scriptBuilder.append("\", 0)")// attempt #0

            webView.evaluateJavascript(scriptBuilder.toString(), null)
        }
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

    /**
     * Extract path with query from URL
     * @param urlString URL
     * @return path with optional query part
     * @throws MalformedURLException
     */
    @Throws(MalformedURLException::class)
    fun extractPathWithQuery(urlString: String?): String {
        val url = URL(urlString)
        val sb = java.lang.StringBuilder(url.path)
        if (url.query != null) {
            sb.append("?")
            sb.append(url.query)
        }
        return sb.toString()
    }

    /**
     * Escape JavaString string
     * @param line unescaped string
     * @return escaped string
     */
    private fun escapeJavaScriptString(line: String): String {
        val sb = StringBuilder()
        for (c in line) {
            when (c) {
                '"', '\'', '\\' -> {
                    sb.append('\\')
                    sb.append(c)
                }
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                else -> sb.append(c)
            }
        }
        return sb.toString()
            .replace(U2028, "\u2028")
            .replace(U2029, "\u2029")
    }

    companion object {
        private val U2028 = String(byteArrayOf(0xE2.toByte(), 0x80.toByte(), 0xA8.toByte()))
        private val U2029 = String(byteArrayOf(0xE2.toByte(), 0x80.toByte(), 0xA9.toByte()))

        private const val DEBUG_FLAG = "{{DEBUG}}"
        private const val JS_BRIDGE = "{{BRIDGE}}"
        private const val HIDDEN_FLAG = "{{HIDDEN_FLAG}}"
        const val JS_BRIDGE_NAME = "getEleHidingStyleSheet"
        private const val HIDING_CSS = "{display: none !important; visibility: hidden !important;}"
    }
}