package io.github.edsuns.adblockclient.sample

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.edsuns.adblockclient.sample.main.MainActivity
import io.github.edsuns.adblockclient.sample.settings.SettingsActivity
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

        viewModel.workToFilterMap.observeForever { notifyDownloading(it.isEmpty()) }
    }

    private var isDownloading = false
    private val channelId = "DOWNLOAD"
    private val notificationId = 1

    private fun notifyDownloading(finished: Boolean) {
        if (isDownloading != finished) {// only accept valid event
            return
        }

        val clazz = if (finished) MainActivity::class.java else SettingsActivity::class.java
        val intent = Intent(this, clazz).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        val builder = NotificationCompat.Builder(this, channelId).apply {
            setContentTitle(getString(R.string.filter_download))
            setContentIntent(pendingIntent)
            setDefaults(NotificationCompat.DEFAULT_ALL)
            setVibrate(longArrayOf(0L))
            setSound(null)
            priority = NotificationCompat.PRIORITY_HIGH
        }
        if (finished) {
            isDownloading = false
            builder.apply {
                setContentText(getString(R.string.download_complete))
                setSmallIcon(android.R.drawable.stat_sys_download_done)
                setProgress(0, 0, false)
                setOngoing(false)
            }
        } else {
            isDownloading = true
            builder.apply {
                setContentText(getString(R.string.download_in_progress))
                setSmallIcon(android.R.drawable.stat_sys_download)
                setProgress(0, 0, true)
                setOngoing(true)// make the notification unable to be cleared
            }
        }
        NotificationManagerCompat.from(this).apply {
            // Make a channel if necessary
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Create the NotificationChannel, but only on API 26+ because
                // the NotificationChannel class is new and not in the support library
                val name: CharSequence = getString(R.string.notification_channel_name)
                val description: String = getString(R.string.notification_description)
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(channelId, name, importance)
                channel.setSound(null, null)
                channel.description = description

                // Add the channel
                createNotificationChannel(channel)
            }
            val notification = builder.build()
            cancel(notificationId)// fix notification remains on MIUI 12.5
            notify(notificationId, notification)
        }
    }
}