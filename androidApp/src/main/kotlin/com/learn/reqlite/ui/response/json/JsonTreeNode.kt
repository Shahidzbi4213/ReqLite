package com.learn.reqlite.ui.response.json

sealed interface JsonTreeNode {
    val key: String?
    val path: String
    val displayKey: String
        get() = key ?: ""

    data class ObjectNode(
        override val key: String?,
        override val path: String,
        val children: List<JsonTreeNode>
    ) : JsonTreeNode {
        val size: Int get() = children.size
    }

    data class ArrayNode(
        override val key: String?,
        override val path: String,
        val children: List<JsonTreeNode>
    ) : JsonTreeNode {
        val size: Int get() = children.size
    }

    data class PrimitiveNode(
        override val key: String?,
        override val path: String,
        val value: String,
        val type: PrimitiveType
    ) : JsonTreeNode

    enum class PrimitiveType {
        STRING,
        NUMBER,
        BOOLEAN,
        NULL
    }
}
