package com.learn.reqlite.domain.graphql.format

object GraphQLFormatter {

    fun format(query: String): String {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return ""

        val lines = trimmed.lines()
        val result = StringBuilder()
        var indentLevel = 0
        val indentStr = "  "

        for (line in lines) {
            val cleanLine = line.trim()
            if (cleanLine.isEmpty()) continue

            // Count closing braces at start of line
            val closingAtStart = cleanLine.takeWhile { it == '}' }.length
            indentLevel = (indentLevel - closingAtStart).coerceAtLeast(0)

            for (i in 0 until indentLevel) {
                result.append(indentStr)
            }
            result.append(cleanLine).append("\n")

            // Count net open braces not accounted for by closing at start
            val opens = cleanLine.count { it == '{' }
            val closes = cleanLine.drop(closingAtStart).count { it == '}' }
            indentLevel = (indentLevel + opens - closes).coerceAtLeast(0)
        }

        return result.toString().trimEnd()
    }
}
