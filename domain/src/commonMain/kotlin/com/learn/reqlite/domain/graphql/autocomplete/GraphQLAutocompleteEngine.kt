package com.learn.reqlite.domain.graphql.autocomplete

import com.learn.reqlite.domain.graphql.model.*

enum class CompletionKind {
    KEYWORD,
    FIELD,
    ARGUMENT,
    TYPE,
    DIRECTIVE,
    SNIPPET
}

data class GraphQLCompletionItem(
    val label: String,
    val insertText: String,
    val detail: String = "",
    val documentation: String? = null,
    val kind: CompletionKind = CompletionKind.FIELD
)

object GraphQLAutocompleteEngine {

    private val KEYWORDS = listOf(
        GraphQLCompletionItem("query", "query ", "Operation", "Defines a read-only fetch operation", CompletionKind.KEYWORD),
        GraphQLCompletionItem("mutation", "mutation ", "Operation", "Defines a write/mutation operation", CompletionKind.KEYWORD),
        GraphQLCompletionItem("subscription", "subscription ", "Operation", "Defines a real-time subscription", CompletionKind.KEYWORD),
        GraphQLCompletionItem("fragment", "fragment ", "Fragment", "Defines a reusable field selection fragment", CompletionKind.KEYWORD)
    )

    fun getCompletions(
        query: String,
        cursorOffset: Int,
        schema: GraphQLSchema?
    ): List<GraphQLCompletionItem> {
        val safeOffset = cursorOffset.coerceIn(0, query.length)
        val textBeforeCursor = query.substring(0, safeOffset)

        // Find current word prefix being typed
        val currentWord = extractCurrentWord(textBeforeCursor)
        val isDirective = isAfterAtSymbol(textBeforeCursor)

        if (isDirective && schema != null) {
            val prefix = currentWord.removePrefix("@")
            return schema.directives
                .filter { it.name.startsWith(prefix, ignoreCase = true) }
                .map { dir ->
                    GraphQLCompletionItem(
                        label = "@${dir.name}",
                        insertText = if (dir.args.isNotEmpty()) "@${dir.name}(${dir.args.first().name}: )" else "@${dir.name}",
                        detail = "Directive",
                        documentation = dir.description,
                        kind = CompletionKind.DIRECTIVE
                    )
                }
        }

        // Analyze syntax stack
        val context = analyzeContext(textBeforeCursor, schema)

        return when {
            context.inArguments -> {
                val currentField = context.currentField
                if (currentField != null) {
                    currentField.args
                        .filter { it.name.startsWith(currentWord, ignoreCase = true) }
                        .map { arg ->
                            GraphQLCompletionItem(
                                label = arg.name,
                                insertText = "${arg.name}: ",
                                detail = arg.typeRef.formatted(),
                                documentation = arg.description,
                                kind = CompletionKind.ARGUMENT
                            )
                        }
                } else emptyList()
            }

            context.currentType != null -> {
                val type = context.currentType
                val fields = when (type) {
                    is GraphQLObjectType -> type.fields.values.toList()
                    is GraphQLInterfaceType -> type.fields.values.toList()
                    is GraphQLUnionType -> emptyList()
                    else -> emptyList()
                }

                val fieldItems = fields
                    .filter { it.name.startsWith(currentWord, ignoreCase = true) }
                    .map { field ->
                        val isComposite = isCompositeType(field.typeRef, schema)
                        val insertSnippet = if (isComposite) {
                            "${field.name} {\n    \n  }"
                        } else {
                            field.name
                        }
                        GraphQLCompletionItem(
                            label = field.name,
                            insertText = insertSnippet,
                            detail = field.typeRef.formatted() + (if (field.isDeprecated) " [Deprecated]" else ""),
                            documentation = field.description ?: (if (field.isDeprecated) field.deprecationReason else null),
                            kind = CompletionKind.FIELD
                        )
                    }

                val metaFields = listOf(
                    GraphQLCompletionItem("__typename", "__typename", "String!", "The name of the current object type", CompletionKind.FIELD)
                ).filter { it.label.startsWith(currentWord, ignoreCase = true) }

                (fieldItems + metaFields).sortedBy { it.label }
            }

            else -> {
                // At root or outside selection set
                val keywords = KEYWORDS.filter { it.label.startsWith(currentWord, ignoreCase = true) }
                val rootSnippets = if (schema != null && currentWord.isEmpty()) {
                    listOf(
                        GraphQLCompletionItem(
                            label = "{ ... } (Query Shorthand)",
                            insertText = "{\n  \n}",
                            detail = "Query",
                            documentation = "Anonymous query selection set",
                            kind = CompletionKind.SNIPPET
                        )
                    )
                } else emptyList()

                keywords + rootSnippets
            }
        }
    }

