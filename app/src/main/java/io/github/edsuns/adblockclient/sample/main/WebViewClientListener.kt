package io.github.edsuns.adblockclient.sample.main

import android.graphics.Bitmap
import android.view.View
import android.webkit.WebChromeClient
import io.github.edsuns.adfilter.MatchedRule

/**
 * Created by Edsuns@qq.com on 2021/1/2.
 */
interface WebViewClientListener {
    fun onPageStarted(url: String?, favicon: Bitmap?)
    fun progressChanged(newProgress: Int)
    fun onShouldInterceptRequest(rule: MatchedRule)
    fun onShowCustomView(view: View?, callback: WebChromeClient.CustomViewCallback?)
    fun onHideCustomView()
}