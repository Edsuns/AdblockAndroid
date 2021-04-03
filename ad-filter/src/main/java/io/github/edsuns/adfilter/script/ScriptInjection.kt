package io.github.edsuns.adfilter.script

import io.github.edsuns.adfilter.BuildConfig

/**
 * Created by Edsuns@qq.com on 2021/4/3.
 */
object ScriptInjection {
    private const val DEBUG_FLAG = "{{DEBUG}}"
    private const val JS_BRIDGE = "{{BRIDGE}}"
    private const val HIDDEN_FLAG = "{{HIDDEN_FLAG}}"

    private val hiddenFlag = randomAlphanumericString()

    private fun randomAlphanumericString(): String {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z')
        val outputStrLength = (10..36).shuffled().first()

        return (1..outputStrLength)
            .map { kotlin.random.Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    fun parseScript(raw: String, bridgeName: String): String {
        var js = raw.replace(DEBUG_FLAG, if (BuildConfig.DEBUG) "" else "//")
        js = js.replace(JS_BRIDGE, bridgeName)
        js = js.replace(HIDDEN_FLAG, hiddenFlag)
        return js
    }
}