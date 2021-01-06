package io.github.edsuns.adfilter.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.github.edsuns.adblockclient.AdBlockClient
import io.github.edsuns.adfilter.AdFilter
import io.github.edsuns.adfilter.KEY_DOWNLOADED_DATA
import io.github.edsuns.adfilter.KEY_FILTERS_COUNT
import io.github.edsuns.adfilter.KEY_FILTER_ID

/**
 * Created by Edsuns@qq.com on 2021/1/5.
 */
class InstallationWorker(context: Context, params: WorkerParameters) : Worker(
    context,
    params
) {
    private val binaryDataStore = AdFilter.get().binaryDataStore

    override fun doWork(): Result {
        val id = inputData.getString(KEY_FILTER_ID) ?: return Result.failure()
        val downloadedData = inputData.getString(KEY_DOWNLOADED_DATA) ?: return Result.failure()
        val rawData = binaryDataStore.loadData(downloadedData)
        val filtersCount = persistFilterData(id, rawData)
        binaryDataStore.clearData(downloadedData)
        return Result.success(workDataOf(KEY_FILTERS_COUNT to filtersCount))
    }

    private fun persistFilterData(id: String, bodyBytes: ByteArray): Int {
        val client = AdBlockClient(id)
        client.loadBasicData(bodyBytes)
        binaryDataStore.saveData(id, client.getProcessedData())
        return client.getFiltersCount()
    }
}