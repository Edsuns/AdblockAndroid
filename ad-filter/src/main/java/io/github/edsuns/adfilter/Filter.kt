package io.github.edsuns.adfilter

import kotlinx.serialization.Serializable

/**
 * Created by Edsuns@qq.com on 2021/1/1.
 */
@Serializable
data class Filter internal constructor(
    val url: String,
) {
    val id by lazy { url.sha1 }

    var name: String = ""
        internal set

    var isEnabled: Boolean = false
        internal set

    var downloadState = DownloadState.NONE
        internal set

    var updateTime: Long = -1L
        internal set

    fun hasDownloaded() = updateTime > 0
}

enum class DownloadState {
    DOWNLOADING, SUCCESS, FAILED, NONE
}