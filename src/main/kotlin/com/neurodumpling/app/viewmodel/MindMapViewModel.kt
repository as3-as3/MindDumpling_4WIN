package com.neurodumpling.app.viewmodel

import com.neurodumpling.app.model.MindMapData
import com.neurodumpling.app.model.Node
import com.neurodumpling.app.model.Relationship
import com.neurodumpling.app.model.Space
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

class MindMapViewModel {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _currentSpaceId = MutableStateFlow("mindmap_1")
    val currentSpaceId = _currentSpaceId.asStateFlow()

    // Persistent storage for spaces (Simulation of Room)
    private val spaceData = mutableMapOf<String, MindMapData>()

    private val _nodes = MutableStateFlow<List<Node>>(emptyList())
    val nodes = _nodes.asStateFlow()

    private val _relationships = MutableStateFlow<List<Relationship>>(emptyList())
    val relationships = _relationships.asStateFlow()

    private val _studioTitle = MutableStateFlow("Studio")
    val studioTitle = _studioTitle.asStateFlow()

    private val _mode = MutableStateFlow("mindmap")
    val mode = _mode.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode = _isDarkMode.asStateFlow()

    private val _history = MutableStateFlow<List<MindMapData>>(emptyList())
    val historyCount = _history.map { it.size }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    private val _redoHistory = MutableStateFlow<List<MindMapData>>(emptyList())
    val canRedo = _redoHistory.map { it.isNotEmpty() }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    init {
        // Initialize default MindMap
        spaceData["mindmap_1"] = MindMapData(
            listOf(Node(id = UUID.randomUUID().toString().take(8), text = "Mind Map Root", x = 707f, y = 500f)),
            emptyList()
        )
        // Initialize default ConceptMap
        spaceData["conceptmap_1"] = MindMapData(
            listOf(Node(id = UUID.randomUUID().toString().take(8), text = "Concept Map Root", x = 707f, y = 500f)),
            emptyList()
        )
        
        loadSpace("mindmap_1")
    }

    private fun loadSpace(id: String) {
        val data = spaceData[id] ?: MindMapData(emptyList(), emptyList())
        _nodes.value = data.nodes
        _relationships.value = data.relationships
        _history.value = emptyList()
        _redoHistory.value = emptyList()
        _studioTitle.value = if (id.contains("mindmap")) "Mind Map Studio" else "Concept Map Studio"
    }

    private fun saveCurrentToSpace() {
        spaceData[_currentSpaceId.value] = MindMapData(_nodes.value.toList(), _relationships.value.toList())
    }

    fun saveToHistory() {
        val currentData = MindMapData(_nodes.value.toList(), _relationships.value.toList())
        _history.update { (it + currentData).takeLast(30) }
        _redoHistory.value = emptyList()
        saveCurrentToSpace()
    }

    fun undo() {
        val hist = _history.value
        if (hist.isEmpty()) return
        val currentData = MindMapData(_nodes.value.toList(), _relationships.value.toList())
        _redoHistory.update { (it + currentData).takeLast(30) }
        val last = hist.last()
        _history.update { it.dropLast(1) }
        _nodes.value = last.nodes
        _relationships.value = last.relationships
        saveCurrentToSpace()
    }

    fun redo() {
        val redoHist = _redoHistory.value
        if (redoHist.isEmpty()) return
        val next = redoHist.last()
        _redoHistory.update { it.dropLast(1) }
        val currentData = MindMapData(_nodes.value.toList(), _relationships.value.toList())
        _history.update { (it + currentData).takeLast(30) }
        _nodes.value = next.nodes
        _relationships.value = next.relationships
        saveCurrentToSpace()
    }

    fun addNode(parentId: String? = null, x: Float, y: Float, text: String = "Neuer Punkt", category: String? = null) {
        saveToHistory()
        val nodeText = category ?: text
        val nodeColor = when(category) {
            "P" -> "#ef4444"
            "E" -> "#facc15"
            "S" -> "#f97316"
            "R" -> "#22c55e"
            else -> null
        }
        val newNode = Node(
            id = UUID.randomUUID().toString().take(8),
            spaceId = _currentSpaceId.value,
            parentId = parentId,
            text = nodeText,
            x = x,
            y = y,
            category = category,
            color = nodeColor
        )
        _nodes.update { it + newNode }
        saveCurrentToSpace()
    }

    fun moveNode(id: String, x: Float, y: Float) {
        _nodes.update { list ->
            list.map { if (it.id == id) it.copy(x = x, y = y) else it }
        }
    }

