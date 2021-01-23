package io.github.edsuns.adblockclient

/**
 * Created by Edsuns@qq.com on 2021/1/23.
 */
data class MatchResult(
    val shouldBlock: Boolean,
    // rules are available only when loading processed data with preserveRules enabled
    val matchedRule: String?,
    val matchedExceptionRule: String?
)
