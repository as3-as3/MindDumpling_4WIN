package com.neurodumpling.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurodumpling.app.model.Node
import com.neurodumpling.app.model.Relationship
import com.neurodumpling.app.viewmodel.MindMapViewModel
import androidx.compose.ui.focus.onFocusChanged
import com.neurodumpling.app.ui.value.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlin.math.*

@Composable
fun MindMapCanvas(viewModel: MindMapViewModel) {
    val nodes by viewModel.nodes.collectAsState()
    val relationships by viewModel.relationships.collectAsState()
    val isDark by viewModel.isDarkMode.collectAsState()
    val currentMode by viewModel.mode.collectAsState()
    val studioTitle by viewModel.studioTitle.collectAsState()
    
    val dragPreviews = remember { mutableStateMapOf<String, Offset>() }
    val labelDragPreviews = remember { mutableStateMapOf<String, Offset>() }
    
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    
    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    var activeTool by remember { mutableStateOf("none") }
    var pMenuNodeId by remember { mutableStateOf<String?>(null) }
    var relStartNodeId by remember { mutableStateOf<String?>(null) }
    var mousePos by remember { mutableStateOf(Offset.Zero) }
    
    var isSmartboardMode by remember { mutableStateOf(false) }
    var isPresentationMode by remember { mutableStateOf(false) }
    var exportFormat by remember { mutableStateOf<String?>(null) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    if (exportFormat != null) {
        AlertDialog(
            onDismissRequest = { exportFormat = null },
            confirmButton = { Button(onClick = { 
                    when(exportFormat) {
                        "pdf" -> viewModel.triggerPdfExport(nodes, relationships, isDark, studioTitle)
                        "svg" -> viewModel.triggerSvgExport(nodes, relationships, isDark, studioTitle)
                        else -> viewModel.triggerPngExport(nodes, relationships, isDark, studioTitle)
                    }
                    exportFormat = null 
                }, colors = ButtonDefaults.buttonColors(containerColor = AccentColor)) { Text("Jetzt Exportieren (AWT)", color = Color.White) } },
            dismissButton = { TextButton(onClick = { exportFormat = null }) { Text("Abbrechen") } },
            title = { Text("Export Vorschau (Desktop)", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Format: ${exportFormat?.uppercase()}", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.size(240.dp).background(Color(0xFFF5F5F0), RoundedCornerShape(12.dp)).border(1.dp, Color.Black.copy(alpha = 0.1f), RoundedCornerShape(12.dp))) {
                        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            if (nodes.isNotEmpty()) {
                                val minX = nodes.minOf { it.x }; val minY = nodes.minOf { it.y }
                                val maxX = nodes.maxOf { it.x + 140f }; val maxY = nodes.maxOf { it.y + 80f }
                                val w = maxX - minX; val h = maxY - minY
                                val scale = (min(size.width / w, size.height / h) * 0.9f).coerceAtMost(2f)
                                drawContext.canvas.save()
                                drawContext.canvas.translate((size.width - w * scale)/2, (size.height - h * scale)/2)
                                drawContext.canvas.scale(scale, scale)
                                drawContext.canvas.translate(-minX, -minY)
                                nodes.forEach { drawRoundRect(Color.Black.copy(alpha = 0.2f), Offset(it.x, it.y), androidx.compose.ui.geometry.Size(140f, 80f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)) }
                                drawContext.canvas.restore()
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Export verwendet Java AWT Rendering.", fontSize = 11.sp, textAlign = TextAlign.Center, color = Color.Gray)
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = false,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(300.dp).fillMaxHeight(),
                    drawerContainerColor = if (isDark) Color(0xFF121214) else Color(0xFFFDFDFB),
                    drawerContentColor = if (isDark) DarkText else LightText,
                    drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp).background(AccentColor.copy(alpha = 0.1f))) {
                        Text("NEURODUMPLING", modifier = Modifier.align(Alignment.Center), color = AccentColor, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    }
                    Column(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                        Text("ARBEITSMODUS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                        DrawerItem("Mind Map Mode", "🧠", currentMode == "mindmap") { viewModel.switchMode("mindmap"); scope.launch { drawerState.close() } }
                        DrawerItem("Concept Map Mode", "🕸", currentMode == "conceptmap") { viewModel.switchMode("conceptmap"); scope.launch { drawerState.close() } }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("EINSTELLUNGEN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                        DrawerItem("Thema wechseln", if (isDark) "☀" else "🌙", false) { viewModel.toggleTheme() }
                        DrawerItem("Smartboard Layout", "🖥", isSmartboardMode) { isSmartboardMode = !isSmartboardMode }
                        DrawerItem("Reset Workspace", "↺", false) { viewModel.reset(); scope.launch { drawerState.close() } }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("EXPORT (AWT)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                        DrawerItem("Export PNG", "🖼", false) { exportFormat = "png"; scope.launch { drawerState.close() } }
                        DrawerItem("Export PDF", "📑", false) { exportFormat = "pdf"; scope.launch { drawerState.close() } }
                        DrawerItem("Export SVG", "📐", false) { exportFormat = "svg"; scope.launch { drawerState.close() } }
                        DrawerItem("Export JSON", "📄", false) { viewModel.triggerJsonExport(nodes, relationships); scope.launch { drawerState.close() } }
                        DrawerItem("Import JSON", "📥", false) { viewModel.triggerJsonImport(); scope.launch { drawerState.close() } }
                        Spacer(modifier = Modifier.weight(1f))
                        Text("Github: as3-as3", fontSize = 11.sp, color = Color(0xFF6366F1), modifier = Modifier.padding(start = 12.dp, bottom = 4.dp))
                        Text("Version 1.1 Desktop", fontSize = 10.sp, color = Color.Gray.copy(alpha = 0.6f), modifier = Modifier.padding(start = 12.dp, bottom = 16.dp))
                    }
                }
            }
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(if (isDark) DarkBg else LightBg).pointerInput(activeTool, relStartNodeId) {
                    coroutineScope {
                        launch { detectTransformGestures { _, pan, zoom, _ -> if (activeTool == "pan" || activeTool == "none") { panOffset += pan; zoomScale = (zoomScale * zoom).coerceIn(0.1f, 5f) } } }
                        launch { detectTapGestures(onTap = { offset ->
                            if (activeTool == "add-node") {
                                val x = (offset.x - panOffset.x) / zoomScale
                                val y = (offset.y - panOffset.y) / zoomScale
                                viewModel.addNode(x = x, y = y)
                            } else {
                                selectedNodeId = null; pMenuNodeId = null; relStartNodeId = null 
                            }
                        }) }
                        launch { awaitPointerEventScope { while (true) { val event = awaitPointerEvent(); mousePos = event.changes.first().position } } }
                    }
                }
            ) {
                Box(modifier = Modifier.fillMaxSize().graphicsLayer(translationX = panOffset.x, translationY = panOffset.y, scaleX = zoomScale, scaleY = zoomScale)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        relationships.forEach { rel ->
                            val fromNode = nodes.find { it.id == rel.fromId }; val toNode = nodes.find { it.id == rel.toId }
                            if (fromNode != null && toNode != null) {
                                val fromPos = Offset(fromNode.x + (dragPreviews[fromNode.id]?.x ?: 0f), fromNode.y + (dragPreviews[fromNode.id]?.y ?: 0f))
                                val toPos = Offset(toNode.x + (dragPreviews[toNode.id]?.x ?: 0f), toNode.y + (dragPreviews[toNode.id]?.y ?: 0f))
                                RelationshipPainter.drawConnection(this, fromPos, toPos, Offset(fromNode.width, fromNode.height), Offset(toNode.width, toNode.height), if (rel.color == "danger") DangerColor else if (rel.color == "success") SuccessColor else AccentColor, rel.curveOffset, rel.labelT)
                            }
                        }
                        // MindMap Hierarchy Lines (Mirroring Android 1:1)
                        if (currentMode == "mindmap" || currentMode == "conceptmap") {
                            nodes.forEach { node ->
                                node.parentId?.let { pId ->
                                    val parent = nodes.find { it.id == pId }
                                    if (parent != null) {
                                        val hasExplicitRel = relationships.any { (it.fromId == parent.id && it.toId == node.id) || (it.fromId == node.id && it.toId == parent.id) }
                                        if (!hasExplicitRel) {
                                            RelationshipPainter.drawConnection(this, Offset(parent.x + (dragPreviews[parent.id]?.x ?: 0f), parent.y + (dragPreviews[parent.id]?.y ?: 0f)), Offset(node.x + (dragPreviews[node.id]?.x ?: 0f), node.y + (dragPreviews[node.id]?.y ?: 0f)), Offset(parent.width, parent.height), Offset(node.width, node.height), AccentColor.copy(alpha = 0.3f), 0f, 0.5f, showArrow = false)
                                        }
                                    }
                                }
                            }
                        }
                        // Relationship Drag Feedback
                        if (relStartNodeId != null) {
                            val startNode = nodes.find { it.id == relStartNodeId }
                            if (startNode != null) {
                                val start = Offset((startNode.x + (dragPreviews[startNode.id]?.x ?: 0f) + startNode.width / 2) * zoomScale + panOffset.x, (startNode.y + (dragPreviews[startNode.id]?.y ?: 0f) + startNode.height / 2) * zoomScale + panOffset.y)
                                val color = if (activeTool == "rel-red") DangerColor else if (activeTool == "rel-green") SuccessColor else AccentColor
                                drawLine(color, start, mousePos, strokeWidth = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                            }
                        }
                    }
                    nodes.forEach { node ->
                        MindMapNode(node, dragPreviews, node.id == selectedNodeId, { dx, dy -> val current = dragPreviews[node.id] ?: Offset.Zero; dragPreviews[node.id] = Offset(current.x + dx, current.y + dy) }, {
                            if (activeTool == "rel-red" || activeTool == "rel-green" || activeTool == "concept-rel") {
                                val targetNode = nodes.find { target -> if (target.id == node.id) return@find false; val nx = target.x * zoomScale + panOffset.x; val ny = target.y * zoomScale + panOffset.y; val nw = target.width * zoomScale; val nh = target.height * zoomScale; mousePos.x >= nx && mousePos.x <= nx + nw && mousePos.y >= ny && mousePos.y <= ny + nh }
                                if (targetNode != null) { viewModel.addRelationship(node.id, targetNode.id, if (activeTool == "rel-red") "danger" else if (activeTool == "rel-green") "success" else "accent") }
                                relStartNodeId = null
                            } else {
                                val finalOffset = dragPreviews[node.id] ?: Offset.Zero; viewModel.moveNode(node.id, node.x + finalOffset.x, node.y + finalOffset.y); viewModel.commitNodePosition(node.id); dragPreviews.remove(node.id)
                            }
                        }, { viewModel.updateNodeText(node.id, it) }, { 
                            if (activeTool == "rel-red" || activeTool == "rel-green" || activeTool == "concept-rel") {
                                if (relStartNodeId == null) relStartNodeId = node.id else { viewModel.addRelationship(relStartNodeId!!, node.id, if (activeTool == "rel-red") "danger" else if (activeTool == "rel-green") "success" else "accent"); relStartNodeId = null }
                            } else if (activeTool == "delete") viewModel.deleteNode(node.id) else selectedNodeId = node.id 
                        }, { text, cat -> viewModel.addNode(parentId = node.id, x = node.x + 250f, y = node.y + 100f, text = text ?: "Pkt", category = cat) }, { viewModel.createPESRCluster(node.id) }, { viewModel.cycleNodeColor(node.id) }, { pMenuNodeId = if (pMenuNodeId == node.id) null else node.id }, pMenuNodeId, activeTool, zoomScale, isDark, relStartNodeId)
                    }
                    relationships.filter { it.label != null }.forEach { rel ->
                        key(rel.id) {
                            val fromNode = nodes.find { it.id == rel.fromId }; val toNode = nodes.find { it.id == rel.toId }
                            if (fromNode != null && toNode != null) {
                                RelationshipLabel(rel = rel, fromNode = fromNode, toNode = toNode, labelDragPreviews = labelDragPreviews, effectiveCurve = rel.curveOffset, onUpdateCurve = { curve, t -> viewModel.moveRelationshipCurve(rel.id, curve, t) }, onDragEnd = { viewModel.commitRelationshipCurve(rel.id) }, onUpdateLabel = { viewModel.updateRelationshipLabel(rel.id, it) }, onDelete = { viewModel.deleteRelationship(rel.id) }, onColorCycle = { viewModel.cycleRelationshipColor(rel.id) }, activeTool = activeTool, zoomScale = zoomScale, panOffset = panOffset)
                            }
                        }
                    }
                }
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            if (!isPresentationMode) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.TopCenter) {
                    BasicTextField(value = studioTitle, onValueChange = { viewModel.updateStudioTitle(it) }, textStyle = TextStyle(color = if (isDark) Color.White else Color.Black, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center), modifier = Modifier.width(IntrinsicSize.Min).padding(horizontal = 32.dp))
                }
            }
            Column(modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = if (isSmartboardMode) 150.dp else 120.dp, end = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ZoomButton("＋", { zoomScale = (zoomScale * 1.25f).coerceAtMost(5f) }, isDark)
                ZoomButton("－", { zoomScale = (zoomScale / 1.25f).coerceAtLeast(0.1f) }, isDark)
            }
            Row(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = if (isSmartboardMode) 48.dp else 32.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(onClick = { scope.launch { if (drawerState.isOpen) drawerState.close() else drawerState.open() } }, modifier = Modifier.size(if (isSmartboardMode) 64.dp else 52.dp).background(if (isDark) Color(0xCC1A1A22) else Color(0xCCF0F0F5), CircleShape).border(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x1A000000), CircleShape)) {
                    Text("☰", color = if (isDark) Color.White else Color.Black, fontSize = if (isSmartboardMode) 24.sp else 20.sp)
                }
                FloatingBottomToolbar(activeTool, { activeTool = if (activeTool == it) "none" else it }, { viewModel.undo() }, { viewModel.redo() }, viewModel.historyCount.collectAsState().value > 0, viewModel.canRedo.collectAsState().value, isDark, currentMode, isSmartboardMode)
            }
            Box(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) { Text("created with NeuroDumpling", fontSize = 10.sp, color = if (isDark) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.3f)) }
        }
    }
}

