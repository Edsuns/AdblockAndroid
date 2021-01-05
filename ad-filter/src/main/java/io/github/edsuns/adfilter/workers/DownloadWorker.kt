package io.github.edsuns.adfilter.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.github.edsuns.adfilter.AdFilter
import io.github.edsuns.adfilter.KEY_DOWNLOADED_DATA
import io.github.edsuns.adfilter.KEY_DOWNLOAD_URL
import io.github.edsuns.adfilter.KEY_FILTER_ID
import io.github.edsuns.net.HttpRequest
import timber.log.Timber
import java.io.IOException

/**
 * Created by Edsuns@qq.com on 2021/1/1.
 */
class DownloadWorker(context: Context, params: WorkerParameters) : Worker(
    context,
    params
) {
    private val binaryDataStore = AdFilter.get().binaryDataStore

    override fun doWork(): Result {
        val id = inputData.getString(KEY_FILTER_ID) ?: return Result.failure()
        val url = inputData.getString(KEY_DOWNLOAD_URL) ?: return Result.failure()
        Timber.v("DownloadWorker: start download $url $id")
        try {
            val request = HttpRequest(url).get()
            val dataName = "_$id"
            binaryDataStore.saveData(dataName, request.bodyBytes)
            return Result.success(
                workDataOf(KEY_FILTER_ID to id, KEY_DOWNLOADED_DATA to dataName)
            )
        } catch (e: IOException) {
            Timber.v(e, "DownloadWorker: failed to download $url $id")
        }
        return Result.failure(inputData)
    }
}