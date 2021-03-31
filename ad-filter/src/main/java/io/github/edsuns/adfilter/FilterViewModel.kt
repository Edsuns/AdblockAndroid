package io.github.edsuns.adfilter

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.work.*
import io.github.edsuns.adfilter.workers.DownloadWorker
import io.github.edsuns.adfilter.workers.InstallationWorker
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Created by Edsuns@qq.com on 2021/1/1.
 */
class FilterViewModel internal constructor(
    context: Context,
    private val filterDataLoader: FilterDataLoader
) {

    internal val sharedPreferences: FilterSharedPreferences =
        FilterSharedPreferences(context)

    val isEnabled: MutableLiveData<Boolean> by lazy { MutableLiveData(sharedPreferences.isEnabled) }

    private val workManager: WorkManager = WorkManager.getInstance(context)

    val workInfo: LiveData<List<WorkInfo>> = workManager.getWorkInfosByTagLiveData(TAG_FILTER_WORK)

    val enabledFilterCount: LiveData<Int> = MutableLiveData()

    internal fun updateEnabledFilterCount() {
        (enabledFilterCount as MutableLiveData).value = filterDataLoader.detector.clients.size
    }

    private val filterMap: MutableLiveData<LinkedHashMap<String, Filter>> by lazy {
        MutableLiveData(Json.decodeFromString(sharedPreferences.filterMap))
    }

    val filters: LiveData<LinkedHashMap<String, Filter>> = filterMap

    val downloadFilterIdMap: HashMap<String, String> by lazy { sharedPreferences.downloadFilterIdMap }

    val onDirty: LiveData<None> = MutableLiveData()

    init {
        workManager.pruneWork()
        // clear bad running download state
        filters.value?.values?.forEach {
            if (it.downloadState.isRunning) {
                val list = workManager.getWorkInfosForUniqueWork(it.id).get()
                if (list == null || list.isEmpty()) {
                    it.downloadState = DownloadState.FAILED
                    flushFilter()
                } else {
                    if (list[0].state == WorkInfo.State.ENQUEUED
                        && it.downloadState != DownloadState.ENQUEUED
                    ) {
                        it.downloadState = DownloadState.ENQUEUED
                        flushFilter()
                    }
                }
            }
        }
    }

    fun addFilter(name: String, url: String): Filter {
        val filter = Filter(url)
        filter.name = name
        filterMap.value?.get(filter.id)?.let {
            return it
        }
        filterMap.value?.set(filter.id, filter)
        flushFilter()
        return filter
    }

    fun removeFilter(id: String) {
        cancelDownload(id)
        filterDataLoader.remove(id)
        filterMap.value?.remove(id)
        flushFilter()
    }

    fun setFilterEnabled(id: String, enabled: Boolean) {
        setFilterEnabled(id, enabled, true)
    }

    fun setFilterEnabled(id: String, enabled: Boolean, post: Boolean) {
        filterMap.value?.get(id)?.let {
            val enableMask = enabled && it.hasDownloaded()
            if (it.isEnabled != enableMask) {
                if (enableMask)
                    enableFilter(it)
                else
                    disableFilter(it)
                // refresh
                if (post)
                    filterMap.postValue(filterMap.value)
                saveFilterMap()
            }
        }
    }

    internal fun enableFilter(filter: Filter) {
        if (isEnabled.value == true && filter.filtersCount > 0) {
            filterDataLoader.load(filter.id)
            filter.isEnabled = true
            updateEnabledFilterCount()
            // notify onDirty
            (onDirty as MutableLiveData).value = None.Value
        }
    }

    private fun disableFilter(filter: Filter) {
        filterDataLoader.unload(filter.id)
        filter.isEnabled = false
        updateEnabledFilterCount()
        // notify onDirty
        (onDirty as MutableLiveData).value = None.Value
    }

    fun renameFilter(id: String, name: String) {
        filterMap.value?.get(id)?.let {
            it.name = name
            flushFilter()
        }
    }

    fun isCustomFilterEnabled(): Boolean = filterDataLoader.isCustomFilterEnabled()

    fun enableCustomFilter() {
        if (!isCustomFilterEnabled()) {
            filterDataLoader.load(FilterDataLoader.ID_CUSTOM)
        }
    }

    fun disableCustomFilter() {
        filterDataLoader.unloadCustomFilter()
    }

    fun download(id: String) {
        filterMap.value?.get(id)?.let {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresCharging(false)
                .build()
            val inputData = workDataOf(
                KEY_FILTER_ID to it.id,
                KEY_DOWNLOAD_URL to it.url
            )
            val download =
                OneTimeWorkRequestBuilder<DownloadWorker>()
                    .setConstraints(constraints)
                    .addTag(TAG_FILTER_WORK)
                    .setInputData(inputData)
                    .build()
            val install =
                OneTimeWorkRequestBuilder<InstallationWorker>()
                    .addTag(TAG_FILTER_WORK)
                    .addTag(TAG_INSTALLATION)
                    .setInputData(
                        workDataOf(
                            KEY_RAW_CHECKSUM to it.checksum,
                            KEY_CHECK_LICENSE to true
                        )
                    )
                    .build()
            val continuation = workManager.beginUniqueWork(
                it.id, ExistingWorkPolicy.KEEP, download
            ).then(install)
            // record worker ids
            downloadFilterIdMap[download.id.toString()] = it.id
            downloadFilterIdMap[install.id.toString()] = it.id
            sharedPreferences.downloadFilterIdMap = downloadFilterIdMap
            // mark the beginning of the download
            it.downloadState = DownloadState.NONE
            // start the work
            continuation.enqueue()
        }
    }

    fun cancelDownload(id: String) {
        workManager.cancelUniqueWork(id)
    }

    fun flushFilter() {
        // refresh
        filterMap.postValue(filterMap.value)
        saveFilterMap()
    }

    private fun saveFilterMap() {
        sharedPreferences.filterMap = Json.encodeToString(filterMap.value)
        Timber.v("Save sharedPreferences.filterMap")
    }

    companion object {
        private const val TAG_FILTER_WORK = "TAG_FILTER_WORK"
    }
}