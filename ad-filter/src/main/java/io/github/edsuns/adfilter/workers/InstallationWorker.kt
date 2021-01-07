package io.github.edsuns.adfilter.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.github.edsuns.adblockclient.AdBlockClient
import io.github.edsuns.adfilter.*
import timber.log.Timber

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
        val downloadedDataName = inputData.getString(KEY_DOWNLOADED_DATA) ?: return Result.failure()
        val rawChecksum = inputData.getString(KEY_RAW_CHECKSUM) ?: return Result.failure()
        val rawData = binaryDataStore.loadData(downloadedDataName)
        val checksum = Checksum(String(rawData))
        Timber.v("Checksum: $rawChecksum, ${checksum.checksumIn}, ${checksum.checksumCalc}, ${checksum.validate()}")
        if (!checksum.validate()) {
            return Result.failure()
        }
        if (checksum.validate(rawChecksum)) {
            Timber.v("Filter is up to date: $id")
            return Result.success(workDataOf(KEY_ALREADY_UP_TO_DATE to true))
        }
        val filtersCount = persistFilterData(id, rawData)
        binaryDataStore.clearData(downloadedDataName)
        return Result.success(
            workDataOf(
                KEY_FILTERS_COUNT to filtersCount,
                KEY_RAW_CHECKSUM to checksum.checksumCalc
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