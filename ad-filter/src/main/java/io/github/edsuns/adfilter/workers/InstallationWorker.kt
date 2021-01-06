package io.github.edsuns.adfilter.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.github.edsuns.adblockclient.AdBlockClient
import io.github.edsuns.adfilter.*

/**
 * Created by Edsuns@qq.com on 2021/1/5.
 */
class InstallationWorker(context: Context, params: WorkerParameters) : Worker(
    context,
    params
) {
    private val binaryDataStore = AdFilter.get().binaryDataStore

    override fun doWork(): Result {
        val alreadyUpToDate = inputData.getBoolean(KEY_ALREADY_UP_TO_DATE, false)
        if (alreadyUpToDate) {
            // don't include KEY_ALREADY_UP_TO_DATE in outputData,
            // because it can't always be received when alreadyUpToDate is true
            return Result.success()
        }
        val id = inputData.getString(KEY_FILTER_ID) ?: return Result.failure()
        val downloadedDataName = inputData.getString(KEY_DOWNLOADED_DATA) ?: return Result.failure()
        val rawSha256 = inputData.getString(KEY_RAW_SHA_256) ?: return Result.failure()
        val rawData = binaryDataStore.loadData(downloadedDataName)
        val filtersCount = persistFilterData(id, rawData)
        binaryDataStore.clearData(downloadedDataName)
        return Result.success(
            workDataOf(
                KEY_FILTERS_COUNT to filtersCount,
                KEY_RAW_SHA_256 to rawSha256
            )
        )
    }

    private fun persistFilterData(id: String, rawBytes: ByteArray): Int {
        val client = AdBlockClient(id)
        client.loadBasicData(rawBytes)
        binaryDataStore.saveData(id, client.getProcessedData())
        return client.getFiltersCount()
    }
}