package io.github.edsuns.adfilter

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Created by Edsuns@qq.com on 2021/1/1.
 */
class FilterViewModel internal constructor(application: Application) {

    private val sharedPreferences: FilterSharedPreferences =
        FilterSharedPreferences(application)

    val isEnabled: MutableLiveData<Boolean> by lazy {
        val data = MutableLiveData<Boolean>()
        data.value = sharedPreferences.isEnabled
        data
    }

    private val workManager: WorkManager = WorkManager.getInstance(application)

    val workInfo: LiveData<List<WorkInfo>> = workManager.getWorkInfosByTagLiveData(TAG_WORK)

    private val filterMap: MutableLiveData<HashMap<String, Filter>> by lazy {
        val data = MutableLiveData<HashMap<String, Filter>>()
        data.value = Json.decodeFromString<HashMap<String, Filter>>(
            sharedPreferences.filterMap
        )
        data
    }

    val filters: LiveData<HashMap<String, Filter>> = filterMap

    fun addFilter(name: String, url: String) {
        val filter = Filter(name, url)
        filterMap.value?.set(filter.id, filter)
        // refresh
        filterMap.value = filterMap.value
    }

    fun updateFilter(filter: Filter) {
        filterMap.value?.get(filter.id)?.let {
            it.name = filter.name
            it.updateTime = filter.updateTime
            it.isEnabled = filter.isEnabled
        }
        // refresh
        filterMap.value = filterMap.value
    }

    fun removeFilter(id: String) {
        filterMap.value?.remove(id)
        // refresh
        filterMap.value = filterMap.value
    }

    fun download(id: String) {
        filterMap.value?.get(id)?.let {
            it.downloading = true
            updateFilter(it)
            TODO("Not yet implemented")
        }
    }

    fun saveSharedPreferences() {
        sharedPreferences.filterMap = Json.encodeToString(filterMap.value)
    }

    companion object {
        private const val TAG_WORK = "tag_ad_filter_work"
    }
}