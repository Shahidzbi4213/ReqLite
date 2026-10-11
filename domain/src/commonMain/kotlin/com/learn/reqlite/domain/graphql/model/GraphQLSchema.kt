package com.learn.reqlite.domain.graphql.model

enum class GraphQLTypeKind {
    SCALAR,
    OBJECT,
    INTERFACE,
    UNION,
    ENUM,
    INPUT_OBJECT,
    LIST,
    NON_NULL
}

data class GraphQLTypeRef(
    val kind: GraphQLTypeKind,
    val name: String? = null,
    val ofType: GraphQLTypeRef? = null
) {
    val unwrappedTypeName: String?
        get() = name ?: ofType?.unwrappedTypeName

    val isNonNull: Boolean
        get() = kind == GraphQLTypeKind.NON_NULL

    val isList: Boolean
        get() = kind == GraphQLTypeKind.LIST || ofType?.isList == true

    fun formatted(): String = when (kind) {
        GraphQLTypeKind.NON_NULL -> "${ofType?.formatted() ?: ""}!"
        GraphQLTypeKind.LIST -> "[${ofType?.formatted() ?: ""}]"
        else -> name ?: "Unknown"
    }
}

data class GraphQLInputField(
    val name: String,
    val description: String? = null,
    val typeRef: GraphQLTypeRef,
    val defaultValue: String? = null
)

data class GraphQLField(
    val name: String,
    val description: String? = null,
    val args: List<GraphQLInputField> = emptyList(),
    val typeRef: GraphQLTypeRef,
    val isDeprecated: Boolean = false,
    val deprecationReason: String? = null
)

data class GraphQLEnumValue(
    val name: String,
    val description: String? = null,
    val isDeprecated: Boolean = false,
    val deprecationReason: String? = null
)

sealed interface GraphQLNamedType {
    val name: String
    val description: String?
    val kind: GraphQLTypeKind
}

data class GraphQLScalarType(
    override val name: String,
    override val description: String? = null
) : GraphQLNamedType {
    override val kind: GraphQLTypeKind = GraphQLTypeKind.SCALAR
}

data class GraphQLObjectType(
    override val name: String,
    override val description: String? = null,
    val fields: Map<String, GraphQLField> = emptyMap(),
    val interfaces: List<String> = emptyList()
) : GraphQLNamedType {
    override val kind: GraphQLTypeKind = GraphQLTypeKind.OBJECT
}

data class GraphQLInterfaceType(
    override val name: String,
    override val description: String? = null,
    val fields: Map<String, GraphQLField> = emptyMap(),
    val possibleTypes: List<String> = emptyList()
) : GraphQLNamedType {
    override val kind: GraphQLTypeKind = GraphQLTypeKind.INTERFACE
}

data class GraphQLUnionType(
    override val name: String,
    override val description: String? = null,
    val possibleTypes: List<String> = emptyList()
) : GraphQLNamedType {
    override val kind: GraphQLTypeKind = GraphQLTypeKind.UNION
}

data class GraphQLEnumType(
    override val name: String,
    override val description: String? = null,
    val enumValues: List<GraphQLEnumValue> = emptyList()
) : GraphQLNamedType {
    override val kind: GraphQLTypeKind = GraphQLTypeKind.ENUM
}

data class GraphQLInputObjectType(
    override val name: String,
    override val description: String? = null,
    val inputFields: Map<String, GraphQLInputField> = emptyMap()
) : GraphQLNamedType {
    override val kind: GraphQLTypeKind = GraphQLTypeKind.INPUT_OBJECT
}

data class GraphQLDirective(
    val name: String,
    val description: String? = null,
    val locations: List<String> = emptyList(),
    val args: List<GraphQLInputField> = emptyList()
)

data class GraphQLSchema(
    val queryType: String? = "Query",
    val mutationType: String? = "Mutation",
    val subscriptionType: String? = "Subscription",
    val types: Map<String, GraphQLNamedType> = emptyMap(),
    val directives: List<GraphQLDirective> = emptyList()
) {
    fun getRootQueryType(): GraphQLObjectType? =
        types[queryType ?: "Query"] as? GraphQLObjectType

    fun getRootMutationType(): GraphQLObjectType? =
        types[mutationType ?: "Mutation"] as? GraphQLObjectType

    fun getRootSubscriptionType(): GraphQLObjectType? =
        types[subscriptionType ?: "Subscription"] as? GraphQLObjectType

    fun getObjectType(name: String): GraphQLObjectType? =
        types[name] as? GraphQLObjectType

    fun getNamedType(name: String): GraphQLNamedType? =
        types[name]

    val rootOperationTypeNames: Set<String>
        get() = setOfNotNull(queryType, mutationType, subscriptionType)

    val customTypes: List<GraphQLNamedType>
        get() = types.values.filter { !it.name.startsWith("__") }

    val queries: List<GraphQLField>
        get() = getRootQueryType()?.fields?.values?.toList() ?: emptyList()

    val mutations: List<GraphQLField>
        get() = getRootMutationType()?.fields?.values?.toList() ?: emptyList()

    val subscriptions: List<GraphQLField>
        get() = getRootSubscriptionType()?.fields?.values?.toList() ?: emptyList()
}
