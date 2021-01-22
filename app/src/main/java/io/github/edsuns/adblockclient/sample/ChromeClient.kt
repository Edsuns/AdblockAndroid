package io.github.edsuns.adblockclient.sample

import android.webkit.WebChromeClient
import android.webkit.WebView
import io.github.edsuns.adfilter.AdFilter

/**
 * Created by Edsuns@qq.com on 2021/1/2.
 */
class ChromeClient(private val webViewClientListener: WebViewClientListener) : WebChromeClient() {

    private val adFilter = AdFilter.get()

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        webViewClientListener.progressChanged(newProgress)
        adFilter.performElementHiding(view, newProgress)
    }
}