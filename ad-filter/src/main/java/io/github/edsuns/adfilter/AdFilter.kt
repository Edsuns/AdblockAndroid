package io.github.edsuns.adfilter

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import io.github.edsuns.adblockclient.ResourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.anko.doAsync

/**
 * Created by Edsuns@qq.com on 2020/10/24.
 */
class AdFilter internal constructor(private val application: Application) {

    private val viewModel = FilterViewModel(application)

    private val adDetector: AdDetector by lazy { AdDetector() }
    private val filterDataLoader: FilterDataLoader by lazy {
        FilterDataLoader(
            adDetector,
            BinaryDataStore(application.getDir(FILE_STORE, Context.MODE_PRIVATE))
        )
    }

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

            val shouldBlock = adDetector.shouldBlock(url, documentUrl, ResourceType.from(request))
            if (shouldBlock)
                WebResourceResponse(null, null, null)
            else
                null
        }
    }

    companion object {
        private const val FILE_STORE = "ad_filter"

        private var instance: AdFilter? = null

        fun get(): AdFilter {
            if (instance == null) {
                throw RuntimeException("Should call create() before get()")
            }
            return instance!!
        }

        fun create(application: Application): AdFilter {
            if (instance != null) {
                throw InstantiationException("Instance already created!")
            }
            instance = AdFilter(application)
            return instance!!
        }
    }
}