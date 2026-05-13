package com.zeros.scriptassistant

import cn.hutool.core.util.NumberUtil
import com.intellij.ui.JBColor
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.image.BufferedImage
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.Timer
import kotlin.math.abs

class ImagePanel : JPanel() {
    private var image: BufferedImage? = null
    private var rect: Rectangle? = null
    private var greenRect: Rectangle? = null
    private var line: IntArray? = null

    private var x = 0
    private var y = 0
    private var panelWidth = 0
    private var panelHeight = 0

    private var scaleX = 0.0
    private var scaleY = 0.0
    private var allNodeRect: Set<Rectangle>? = null
    private var listener: ImagePanelListener? = null

    private var singleClickTask: Runnable? = null
    private var singleClickTimer: Timer? = null

    init {
        addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                if (image == null || listener == null) return
                val imagePoint = scaleToImagePoint(e)
                if (imagePoint == null) {
                    listener!!.onMousePositionChange(-1, -1)
                } else {
                    listener!!.onMousePositionChange(imagePoint.x, imagePoint.y)
                }
            }
        })

        addMouseListener(object : MouseAdapter() {
            private var pressX = 0
            private var pressY = 0
            private var pressTime: Long = 0
            private var rightBtn = false
            private var lastClickedTime = 0L
            private var lastX = 0
            private var lastY = 0

            override fun mousePressed(e: MouseEvent) {
                pressX = e.x
                pressY = e.y
                pressTime = System.currentTimeMillis()
                rightBtn = SwingUtilities.isRightMouseButton(e)
            }

            override fun mouseReleased(e: MouseEvent) {
                if (image == null || listener == null) return
                val releaseX = e.x
                val releaseY = e.y
                val isCtrlPressed = e.isControlDown

                if (abs(pressX - e.x) > 10 || abs(pressY - e.y) > 10) {
                    val start = scaleToImagePoint(pressX, pressY)
                    val end = scaleToImagePoint(releaseX, releaseY)
                    if (start == null || end == null) return

                    // 画线
                    line = intArrayOf(pressX, pressY, releaseX, releaseY)
                    rect = null
                    greenRect = null

                    val duration = NumberUtil.div((System.currentTimeMillis() - pressTime).toDouble(), 1000.0, 2)
                    if (rightBtn) {
                        listener!!.onSwipe(
                            start.x.toDouble(), start.y.toDouble(),
                            end.x.toDouble(), end.y.toDouble(), duration, isCtrlPressed
                        )
                    } else {
                        val percentStartX = NumberUtil.div(start.x.toDouble(), image!!.width.toDouble(), 2)
                        val percentStartY = NumberUtil.div(start.y.toDouble(), image!!.height.toDouble(), 2)
                        val percentEndX = NumberUtil.div(end.x.toDouble(), image!!.width.toDouble(), 2)
                        val percentEndY = NumberUtil.div(end.y.toDouble(), image!!.height.toDouble(), 2)
                        listener!!.onSwipe(
                            percentStartX,
                            percentStartY,
                            percentEndX,
                            percentEndY,
                            duration,
                            isCtrlPressed
                        )
                    }
                } else {
                    line = null
                }
                repaint()
            }

            override fun mouseClicked(e: MouseEvent) {
                if (image == null || listener == null) return
                val imagePoint = scaleToImagePoint(e) ?: return
                val imageX = imagePoint.x
                val imageY = imagePoint.y
                val isCtrlPressed = e.isControlDown
                val now = System.currentTimeMillis()

                if (now - lastClickedTime < 200) {
                    if (abs(lastX - e.x) < 10 && abs(lastY - e.y) < 10) {
                        // 双击，取消之前的单击任务
                        singleClickTimer?.stop()
                        listener!!.onLeftDoubleClicked(imageX, imageY, isCtrlPressed)
                        return
                    }
                }

                lastX = e.x
                lastY = e.y
                lastClickedTime = now

                listener!!.onNodeSelected(imageX, imageY)

                // 延迟执行单击任务
                val task = when (e.button) {
                    MouseEvent.BUTTON3 -> Runnable { listener!!.onRightClicked(imageX, imageY, isCtrlPressed) }
                    MouseEvent.BUTTON1 -> Runnable { listener!!.onLeftClicked(imageX, imageY, isCtrlPressed) }
                    else -> null
                }

                // 启动定时器，延迟 200 毫秒执行单击任务
                singleClickTimer?.stop()
                singleClickTimer = Timer(200) {
                    task?.run()
                }
                singleClickTimer?.isRepeats = false
                singleClickTimer?.start()
            }
        })

        addMouseWheelListener { e ->
            if (image == null || listener == null) return@addMouseWheelListener
            listener!!.onMouseWheel()
        }
    }

    private fun scaleToImagePoint(e: MouseEvent): Point? {
        return scaleToImagePoint(e.x, e.y)
    }

    private fun scaleToImagePoint(originX: Int, originY: Int): Point? {
        val imgX = ((originX - x) * scaleX).toInt()
        val imgY = ((originY - y) * scaleY).toInt()
        if (imgX < 0 || imgY < 0 || imgX >= image!!.width || imgY >= image!!.height) {
            return null
        }
        return Point(imgX, imgY)
    }

    fun setListener(listener: ImagePanelListener?) {
        println("setListener: $listener")
        this.listener = listener
    }

    fun setAllNodeRect(allNodeRect: Set<Rectangle>?) {
        this.allNodeRect = allNodeRect
    }

    fun setImage(image: BufferedImage?) {
        clearRect()
        this.image = image
        repaint()
    }

    fun clearRect() {
        rect = null
        greenRect = null
        line = null
    }

    fun paintRect(info: NodeInfo?) {
        rect = info?.let { Rectangle(it.bounds.x, it.bounds.y, it.bounds.width, it.bounds.height) }
        line = null
        repaint()
    }

    fun paintGreenRect(info: NodeInfo?) {
        greenRect = info?.let { Rectangle(it.bounds.x, it.bounds.y, it.bounds.width, it.bounds.height) }
        line = null
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        if (image != null) {
            val g2d = g as Graphics2D
            drawImage(g2d)
            drawAllNodeRect(g2d)
            drawSelectedRect(g2d)
            drawSwipeLine(g2d)
        }
    }

    private fun drawImage(g2d: Graphics2D) {
        val aspectRatio = image!!.width.toDouble() / image!!.height.toDouble()
        if (aspectRatio >= this.width.toDouble() / this.height.toDouble()) {
            panelWidth = this.width
            panelHeight = (panelWidth / aspectRatio).toInt()
        } else {
            panelHeight = this.height
            panelWidth = (panelHeight * aspectRatio).toInt()
        }
        x = (this.width - panelWidth) / 2
        y = 0
        g2d.drawImage(image, x, y, panelWidth, panelHeight, this)
        // 计算缩放比
        scaleX = image!!.width.toDouble() / panelWidth.toDouble()
        scaleY = image!!.height.toDouble() / panelHeight.toDouble()
    }

    private fun drawSelectedRect(g2d: Graphics2D) {
        if (rect != null) {
            g2d.color = JBColor.RED
            g2d.stroke = BasicStroke(2f)
            val scaleRect = scaleRect(rect!!)
            g2d.drawRect(scaleRect.x, scaleRect.y, scaleRect.width, scaleRect.height)
        }
        if (greenRect != null) {
            g2d.color = JBColor.GREEN
            g2d.stroke = BasicStroke(2f)
            val scaleRect = scaleRect(greenRect!!)
            g2d.drawRect(scaleRect.x, scaleRect.y, scaleRect.width, scaleRect.height)
        }
    }

    private fun drawAllNodeRect(g2d: Graphics2D) {
        if (allNodeRect == null) return
        val dashPattern = floatArrayOf(5f, 5f)
        g2d.color = JBColor.GRAY
        g2d.stroke = BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dashPattern, 0f)
        for (rectangle in allNodeRect) {
            if (rectangle.width > 10000) continue
            val scaleRect = scaleRect(rectangle)
            g2d.drawRect(scaleRect.x, scaleRect.y, scaleRect.width, scaleRect.height)
        }
    }

    private fun drawSwipeLine(g2d: Graphics2D) {
        if (line != null) {
            g2d.color = JBColor.RED
            g2d.stroke = BasicStroke(1f)
            g2d.drawLine(line!![0], line!![1], line!![2], line!![3])
        }
    }

    private fun scaleRect(rectangle: Rectangle): Rectangle {
        val x1 = ((rectangle.x / scaleX) + x).toInt()
        val y1 = ((rectangle.y / scaleY) + y).toInt()
        val w = (rectangle.width / scaleX).toInt()
        val h = (rectangle.height / scaleY).toInt()
        return Rectangle(x1, y1, w, h)
    }

    fun getImage(): BufferedImage? = image

    fun getRect(): Rectangle? = rect

    open class ImagePanelListener {
        open fun onMousePositionChange(imageX: Int, imageY: Int) {}
        open fun onNodeSelected(imageX: Int, imageY: Int) {}
        open fun onLeftDoubleClicked(imageX: Int, imageY: Int, isCtrlPressed: Boolean) {}
        open fun onLeftClicked(imageX: Int, imageY: Int, isCtrlPressed: Boolean) {}
        open fun onRightClicked(imageX: Int, imageY: Int, isCtrlPressed: Boolean) {}
        open fun onMouseWheel() {}
        open fun onSwipe(
            startX: Double,
            startY: Double,
            endX: Double,
            endY: Double,
            duration: Double,
            isCtrlPressed: Boolean
        ) {
        }
    }
}
