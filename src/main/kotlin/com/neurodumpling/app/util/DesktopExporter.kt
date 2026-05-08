package com.neurodumpling.app.util

import com.neurodumpling.app.model.Node
import com.neurodumpling.app.model.Relationship
import java.awt.*
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.*
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import org.jfree.graphics2d.svg.SVGGraphics2D

object DesktopExporter {

    fun exportAsImage(nodes: List<Node>, relationships: List<Relationship>, isDark: Boolean, title: String) {
        if (nodes.isEmpty()) return
        val metrics = calculateBounds(nodes)
        val image = BufferedImage(metrics.width, metrics.height, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()
        renderToGraphics(g2d, nodes, relationships, isDark, metrics, title)
        g2d.dispose()
        EventQueue.invokeLater {
            val chooser = JFileChooser()
            chooser.dialogTitle = "Als PNG exportieren"
            chooser.fileFilter = FileNameExtensionFilter("PNG Image", "png")
            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                var file = chooser.selectedFile
                if (!file.name.lowercase().endsWith(".png")) file = File(file.absolutePath + ".png")
                ImageIO.write(image, "png", file)
            }
        }
    }

    fun exportAsPdf(nodes: List<Node>, relationships: List<Relationship>, isDark: Boolean, title: String) {
        if (nodes.isEmpty()) return
        val metrics = calculateBounds(nodes)
        val image = BufferedImage(metrics.width, metrics.height, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()
        renderToGraphics(g2d, nodes, relationships, isDark, metrics, title)
        g2d.dispose()
        EventQueue.invokeLater {
            val chooser = JFileChooser()
            chooser.dialogTitle = "Als PDF exportieren"
            chooser.fileFilter = FileNameExtensionFilter("PDF Document", "pdf")
            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                var file = chooser.selectedFile
                if (!file.name.lowercase().endsWith(".pdf")) file = File(file.absolutePath + ".pdf")
                PDDocument().use { doc ->
                    val page = PDPage(PDRectangle(metrics.width.toFloat(), metrics.height.toFloat()))
                    doc.addPage(page)
                    val pdImage = LosslessFactory.createFromImage(doc, image)
                    PDPageContentStream(doc, page).use { content ->
                        content.drawImage(pdImage, 0f, 0f, metrics.width.toFloat(), metrics.height.toFloat())
                    }
                    doc.save(file)
                }
            }
        }
    }

