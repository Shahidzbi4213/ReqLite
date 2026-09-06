package com.learn.reqlite.ui.response.json

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

object JsonTreeParser {

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = false
        prettyPrint = false
    }

    private val prettyJson = Json {
        ignoreUnknownKeys = true
        isLenient = false
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    fun parseJsonToTree(rawJson: String): JsonTreeNode? {
        if (rawJson.isBlank()) return null
        return try {
            val element = jsonParser.parseToJsonElement(rawJson.trim())
            parseElement(element, key = null, path = "$")
        } catch (_: Exception) {
            null
        }
    }

    private fun parseElement(element: JsonElement, key: String?, path: String): JsonTreeNode {
        return when (element) {
            is JsonObject -> {
                val children = element.entries.map { (childKey, childValue) ->
                    val childPath = if (path == "$") "$.$childKey" else "$path.$childKey"
                    parseElement(childValue, key = childKey, path = childPath)
                }
                JsonTreeNode.ObjectNode(key = key, path = path, children = children)
            }
            is JsonArray -> {
                val children = element.mapIndexed { index, childValue ->
                    val childPath = "$path[$index]"
                    parseElement(childValue, key = "[$index]", path = childPath)
                }
                JsonTreeNode.ArrayNode(key = key, path = path, children = children)
            }
            is JsonNull -> {
                JsonTreeNode.PrimitiveNode(
                    key = key,
                    path = path,
                    value = "null",
                    type = JsonTreeNode.PrimitiveType.NULL
                )
            }
            is JsonPrimitive -> {
                val type = when {
                    element.isString -> JsonTreeNode.PrimitiveType.STRING
                    element.booleanOrNull != null -> JsonTreeNode.PrimitiveType.BOOLEAN
                    else -> JsonTreeNode.PrimitiveType.NUMBER
                }
                JsonTreeNode.PrimitiveNode(
                    key = key,
                    path = path,
                    value = element.content,
                    type = type
                )
            }
        }
    }

    fun formatPrettyJson(rawJson: String): String {
        if (rawJson.isBlank()) return ""
        return try {
            val element = jsonParser.parseToJsonElement(rawJson.trim())
            prettyJson.encodeToString(JsonElement.serializer(), element)
        } catch (_: Exception) {
            rawJson
        }
    }

    fun highlightJsonSyntaxLines(
        jsonString: String,
        isDark: Boolean = true
    ): List<AnnotatedString> {
        val lines = jsonString.lines()
        return lines.map { highlightJsonSyntax(it, isDark) }
    }

    fun highlightJsonSyntax(
        jsonString: String,
        isDark: Boolean = true
    ): AnnotatedString {
        if (jsonString.isBlank()) return AnnotatedString("")

        val keyColor = if (isDark) Color(0xFF90CAF9) else Color(0xFF1565C0)
        val stringColor = if (isDark) Color(0xFFA5D6A7) else Color(0xFF2E7D32)
        val numberColor = if (isDark) Color(0xFFFFCC80) else Color(0xFFE65100)
        val boolColor = if (isDark) Color(0xFFCE93D8) else Color(0xFF7B1FA2)
        val nullColor = if (isDark) Color(0xFFB0BEC5) else Color(0xFF546E7A)
        val punctuationColor = if (isDark) Color(0xFFEEEEEE) else Color(0xFF212121)

        val annotatedString = buildAnnotatedString {
            var i = 0
            val len = jsonString.length

            while (i < len) {
                val c = jsonString[i]

                if (c == '"') {
                    // String or Key
                    val start = i
                    i++
                    var isEscaped = false
                    while (i < len) {
                        val curr = jsonString[i]
                        if (curr == '\\' && !isEscaped) {
                            isEscaped = true
                        } else if (curr == '"' && !isEscaped) {
                            i++
                            break
                        } else {
                            isEscaped = false
                        }
                        i++
                    }
                    val token = jsonString.substring(start, i)
                    
                    // Look ahead for colon (indicating a key)
                    var j = i
                    while (j < len && (jsonString[j] == ' ' || jsonString[j] == '\t')) {
                        j++
                    }
                    val isKey = j < len && jsonString[j] == ':'

                    val style = if (isKey) {
                        SpanStyle(color = keyColor, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    } else {
                        SpanStyle(color = stringColor, fontFamily = FontFamily.Monospace)
                    }

                    val startIndex = length
                    append(token)
                    addStyle(style, startIndex, length)
                } else if (c.isDigit() || (c == '-' && i + 1 < len && jsonString[i + 1].isDigit())) {
                    // Number
                    val start = i
                    i++
                    while (i < len && (jsonString[i].isDigit() || jsonString[i] == '.' || jsonString[i] == 'e' || jsonString[i] == 'E' || jsonString[i] == '+' || jsonString[i] == '-')) {
                        i++
                    }
                    val token = jsonString.substring(start, i)
                    val startIndex = length
                    append(token)
                    addStyle(SpanStyle(color = numberColor, fontFamily = FontFamily.Monospace), startIndex, length)
                } else if (jsonString.startsWith("true", i)) {
                    val startIndex = length
                    append("true")
                    addStyle(SpanStyle(color = boolColor, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace), startIndex, length)
                    i += 4
                } else if (jsonString.startsWith("false", i)) {
                    val startIndex = length
                    append("false")
                    addStyle(SpanStyle(color = boolColor, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace), startIndex, length)
                    i += 5
                } else if (jsonString.startsWith("null", i)) {
                    val startIndex = length
                    append("null")
                    addStyle(SpanStyle(color = nullColor, fontWeight = FontWeight.Normal, fontFamily = FontFamily.Monospace), startIndex, length)
                    i += 4
                } else {
                    // Punctuations / whitespace / brackets
                    val style = if (c in "{}[],:") {
                        SpanStyle(color = punctuationColor, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
                    } else {
                        SpanStyle(fontFamily = FontFamily.Monospace)
                    }
                    val startIndex = length
                    append(c)
                    addStyle(style, startIndex, length)
                    i++
                }
            }
        }

        return annotatedString
    }
}
