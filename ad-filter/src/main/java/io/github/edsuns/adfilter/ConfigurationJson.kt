package io.github.edsuns.adfilter

import org.json.JSONArray
import org.json.JSONObject

/**
 * Created by Edsuns@qq.com on 2020/10/24.
 */
interface Configuration {
    var isEnabled: Boolean
    val subscriptionList: MutableList<Subscription>

    fun configured(): Boolean

    companion object {
        val DEFAULT_SUBSCRIPTIONS = arrayListOf(
            Subscription(
                "https://easylist-downloads.adblockplus.org/easylist.txt",
                "EasyList",
                isEnabled = true,
                isBuiltin = true
            ),
            Subscription(
                "https://easylist-downloads.adblockplus.org/easyprivacy.txt",
                "EasyPrivacy",
                isEnabled = true,
                isBuiltin = true
            ),
            Subscription(
                "https://easylist-downloads.adblockplus.org/easylistchina.txt",
                "EasyListChina",
                isEnabled = true,
                isBuiltin = true
            ),
        )
    }
}

fun toJson(configuration: Configuration): String {
    val jsonObject = JSONObject()

    jsonObject.put("isEnabled", configuration.isEnabled)

    val array = JSONArray()
    for (s in configuration.subscriptionList) {
        array.put(toJSONObject(s))
    }
    jsonObject.put("subscriptionList", array)

    return jsonObject.toString()
}

fun injectFromJson(json: String, configuration: Configuration) {
    val jsonObject = JSONObject(json)

    configuration.isEnabled = jsonObject.getBoolean("isEnabled")

    val list = configuration.subscriptionList
    val array = jsonObject.getJSONArray("subscriptionList")
    for (i in 0 until array.length()) {
        list.add(fromJSONObject(array.getJSONObject(i)))
    }
}

fun toJSONObject(subscription: Subscription): JSONObject {
    val jsonObject = JSONObject()
    jsonObject.put("url", subscription.url)
    jsonObject.put("name", subscription.name)
    jsonObject.put("isEnabled", subscription.isEnabled)
    jsonObject.put("isBuiltin", subscription.isBuiltin)
    jsonObject.put("id", subscription.id)
    jsonObject.put("updateTimestamp", subscription.updateTimestamp)

    return jsonObject
}

fun fromJSONObject(jsonObject: JSONObject): Subscription {
    return Subscription(
        url = jsonObject.getString("url"),
        name = jsonObject.getString("name"),
        isEnabled = jsonObject.getBoolean("isEnabled"),
        isBuiltin = jsonObject.getBoolean("isBuiltin"),
        updateTimestamp = jsonObject.getLong("updateTimestamp"),
    )
}