@Composable
fun DrawerItem(label: String, icon: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), color = if (isSelected) AccentColor.copy(alpha = 0.15f) else Color.Transparent, shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.padding(12.dp, 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 18.sp, modifier = Modifier.width(32.dp))
            Text(label, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) AccentColor else Color.Unspecified)
        }
    }
}

@Composable
fun MindMapNode(node: Node, dragPreviews: SnapshotStateMap<String, Offset>, isSelected: Boolean, onPositionChange: (Float, Float) -> Unit, onDragEnd: () -> Unit, onTextChange: (String) -> Unit, onSelect: () -> Unit, onAddChild: (String?, String?) -> Unit, onCreatePESR: () -> Unit, onColorCycle: () -> Unit, onTogglePMenu: () -> Unit, pMenuOpenId: String?, activeTool: String, zoomScale: Float, isDark: Boolean, relStartNodeId: String?) {
    val dragPreview = dragPreviews[node.id] ?: Offset.Zero
    Box(modifier = Modifier.offset { IntOffset((node.x + dragPreview.x).roundToInt(), (node.y + dragPreview.y).roundToInt()) }.sizeIn(minWidth = 140.dp, maxWidth = 300.dp).background(color = when (node.color) { "accent" -> AccentColor.copy(alpha = 0.8f); "danger" -> DangerColor.copy(alpha = 0.8f); "success" -> SuccessColor.copy(alpha = 0.8f); else -> if (isDark) DarkNodeBg else LightNodeBg }, shape = RoundedCornerShape(16.dp)).border(if (isSelected) 2.dp else 1.dp, if (isSelected) AccentColor else if (isDark) Color(0x1AFFFFFF) else Color(0x1A000000), RoundedCornerShape(16.dp)).pointerInput(node.id, zoomScale) { detectTapGestures(onTap = { onSelect() }, onLongPress = { onTogglePMenu() }) }.pointerInput(node.id, zoomScale, activeTool) { detectDragGestures(onDragStart = { if (activeTool != "pan") onSelect() }, onDrag = { change, delta -> change.consume(); if (activeTool == "none" || activeTool == "delete") onPositionChange(delta.x / zoomScale, delta.y / zoomScale) }, onDragEnd = { onDragEnd() }) }.padding(4.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
            node.category?.let { cat -> Surface(color = if (cat == "P") DangerColor else if (cat == "E") Color(0xFFFACC15) else if (cat == "S") Color(0xFFF97316) else SuccessColor, shape = RoundedCornerShape(6.dp), modifier = Modifier.padding(bottom = 8.dp)) { Text(cat, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) } }
            BasicTextField(value = node.text, onValueChange = onTextChange, textStyle = TextStyle(color = if (isDark) Color.White else Color.Black, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center), modifier = Modifier.onFocusChanged { if (it.isFocused && node.text == "Neuer Punkt") onTextChange("") })
            if (isSelected) { Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { IconButton(onClick = { onAddChild(null, null) }, modifier = Modifier.size(28.dp).background(AccentColor.copy(alpha = 0.2f), CircleShape)) { Text("+", color = AccentColor) }; IconButton(onClick = onCreatePESR, modifier = Modifier.size(28.dp).background(SuccessColor.copy(alpha = 0.2f), CircleShape)) { Text("📋", fontSize = 12.sp) }; IconButton(onClick = onColorCycle, modifier = Modifier.size(28.dp).background(Color.Gray.copy(alpha = 0.1f), CircleShape)) { Text("🎨", fontSize = 12.sp) } } }
        }
    }
}

