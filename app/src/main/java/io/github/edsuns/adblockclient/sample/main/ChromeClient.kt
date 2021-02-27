package io.github.edsuns.adblockclient.sample.main

import android.webkit.WebChromeClient
import android.webkit.WebView

/**
 * Created by Edsuns@qq.com on 2021/1/2.
 */
class ChromeClient(private val webViewClientListener: WebViewClientListener) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        webViewClientListener.progressChanged(newProgress)
    }
}