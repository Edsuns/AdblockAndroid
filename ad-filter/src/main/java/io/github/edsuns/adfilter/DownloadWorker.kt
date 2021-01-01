package io.github.edsuns.adfilter

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import io.github.edsuns.adblockclient.AdBlockClient
import io.github.edsuns.net.HttpRequest
import java.io.IOException

/**
 * Created by Edsuns@qq.com on 2021/1/1.
 */
class DownloadWorker(context: Context, params: WorkerParameters) : Worker(
    context,
    params
) {
    override fun doWork(): Result {
        val id = inputData.getString(KEY_FILTER_ID)
        val url = inputData.getString(KEY_DOWNLOAD_URL)
        try {
            url?.let {
                id?.let {
                    val request = HttpRequest(url).get()
                    persistFilterData(it, request.bodyBytes)
                    return Result.success(inputData)
                }
            }
        } catch (ignore: IOException) {
        }
        return Result.failure(inputData)
    }

    private fun persistFilterData(id: String, bodyBytes: ByteArray) {
        val client = AdBlockClient(id)
        client.loadBasicData(bodyBytes)
        AdFilter.get().binaryDataStore.saveData(id, client.getProcessedData())
    }
}