@Composable
fun RelationshipLabel(rel: Relationship, fromNode: Node, toNode: Node, labelDragPreviews: SnapshotStateMap<String, Offset>, effectiveCurve: Float, onUpdateCurve: (Float, Float) -> Unit, onDragEnd: () -> Unit, onUpdateLabel: (String) -> Unit, onDelete: () -> Unit, onColorCycle: () -> Unit, activeTool: String, zoomScale: Float, panOffset: Offset) {
    val startX = fromNode.x + fromNode.width / 2; val startY = fromNode.y + fromNode.height / 2
    val endX = toNode.x + toNode.width / 2; val endY = toNode.y + toNode.height / 2
    val dx = endX - startX; val dy = endY - startY; val dist = sqrt(dx*dx + dy*dy).coerceAtLeast(1f)
    val lineX = dx / dist; val lineY = dy / dist; val pnx = -dy / dist; val pny = dx / dist
    var localPreview by remember(rel.id, effectiveCurve) { mutableStateOf(Offset(effectiveCurve, rel.labelT)) }
    val cpX = (startX+endX)/2 + pnx * localPreview.x; val cpY = (startY+endY)/2 + pny * localPreview.x
    val invT = 1-localPreview.y; val lX = invT*invT*startX + 2*invT*localPreview.y*cpX + localPreview.y*localPreview.y*endX; val lY = invT*invT*startY + 2*invT*localPreview.y*cpY + localPreview.y*localPreview.y*endY
    Box(modifier = Modifier.offset { IntOffset(lX.roundToInt(), lY.roundToInt()) }.pointerInput(rel.id, activeTool) { detectTapGestures(onTap = { if (activeTool == "delete") onDelete() else onColorCycle() }) }.pointerInput(rel.id, zoomScale, activeTool) { detectDragGestures(onDrag = { change, delta -> if (activeTool == "none") { change.consume(); val deltaCurve = (delta.x / zoomScale * pnx + delta.y / zoomScale * pny); val deltaT = (delta.x / zoomScale * lineX + delta.y / zoomScale * lineY) / dist; localPreview = Offset(localPreview.x + deltaCurve, (localPreview.y + deltaT).coerceIn(0.1f, 0.9f)); onUpdateCurve(localPreview.x, localPreview.y) } }, onDragEnd = { if (activeTool == "none") onDragEnd() }) }.background(Color(0xCC1A1A1A), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 8.dp)) { BasicTextField(value = rel.label ?: "", onValueChange = onUpdateLabel, textStyle = TextStyle(color = Color.White, fontSize = 12.sp)) }
}

