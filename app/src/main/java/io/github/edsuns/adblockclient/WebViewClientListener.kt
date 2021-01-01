package io.github.edsuns.adblockclient

import android.graphics.Bitmap

/**
 * Created by Edsuns@qq.com on 2021/1/2.
 */
interface WebViewClientListener {
    fun onPageStarted(url: String?, favicon: Bitmap?)
    fun progressChanged(newProgress: Int)
    fun requestBlocked(url: String)
}