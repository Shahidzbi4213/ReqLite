package com.learn.reqlite.domain.graphql.explorer

import com.learn.reqlite.domain.graphql.model.*
import kotlin.test.*

class GraphQLSchemaExplorerTest {

    private val userType = GraphQLObjectType(
        name = "User",
        description = "User entity",
        fields = mapOf(
            "id" to GraphQLField(name = "id", typeRef = GraphQLTypeRef(GraphQLTypeKind.NON_NULL, ofType = GraphQLTypeRef(GraphQLTypeKind.SCALAR, "ID"))),
            "name" to GraphQLField(name = "name", typeRef = GraphQLTypeRef(GraphQLTypeKind.SCALAR, "String"))
        )
    )

    private val queryType = GraphQLObjectType(
        name = "Query",
        description = "Root queries",
        fields = mapOf(
            "user" to GraphQLField(
                name = "user",
                description = "Get a user by ID",
                args = listOf(
                    GraphQLInputField(name = "id", typeRef = GraphQLTypeRef(kind = GraphQLTypeKind.NON_NULL, ofType = GraphQLTypeRef(kind = GraphQLTypeKind.SCALAR, name = "ID")))
                ),
                typeRef = GraphQLTypeRef(GraphQLTypeKind.OBJECT, "User")
            )
        )
    )

    private val schema = GraphQLSchema(
        queryType = "Query",
        types = mapOf(
            "Query" to queryType,
            "User" to userType
        )
    )

    @Test
    fun testFilterAll() {
        val categories = GraphQLSchemaExplorer.filter(schema, "")
        assertEquals(2, categories.size) // Queries, Types
        assertEquals("Queries", categories[0].title)
        assertEquals("Types", categories[1].title)
    }

    @Test
    fun testFilterQueryField() {
        val categories = GraphQLSchemaExplorer.filter(schema, "user")
        assertTrue(categories.any { it.title == "Queries" && it.items.any { item -> item.name == "user" } })
    }

    @Test
    fun testGenerateSnippet() {
        val userField = queryType.fields["user"]!!
        val snippet = GraphQLSchemaExplorer.generateSnippet(userField, schema, "query")
        assertTrue(snippet.contains("query UserQuery(\$id: ID!)"))
        assertTrue(snippet.contains("user(id: \$id)"))
        assertTrue(snippet.contains("id"))
        assertTrue(snippet.contains("name"))
    }
}
