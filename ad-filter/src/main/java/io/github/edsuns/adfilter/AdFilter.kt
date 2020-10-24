package io.github.edsuns.adfilter

import android.app.Application
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
class AdFilter(private val application: Application) {

    private val adDetector: AdDetector by lazy { AdDetector() }
    private val filterDataStore: FilterDataStore by lazy { FilterDataStore(application) }
    private val filterDataLoader: FilterDataLoader by lazy {
        FilterDataLoader(
            adDetector,
            filterDataStore
        )
    }
    private val dataDownloader: DataDownloader by lazy {
        DataDownloader(
            application,
            filterDataStore
        )
    }

    private fun init() {
        doAsync {
            if (filterDataStore.configured()) {
                filterDataStore.subscriptionList.forEach {
                    if (it.isEnabled && it.downloaded()) {
                        filterDataLoader.load(it.id)
                    }
                }
            } else {
                val enabled = filterDataStore.subscriptionList.filter { it.isEnabled }
                dataDownloader.download(enabled)
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
            instance!!.init()
            return instance!!
        }
    }
}