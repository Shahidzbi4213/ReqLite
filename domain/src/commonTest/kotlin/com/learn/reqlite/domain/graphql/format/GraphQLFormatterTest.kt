package com.learn.reqlite.domain.graphql.format

import kotlin.test.*

class GraphQLFormatterTest {

    @Test
    fun testFormatSimpleQuery() {
        val raw = "query { user { id name } }"
        val formatted = GraphQLFormatter.format(raw)
        val expected = """
        query { user { id name } }
        """.trimIndent()
        // If single line, format maintains trimmed content
        assertEquals(expected, formatted)
    }

    @Test
    fun testFormatMultilineQuery() {
        val raw = """
query {
user(id: "1") {
id
name
profile {
avatarUrl
}
}
}
        """.trimIndent()

        val formatted = GraphQLFormatter.format(raw)
        val expected = """
query {
  user(id: "1") {
    id
    name
    profile {
      avatarUrl
    }
  }
}
        """.trimIndent()

        assertEquals(expected, formatted)
    }
}
