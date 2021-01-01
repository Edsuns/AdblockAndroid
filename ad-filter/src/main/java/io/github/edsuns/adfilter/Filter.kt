package io.github.edsuns.adfilter

import kotlinx.serialization.Serializable

/**
 * Created by Edsuns@qq.com on 2021/1/1.
 */
@Serializable
data class Filter internal constructor(
    var name: String,
    val url: String,
    var updateTime: Long = -1L,
    var isEnabled: Boolean = true
) {
    val id by lazy { url.sha1 }

    var downloadState = DownloadState.NONE
        internal set

    fun hasDownloaded() = updateTime > 0
}

enum class DownloadState {
    DOWNLOADING, SUCCESS, FAILED, NONE
}