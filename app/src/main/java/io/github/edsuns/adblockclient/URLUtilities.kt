package io.github.edsuns.adblockclient

/**
 * Created by Edsuns@qq.com on 2021/1/3.
 */
fun String.stripParamsAndAnchor(): String {
    var result = this
    var index = this.indexOf('?')
    if (index != -1)
        result = this.substring(0, index)
    index = this.indexOf('#')
    if (index != -1)
        result = this.substring(0, index)
    return result
}