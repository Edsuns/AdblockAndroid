package io.github.edsuns.adfilter

import android.content.Context
import io.github.edsuns.adfilter.Configuration.Companion.DEFAULT_SUBSCRIPTIONS
import java.io.File

/**
 * Created by Edsuns@qq.com on 2020/10/24.
 */
class FilterDataStore(context: Context) : Configuration {
    override var isEnabled: Boolean = true
    override val subscriptionList: MutableList<Subscription> = mutableListOf()
    private val dir = File(context.filesDir, FILTER_DIRECTORY)
    private val store = BinaryDataStore(dir)

    init {
        if (configured()) {
            injectFromJson(readConfig(), this)
        } else {
            subscriptionList.addAll(DEFAULT_SUBSCRIPTIONS)
        }
    }

    override fun configured(): Boolean =
        File(dir, FILE_CONFIG).exists()

    private fun saveConfig() {
        val json = toJson(this)
        dir.mkdirs()
        File(dir, FILE_CONFIG).writeBytes(json.toByteArray())
    }

    private fun readConfig(): String {
        val file = File(dir, FILE_CONFIG)
        return file.readText()
    }

    fun hasData(id: String) = store.hasData(id)

    fun loadData(id: String): ByteArray = store.loadData(id)

    fun saveData(id: String, data: ByteArray) {
        store.saveData(id, data)
        subscriptionList.forEach {
            if (it.id == id) it.updateTimestamp = System.currentTimeMillis()
        }
        saveConfig()
    }

    fun deleteData(id: String) {
        store.clearData(id)
        subscriptionList.removeAll { it.id == id }
        saveConfig()
    }

    companion object {
        const val FILTER_DIRECTORY = "AdFilter"
        const val FILE_CONFIG = "config"
    }
}