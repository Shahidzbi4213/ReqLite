package com.learn.reqlite.domain.graphql.autocomplete

import com.learn.reqlite.domain.graphql.model.*
import kotlin.test.*

class GraphQLAutocompleteEngineTest {

    private val userType = GraphQLObjectType(
        name = "User",
        description = "User type",
        fields = mapOf(
            "id" to GraphQLField(name = "id", typeRef = GraphQLTypeRef(kind = GraphQLTypeKind.NON_NULL, ofType = GraphQLTypeRef(kind = GraphQLTypeKind.SCALAR, name = "ID"))),
            "name" to GraphQLField(name = "name", typeRef = GraphQLTypeRef(kind = GraphQLTypeKind.SCALAR, name = "String")),
            "email" to GraphQLField(name = "email", typeRef = GraphQLTypeRef(kind = GraphQLTypeKind.SCALAR, name = "String")),
            "profile" to GraphQLField(name = "profile", typeRef = GraphQLTypeRef(kind = GraphQLTypeKind.OBJECT, name = "Profile"))
        )
    )

    private val profileType = GraphQLObjectType(
        name = "Profile",
        description = "Profile type",
        fields = mapOf(
            "avatarUrl" to GraphQLField(name = "avatarUrl", typeRef = GraphQLTypeRef(kind = GraphQLTypeKind.SCALAR, name = "String")),
            "bio" to GraphQLField(name = "bio", typeRef = GraphQLTypeRef(kind = GraphQLTypeKind.SCALAR, name = "String"))
        )
    )

    private val queryType = GraphQLObjectType(
        name = "Query",
        description = "Root Query",
        fields = mapOf(
            "user" to GraphQLField(
                name = "user",
                args = listOf(
                    GraphQLInputField(name = "id", typeRef = GraphQLTypeRef(kind = GraphQLTypeKind.NON_NULL, ofType = GraphQLTypeRef(kind = GraphQLTypeKind.SCALAR, name = "ID"))),
                    GraphQLInputField(name = "includeProfile", typeRef = GraphQLTypeRef(kind = GraphQLTypeKind.SCALAR, name = "Boolean"))
                ),
                typeRef = GraphQLTypeRef(kind = GraphQLTypeKind.OBJECT, name = "User")
            ),
            "allUsers" to GraphQLField(
                name = "allUsers",
                typeRef = GraphQLTypeRef(kind = GraphQLTypeKind.LIST, ofType = GraphQLTypeRef(kind = GraphQLTypeKind.OBJECT, name = "User"))
            )
        )
    )

    private val schema = GraphQLSchema(
        queryType = "Query",
        types = mapOf(
            "Query" to queryType,
            "User" to userType,
            "Profile" to profileType
        ),
        directives = listOf(
            GraphQLDirective(name = "include", description = "Include field")
        )
    )

    @Test
    fun testRootKeywordCompletions() {
        val completions = GraphQLAutocompleteEngine.getCompletions("q", 1, schema)
        assertTrue(completions.any { it.label == "query" && it.kind == CompletionKind.KEYWORD })
    }

    @Test
    fun testRootQueryFieldCompletions() {
        val query = "query {\n  \n}"
        val cursorOffset = 10 // right at the blank line inside query { }
        val completions = GraphQLAutocompleteEngine.getCompletions(query, cursorOffset, schema)

        val labels = completions.map { it.label }
        assertTrue(labels.contains("user"))
        assertTrue(labels.contains("allUsers"))
        assertTrue(labels.contains("__typename"))
    }

    @Test
    fun testNestedFieldCompletions() {
        val query = """
        query {
          user {
            
          }
        }
        """.trimIndent()
        val cursorOffset = query.indexOf("user {") + 7 + 5 // inside user { }
        val completions = GraphQLAutocompleteEngine.getCompletions(query, cursorOffset, schema)

        val labels = completions.map { it.label }
        assertTrue(labels.contains("id"))
        assertTrue(labels.contains("name"))
        assertTrue(labels.contains("email"))
        assertTrue(labels.contains("profile"))
    }

    @Test
    fun testDeeplyNestedFieldCompletions() {
        val query = """
        query {
          user {
            profile {
              
            }
          }
        }
        """.trimIndent()
        val cursorOffset = query.indexOf("profile {") + 10 + 7
        val completions = GraphQLAutocompleteEngine.getCompletions(query, cursorOffset, schema)

        val labels = completions.map { it.label }
        assertTrue(labels.contains("avatarUrl"))
        assertTrue(labels.contains("bio"))
    }

    @Test
    fun testArgumentCompletions() {
        val query = "query { user( "
        val cursorOffset = query.length
        val completions = GraphQLAutocompleteEngine.getCompletions(query, cursorOffset, schema)

        val labels = completions.map { it.label }
        assertTrue(labels.contains("id"))
        assertTrue(labels.contains("includeProfile"))
    }

    @Test
    fun testDirectiveCompletions() {
        val query = "query { user @inc"
        val cursorOffset = query.length
        val completions = GraphQLAutocompleteEngine.getCompletions(query, cursorOffset, schema)

        assertTrue(completions.any { it.label == "@include" && it.kind == CompletionKind.DIRECTIVE })
    }
}
