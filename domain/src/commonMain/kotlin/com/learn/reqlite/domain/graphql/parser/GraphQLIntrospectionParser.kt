package com.learn.reqlite.domain.graphql.parser

import com.learn.reqlite.domain.graphql.model.*
import kotlinx.serialization.json.*

object GraphQLIntrospectionParser {

    const val INTROSPECTION_QUERY = """
    query IntrospectionQuery {
      __schema {
        queryType { name }
        mutationType { name }
        subscriptionType { name }
        types {
          kind
          name
          description
          fields(includeDeprecated: true) {
            name
            description
            args {
              name
              description
              type {
                ...TypeRef
              }
              defaultValue
            }
            type {
              ...TypeRef
            }
            isDeprecated
            deprecationReason
          }
          inputFields {
            name
            description
            type {
              ...TypeRef
            }
            defaultValue
          }
          interfaces {
            ...TypeRef
          }
          enumValues(includeDeprecated: true) {
            name
            description
            isDeprecated
            deprecationReason
          }
          possibleTypes {
            ...TypeRef
          }
        }
        directives {
          name
          description
          locations
          args {
            name
            description
            type {
              ...TypeRef
            }
            defaultValue
          }
        }
      }
    }

    fragment TypeRef on __Type {
      kind
      name
      ofType {
        kind
        name
        ofType {
          kind
          name
          ofType {
            kind
            name
            ofType {
              kind
              name
            }
          }
        }
      }
    }
    """

    fun parse(jsonContent: String): GraphQLSchema {
        val root = try {
            Json.parseToJsonElement(jsonContent) as? JsonObject
                ?: throw IllegalArgumentException("Root element is not a JSON object")
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid JSON introspection payload: ${e.message}", e)
        }

        // Support both { "data": { "__schema": { ... } } } and { "__schema": { ... } }
        val dataObj = (root["data"] as? JsonObject) ?: root
        val schemaObj = (dataObj["__schema"] as? JsonObject)
            ?: throw IllegalArgumentException("Missing '__schema' object in introspection result")

        val queryTypeName = (schemaObj["queryType"] as? JsonObject)?.get("name")?.jsonPrimitive?.contentOrNull
        val mutationTypeName = (schemaObj["mutationType"] as? JsonObject)?.get("name")?.jsonPrimitive?.contentOrNull
        val subscriptionTypeName = (schemaObj["subscriptionType"] as? JsonObject)?.get("name")?.jsonPrimitive?.contentOrNull

        val typesArray = schemaObj["types"] as? JsonArray ?: JsonArray(emptyList())
        val typesMap = mutableMapOf<String, GraphQLNamedType>()

        for (typeElement in typesArray) {
            val typeObj = typeElement as? JsonObject ?: continue
            val name = typeObj["name"]?.jsonPrimitive?.contentOrNull ?: continue
            val description = typeObj["description"]?.jsonPrimitive?.contentOrNull
            val kindStr = typeObj["kind"]?.jsonPrimitive?.contentOrNull ?: continue
            val kind = parseKind(kindStr)

            val parsedType: GraphQLNamedType = when (kind) {
                GraphQLTypeKind.OBJECT -> {
                    val fields = parseFields(typeObj["fields"] as? JsonArray)
                    val interfaces = parseInterfaceNames(typeObj["interfaces"] as? JsonArray)
                    GraphQLObjectType(name, description, fields, interfaces)
                }
                GraphQLTypeKind.INTERFACE -> {
                    val fields = parseFields(typeObj["fields"] as? JsonArray)
                    val possibleTypes = parseTypeNames(typeObj["possibleTypes"] as? JsonArray)
                    GraphQLInterfaceType(name, description, fields, possibleTypes)
                }
                GraphQLTypeKind.UNION -> {
                    val possibleTypes = parseTypeNames(typeObj["possibleTypes"] as? JsonArray)
                    GraphQLUnionType(name, description, possibleTypes)
                }
                GraphQLTypeKind.ENUM -> {
                    val enumValues = parseEnumValues(typeObj["enumValues"] as? JsonArray)
                    GraphQLEnumType(name, description, enumValues)
                }
                GraphQLTypeKind.INPUT_OBJECT -> {
                    val inputFields = parseInputFields(typeObj["inputFields"] as? JsonArray)
                    GraphQLInputObjectType(name, description, inputFields)
                }
                GraphQLTypeKind.SCALAR -> {
                    GraphQLScalarType(name, description)
                }
                else -> {
                    GraphQLScalarType(name, description)
                }
            }
            typesMap[name] = parsedType
        }

        val directives = parseDirectives(schemaObj["directives"] as? JsonArray)

        return GraphQLSchema(
            queryType = queryTypeName,
            mutationType = mutationTypeName,
            subscriptionType = subscriptionTypeName,
            types = typesMap,
            directives = directives
        )
    }

