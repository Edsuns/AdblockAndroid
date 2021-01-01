package io.github.edsuns.adfilter

import io.github.edsuns.adblockclient.AdBlockClient
import timber.log.Timber

/**
 * Created by Edsuns@qq.com on 2020/10/24.
 */
internal class FilterDataLoader(
    private val detector: Detector,
    private val filterDataStore: BinaryDataStore
) {

    fun load(id: String) {
        if (filterDataStore.hasData(id)) {
            val client = AdBlockClient(id)
            client.loadProcessedData(filterDataStore.loadData(id))
            detector.addClient(client)
        } else {
            Timber.v("Couldn't find client processed data: $id")
        }
    }
}