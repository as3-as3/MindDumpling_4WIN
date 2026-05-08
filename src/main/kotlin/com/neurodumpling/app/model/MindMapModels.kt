package com.neurodumpling.app.model

data class Node(
    val id: String,
    val spaceId: String = "default",
    val parentId: String? = null,
    val text: String,
    val x: Float,
    val y: Float,
    val width: Float = 180f,
    val height: Float = 80f,
    val color: String? = null,
    val category: String? = null
)

data class Relationship(
    val id: String,
    val spaceId: String = "default",
    val fromId: String,
    val toId: String,
    val color: String,
    val label: String? = null,
    val labelT: Float = 0.5f,
    val curveOffset: Float = 30f
)

data class Space(
    val id: String,
    val name: String,
    val type: String
)

data class MindMapData(
    val nodes: List<Node>,
    val relationships: List<Relationship>
)
