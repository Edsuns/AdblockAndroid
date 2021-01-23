package io.github.edsuns.adblockclient.sample

import android.graphics.Bitmap
import io.github.edsuns.adfilter.MatchedRule

/**
 * Created by Edsuns@qq.com on 2021/1/2.
 */
interface WebViewClientListener {
    fun onPageStarted(url: String?, favicon: Bitmap?)
    fun progressChanged(newProgress: Int)
    fun requestBlocked(rule: MatchedRule)
}