    private fun isCompositeType(typeRef: GraphQLTypeRef, schema: GraphQLSchema?): Boolean {
        val typeName = typeRef.unwrappedTypeName ?: return false
        val namedType = schema?.getNamedType(typeName) ?: return false
        return namedType is GraphQLObjectType || namedType is GraphQLInterfaceType || namedType is GraphQLUnionType
    }

    private data class ParseContext(
        val currentType: GraphQLNamedType? = null,
        val inArguments: Boolean = false,
        val currentField: GraphQLField? = null
    )

    private fun analyzeContext(text: String, schema: GraphQLSchema?): ParseContext {
        if (schema == null) return ParseContext()

        val tokens = tokenize(text)
        var operationType = "query"
        val typeStack = mutableListOf<GraphQLNamedType>()
        var pendingFieldName: String? = null
        var inArgs = false
        var activeArgField: GraphQLField? = null

        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            when (token) {
                "query", "mutation", "subscription" -> {
                    operationType = token
                }
                "{" -> {
                    if (typeStack.isEmpty()) {
                        val rootType: GraphQLNamedType? = when (operationType) {
                            "mutation" -> schema.getRootMutationType()
                            "subscription" -> schema.getRootSubscriptionType()
                            else -> schema.getRootQueryType()
                        }
                        if (rootType != null) {
                            typeStack.add(rootType)
                        }
                    } else {
                        val parentType = typeStack.lastOrNull()
                        val fieldName = pendingFieldName
                        if (parentType is GraphQLObjectType && fieldName != null) {
                            val field = parentType.fields[fieldName]
                            val childTypeName = field?.typeRef?.unwrappedTypeName
                            val childType = childTypeName?.let { schema.getNamedType(it) }
                            if (childType != null) {
                                typeStack.add(childType)
                            }
                        } else if (parentType is GraphQLInterfaceType && fieldName != null) {
                            val field = parentType.fields[fieldName]
                            val childTypeName = field?.typeRef?.unwrappedTypeName
                            val childType = childTypeName?.let { schema.getNamedType(it) }
                            if (childType != null) {
                                typeStack.add(childType)
                            }
                        }
                    }
                    pendingFieldName = null
                }
                "}" -> {
                    if (typeStack.isNotEmpty()) {
                        typeStack.removeAt(typeStack.size - 1)
                    }
                    pendingFieldName = null
                }
                "(" -> {
                    inArgs = true
                    val currentType = typeStack.lastOrNull()
                    val fieldName = pendingFieldName
                    if (currentType is GraphQLObjectType && fieldName != null) {
                        activeArgField = currentType.fields[fieldName]
                    } else if (currentType is GraphQLInterfaceType && fieldName != null) {
                        activeArgField = currentType.fields[fieldName]
                    }
                }
                ")" -> {
                    inArgs = false
                    activeArgField = null
                }
                else -> {
                    if (!token.startsWith("@") && !token.startsWith("$") && token.all { it.isLetterOrDigit() || it == '_' }) {
                        if (!inArgs) {
                            pendingFieldName = token
                        }
                    }
                }
            }
            i++
        }

        return ParseContext(
            currentType = typeStack.lastOrNull(),
            inArguments = inArgs,
            currentField = activeArgField
        )
    }

    private fun tokenize(text: String): List<String> {
        val result = mutableListOf<String>()
        var idx = 0
        while (idx < text.length) {
            val c = text[idx]
            when {
                c.isWhitespace() || c == ',' -> {
                    idx++
                }
                c == '#' -> {
                    // Skip comment to end of line
                    while (idx < text.length && text[idx] != '\n') {
                        idx++
                    }
                }
                c == '"' -> {
                    // Skip string literal
                    idx++
                    while (idx < text.length && text[idx] != '"') {
                        if (text[idx] == '\\' && idx + 1 < text.length) idx++
                        idx++
                    }
                    if (idx < text.length) idx++
                }
                c in listOf('{', '}', '(', ')', ':', '@', '$') -> {
                    result.add(c.toString())
                    idx++
                }
                c.isLetter() || c == '_' -> {
                    val start = idx
                    while (idx < text.length && (text[idx].isLetterOrDigit() || text[idx] == '_')) {
                        idx++
                    }
                    result.add(text.substring(start, idx))
                }
                else -> {
                    idx++
                }
            }
        }
        return result
    }

    private fun extractCurrentWord(text: String): String {
        var i = text.length - 1
        while (i >= 0 && (text[i].isLetterOrDigit() || text[i] == '_' || text[i] == '@')) {
            i--
        }
        return text.substring(i + 1)
    }

    private fun isAfterAtSymbol(text: String): Boolean {
        val word = extractCurrentWord(text)
        return word.startsWith("@")
    }
}