@Composable
fun FloatingBottomToolbar(activeTool: String, onToolSelect: (String) -> Unit, onUndo: () -> Unit, onRedo: () -> Unit, canUndo: Boolean, canRedo: Boolean, isDark: Boolean, mode: String, isSmartboard: Boolean) {
    Surface(shape = RoundedCornerShape(32.dp), color = if (isDark) Color(0xCC1A1A22) else Color(0xCCF0F0F5), modifier = Modifier.height(if (isSmartboard) 88.dp else 72.dp), border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x1A000000)), shadowElevation = 12.dp) {
        Row(modifier = Modifier.padding(horizontal = if (isSmartboard) 24.dp else 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (isSmartboard) 16.dp else 8.dp)) {
            ToolButton("↩", "Undo", false, onUndo, canUndo, isDark = isDark, size = if (isSmartboard) 64.dp else 52.dp)
            ToolButton("↪", "Redo", false, onRedo, canRedo, isDark = isDark, size = if (isSmartboard) 64.dp else 52.dp)
            ToolButton("✋", "Pan", activeTool == "pan", { onToolSelect("pan") }, isDark = isDark, size = if (isSmartboard) 64.dp else 52.dp)
            ToolButton("＋", "Add", activeTool == "add-node", { onToolSelect("add-node") }, isDark = isDark, size = if (isSmartboard) 64.dp else 52.dp)
            if (mode == "mindmap") {
                ToolButton("P", "Patient", activeTool == "p_mode", { onToolSelect("p_mode") }, color = DangerColor, isDark = isDark, size = if (isSmartboard) 64.dp else 52.dp)
                ToolButton("●", "-", activeTool == "rel-red", { onToolSelect("rel-red") }, color = DangerColor, isDark = isDark, size = if (isSmartboard) 64.dp else 52.dp)
                ToolButton("●", "+", activeTool == "rel-green", { onToolSelect("rel-green") }, color = SuccessColor, isDark = isDark, size = if (isSmartboard) 64.dp else 52.dp)
            }
            ToolButton("🔗", "Link", activeTool == "concept-rel", { onToolSelect("concept-rel") }, isDark = isDark, size = if (isSmartboard) 64.dp else 52.dp)
            ToolButton("🗑", "Del", activeTool == "delete", { onToolSelect("delete") }, color = DangerColor, isDark = isDark, size = if (isSmartboard) 64.dp else 52.dp)
        }
    }
}

@Composable
fun ToolButton(icon: String, label: String, isActive: Boolean, onClick: () -> Unit, enabled: Boolean = true, color: Color = Color.Unspecified, isDark: Boolean, size: androidx.compose.ui.unit.Dp = 52.dp) {
    val finalColor = if (color == Color.Unspecified) (if (isDark) Color.White else Color.Black) else color
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(size).background(if (isActive) finalColor.copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(16.dp))) {
        Text(icon, style = TextStyle(fontSize = if (size > 52.dp) 28.sp else 22.sp, color = if (enabled) finalColor else Color.Gray))
    }
}

@Composable
fun ZoomButton(icon: String, onClick: () -> Unit, isDark: Boolean) {
    IconButton(onClick = onClick, modifier = Modifier.size(44.dp).background(if (isDark) Color(0xCC1A1A1E) else Color(0xCCFFFFFF), CircleShape).border(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x1A000000), CircleShape)) {
        Text(icon, color = if (isDark) Color.White else Color.Black, fontSize = 18.sp)
    }
}