    fun exportAsSvg(nodes: List<Node>, relationships: List<Relationship>, isDark: Boolean, title: String) {
        if (nodes.isEmpty()) return
        val metrics = calculateBounds(nodes)
        val g2d = SVGGraphics2D(metrics.width, metrics.height)
        renderToGraphics(g2d, nodes, relationships, isDark, metrics, title)
        val svgElement = g2d.svgElement
        EventQueue.invokeLater {
            val chooser = JFileChooser()
            chooser.dialogTitle = "Als SVG exportieren"
            chooser.fileFilter = FileNameExtensionFilter("SVG Vector Graphic", "svg")
            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                var file = chooser.selectedFile
                if (!file.name.lowercase().endsWith(".svg")) file = File(file.absolutePath + ".svg")
                file.writeText(svgElement)
            }
        }
    }

    private data class ExportMetrics(val width: Int, val height: Int, val minX: Float, val minY: Float, val padding: Int, val topPadding: Int = 150)

    private fun calculateBounds(nodes: List<Node>): ExportMetrics {
        val minX = nodes.minOf { it.x }; val minY = nodes.minOf { it.y }
        val maxX = nodes.maxOf { it.x + it.width }; val maxY = nodes.maxOf { it.y + it.height }
        val padding = 100
        val topPadding = 150
        val bottomPadding = 150
        return ExportMetrics(
            width = (maxX - minX + padding * 2).toInt(),
            height = (maxY - minY + padding + topPadding + bottomPadding).toInt(),
            minX = minX,
            minY = minY,
            padding = padding,
            topPadding = topPadding
        )
    }

    private fun renderToGraphics(g2d: Graphics2D, nodes: List<Node>, relationships: List<Relationship>, isDark: Boolean, m: ExportMetrics, title: String) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        
        // RULE: Always use Light Background for Exports
        g2d.color = Color(245, 245, 250)
        g2d.fillRect(0, 0, m.width, m.height)

        // Draw Title - Always Black
        g2d.color = Color.BLACK
        g2d.font = Font("SansSerif", Font.BOLD, 28)
        val titleWidth = g2d.fontMetrics.stringWidth(title)
        g2d.drawString(title, (m.width - titleWidth) / 2, 80)

        g2d.translate(-m.minX.toDouble() + m.padding, -m.minY.toDouble() + m.topPadding)

        // Relationships & Labels
        relationships.forEach { rel ->
            val from = nodes.find { it.id == rel.fromId }; val to = nodes.find { it.id == rel.toId }
            if (from != null && to != null) {
                drawAwtConnection(g2d, from, to, rel.color, rel.curveOffset)
                rel.label?.let { label ->
                    val startX = from.x + from.width / 2; val startY = from.y + from.height / 2
                    val endX = to.x + to.width / 2; val endY = to.y + to.height / 2
                    val dx = endX - startX; val dy = endY - startY; val dist = sqrt(dx*dx + dy*dy).coerceAtLeast(1f)
                    val pnx = -dy / dist; val pny = dx / dist
                    val cpX = (startX + endX) / 2 + pnx * rel.curveOffset; val cpY = (startY + endY) / 2 + pny * rel.curveOffset
                    val invT = 1 - rel.labelT; val lX = invT*invT*startX + 2*invT*rel.labelT*cpX + rel.labelT*rel.labelT*endX; val lY = invT*invT*startY + 2*invT*rel.labelT*cpY + rel.labelT*rel.labelT*endY
                    g2d.color = Color.BLACK
                    g2d.font = Font("SansSerif", Font.ITALIC, 12)
                    g2d.drawString(label, lX.toInt(), lY.toInt())
                }
            }
        }

        // Hierarchy
        nodes.forEach { node ->
            node.parentId?.let { pId ->
                val parent = nodes.find { it.id == pId }
                if (parent != null) drawAwtConnection(g2d, parent, node, "accent", 0f, isHierarchy = true)
            }
        }

        // Nodes & Categories
        nodes.forEach { node ->
            val nColor = when(node.color) {
                "danger" -> Color(239, 68, 68); "success" -> Color(34, 197, 94); "accent" -> Color(99, 102, 241)
                else -> Color(255, 255, 255) // Force light node bg
            }
            g2d.color = nColor
            g2d.fillRoundRect(node.x.toInt(), node.y.toInt(), node.width.toInt(), node.height.toInt(), 16, 16)
            
            // Dark border for better definition on light export background
            g2d.color = Color(0, 0, 0, 40)
            g2d.stroke = BasicStroke(1f)
            g2d.drawRoundRect(node.x.toInt(), node.y.toInt(), node.width.toInt(), node.height.toInt(), 16, 16)
            
            // Category Badge
            node.category?.let { cat ->
                val catColor = when(cat) { "P" -> Color(239, 68, 68); "E" -> Color(250, 204, 21); "S" -> Color(249, 115, 22); else -> Color(34, 197, 94) }
                g2d.color = catColor
                g2d.fillRoundRect(node.x.toInt() + 10, node.y.toInt() + 10, 20, 20, 6, 6)
                g2d.color = Color.WHITE
                g2d.font = Font("SansSerif", Font.BOLD, 10)
                g2d.drawString(cat, node.x.toInt() + 16, node.y.toInt() + 24)
            }

            g2d.color = Color.BLACK
            g2d.font = Font("SansSerif", Font.BOLD, 16)
            val fm = g2d.fontMetrics; val tw = fm.stringWidth(node.text)
            g2d.drawString(node.text, node.x.toInt() + (node.width.toInt() - tw)/2, node.y.toInt() + node.height.toInt()/2 + 6)
        }

        // Reset transform for Legend
        g2d.translate(m.minX.toDouble() - m.padding, m.minY.toDouble() - m.topPadding)
        
        // Draw Legend
        val legendY = m.height - 80
        val startX = 50
        g2d.font = Font("SansSerif", Font.BOLD, 12)
        val cats = listOf("P" to Color(239, 68, 68), "E" to Color(250, 204, 21), "S" to Color(249, 115, 22), "R" to Color(34, 197, 94))
        val labels = listOf("Problem", "Einfluss", "Symptom", "Ressource")
        
        cats.forEachIndexed { i, (cat, color) ->
            val curX = startX + i * 140
            g2d.color = color
            g2d.fillRoundRect(curX, legendY, 20, 20, 6, 6)
            g2d.color = Color.WHITE
            g2d.drawString(cat, curX + 6, legendY + 14)
            g2d.color = Color.BLACK
            g2d.drawString(labels[i], curX + 30, legendY + 14)
        }
        
        g2d.color = Color(0, 0, 0, 100)
        g2d.font = Font("SansSerif", Font.PLAIN, 10)
        g2d.drawString("created with NeuroDumpling Desktop", 50, m.height - 30)
    }

    private fun drawAwtConnection(g2d: Graphics2D, from: Node, to: Node, colorStr: String, curve: Float, isHierarchy: Boolean = false) {
        val startX = from.x + from.width / 2; val startY = from.y + from.height / 2
        val endX = to.x + to.width / 2; val endY = to.y + to.height / 2
        val dx = endX - startX; val dy = endY - startY
        val dist = sqrt(dx*dx + dy*dy).coerceAtLeast(1f)
        val pnx = -dy / dist; val pny = dx / dist
        val cpX = (startX + endX) / 2 + pnx * curve; val cpY = (startY + endY) / 2 + pny * curve
        g2d.color = when(colorStr) {
            "danger" -> Color(239, 68, 68, 150); "success" -> Color(34, 197, 94, 150)
            else -> Color(99, 102, 241, if (isHierarchy) 80 else 150)
        }
        g2d.stroke = BasicStroke(if (isHierarchy) 2f else 4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        val path = java.awt.geom.Path2D.Float()
        path.moveTo(startX, startY); path.quadTo(cpX, cpY, endX, endY)
        g2d.draw(path)
    }
}
