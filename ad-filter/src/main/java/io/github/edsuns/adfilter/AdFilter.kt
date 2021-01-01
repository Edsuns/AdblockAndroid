package io.github.edsuns.adfilter

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.work.WorkInfo
import io.github.edsuns.adblockclient.ResourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.anko.doAsync

/**
 * Created by Edsuns@qq.com on 2020/10/24.
 */
class AdFilter internal constructor(application: Application) {

    private val detector: Detector = Detector()

    internal val binaryDataStore: BinaryDataStore = BinaryDataStore(
        application.getDir(
            FILE_STORE_DIR,
            Context.MODE_PRIVATE
        )
    )

    val viewModel = FilterViewModel(application, binaryDataStore)

    private val filterDataLoader: FilterDataLoader = FilterDataLoader(detector, binaryDataStore)

    init {
        application.registerActivityLifecycleCallbacks(object :
            Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            }

            override fun onActivityStarted(activity: Activity) {
            }

            override fun onActivityResumed(activity: Activity) {
            }

            override fun onActivityPaused(activity: Activity) {
            }

            override fun onActivityStopped(activity: Activity) {
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            }

            override fun onActivityDestroyed(activity: Activity) {
                viewModel.saveSharedPreferences()
            }
        })
        doAsync {
            if (viewModel.isEnabled.value!! && !viewModel.filters.value.isNullOrEmpty()) {
                viewModel.filters.value!!.values.forEach {
                    if (it.isEnabled && it.hasDownloaded()) {
                        filterDataLoader.load(it.id)
                    }
                }
            }
        }
        viewModel.workInfo.observeForever { list ->
            processWorkInfo(list)
        }
    }

    private fun processWorkInfo(list: List<WorkInfo>?) {
        list?.forEach { workInfo ->
            val state = workInfo.state
            if (state.isFinished) {
                val id = workInfo.outputData.getString(KEY_FILTER_ID)
                viewModel.filters.value?.get(id)?.let {
                    val downloadState = when (state) {
                        WorkInfo.State.SUCCEEDED -> {
                            it.updateTime = System.currentTimeMillis()
                            DownloadState.SUCCESS
                        }
                        WorkInfo.State.FAILED -> DownloadState.FAILED
                        else -> DownloadState.NONE
                    }
                    it.downloadState = downloadState
                    viewModel.updateFilter(it)
                }
            }
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
        private var instance: AdFilter? = null

        fun get(): AdFilter {
            if (instance == null) {
                throw RuntimeException("Should call create() before get()")
            }
            return instance!!
        }

        fun create(application: Application): AdFilter {
            if (instance == null) {
                instance = AdFilter(application)
            }
            return instance!!
        }
    }
}