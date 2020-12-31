package io.github.edsuns.adfilter

import io.github.edsuns.adblockclient.Client
import io.github.edsuns.adblockclient.ResourceType
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Created by Edsuns@qq.com on 2020/10/24.
 */
interface AbstractAdDetector {
    fun addClient(client: Client)
    fun shouldBlock(url: String, documentUrl: String, resourceType: ResourceType): Boolean
}

internal class AdDetector : AbstractAdDetector {

    private val clients = CopyOnWriteArrayList<Client>()

    override fun addClient(client: Client) {
        clients.removeAll { it.id == client.id }
        clients.add(client)
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