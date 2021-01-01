package io.github.edsuns.adblockclient

import android.graphics.Bitmap
import android.webkit.*
import io.github.edsuns.adfilter.AdFilter

/**
 * Created by Edsuns@qq.com on 2021/1/1.
 */
class WebClient(private val webViewClientListener: WebViewClientListener) : WebViewClient() {

    private val filter = AdFilter.get()

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request!!.url.toString()
        return !URLUtil.isNetworkUrl(url)
    }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        val shouldBlock = filter.shouldIntercept(view!!, request!!)
        if (shouldBlock != null)
            webViewClientListener.requestBlocked(request.url.toString())
        return shouldBlock
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        webViewClientListener.onPageStarted(url, favicon)
    }
}