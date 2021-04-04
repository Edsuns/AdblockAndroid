package io.github.edsuns.adfilter.script

import io.github.edsuns.adfilter.BuildConfig

/**
 * Created by Edsuns@qq.com on 2021/4/3.
 */
object ScriptInjection {
    private const val DEBUG_FLAG = "{{DEBUG}}"
    private const val JS_BRIDGE = "{{BRIDGE}}"
    private const val HIDDEN_FLAG = "{{HIDDEN_FLAG}}"

    private val bridgeRegister = arrayListOf(
        ElementHiding::class.java,
        Scriptlet::class.java
    )

    private val hiddenFlag = randomAlphanumericString()
    private val bridgeNamePrefix = randomAlphanumericString()

    fun bridgeNameFor(owner: Any): String {
        val clazz = owner::class.java
        val index = bridgeRegister.indexOf(clazz)
        if (index < 0) {
            error("$clazz isn't registered as a bridge!")
        }
        return bridgeNamePrefix + index
    }

    private fun randomAlphanumericString(): String {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z')
        val outputStrLength = (10..36).shuffled().first()

        return (1..outputStrLength)
            .map { kotlin.random.Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    fun parseScript(owner: Any, raw: String): String {
        var js = raw.replace(DEBUG_FLAG, if (BuildConfig.DEBUG) "" else "//")
        js = js.replace(JS_BRIDGE, bridgeNameFor(owner))
        js = js.replace(HIDDEN_FLAG, hiddenFlag)
        return js
    }
}