    fun commitNodePosition(id: String) {
        val node = _nodes.value.find { it.id == id } ?: return
        val snappedNode = node.copy(
            x = (node.x / 20f).roundToInt() * 20f,
            y = (node.y / 20f).roundToInt() * 20f
        )
        _nodes.update { list -> list.map { if (it.id == id) snappedNode else it } }
        saveCurrentToSpace()
    }

    fun updateNodeText(id: String, text: String) {
        _nodes.update { list ->
            list.map { if (it.id == id) it.copy(text = text) else it }
        }
        saveCurrentToSpace()
    }

    fun deleteNode(id: String) {
        saveToHistory()
        val remainingNodes = _nodes.value.filter { it.id != id }.map {
            if (it.parentId == id) it.copy(parentId = null) else it
        }
        val remainingRels = _relationships.value.filter { it.fromId != id && it.toId != id }
        _nodes.value = remainingNodes
        _relationships.value = remainingRels
        saveCurrentToSpace()
    }

    fun addRelationship(fromId: String, toId: String, color: String = "accent", label: String = "Link") {
        saveToHistory()
        val newRel = Relationship(
            id = UUID.randomUUID().toString().take(8),
            spaceId = _currentSpaceId.value,
            fromId = fromId,
            toId = toId,
            color = color,
            label = label
        )
        _relationships.update { it + newRel }
        saveCurrentToSpace()
    }

    fun deleteRelationship(id: String) {
        saveToHistory()
        _relationships.update { it.filter { rel -> rel.id != id } }
        saveCurrentToSpace()
    }

    fun moveRelationshipCurve(id: String, curveOffset: Float, labelT: Float) {
        _relationships.update { list ->
            list.map { if (it.id == id) it.copy(curveOffset = curveOffset, labelT = labelT) else it }
        }
    }

    fun commitRelationshipCurve(id: String) {
        val rel = _relationships.value.find { it.id == id } ?: return
        _relationships.update { list -> list.map { if (it.id == id) rel else it } }
        saveCurrentToSpace()
    }

    fun updateRelationshipLabel(id: String, label: String) {
        _relationships.update { list ->
            list.map { if (it.id == id) it.copy(label = label) else it }
        }
        saveCurrentToSpace()
    }

    fun reset() {
        saveToHistory()
        _nodes.value = listOf(Node(id = UUID.randomUUID().toString().take(8), text = "Brainstorm", x = 707f, y = 500f))
        _relationships.value = emptyList()
        _studioTitle.value = "Studio"
        saveCurrentToSpace()
    }

    fun switchMode(newMode: String) {
        saveCurrentToSpace() // Save current before switching
        _mode.value = newMode
        val nextSpaceId = if (newMode == "mindmap") "mindmap_1" else "conceptmap_1"
        _currentSpaceId.value = nextSpaceId
        loadSpace(nextSpaceId)
    }

    fun updateStudioTitle(title: String) {
        _studioTitle.value = title
    }

    fun toggleTheme() {
        _isDarkMode.value = !_isDarkMode.value
    }
    
    fun cycleNodeColor(nodeId: String) {
        val node = _nodes.value.find { it.id == nodeId } ?: return
        val colors = listOf(null, "accent", "danger", "success", "info")
        val currentIndex = colors.indexOf(node.color)
        val nextIndex = (currentIndex + 1) % colors.size
        _nodes.update { list ->
            list.map { if (it.id == nodeId) it.copy(color = colors[nextIndex]) else it }
        }
        saveCurrentToSpace()
    }

    fun cycleRelationshipColor(relId: String) {
        val rel = _relationships.value.find { it.id == relId } ?: return
        val colors = listOf("accent", "red", "green", "blue", "info")
        val currentIndex = colors.indexOf(rel.color)
        val nextIndex = (currentIndex + 1) % colors.size
        _relationships.update { list ->
            list.map { if (it.id == relId) it.copy(color = colors[nextIndex]) else it }
        }
        saveCurrentToSpace()
    }

