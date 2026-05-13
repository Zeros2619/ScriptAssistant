package com.zeros.scriptassistant

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.observable.util.not
import com.intellij.openapi.project.Project
import com.intellij.ui.*
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.actionButton
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.UnscaledGaps
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.table.JBTable
import com.intellij.ui.treeStructure.Tree
import com.intellij.openapi.actionSystem.impl.ActionButtonWithText
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.gridLayout.UnscaledGapsY
import java.awt.BorderLayout
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.util.HashSet
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class ToolUI(private val project: Project) : ToolManagerService.UiUpdater {
    private val manager = ToolManagerService.getInstance(project)

    init {
        manager.uiUpdater = this
    }

    // UI 组件引用
    private lateinit var mousePosLabel: JLabel
    private lateinit var imagePanel: ImagePanel
    private lateinit var nodeTree: Tree
    private lateinit var nodeInfoTable: JBTable
    private lateinit var tableModel: DefaultTableModel
    private lateinit var matchCodeTF: JTextField
    private lateinit var aliasTextField: JTextField

    // 防止更新树模型时重复触发选择事件
    private var isUpdatingTree = false

    // 图片面板监听器
    private val imagePanelListener = object : ImagePanel.ImagePanelListener() {
        override fun onMousePositionChange(imageX: Int, imageY: Int) {
            mousePosLabel.text = "Mouse Position: ($imageX, $imageY)"
        }

        override fun onNodeSelected(imageX: Int, imageY: Int) {
            manager.updateSelectedNode(nodeTree, imageX, imageY)
        }

        override fun onLeftDoubleClicked(imageX: Int, imageY: Int, isCtrlPressed: Boolean) {
            if (isCtrlPressed) {
                manager.generateCtrlClickCode(true)
            } else {
                manager.generateClickCode()
            }
        }

        override fun onLeftClicked(imageX: Int, imageY: Int, isCtrlPressed: Boolean) {
            if (isCtrlPressed) {
                val img = imagePanel.getImage()
                if (img != null) {
                    val percentX = imageX.toDouble() / img.width / 2.0
                    val percentY = imageY.toDouble() / img.height / 2.0
                    manager.generatePercentClickCode(percentX, percentY)
                }
            }
        }

        override fun onRightClicked(imageX: Int, imageY: Int, isCtrlPressed: Boolean) {
            if (isCtrlPressed) {
                manager.generateCtrlSelectorCode(true)
            } else {
                manager.generateSelectorCode()
            }
        }

        override fun onMouseWheel() {
            imagePanel.paintRect(null)
            imagePanel.paintGreenRect(null)
            nodeTree.selectionPath = null
        }

        override fun onSwipe(
            startX: Double,
            startY: Double,
            endX: Double,
            endY: Double,
            duration: Double,
            isCtrlPressed: Boolean
        ) {
            if (isCtrlPressed) {
                manager.generateCtrlSwipeCode(startX, startY, endX, endY, duration)
            } else {
                manager.generateSwipeCode(startX, startY, endX, endY, duration)
            }
        }
    }

    val contentPanel: JPanel = createMainPanel()

    private fun createMainPanel(): JPanel {
        val mainPanel = JPanel(BorderLayout())
        mainPanel.add(createTopToolbar(), BorderLayout.NORTH)
        mainPanel.add(createCenterSplitter(), BorderLayout.CENTER)
        mainPanel.add(createBottomSearchBar(), BorderLayout.SOUTH)
        return mainPanel
    }

    /**
     * 创建顶部操作栏
     */
    private fun createTopToolbar(): JPanel {
        return panel {
            row {
                // 刷新图标按钮
                actionButton(
                    object : AnAction("Refresh", "Refresh connected devices", AllIcons.General.Refresh) {
                        override fun actionPerformed(e: AnActionEvent) {
                            manager.refreshDevices()
                        }
                    }
                ).customize(UnscaledGaps(left = 10, right = 10))

                // 设备序列号下拉列表
                comboBox(manager.deviceModel)
                    .enabledIf(manager.idleProperty).gap(RightGap.SMALL)
                    .applyToComponent {
                        addItemListener { e ->
                            if (e.stateChange == java.awt.event.ItemEvent.SELECTED) {
                                manager.selectedDevice = e.item as? String
                            }
                        }
                    }

                // 设备别名输入框
                aliasTextField = textField()
                    .applyToComponent {
                        text = "d"
                        columns = 5
                    }.visibleIf(manager.isConnectedProperty).enabledIf(manager.idleProperty)
                    .onChanged {
                        manager.updateDeviceAlias(it.text)
                    }
                    .component

                // 连接按钮
                cell(createTextActionButton("Connect") { manager.connectDevice(manager.selectedDevice) })
                    .visibleIf(manager.isConnectedProperty.not()).gap(RightGap.SMALL)

                // 断开连接按钮
                cell(createTextActionButton("Disconnect") { manager.disconnectDevice() })
                    .visibleIf(manager.isConnectedProperty).enabledIf(manager.idleProperty).gap(RightGap.SMALL)

                // dump ui 信息按钮
                cell(createTextActionButton("Dump") { manager.dumpUI() })
                    .visibleIf(manager.isConnectedProperty).enabledIf(manager.idleProperty).gap(RightGap.SMALL)

                // ----------------------------------------------------

                // 生成选择器复选框
                checkBox("Selector")
                    .visibleIf(manager.isConnectedProperty).enabledIf(manager.idleProperty).gap(RightGap.SMALL)

                // 占位符，利用 AlignX.FILL 将后面的设置按钮推到最右侧
                cell(JPanel()).align(AlignX.FILL).resizableColumn()

                // 设置图标按钮
                actionButton(
                    object : AnAction("Settings", "Script assistant settings", AllIcons.General.Settings) {
                        override fun actionPerformed(e: AnActionEvent) {
                            val dialog =
                                SettingsDialog(project, DeviceAliasConfig.getInstance(project) ?: DeviceAliasConfig())
                            dialog.show()
                        }
                    }
                ).customize(UnscaledGaps(left = 10, right = 10))
            }
        }
    }

    /**
     * 创建纯文本无图标的 ActionButton
     */
    private fun createTextActionButton(text: String, action: () -> Unit): ActionButtonWithText {
        val anAction = object : AnAction(text) {
            override fun actionPerformed(e: AnActionEvent) {
                action()
            }
        }
        // 使用 ActionButtonWithText，尺寸给 (0,0) 它会自动根据文本内容计算所需宽度
        return ActionButtonWithText(
            anAction,
            anAction.templatePresentation.clone(),
            ActionPlaces.UNKNOWN,
            java.awt.Dimension(0, 30)
        )
    }

    /**
     * 创建中间的可调整大小的分隔面板（核心业务区）
     */
    private fun createCenterSplitter(): JBSplitter {
        // 主分割器：水平分割，左侧占 60%
        val mainSplitter = JBSplitter(false, 0.6f)

        // --- 左侧：截图与位置信息区域 ---
        val screenshotPanel = createScreenshotPanel()
        mainSplitter.firstComponent = screenshotPanel

        // --- 右侧：控件树与属性表格区域 ---
        // 右侧分割器：垂直分割，上下各占 60%
        val rightSplitter = JBSplitter(true, 0.6f)

        // 右上：控件树 (Hierarchy)
        val rootNode = DefaultMutableTreeNode("hierarchy")
        nodeTree = Tree(rootNode)
        nodeTree.addTreeSelectionListener { e ->
            // 防止更新树模型时触发选择事件
            if (isUpdatingTree) return@addTreeSelectionListener
            val selectedNode = e.path.lastPathComponent as? DefaultMutableTreeNode ?: return@addTreeSelectionListener
            val nodeInfo = selectedNode.userObject as? NodeInfo ?: return@addTreeSelectionListener
            // 更新属性表
            updateTable(nodeInfo)
            // 绘制选中矩形
            imagePanel.paintRect(nodeInfo)
            imagePanel.paintGreenRect(null)
            // 设置目标节点
            println("setTarget: $selectedNode")
            manager.setTarget(selectedNode)
        }
        // 添加右键菜单监听
        nodeTree.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    val path = nodeTree.getPathForLocation(e.x, e.y)
                    path?.let { nodeTree.selectionPath = it }
                    manager.generateSelectorCode()
                }
            }
        })
        TreeSpeedSearch.installOn(nodeTree)
        val treeScrollPane = ScrollPaneFactory.createScrollPane(nodeTree)
        rightSplitter.firstComponent = treeScrollPane

        // 右下：属性表格 (Properties)
        tableModel = DefaultTableModel(arrayOf(arrayOf("", "")), arrayOf("property", "value"))
        nodeInfoTable = JBTable(tableModel).apply {
            autoCreateRowSorter = true
            // 添加行选择监听 - 复制属性值
            selectionModel.addListSelectionListener { e ->
                if (!e.valueIsAdjusting && selectedRow != -1) {
                    val value = getValueAt(selectedRow, 1) as? String ?: return@addListSelectionListener
                    val selection = StringSelection("\"$value\"")
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
                }
            }
            // 添加右键菜单监听 - 插入属性到代码
            addMouseListener(object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        val row = rowAtPoint(e.point)
                        if (row in 0 until rowCount) {
                            setRowSelectionInterval(row, row)
                            val prop = getModel().getValueAt(row, 0) as String
                            val value = getModel().getValueAt(row, 1) as String
                            manager.generateAddSelectorParamCode(prop, value)
                        }
                    }
                }
            })
        }
        nodeInfoTable.addMouseWheelListener { e ->
            // 禁用表格的鼠标滚轮滚动，避免与图片面板冲突
            e.consume()
        }
        TableSpeedSearch.installOn(nodeInfoTable)
        val tableScrollPane = ScrollPaneFactory.createScrollPane(nodeInfoTable)
        rightSplitter.secondComponent = tableScrollPane

        mainSplitter.secondComponent = rightSplitter

        return mainSplitter
    }

    /**
     * 创建截图面板（带物理按键）
     */
    private fun createScreenshotPanel(): JPanel {
        // 使用 BorderLayout 布局：中间是图片，右侧是按键列，下方是按键行
        val panel = JPanel(BorderLayout()).apply {
            background = JBColor.PanelBackground
        }

        // 顶部：鼠标位置标签
        mousePosLabel = JLabel("Mouse Position: (0, 0)").apply {
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        }
        panel.add(mousePosLabel, BorderLayout.NORTH)

        // 中间：图片面板和右侧按钮面板
        val centerPanel = JPanel(BorderLayout()).apply {
            background = JBColor.PanelBackground
        }

        // 图片面板
        imagePanel = ImagePanel()
        imagePanel.setListener(imagePanelListener)
        centerPanel.add(imagePanel, BorderLayout.CENTER)

        // 右侧按钮面板（音量 +、音量 -、电源）
        val rightButtonPanel = createRightButtonPanel()
        centerPanel.add(rightButtonPanel, BorderLayout.EAST)

        panel.add(centerPanel, BorderLayout.CENTER)

        // 下方：物理按键行（返回、Home、菜单）
        val bottomButtonPanel = createBottomButtonPanel()
        panel.add(bottomButtonPanel, BorderLayout.SOUTH)

        return panel
    }

    /**
     * 创建右侧物理按键面板 (使用 UI DSL 实现)
     */
    private fun createRightButtonPanel(): JPanel {
        return panel {
            row {
                cell(JPanel()).customize(UnscaledGaps(top = 80))
            }
            row {
                cell(createIconButton("volume_up.svg", "Volume Up", "volume_up"))
                    .visibleIf(manager.isConnectedProperty).enabledIf(manager.idleProperty)
            }
            row {
                cell(createIconButton("volume_down.svg", "Volume Down", "volume_down"))
                    .visibleIf(manager.isConnectedProperty).enabledIf(manager.idleProperty)
            }
            row {
                cell(createIconButton("power.svg", "Power", "power"))
                    .visibleIf(manager.isConnectedProperty).enabledIf(manager.idleProperty)
            }
            row {
                cell(JPanel()).align(AlignY.FILL).resizableColumn()
            }
        }
    }

    /**
     * 创建下方物理按键面板 (使用 UI DSL 实现)
     */
    private fun createBottomButtonPanel(): JPanel {
        return panel {
            row {
                cell(JPanel()).align(AlignX.FILL).resizableColumn()
                cell(createIconButton("back.svg", "Back", "Back"))
                    .visibleIf(manager.isConnectedProperty).enabledIf(manager.idleProperty)
                cell(createIconButton("home.svg", "Home", "Home"))
                    .visibleIf(manager.isConnectedProperty).enabledIf(manager.idleProperty)
                cell(createIconButton("recent.svg", "Recent Apps", "RecentApps"))
                    .visibleIf(manager.isConnectedProperty).enabledIf(manager.idleProperty)
                cell(JPanel()).align(AlignX.FILL).resizableColumn()
            }
        }
    }

    /**
     * 创建图标按钮 (替换为 IntelliJ ActionButton，并支持右键)
     */
    private fun createIconButton(iconName: String, tooltip: String, keyCode: String): JComponent {
        val icon = IconLoader.getIcon("/$iconName", javaClass)

        // 创建 Action 定义
        val action = object : AnAction(tooltip, tooltip, icon) {
            override fun actionPerformed(e: AnActionEvent) {
                // 左键点击触发正常的物理按键事件
                manager.executeKeyEvent(keyCode)
            }
        }

        // 使用 ActionButton 替代普通 JButton 以适配 IntelliJ 主题风格
        val button = ActionButton(
            action,
            action.templatePresentation.clone(),
            ActionPlaces.UNKNOWN,
            java.awt.Dimension(40, 40)
        ).apply {
            preferredSize = java.awt.Dimension(40, 40)
            maximumSize = java.awt.Dimension(40, 40)

            // 监听鼠标事件，增加右键点击支持
            addMouseListener(object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        manager.executeKeyEvent(keyCode, isRightClick = true)
                    }
                }
            })
        }

        return button
    }

    /**
     * 创建底部搜索栏
     */
    private fun createBottomSearchBar(): JPanel {
        return panel {
            row("Code:") {
                matchCodeTF = textField()
                    .applyToComponent {
                        text = ""
                    }
                    .enabledIf(manager.idleProperty)
                    .align(AlignX.FILL).gap(RightGap.SMALL).resizableColumn().component

                button("Find") {
                    manager.matchNodeByCode(matchCodeTF.text)
                }.enabledIf(manager.idleProperty)
            }
        }
    }

    private fun updateTable(nodeInfo: NodeInfo?) {
        val data = nodeInfo?.getTableArray() ?: arrayOf(arrayOf("", ""))
        tableModel.dataVector.clear()
        data.forEach { row ->
            tableModel.addRow(row)
        }
        // 调整列宽
        for (column in 0 until nodeInfoTable.columnCount) {
            var width = 50
            for (row in 0 until nodeInfoTable.rowCount) {
                val renderer = nodeInfoTable.getCellRenderer(row, column)
                val comp = nodeInfoTable.prepareRenderer(renderer, row, column)
                width = maxOf(comp.preferredSize.width + 10, width)
            }
            nodeInfoTable.columnModel.getColumn(column).preferredWidth = width
        }
    }

    override fun displayImage(img: BufferedImage, allNodeRect: HashSet<Rectangle>) {
        SwingUtilities.invokeLater {
            imagePanel.setAllNodeRect(allNodeRect)
            imagePanel.setImage(img)
        }
    }

    override fun updateTree(hierarchy: DefaultMutableTreeNode?) {
        if (hierarchy == null) {
            return
        }
        SwingUtilities.invokeLater {
            try {
                isUpdatingTree = true
                nodeTree.model = DefaultTreeModel(hierarchy)
            } finally {
                isUpdatingTree = false
            }
        }
    }

    override fun paintGreenRect(nodeInfo: NodeInfo?) {
        SwingUtilities.invokeLater {
            if (nodeInfo == null) {
                // 设置文本颜色为红色
                matchCodeTF.foreground = JBColor.RED
                matchCodeTF.grabFocus()
            } else {
                imagePanel.paintGreenRect(nodeInfo)
                // 恢复默认文本颜色
                matchCodeTF.foreground = JBColor.foreground()
            }
        }
    }

    override fun updateMatchCodeTF(code: String) {
        println("updateMatchCodeTF: $code")
        SwingUtilities.invokeLater {
            matchCodeTF.text = code
            // 恢复默认文本颜色
            matchCodeTF.foreground = JBColor.foreground()
        }
    }
}