package io.github.edsuns.adfilter

import io.github.edsuns.adfilter.util.RuleIterator

/**
 * Created by Edsuns@qq.com on 2021/1/24.
 */
class CustomFilter internal constructor(
    private val filterDataLoader: FilterDataLoader,
    data: String? = null
) : RuleIterator(data) {

    fun flush() {
        val blacklistStr = dataBuilder.toString()
        if (blacklistStr.isNotBlank()) {
            val rawData = blacklistStr.toByteArray()
            filterDataLoader.loadCustomFilter(rawData)
        }
    }
}