    private fun parseKind(kindStr: String): GraphQLTypeKind = when (kindStr.uppercase()) {
        "SCALAR" -> GraphQLTypeKind.SCALAR
        "OBJECT" -> GraphQLTypeKind.OBJECT
        "INTERFACE" -> GraphQLTypeKind.INTERFACE
        "UNION" -> GraphQLTypeKind.UNION
        "ENUM" -> GraphQLTypeKind.ENUM
        "INPUT_OBJECT" -> GraphQLTypeKind.INPUT_OBJECT
        "LIST" -> GraphQLTypeKind.LIST
        "NON_NULL" -> GraphQLTypeKind.NON_NULL
        else -> GraphQLTypeKind.SCALAR
    }

    private fun parseTypeRef(typeObj: JsonObject?): GraphQLTypeRef {
        if (typeObj == null) return GraphQLTypeRef(kind = GraphQLTypeKind.SCALAR, name = "String")
        val kind = parseKind(typeObj["kind"]?.jsonPrimitive?.contentOrNull ?: "SCALAR")
        val name = typeObj["name"]?.jsonPrimitive?.contentOrNull
        val ofTypeObj = typeObj["ofType"] as? JsonObject
        val ofType = ofTypeObj?.let { parseTypeRef(it) }
        return GraphQLTypeRef(kind = kind, name = name, ofType = ofType)
    }

    private fun parseFields(fieldsArray: JsonArray?): Map<String, GraphQLField> {
        if (fieldsArray == null) return emptyMap()
        val result = mutableMapOf<String, GraphQLField>()
        for (fieldElement in fieldsArray) {
            val fieldObj = fieldElement as? JsonObject ?: continue
            val name = fieldObj["name"]?.jsonPrimitive?.contentOrNull ?: continue
            val description = fieldObj["description"]?.jsonPrimitive?.contentOrNull
            val isDeprecated = fieldObj["isDeprecated"]?.jsonPrimitive?.booleanOrNull ?: false
            val deprecationReason = fieldObj["deprecationReason"]?.jsonPrimitive?.contentOrNull
            val typeRef = parseTypeRef(fieldObj["type"] as? JsonObject)
            val args = parseInputFieldsList(fieldObj["args"] as? JsonArray)

            result[name] = GraphQLField(
                name = name,
                description = description,
                args = args,
                typeRef = typeRef,
                isDeprecated = isDeprecated,
                deprecationReason = deprecationReason
            )
        }
        return result
    }

    private fun parseInputFields(inputFieldsArray: JsonArray?): Map<String, GraphQLInputField> {
        val list = parseInputFieldsList(inputFieldsArray)
        return list.associateBy { it.name }
    }

    private fun parseInputFieldsList(inputFieldsArray: JsonArray?): List<GraphQLInputField> {
        if (inputFieldsArray == null) return emptyList()
        val result = mutableListOf<GraphQLInputField>()
        for (inputElement in inputFieldsArray) {
            val inputObj = inputElement as? JsonObject ?: continue
            val name = inputObj["name"]?.jsonPrimitive?.contentOrNull ?: continue
            val description = inputObj["description"]?.jsonPrimitive?.contentOrNull
            val defaultValue = inputObj["defaultValue"]?.jsonPrimitive?.contentOrNull
            val typeRef = parseTypeRef(inputObj["type"] as? JsonObject)

            result.add(
                GraphQLInputField(
                    name = name,
                    description = description,
                    typeRef = typeRef,
                    defaultValue = defaultValue
                )
            )
        }
        return result
    }

    private fun parseEnumValues(enumValuesArray: JsonArray?): List<GraphQLEnumValue> {
        if (enumValuesArray == null) return emptyList()
        val result = mutableListOf<GraphQLEnumValue>()
        for (enumElement in enumValuesArray) {
            val enumObj = enumElement as? JsonObject ?: continue
            val name = enumObj["name"]?.jsonPrimitive?.contentOrNull ?: continue
            val description = enumObj["description"]?.jsonPrimitive?.contentOrNull
            val isDeprecated = enumObj["isDeprecated"]?.jsonPrimitive?.booleanOrNull ?: false
            val deprecationReason = enumObj["deprecationReason"]?.jsonPrimitive?.contentOrNull

            result.add(
                GraphQLEnumValue(
                    name = name,
                    description = description,
                    isDeprecated = isDeprecated,
                    deprecationReason = deprecationReason
                )
            )
        }
        return result
    }

    private fun parseInterfaceNames(interfacesArray: JsonArray?): List<String> =
        parseTypeNames(interfacesArray)

    private fun parseTypeNames(typesArray: JsonArray?): List<String> {
        if (typesArray == null) return emptyList()
        return typesArray.mapNotNull { (it as? JsonObject)?.get("name")?.jsonPrimitive?.contentOrNull }
    }

    private fun parseDirectives(directivesArray: JsonArray?): List<GraphQLDirective> {
        if (directivesArray == null) return emptyList()
        val result = mutableListOf<GraphQLDirective>()
        for (dirElement in directivesArray) {
            val dirObj = dirElement as? JsonObject ?: continue
            val name = dirObj["name"]?.jsonPrimitive?.contentOrNull ?: continue
            val description = dirObj["description"]?.jsonPrimitive?.contentOrNull
            val locations = (dirObj["locations"] as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
            val args = parseInputFieldsList(dirObj["args"] as? JsonArray)

            result.add(GraphQLDirective(name, description, locations, args))
        }
        return result
    }
}
