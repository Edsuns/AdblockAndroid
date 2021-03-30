package io.github.edsuns.adfilter

import android.content.Context
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
class AdFilter internal constructor(appContext: Context) {

    private val detector: Detector = Detector()
    internal val binaryDataStore: BinaryDataStore =
        BinaryDataStore(File(appContext.filesDir, FILE_STORE_DIR))
    private val filterDataLoader: FilterDataLoader = FilterDataLoader(detector, binaryDataStore)
    private val elementHiding: ElementHiding = ElementHiding(detector)
    val customFilters = CustomFilters(binaryDataStore, filterDataLoader)
    val viewModel = FilterViewModel(appContext, filterDataLoader)

    val hasInstallation: Boolean
        get() = viewModel.sharedPreferences.hasInstallation

    init {
        viewModel.isEnabled.observeForever { enable ->
            if (enable) {
                viewModel.filters.value?.values?.forEach {
                    if (it.isEnabled && it.hasDownloaded()) {
                        viewModel.enableFilter(it)
                    }
                }
                customFilters.load()
            } else {
                filterDataLoader.unloadAll()
                customFilters.unload()
            }
            viewModel.sharedPreferences.isEnabled = enable
            // notify onDirty
            viewModel._onDirty.value = None.Value
        }
        viewModel.workInfo.observeForever { list -> processWorkInfo(list) }
    }

    private fun processWorkInfo(workInfoList: List<WorkInfo>) {
        workInfoList.forEach { workInfo ->
            val filterId = viewModel.downloadFilterIdMap[workInfo.id.toString()]
            viewModel.filters.value?.get(filterId)?.let {
                updateFilter(it, workInfo)
            }
        }
    }

    private fun updateFilter(filter: Filter, workInfo: WorkInfo) {
        val state = workInfo.state
        val isInstallation = workInfo.tags.contains(TAG_INSTALLATION)
        var downloadState = filter.downloadState
        if (isInstallation) {
            downloadState =
                when (state) {
                    WorkInfo.State.RUNNING -> DownloadState.INSTALLING
                    WorkInfo.State.SUCCEEDED -> {
                        val alreadyUpToDate =
                            workInfo.outputData.getBoolean(KEY_ALREADY_UP_TO_DATE, false)
                        if (!alreadyUpToDate) {
                            filter.filtersCount =
                                workInfo.outputData.getInt(KEY_FILTERS_COUNT, 0)
                            workInfo.outputData.getString(KEY_RAW_CHECKSUM)
                                ?.let { filter.checksum = it }
                            if (filter.isEnabled || !filter.hasDownloaded()) {
                                viewModel.enableFilter(filter)
                            }
                        }
                        if (filter.name.isBlank()) {
                            workInfo.outputData.getString(KEY_FILTER_NAME)
                                ?.let { filter.name = it }
                        }
                        filter.updateTime = System.currentTimeMillis()
                        DownloadState.SUCCESS
                    }
                    WorkInfo.State.FAILED -> DownloadState.FAILED
                    WorkInfo.State.CANCELLED -> DownloadState.CANCELLED
                    else -> downloadState
                }
        } else {
            downloadState = when (state) {
                WorkInfo.State.ENQUEUED -> DownloadState.ENQUEUED
                WorkInfo.State.RUNNING -> DownloadState.DOWNLOADING
                WorkInfo.State.FAILED -> DownloadState.FAILED
                else -> downloadState
            }
        }
        if (state.isFinished) {
            viewModel.downloadFilterIdMap.remove(workInfo.id.toString())
            // save shared preferences
            viewModel.sharedPreferences.downloadFilterIdMap = viewModel.downloadFilterIdMap
        }
        if (downloadState != filter.downloadState) {
            filter.downloadState = downloadState
            viewModel.flushFilter()
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
    ): MatchedRule {
        return runBlocking {
            val url = request.url.toString()
            if (request.isForMainFrame) {
                return@runBlocking MatchedRule(null, url, null)
            }

            val documentUrl = withContext(Dispatchers.Main) { webView.url }
                ?: return@runBlocking MatchedRule(null, url, null)

            val resourceType = ResourceType.from(request)
            val rule = detector.shouldBlock(url, documentUrl, resourceType)

            return@runBlocking if (rule != null) {
                if (resourceType.isVisibleResource()) {
                    elementHiding.elemhideBlockedResource(webView, url)
                }
                MatchedRule(rule, url, WebResourceResponse(null, null, null))
            } else MatchedRule(null, url, null)
        }
    }

    private fun ResourceType.isVisibleResource(): Boolean =
        this === ResourceType.IMAGE || this === ResourceType.MEDIA || this === ResourceType.SUBDOCUMENT

    fun setupWebView(webView: WebView) {
        webView.addJavascriptInterface(elementHiding, ElementHiding.JS_BRIDGE_NAME)
    }

    fun performElementHiding(webView: WebView?, url: String?) {
        if (viewModel.isEnabled.value == true) {
            elementHiding.perform(webView, url)
        }
    }

    companion object {
        @Volatile
        private var instance: AdFilter? = null

        fun get(): AdFilter =
            instance ?: throw RuntimeException("Should call create() before get()")

        fun get(context: Context): AdFilter {
            return instance ?: synchronized(this) {
                // keep application context rather than any other context to avoid memory leak
                instance = instance ?: AdFilter(context.applicationContext)
                instance!!
            }
        }

        fun create(context: Context): AdFilter = get(context)
    }
}