    fun createPESRCluster(parentId: String) {
        saveToHistory()
        val parent = _nodes.value.find { it.id == parentId } ?: return
        val startX = parent.x
        val startY = parent.y + 150f
        
        val pNode = Node(UUID.randomUUID().toString().take(8), _currentSpaceId.value, parentId, "Problem", startX, startY, category = "P", color = "#ef4444")
        val eNode = Node(UUID.randomUUID().toString().take(8), _currentSpaceId.value, pNode.id, "Einfluss/Herkunft", startX - 250f, startY + 150f, category = "E", color = "#facc15")
        val sNode = Node(UUID.randomUUID().toString().take(8), _currentSpaceId.value, pNode.id, "Symptome", startX, startY + 150f, category = "S", color = "#f97316")
        val rNode = Node(UUID.randomUUID().toString().take(8), _currentSpaceId.value, pNode.id, "Ressourcen", startX + 250f, startY + 150f, category = "R", color = "#22c55e")
        
        _nodes.update { it + listOf(pNode, eNode, sNode, rNode) }
        addRelationship(parentId, pNode.id, "accent", "Diagnose")
        addRelationship(pNode.id, eNode.id, "accent", "zeigt sich")
        addRelationship(pNode.id, sNode.id, "accent", "basiert auf")
        addRelationship(pNode.id, rNode.id, "accent", "nutzt")
        saveCurrentToSpace()
    }

    fun triggerPngExport(nodes: List<Node>, relationships: List<Relationship>, isDark: Boolean, title: String) {
        com.neurodumpling.app.util.DesktopExporter.exportAsImage(nodes, relationships, isDark, title)
    }

    fun triggerPdfExport(nodes: List<Node>, relationships: List<Relationship>, isDark: Boolean, title: String) {
        com.neurodumpling.app.util.DesktopExporter.exportAsPdf(nodes, relationships, isDark, title)
    }

    fun triggerSvgExport(nodes: List<Node>, relationships: List<Relationship>, isDark: Boolean, title: String) {
        com.neurodumpling.app.util.DesktopExporter.exportAsSvg(nodes, relationships, isDark, title)
    }

    fun triggerJsonExport(nodes: List<Node>, relationships: List<Relationship>) {
        val root = JSONObject()
        val nodesArr = JSONArray()
        nodes.forEach { n ->
            val jo = JSONObject()
            jo.put("id", n.id); jo.put("text", n.text); jo.put("x", n.x); jo.put("y", n.y)
            jo.put("parentId", n.parentId); jo.put("color", n.color); jo.put("category", n.category)
            nodesArr.put(jo)
        }
        val relsArr = JSONArray()
        relationships.forEach { r ->
            val jo = JSONObject()
            jo.put("id", r.id); jo.put("fromId", r.fromId); jo.put("toId", r.toId)
            jo.put("color", r.color); jo.put("label", r.label); jo.put("labelT", r.labelT)
            jo.put("curveOffset", r.curveOffset)
            relsArr.put(jo)
        }
        root.put("nodes", nodesArr)
        root.put("relationships", relsArr)
        val json = root.toString(2)
        java.awt.EventQueue.invokeLater {
            val chooser = javax.swing.JFileChooser()
            chooser.dialogTitle = "Mind Map als JSON speichern"
            chooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter("JSON File", "json")
            if (chooser.showSaveDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
                var file = chooser.selectedFile
                if (!file.name.lowercase().endsWith(".json")) file = java.io.File(file.absolutePath + ".json")
                file.writeText(json)
            }
        }
    }

    fun triggerJsonImport() {
        java.awt.EventQueue.invokeLater {
            val chooser = javax.swing.JFileChooser()
            chooser.dialogTitle = "Mind Map JSON importieren"
            chooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter("JSON File", "json")
            if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
                val json = chooser.selectedFile.readText()
                importFromJson(json)
            }
        }
    }

    private fun importFromJson(json: String) {
        try {
            val root = JSONObject(json)
            val nodesArr = root.getJSONArray("nodes")
            val newNodes = mutableListOf<Node>()
            for (i in 0 until nodesArr.length()) {
                val o = nodesArr.getJSONObject(i)
                newNodes.add(Node(
                    id = o.getString("id"), text = o.getString("text"), x = o.getFloat("x"), y = o.getFloat("y"),
                    parentId = if (o.isNull("parentId")) null else o.getString("parentId"),
                    color = if (o.isNull("color")) null else o.getString("color"),
                    category = if (o.isNull("category")) null else o.getString("category")
                ))
            }
            val relsArr = root.getJSONArray("relationships")
            val newRels = mutableListOf<Relationship>()
            for (i in 0 until relsArr.length()) {
                val o = relsArr.getJSONObject(i)
                newRels.add(Relationship(
                    id = o.getString("id"), fromId = o.getString("fromId"), toId = o.getString("toId"),
                    color = o.getString("color"), label = if (o.isNull("label")) null else o.getString("label"),
                    labelT = o.optDouble("labelT", 0.5).toFloat(), curveOffset = o.optDouble("curveOffset", 0.0).toFloat()
                ))
            }
            saveToHistory()
            _nodes.value = newNodes
            _relationships.value = newRels
            saveCurrentToSpace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
