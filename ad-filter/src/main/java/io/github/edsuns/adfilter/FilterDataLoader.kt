package io.github.edsuns.adfilter

import io.github.edsuns.adblockclient.AdBlockClient
import timber.log.Timber

/**
 * Created by Edsuns@qq.com on 2020/10/24.
 */
internal class FilterDataLoader(
    private val detector: Detector,
    private val binaryDataStore: BinaryDataStore
) {

    fun load(id: String) {
        if (binaryDataStore.hasData(id)) {
            val client = AdBlockClient(id)
            client.loadProcessedData(binaryDataStore.loadData(id))
            detector.addClient(client)
        } else {
            Timber.v("Couldn't find client processed data: $id")
        }
    }

    fun loadWhitelist(id: String) {
        if (binaryDataStore.hasData(id)) {
            val client = AdBlockClient(id)
            client.loadProcessedData(binaryDataStore.loadData(id))
            detector.whitelistClient = client
        } else {
            Timber.v("Couldn't find client processed data: $id")
        }
    }

    fun loadBlacklist(id: String) {
        if (binaryDataStore.hasData(id)) {
            val client = AdBlockClient(id)
            client.loadProcessedData(binaryDataStore.loadData(id))
            detector.blacklistClient = client
        } else {
            Timber.v("Couldn't find client processed data: $id")
        }
    }

    fun unloadCustomFilters() {
        detector.whitelistClient = null
        detector.blacklistClient = null
    }

    fun unload(id: String) {
        detector.removeClient(id)
    }

    fun unloadAll() {
        detector.clearAllClient()
    }

    fun remove(id: String) {
        binaryDataStore.clearData(id)
        binaryDataStore.clearData("_$id")
        unload(id)
    }
}