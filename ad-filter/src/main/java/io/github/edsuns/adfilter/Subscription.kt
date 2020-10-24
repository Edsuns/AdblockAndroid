package io.github.edsuns.adfilter

/**
 * Created by Edsuns@qq.com on 2020/10/24.
 */
data class Subscription(
    val url: String,
    var name: String,
    var isEnabled: Boolean,
    val isBuiltin: Boolean,
    val id: String = (if (isBuiltin) "1" else "0") + url.sha1,
    var updateTimestamp: Long = 0,
)

fun Subscription.downloaded(): Boolean {
    return this.updateTimestamp > 0
}