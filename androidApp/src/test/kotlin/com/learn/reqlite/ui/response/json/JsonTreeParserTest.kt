package com.learn.reqlite.ui.response.json

import org.junit.Assert.*
import org.junit.Test

class JsonTreeParserTest {

    @Test
    fun parseJsonToTree_emptyOrBlank_returnsNull() {
        assertNull(JsonTreeParser.parseJsonToTree(""))
        assertNull(JsonTreeParser.parseJsonToTree("   \n\t"))
    }

    @Test
    fun parseJsonToTree_invalidJson_returnsNull() {
        assertNull(JsonTreeParser.parseJsonToTree("not a valid json {]"))
        assertNull(JsonTreeParser.parseJsonToTree("{ key: unmatched }"))
    }

    @Test
    fun parseJsonToTree_simpleObject() {
        val json = """{"name": "ReqLite", "version": 1, "active": true, "extra": null}"""
        val node = JsonTreeParser.parseJsonToTree(json)

        assertNotNull(node)
        assertTrue(node is JsonTreeNode.ObjectNode)
        val obj = node as JsonTreeNode.ObjectNode
        assertEquals(4, obj.size)

        val nameNode = obj.children.find { it.key == "name" } as JsonTreeNode.PrimitiveNode
        assertEquals("ReqLite", nameNode.value)
        assertEquals(JsonTreeNode.PrimitiveType.STRING, nameNode.type)
        assertEquals("$.name", nameNode.path)

        val versionNode = obj.children.find { it.key == "version" } as JsonTreeNode.PrimitiveNode
        assertEquals("1", versionNode.value)
        assertEquals(JsonTreeNode.PrimitiveType.NUMBER, versionNode.type)
        assertEquals("$.version", versionNode.path)

        val activeNode = obj.children.find { it.key == "active" } as JsonTreeNode.PrimitiveNode
        assertEquals("true", activeNode.value)
        assertEquals(JsonTreeNode.PrimitiveType.BOOLEAN, activeNode.type)
        assertEquals("$.active", activeNode.path)

        val nullNode = obj.children.find { it.key == "extra" } as JsonTreeNode.PrimitiveNode
        assertEquals("null", nullNode.value)
        assertEquals(JsonTreeNode.PrimitiveType.NULL, nullNode.type)
        assertEquals("$.extra", nullNode.path)
    }

    @Test
    fun parseJsonToTree_arrayStructure() {
        val json = """["apple", "banana", 42]"""
        val node = JsonTreeParser.parseJsonToTree(json)

        assertNotNull(node)
        assertTrue(node is JsonTreeNode.ArrayNode)
        val arr = node as JsonTreeNode.ArrayNode
        assertEquals(3, arr.size)

        val first = arr.children[0] as JsonTreeNode.PrimitiveNode
        assertEquals("apple", first.value)
        assertEquals("$[0]", first.path)

        val third = arr.children[2] as JsonTreeNode.PrimitiveNode
        assertEquals("42", third.value)
        assertEquals("$[2]", third.path)
    }

    @Test
    fun parseJsonToTree_nestedObjectAndArray() {
        val json = """
            {
                "user": {
                    "id": "u1",
                    "roles": ["admin", "editor"]
                }
            }
        """.trimIndent()
        val node = JsonTreeParser.parseJsonToTree(json)

        assertNotNull(node)
        val root = node as JsonTreeNode.ObjectNode
        val userNode = root.children[0] as JsonTreeNode.ObjectNode
        assertEquals("$.user", userNode.path)

        val rolesNode = userNode.children.find { it.key == "roles" } as JsonTreeNode.ArrayNode
        assertEquals("$.user.roles", rolesNode.path)
        assertEquals(2, rolesNode.size)

        val role0 = rolesNode.children[0] as JsonTreeNode.PrimitiveNode
        assertEquals("admin", role0.value)
        assertEquals("$.user.roles[0]", role0.path)
    }

    @Test
    fun formatPrettyJson_formatsWithIndentations() {
        val compact = """{"id":1,"name":"test"}"""
        val formatted = JsonTreeParser.formatPrettyJson(compact)

        assertTrue(formatted.contains("\n"))
        assertTrue(formatted.contains("  \"id\": 1"))
        assertTrue(formatted.contains("  \"name\": \"test\""))
    }

    @Test
    fun highlightJsonSyntax_returnsFormattedAnnotatedString() {
        val json = """{"status": "ok", "code": 200, "debug": false, "data": null}"""
        val annotated = JsonTreeParser.highlightJsonSyntax(json, isDark = true)

        assertEquals(json, annotated.text)
        assertTrue(annotated.spanStyles.isNotEmpty())
    }
}
