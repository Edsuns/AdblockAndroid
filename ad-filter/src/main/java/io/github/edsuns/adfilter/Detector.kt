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
    fun shouldBlock(url: String, documentUrl: String, resourceType: ResourceType): Boolean
}

internal class Detector : AbstractDetector {

    private val clients = CopyOnWriteArrayList<Client>()

    override fun addClient(client: Client) {
        clients.removeAll { it.id == client.id }
        clients.add(client)
        Timber.v("Client count: ${clients.size} (addClient)")
    }

    override fun removeClient(id: String) {
        clients.removeAll { it.id == id }
        Timber.v("Client count: ${clients.size} (removeClient)")
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
}