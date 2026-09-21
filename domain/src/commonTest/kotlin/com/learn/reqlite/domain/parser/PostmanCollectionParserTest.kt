package com.learn.reqlite.domain.parser

import com.learn.reqlite.domain.model.HttpMethod
import com.learn.reqlite.domain.model.RequestBody
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PostmanCollectionParserTest {

    private val parser = PostmanCollectionParserImpl()

    @Test
    fun parse_validPostmanV21_extractsCollectionAndRequests() {
        val json = """
        {
          "info": {
            "_postman_id": "12345-abcde",
            "name": "E-Commerce API",
            "description": "API collection for testing e-commerce endpoints",
            "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
          },
          "item": [
            {
              "name": "Get Products",
              "request": {
                "method": "GET",
                "header": [
                  {
                    "key": "Accept",
                    "value": "application/json",
                    "disabled": false
                  }
                ],
                "url": {
                  "raw": "https://api.store.com/products?category=books&limit=10",
                  "protocol": "https",
                  "host": ["api", "store", "com"],
                  "path": ["products"],
                  "query": [
                    {
                      "key": "category",
                      "value": "books"
                    },
                    {
                      "key": "limit",
                      "value": "10"
                    }
                  ]
                },
                "description": "Fetch list of products with pagination"
              }
            },
            {
              "name": "Create Product",
              "request": {
                "method": "POST",
                "header": [
                  {
                    "key": "Content-Type",
                    "value": "application/json"
                  }
                ],
                "body": {
                  "mode": "raw",
                  "raw": "{\"title\": \"Clean Code\", \"price\": 29.99}",
                  "options": {
                    "raw": {
                      "language": "json"
                    }
                  }
                },
                "url": {
                  "raw": "https://api.store.com/products"
                },
                "auth": {
                  "type": "bearer",
                  "bearer": [
                    {
                      "key": "token",
                      "value": "secret-jwt-token"
                    }
                  ]
                }
              }
            }
          ]
        }
        """.trimIndent()

        val result = parser.parse(json)
        assertTrue(result is PostmanParseResult.Success)

        val collection = result.collection
        assertEquals("E-Commerce API", collection.name)
        assertEquals("API collection for testing e-commerce endpoints", collection.description)

        val requests = result.requests
        assertEquals(2, requests.size)

        // Request 1: Get Products
        val req1 = requests[0]
        assertEquals("Get Products", req1.name)
        assertEquals(HttpMethod.GET, req1.method)
        assertEquals("https://api.store.com/products", req1.url)
        assertEquals(2, req1.queryParams.size)
        assertEquals("category", req1.queryParams[0].key)
        assertEquals("books", req1.queryParams[0].value)
        assertEquals("limit", req1.queryParams[1].key)
        assertEquals("10", req1.queryParams[1].value)
        assertEquals(1, req1.headers.size)
        assertEquals("Accept", req1.headers[0].key)
        assertEquals(collection.id, req1.collectionId)
        assertNull(req1.folderId)

        // Request 2: Create Product with raw JSON body and Bearer auth
        val req2 = requests[1]
        assertEquals("Create Product", req2.name)
        assertEquals(HttpMethod.POST, req2.method)
        assertEquals("https://api.store.com/products", req2.url)
        assertTrue(req2.body is RequestBody.TextBody)
        val textBody = req2.body as RequestBody.TextBody
        assertTrue(textBody.content.contains("Clean Code"))
        assertEquals("application/json", textBody.contentType)

        // Auth should have injected Authorization header
        val authHeader = req2.headers.firstOrNull { it.key == "Authorization" }
        assertNotNull(authHeader)
        assertEquals("Bearer secret-jwt-token", authHeader.value)
    }

    @Test
    fun parse_nestedFolders_preservesHierarchy() {
        val json = """
        {
          "info": {
            "name": "Nested API",
            "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
          },
          "item": [
            {
              "name": "Users Folder",
              "description": "User management endpoints",
              "item": [
                {
                  "name": "Admin Subfolder",
                  "item": [
                    {
                      "name": "Delete User",
                      "request": {
                        "method": "DELETE",
                        "url": "https://api.com/users/42"
                      }
                    }
                  ]
                },
                {
                  "name": "List Users",
                  "request": {
                    "method": "GET",
                    "url": "https://api.com/users"
                  }
                }
              ]
            }
          ]
        }
        """.trimIndent()

        val result = parser.parse(json)
        assertTrue(result is PostmanParseResult.Success)

        assertEquals("Nested API", result.collection.name)
        assertEquals(2, result.folders.size)
        assertEquals(2, result.requests.size)

        val topFolder = result.folders.first { it.name == "Users Folder" }
        assertNull(topFolder.parentFolderId)
        assertEquals(result.collection.id, topFolder.collectionId)

        val subFolder = result.folders.first { it.name == "Admin Subfolder" }
        assertEquals(topFolder.id, subFolder.parentFolderId)

        val deleteReq = result.requests.first { it.name == "Delete User" }
        assertEquals(subFolder.id, deleteReq.folderId)
        assertEquals(HttpMethod.DELETE, deleteReq.method)

        val listReq = result.requests.first { it.name == "List Users" }
        assertEquals(topFolder.id, listReq.folderId)
        assertEquals(HttpMethod.GET, listReq.method)
    }

    @Test
    fun parse_urlencoded_and_formdata_bodies() {
        val json = """
        {
          "info": {
            "name": "Forms API",
            "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
          },
          "item": [
            {
              "name": "Url Encoded Request",
              "request": {
                "method": "POST",
                "url": "https://api.com/login",
                "body": {
                  "mode": "urlencoded",
                  "urlencoded": [
                    { "key": "user", "value": "alice" },
                    { "key": "pass", "value": "secret", "disabled": true }
                  ]
                }
              }
            },
            {
              "name": "Form Data Request",
              "request": {
                "method": "POST",
                "url": "https://api.com/upload",
                "body": {
                  "mode": "formdata",
                  "formdata": [
                    { "key": "fileDescription", "value": "Avatar image" }
                  ]
                }
              }
            }
          ]
        }
        """.trimIndent()

        val result = parser.parse(json)
        assertTrue(result is PostmanParseResult.Success)

        val req1 = result.requests.first { it.name == "Url Encoded Request" }
        assertTrue(req1.body is RequestBody.UrlEncodedBody)
        val urlencodedBody = req1.body as RequestBody.UrlEncodedBody
        assertEquals(2, urlencodedBody.fields.size)
        assertEquals("user", urlencodedBody.fields[0].key)
        assertTrue(urlencodedBody.fields[0].isEnabled)
        assertEquals("pass", urlencodedBody.fields[1].key)
        assertTrue(!urlencodedBody.fields[1].isEnabled)

        val req2 = result.requests.first { it.name == "Form Data Request" }
        assertTrue(req2.body is RequestBody.FormDataBody)
        val formDataBody = req2.body as RequestBody.FormDataBody
        assertEquals(1, formDataBody.parts.size)
        assertEquals("fileDescription", formDataBody.parts[0].key)
        assertEquals("Avatar image", formDataBody.parts[0].value)
    }

    @Test
    fun parse_invalidJson_returnsError() {
        val result = parser.parse("{ invalid json }")
        assertTrue(result is PostmanParseResult.Error)

        val emptyResult = parser.parse("")
        assertTrue(emptyResult is PostmanParseResult.Error)

        val missingInfo = parser.parse("{\"item\": []}")
        assertTrue(missingInfo is PostmanParseResult.Error)
    }
}
