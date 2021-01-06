package io.github.edsuns.adfilter.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.github.edsuns.adfilter.*
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
        val rawSha256 = inputData.getString(KEY_RAW_SHA_256) ?: return Result.failure()
        Timber.v("DownloadWorker: start download $url $id")
        try {
            val bodyBytes = HttpRequest(url).get().bodyBytes
            val sha256 = bodyBytes.sha256
            if (sha256 == rawSha256) {
                Timber.v("DownloadWorker: already up to date $url $id")
                return Result.success(workDataOf(KEY_ALREADY_UP_TO_DATE to true))
            }
            val dataName = "_$id"
            binaryDataStore.saveData(dataName, bodyBytes)
            return Result.success(
                workDataOf(
                    KEY_FILTER_ID to id,
                    KEY_DOWNLOADED_DATA to dataName,
                    KEY_RAW_SHA_256 to sha256
                )
            )
        } catch (e: IOException) {
            Timber.v(e, "DownloadWorker: failed to download $url $id")
        }
        return Result.failure(inputData)
    }
}