package com.learn.reqlite.domain.graphql.parser

import com.learn.reqlite.domain.graphql.model.*
import kotlin.test.*

class GraphQLIntrospectionParserTest {

    private val sampleIntrospectionJson = """
    {
      "data": {
        "__schema": {
          "queryType": { "name": "Query" },
          "mutationType": { "name": "Mutation" },
          "subscriptionType": null,
          "types": [
            {
              "kind": "OBJECT",
              "name": "Query",
              "description": "Root query type",
              "fields": [
                {
                  "name": "user",
                  "description": "Fetch user by ID",
                  "args": [
                    {
                      "name": "id",
                      "description": "User identifier",
                      "type": {
                        "kind": "NON_NULL",
                        "name": null,
                        "ofType": { "kind": "SCALAR", "name": "ID", "ofType": null }
                      },
                      "defaultValue": null
                    }
                  ],
                  "type": {
                    "kind": "OBJECT",
                    "name": "User",
                    "ofType": null
                  },
                  "isDeprecated": false,
                  "deprecationReason": null
                },
                {
                  "name": "allUsers",
                  "description": "List all users",
                  "args": [],
                  "type": {
                    "kind": "NON_NULL",
                    "name": null,
                    "ofType": {
                      "kind": "LIST",
                      "name": null,
                      "ofType": { "kind": "OBJECT", "name": "User", "ofType": null }
                    }
                  },
                  "isDeprecated": true,
                  "deprecationReason": "Use users(limit: Int) instead"
                }
              ]
            },
            {
              "kind": "OBJECT",
              "name": "Mutation",
              "description": "Root mutation type",
              "fields": [
                {
                  "name": "createUser",
                  "description": "Create a new user",
                  "args": [
                    {
                      "name": "input",
                      "description": null,
                      "type": {
                        "kind": "NON_NULL",
                        "name": null,
                        "ofType": { "kind": "INPUT_OBJECT", "name": "CreateUserInput", "ofType": null }
                      },
                      "defaultValue": null
                    }
                  ],
                  "type": { "kind": "OBJECT", "name": "User", "ofType": null },
                  "isDeprecated": false,
                  "deprecationReason": null
                }
              ]
            },
            {
              "kind": "OBJECT",
              "name": "User",
              "description": "A registered user entity",
              "fields": [
                {
                  "name": "id",
                  "description": null,
                  "args": [],
                  "type": { "kind": "NON_NULL", "name": null, "ofType": { "kind": "SCALAR", "name": "ID", "ofType": null } },
                  "isDeprecated": false,
                  "deprecationReason": null
                },
                {
                  "name": "name",
                  "description": "Full name",
                  "args": [],
                  "type": { "kind": "SCALAR", "name": "String", "ofType": null },
                  "isDeprecated": false,
                  "deprecationReason": null
                },
                {
                  "name": "status",
                  "description": null,
                  "args": [],
                  "type": { "kind": "ENUM", "name": "UserStatus", "ofType": null },
                  "isDeprecated": false,
                  "deprecationReason": null
                }
              ]
            },
            {
              "kind": "INPUT_OBJECT",
              "name": "CreateUserInput",
              "description": "Input payload for creating a user",
              "inputFields": [
                {
                  "name": "name",
                  "description": "User name",
                  "type": { "kind": "NON_NULL", "name": null, "ofType": { "kind": "SCALAR", "name": "String", "ofType": null } },
                  "defaultValue": null
                }
              ]
            },
            {
              "kind": "ENUM",
              "name": "UserStatus",
              "description": "Account status",
              "enumValues": [
                { "name": "ACTIVE", "description": "Active user", "isDeprecated": false, "deprecationReason": null },
                { "name": "BANNED", "description": "Banned user", "isDeprecated": true, "deprecationReason": "Deprecated in favor of SUSPENDED" }
              ]
            },
            {
              "kind": "SCALAR",
              "name": "String",
              "description": "Built-in String"
            },
            {
              "kind": "SCALAR",
              "name": "ID",
              "description": "Built-in ID"
            }
          ],
          "directives": [
            {
              "name": "include",
              "description": "Directs the executor to include this field or fragment only when the 'if' argument is true.",
              "locations": ["FIELD", "FRAGMENT_SPREAD", "INLINE_FRAGMENT"],
              "args": [
                {
                  "name": "if",
                  "description": "Included when true.",
                  "type": { "kind": "NON_NULL", "name": null, "ofType": { "kind": "SCALAR", "name": "Boolean", "ofType": null } },
                  "defaultValue": null
                }
              ]
            }
          ]
        }
      }
    }
    """

    @Test
    fun testParseSchema() {
        val schema = GraphQLIntrospectionParser.parse(sampleIntrospectionJson)

        assertEquals("Query", schema.queryType)
        assertEquals("Mutation", schema.mutationType)
        assertNull(schema.subscriptionType)

        val queryType = schema.getRootQueryType()
        assertNotNull(queryType)
        assertEquals(2, queryType.fields.size)

        val userField = queryType.fields["user"]
        assertNotNull(userField)
        assertEquals("User", userField.typeRef.name)
        assertEquals(1, userField.args.size)
        assertEquals("id", userField.args[0].name)
        assertEquals("ID!", userField.args[0].typeRef.formatted())

        val allUsersField = queryType.fields["allUsers"]
        assertNotNull(allUsersField)
        assertTrue(allUsersField.isDeprecated)
        assertEquals("Use users(limit: Int) instead", allUsersField.deprecationReason)
        assertEquals("[User]!", allUsersField.typeRef.formatted())

        val userType = schema.getObjectType("User")
        assertNotNull(userType)
        assertEquals(3, userType.fields.size)

        val userStatus = schema.types["UserStatus"] as? GraphQLEnumType
        assertNotNull(userStatus)
        assertEquals(2, userStatus.enumValues.size)
        assertEquals("ACTIVE", userStatus.enumValues[0].name)
        assertTrue(userStatus.enumValues[1].isDeprecated)

        val inputType = schema.types["CreateUserInput"] as? GraphQLInputObjectType
        assertNotNull(inputType)
        assertEquals(1, inputType.inputFields.size)
        assertEquals("name", inputType.inputFields["name"]?.name)

        assertEquals(1, schema.directives.size)
        assertEquals("include", schema.directives[0].name)
    }
}
