package io.github.edsuns.adfilter

import io.github.edsuns.adblockclient.AdBlockClient

/**
 * Created by Edsuns@qq.com on 2021/1/24.
 */
class CustomFilters internal constructor(
    private val binaryDataStore: BinaryDataStore,
    private val filterDataLoader: FilterDataLoader
) {

    val whitelist =
        if (binaryDataStore.hasData(RAW_WHITELIST))
            RuleIterator(String(binaryDataStore.loadData(RAW_WHITELIST)))
        else
            RuleIterator()

    val blacklist =
        if (binaryDataStore.hasData(RAW_BLACKLIST))
            RuleIterator(String(binaryDataStore.loadData(RAW_BLACKLIST)))
        else
            RuleIterator()

    internal fun load() {
        filterDataLoader.loadWhitelist(WHITELIST)
        filterDataLoader.loadBlacklist(BLACKLIST)
    }

    internal fun unload() {
        filterDataLoader.unloadCustomFilters()
    }

    fun appendWhitelist(rule: String) {
        // don't allow whitelist and blacklist contains the same rule
        blacklist.remove(rule)
        whitelist.append(rule)
    }

    fun appendBlacklist(rule: String) {
        // don't allow whitelist and blacklist contains the same rule
        whitelist.remove(rule)
        blacklist.append(rule)
    }

    fun flushWhitelist() {
        val whitelistStr = whitelist.dataBuilder.toString()
        if (whitelistStr.isNotBlank()) {
            val rawData = whitelistStr.toByteArray()
            binaryDataStore.saveData(RAW_WHITELIST, rawData)
            val client = AdBlockClient(WHITELIST)
            client.loadBasicData(rawData, true)
            binaryDataStore.saveData(WHITELIST, client.getProcessedData())
            filterDataLoader.loadWhitelist(WHITELIST)
        }
    }

    fun flushBlacklist() {
        val blacklistStr = blacklist.dataBuilder.toString()
        if (blacklistStr.isNotBlank()) {
            val rawData = blacklistStr.toByteArray()
            binaryDataStore.saveData(RAW_BLACKLIST, rawData)
            val client = AdBlockClient(BLACKLIST)
            client.loadBasicData(rawData, true)
            binaryDataStore.saveData(BLACKLIST, client.getProcessedData())
            filterDataLoader.loadBlacklist(BLACKLIST)
        }
    }

    companion object {
        private const val RAW_WHITELIST = "_whitelist"
        private const val RAW_BLACKLIST = "_blacklist"
        private const val WHITELIST = "whitelist"
        private const val BLACKLIST = "blacklist"
    }
}