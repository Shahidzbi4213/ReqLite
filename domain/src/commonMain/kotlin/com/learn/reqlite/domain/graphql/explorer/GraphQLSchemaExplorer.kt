package com.learn.reqlite.domain.graphql.explorer

import com.learn.reqlite.domain.graphql.model.*

data class SchemaExplorerCategory(
    val title: String,
    val items: List<SchemaExplorerItem>
)

data class SchemaExplorerItem(
    val id: String,
    val name: String,
    val detail: String,
    val description: String?,
    val isDeprecated: Boolean = false,
    val kind: SchemaItemKind,
    val rawField: GraphQLField? = null,
    val rawType: GraphQLNamedType? = null
)

enum class SchemaItemKind {
    QUERY,
    MUTATION,
    SUBSCRIPTION,
    OBJECT_TYPE,
    INPUT_TYPE,
    ENUM_TYPE,
    SCALAR_TYPE,
    INTERFACE_TYPE,
    UNION_TYPE
}

object GraphQLSchemaExplorer {

    fun filter(schema: GraphQLSchema, searchQuery: String): List<SchemaExplorerCategory> {
        val query = searchQuery.trim().lowercase()

        val queryFields = schema.queries
            .filter { matches(it.name, it.description, query) }
            .map { field ->
                SchemaExplorerItem(
                    id = "q_${field.name}",
                    name = field.name,
                    detail = field.typeRef.formatted(),
                    description = field.description,
                    isDeprecated = field.isDeprecated,
                    kind = SchemaItemKind.QUERY,
                    rawField = field
                )
            }

        val mutationFields = schema.mutations
            .filter { matches(it.name, it.description, query) }
            .map { field ->
                SchemaExplorerItem(
                    id = "m_${field.name}",
                    name = field.name,
                    detail = field.typeRef.formatted(),
                    description = field.description,
                    isDeprecated = field.isDeprecated,
                    kind = SchemaItemKind.MUTATION,
                    rawField = field
                )
            }

        val subscriptionFields = schema.subscriptions
            .filter { matches(it.name, it.description, query) }
            .map { field ->
                SchemaExplorerItem(
                    id = "s_${field.name}",
                    name = field.name,
                    detail = field.typeRef.formatted(),
                    description = field.description,
                    isDeprecated = field.isDeprecated,
                    kind = SchemaItemKind.SUBSCRIPTION,
                    rawField = field
                )
            }

        val types = schema.customTypes
            .filter { !schema.rootOperationTypeNames.contains(it.name) }
            .filter { matches(it.name, it.description, query) }
            .map { namedType ->
                val kind = when (namedType) {
                    is GraphQLObjectType -> SchemaItemKind.OBJECT_TYPE
                    is GraphQLInputObjectType -> SchemaItemKind.INPUT_TYPE
                    is GraphQLEnumType -> SchemaItemKind.ENUM_TYPE
                    is GraphQLScalarType -> SchemaItemKind.SCALAR_TYPE
                    is GraphQLInterfaceType -> SchemaItemKind.INTERFACE_TYPE
                    is GraphQLUnionType -> SchemaItemKind.UNION_TYPE
                }
                SchemaExplorerItem(
                    id = "t_${namedType.name}",
                    name = namedType.name,
                    detail = kind.name.replace("_", " "),
                    description = namedType.description,
                    kind = kind,
                    rawType = namedType
                )
            }

        val categories = mutableListOf<SchemaExplorerCategory>()
        if (queryFields.isNotEmpty()) {
            categories.add(SchemaExplorerCategory("Queries", queryFields))
        }
        if (mutationFields.isNotEmpty()) {
            categories.add(SchemaExplorerCategory("Mutations", mutationFields))
        }
        if (subscriptionFields.isNotEmpty()) {
            categories.add(SchemaExplorerCategory("Subscriptions", subscriptionFields))
        }
        if (types.isNotEmpty()) {
            categories.add(SchemaExplorerCategory("Types", types))
        }

        return categories
    }

    private fun matches(name: String, description: String?, query: String): Boolean {
        if (query.isEmpty()) return true
        if (name.lowercase().contains(query)) return true
        if (description?.lowercase()?.contains(query) == true) return true
        return false
    }

    fun generateSnippet(field: GraphQLField, schema: GraphQLSchema, operation: String = "query"): String {
        val opName = operation.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        val capitalizedFieldName = field.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        val hasArgs = field.args.isNotEmpty()
        val argDefs = if (hasArgs) {
            "(" + field.args.joinToString(", ") { "$${it.name}: ${it.typeRef.formatted()}" } + ")"
        } else ""

        val argCalls = if (hasArgs) {
            "(" + field.args.joinToString(", ") { "${it.name}: $${it.name}" } + ")"
        } else ""

        val returnTypeName = field.typeRef.unwrappedTypeName
        val returnType = returnTypeName?.let { schema.getNamedType(it) }

        val selectionSet = when (returnType) {
            is GraphQLObjectType -> {
                val subFields = returnType.fields.values.take(3)
                if (subFields.isNotEmpty()) {
                    " {\n    " + subFields.joinToString("\n    ") { it.name } + "\n  }"
                } else {
                    " {\n    __typename\n  }"
                }
            }
            is GraphQLInterfaceType -> {
                val subFields = returnType.fields.values.take(3)
                if (subFields.isNotEmpty()) {
                    " {\n    " + subFields.joinToString("\n    ") { it.name } + "\n  }"
                } else {
                    " {\n    __typename\n  }"
                }
            }
            else -> ""
        }

        return "$operation $capitalizedFieldName$opName$argDefs {\n  ${field.name}$argCalls$selectionSet\n}"
    }
}
