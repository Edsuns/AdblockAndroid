package io.github.edsuns.adfilter

import android.app.Application
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.work.WorkInfo
import io.github.edsuns.adblockclient.ResourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Created by Edsuns@qq.com on 2020/10/24.
 */
class AdFilter internal constructor(application: Application) {

    private val detector: Detector = Detector()
    internal val binaryDataStore: BinaryDataStore =
        BinaryDataStore(File(application.filesDir, FILE_STORE_DIR))
    private val filterDataLoader: FilterDataLoader = FilterDataLoader(detector, binaryDataStore)
    val viewModel = FilterViewModel(application, filterDataLoader)

    val hasInstallation: Boolean
        get() = viewModel.sharedPreferences.hasInstallation

    init {
        viewModel.isEnabled.observeForever { enable ->
            if (enable) {
                viewModel.filters.value?.values?.forEach {
                    if (it.isEnabled && it.hasDownloaded()) {
                        filterDataLoader.load(it.id)
                    }
                }
            } else {
                filterDataLoader.unloadAll()
            }
            viewModel.sharedPreferences.isEnabled = enable
        }
        viewModel.workInfo.observeForever { list ->
            list?.forEach {
                processWorkInfo(it)
            }
        }
    }

    private fun processWorkInfo(workInfo: WorkInfo) {
        val filterId = viewModel.downloadFilterIdMap[workInfo.id.toString()]
        viewModel.filters.value?.get(filterId)?.let {
            updateFilter(it, workInfo)
        }
    }

    private fun updateFilter(filter: Filter, workInfo: WorkInfo) {
        val state = workInfo.state
        val isInstallation = workInfo.tags.contains(TAG_INSTALLATION)
        var downloadState = filter.downloadState
        if (isInstallation) {
            // when alreadyUpToDate is true
            // WorkInfo.State.SUCCEEDED can't always be received here
            // maybe it is squeezed by other WorkInfo.State
            // so we receive it in the else block
            if (filter.downloadState != DownloadState.SUCCESS) {
                downloadState =
                    when (state) {
                        WorkInfo.State.RUNNING -> DownloadState.INSTALLING
                        WorkInfo.State.SUCCEEDED -> {
                            if (filter.isEnabled || !filter.hasDownloaded())
                                viewModel.enableFilter(filter.id)
                            filter.filtersCount =
                                workInfo.outputData.getInt(KEY_FILTERS_COUNT, 0)
                            workInfo.outputData.getString(KEY_RAW_SHA_256)?.let {
                                filter.rawSha256 = it
                            }
                            filter.updateTime = System.currentTimeMillis()
                            DownloadState.SUCCESS
                        }
                        WorkInfo.State.FAILED -> DownloadState.FAILED
                        WorkInfo.State.CANCELLED -> DownloadState.CANCELLED
                        else -> downloadState
                    }
            }
        } else {
            downloadState = when (state) {
                WorkInfo.State.ENQUEUED -> DownloadState.ENQUEUED
                WorkInfo.State.RUNNING -> DownloadState.DOWNLOADING
                WorkInfo.State.SUCCEEDED -> {
                    val alreadyUpToDate =
                        workInfo.outputData.getBoolean(KEY_ALREADY_UP_TO_DATE, false)
                    if (alreadyUpToDate) {
                        filter.updateTime = System.currentTimeMillis()
                        DownloadState.SUCCESS
                    } else {
                        downloadState
                    }
                }
                else -> downloadState
            }
        }
        if (state.isFinished) {
            viewModel.downloadFilterIdMap.remove(workInfo.id.toString())
            // save shared preferences
            viewModel.sharedPreferences.downloadFilterIdMap = viewModel.downloadFilterIdMap
            viewModel.workManager.pruneWork()
        }
        if (downloadState != filter.downloadState) {
            filter.downloadState = downloadState
            viewModel.updateFilter(filter)
        }
    }

    /**
     * Notify the application of a resource request and allow the application to return the data.
     *
     * If the return value is null, the WebView will continue to load the resource as usual.
     * Otherwise, the return response and data will be used.
     *
     * NOTE: This method is called on a thread other than the UI thread so clients should exercise
     * caution when accessing private data or the view system.
     */
    fun shouldIntercept(
        webView: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        return runBlocking {
            if (request.isForMainFrame) {
                return@runBlocking null
            }

            val url = request.url.toString()
            val documentUrl = withContext(Dispatchers.Main) { webView.url }

            val shouldBlock = detector.shouldBlock(url, documentUrl, ResourceType.from(request))
            if (shouldBlock)
                WebResourceResponse(null, null, null)
            else
                null
        }
    }

    companion object {
        @Volatile
        private var instance: AdFilter? = null

        fun get(): AdFilter {
            if (instance == null) {
                throw RuntimeException("Should call create() before get()")
            }
            return instance!!
        }

        fun create(application: Application): AdFilter {
            return instance ?: synchronized(this) {
                instance = instance ?: AdFilter(application)
                instance!!
            }
        }
    }
}