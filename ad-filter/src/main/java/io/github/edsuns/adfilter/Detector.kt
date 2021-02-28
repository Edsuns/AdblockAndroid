package io.github.edsuns.adfilter

import io.github.edsuns.adblockclient.Client
import io.github.edsuns.adblockclient.MatchResult
import io.github.edsuns.adblockclient.ResourceType
import io.github.edsuns.adblockclient.isException
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Created by Edsuns@qq.com on 2020/10/24.
 */
interface AbstractDetector {
    fun addClient(client: Client)
    fun removeClient(id: String)
    fun clearAllClient()
    fun shouldBlock(url: String, documentUrl: String, resourceType: ResourceType): String?
    fun getElementHidingSelectors(documentUrl: String): String
    fun getCustomElementHidingSelectors(documentUrl: String): String
}

internal class Detector : AbstractDetector {

    private val clients = CopyOnWriteArrayList<Client>()
    internal var whitelistClient: Client? = null
        set(value) {
            field = value
            Timber.v("Whitelist client changed")
        }
    internal var blacklistClient: Client? = null
        set(value) {
            field = value
            Timber.v("Blacklist client changed")
        }

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

    /**
     * returns not null if should block the web resource
     */
    override fun shouldBlock(
        url: String,
        documentUrl: String,
        resourceType: ResourceType
    ): String? {
        blacklistClient?.matches(url, documentUrl, resourceType)?.let {
            if (it.shouldBlock) {
                return it.matchedRule ?: ""
            }
        }
        whitelistClient?.matches(url, documentUrl, resourceType)?.let {
            if (it.shouldBlock) {
                return null// don't block whitelist
            }
        }
        var matchResult: MatchResult? = null
        for (client in clients) {
            val match: MatchResult = client.matches(url, documentUrl, resourceType)
            if (match.isException) {
                return null// don't block exception
            }
            if (match.shouldBlock) {
                matchResult = match
            }
        }
        return if (matchResult?.shouldBlock == true) matchResult.matchedRule ?: "" else null
    }

    override fun getElementHidingSelectors(documentUrl: String): String {
        val builder = StringBuilder()
        for (client in clients) {
            val selector = client.getElementHidingSelectors(documentUrl)
            if (!selector.isNullOrBlank()) {
                if (builder.isNotEmpty()) {
                    builder.append(", ")
                }
                builder.append(selector)
            }
        }
        return builder.toString()
    }

    override fun getCustomElementHidingSelectors(documentUrl: String): String {
        return blacklistClient?.getElementHidingSelectors(documentUrl) ?: ""
    }
}