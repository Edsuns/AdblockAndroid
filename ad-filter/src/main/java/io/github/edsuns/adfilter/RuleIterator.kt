package io.github.edsuns.adfilter

/**
 * Created by Edsuns@qq.com on 2021/1/24.
 */
class RuleIterator internal constructor(data: String? = null) {

    val dataBuilder: StringBuilder = if (data == null) StringBuilder() else StringBuilder(data)

    private var curLine: Int = -1
    private var lineStart: Int = 0
    private var lineEnd: Int = 0

    private fun reset() {
        curLine = -1
        lineStart = 0
        lineEnd = 0
    }

    fun hasNext(): Boolean = lineEnd + 1 < dataBuilder.length

    fun next(): String = get(curLine + 1)

    fun get(line: Int): String {
        if (curLine == line) {
            return dataBuilder.substring(lineStart, lineEnd)
        }
        if (curLine > line) {
            reset()
        }
        while (curLine + 1 < line) {
            lineStart = dataBuilder.indexOf(LINE_END, lineStart)
            if (lineStart == -1 || lineStart + 1 >= dataBuilder.length) {
                return ""
            }
            lineStart++
            curLine++
        }
        lineEnd = dataBuilder.indexOf(LINE_END, lineStart)
        if (lineEnd == -1) {
            return ""
        }
        return dataBuilder.substring(lineStart, lineEnd)
    }

    fun contains(rule: String): Boolean {
        return dataBuilder.contains(rule + LINE_END)
    }

    internal fun append(rule: String) {
        if (!contains(rule)) {
            dataBuilder.append(rule).append(LINE_END)
        }
    }

    fun isComment(rule: String): Boolean {
        return rule.startsWith("! ")
    }

    fun comment(rule: String) {
        if (contains(rule)) {
            dataBuilder.replace(Regex("^(!\\s+)?$rule\\n$"), "! $rule")
        }
    }

    fun uncomment(rule: String) {
        var mRule = rule
        if (isComment(rule)) {
            mRule = rule.substring(2).trim()
        }
        dataBuilder.replace(Regex("^!\\s+$mRule\\n$", RegexOption.MULTILINE), mRule)
    }

    fun remove(rule: String) {
        dataBuilder.replace(Regex("^$rule\\n$", RegexOption.MULTILINE), "")
    }

    companion object {
        private const val LINE_END = "\n"
    }
}