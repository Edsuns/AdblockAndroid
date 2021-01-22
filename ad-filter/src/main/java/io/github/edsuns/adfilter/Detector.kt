package io.github.edsuns.adfilter

import io.github.edsuns.adblockclient.Client
import io.github.edsuns.adblockclient.ResourceType
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Created by Edsuns@qq.com on 2020/10/24.
 */
interface AbstractDetector {
    fun addClient(client: Client)
    fun removeClient(id: String)
    fun clearAllClient()
    fun shouldBlock(url: String, documentUrl: String, resourceType: ResourceType): Boolean
    fun getElementHidingSelectors(documentUrl: String): String
}

internal class Detector : AbstractDetector {

    private val clients = CopyOnWriteArrayList<Client>()

    override fun addClient(client: Client) {
        clients.removeAll { it.id == client.id }
        clients.add(client)
        Timber.v("Client count: ${clients.size} (after addClient)")
    }

    override fun removeClient(id: String) {
        clients.removeAll { it.id == id }
        Timber.v("Client count: ${clients.size} (after removeClient)")
    }

    override fun clearAllClient() {
        clients.clear()
        Timber.v("Client count: ${clients.size} (after clearAllClient)")
    }

    override fun shouldBlock(
        url: String,
        documentUrl: String,
        resourceType: ResourceType
    ): Boolean {
        return clients.any {
            it.matches(url, documentUrl, resourceType)
        }
    }

    override fun getElementHidingSelectors(documentUrl: String): String {
        val builder = StringBuilder()
        for ((index, client) in clients.withIndex()) {
            val selector = client.getElementHidingSelectors(documentUrl) ?: continue
            if (selector.isNotBlank()) {
                if (index > 0) {
                    builder.append(", ")
                }
                builder.append(selector)
            }
        }
        return builder.toString()
    }
}