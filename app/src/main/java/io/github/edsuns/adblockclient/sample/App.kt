package io.github.edsuns.adblockclient.sample

import android.app.Application
import io.github.edsuns.adfilter.AdFilter
import timber.log.Timber

/**
 * Created by Edsuns@qq.com on 2021/1/1.
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        val filter = AdFilter.create(this)
        val viewModel = filter.viewModel
        if (!filter.hasInstallation) {
            val map = mapOf(
                "EasyList" to "https://easylist-downloads.adblockplus.org/easylist.txt",
                "EasyPrivacy" to "https://easylist-downloads.adblockplus.org/easyprivacy.txt",
                "EasyList China" to "https://easylist-downloads.adblockplus.org/easylistchina.txt"
            )
            for ((key, value) in map) {
                val f = viewModel.addFilter(key, value)
                viewModel.download(f.id)
            }
        }
    }
}