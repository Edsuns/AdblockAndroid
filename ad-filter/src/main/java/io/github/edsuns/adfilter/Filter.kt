package io.github.edsuns.adfilter

/**
 * Created by Edsuns@qq.com on 2021/1/1.
 */
data class Filter internal constructor(
    var name: String,
    val url: String,
    var updateTime: Long = -1L,
    var isEnabled: Boolean = false
) {
    val id by lazy { url.sha1 }

    var downloading = false
        internal set

    fun hasDownloaded() = updateTime > 0
}