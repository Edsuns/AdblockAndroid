package io.github.edsuns.adfilter

/**
 * Created by Edsuns@qq.com on 2021/1/24.
 */
class CustomFilter internal constructor(
    private val filterDataLoader: FilterDataLoader
) {

    private val buffer = filterDataLoader.getRawCustomFilter()

    fun appendRule(rule: String) {
        buffer.append(rule)
    }

    fun flush() {
        val blacklistStr = buffer.dataBuilder.toString()
        if (blacklistStr.isNotBlank()) {
            val rawData = blacklistStr.toByteArray()
            filterDataLoader.loadCustomFilter(rawData)
        }
    }
}