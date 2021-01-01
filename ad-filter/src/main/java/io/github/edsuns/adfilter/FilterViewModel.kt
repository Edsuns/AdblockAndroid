package io.github.edsuns.adfilter

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.work.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Created by Edsuns@qq.com on 2021/1/1.
 */
class FilterViewModel internal constructor(
    application: Application,
    private val filterDataLoader: FilterDataLoader
) {

    internal val sharedPreferences: FilterSharedPreferences =
        FilterSharedPreferences(application)

    val isEnabled: MutableLiveData<Boolean> by lazy { MutableLiveData(sharedPreferences.isEnabled) }

    private val workManager: WorkManager = WorkManager.getInstance(application)

    val workInfo: LiveData<List<WorkInfo>> = workManager.getWorkInfosByTagLiveData(TAG_WORK)

    private val filterMap: MutableLiveData<HashMap<String, Filter>> by lazy {
        MutableLiveData(Json.decodeFromString<HashMap<String, Filter>>(sharedPreferences.filterMap))
    }

    val filters: LiveData<HashMap<String, Filter>> = filterMap

    fun addFilter(name: String, url: String): Filter {
        val filter = Filter(url)
        filter.name = name
        filterMap.value?.set(filter.id, filter)
        // refresh
        filterMap.postValue(filterMap.value)
        return filter
    }

    internal fun updateFilter(filter: Filter) {
        filterMap.value?.get(filter.id)?.let {
            it.name = filter.name
            it.updateTime = filter.updateTime
            it.isEnabled = filter.isEnabled
        }
        // refresh
        filterMap.postValue(filterMap.value)
    }

    fun removeFilter(id: String) {
        filterDataLoader.remove(id)
        filterMap.value?.remove(id)
        // refresh
        filterMap.postValue(filterMap.value)
    }

    fun setFilterEnabled(id: String, enabled: Boolean) {
        filterMap.value?.get(id)?.let {
            if (enabled)
                filterDataLoader.load(it.id)
            else
                filterDataLoader.unload(it.id)
            it.isEnabled = enabled
        }
        // refresh
        filterMap.postValue(filterMap.value)
    }

    fun renameFilter(id: String, name: String) {
        filterMap.value?.get(id)?.let { it.name = name }
        // refresh
        filterMap.postValue(filterMap.value)
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
            val request =
                OneTimeWorkRequestBuilder<DownloadWorker>()
                    .setConstraints(constraints)
                    .addTag(TAG_WORK)
                    .setInputData(inputData)
                    .build()
            val continuation = workManager.beginUniqueWork(
                it.id, ExistingWorkPolicy.KEEP, request
            )
            continuation.enqueue()
            it.downloadState = DownloadState.DOWNLOADING
            updateFilter(it)
        }
    }

    fun saveSharedPreferences() {
        sharedPreferences.isEnabled = isEnabled.value ?: false
        sharedPreferences.filterMap = Json.encodeToString(filterMap.value)
    }

    companion object {
        private const val TAG_WORK = "tag_ad_filter_work"
    }
}