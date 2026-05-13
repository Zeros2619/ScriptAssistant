package com.zeros.scriptassistant

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import java.awt.Rectangle
import java.awt.image.BufferedImage
import javax.swing.DefaultComboBoxModel
import javax.swing.SwingWorker
import javax.swing.tree.DefaultMutableTreeNode

@Service(Service.Level.PROJECT)
class ToolManagerService(private val project: Project) {
    companion object {
        fun getInstance(project: Project): ToolManagerService {
            return project.getService(ToolManagerService::class.java)
        }
    }

    interface UiUpdater {
        fun paintGreenRect(nodeInfo: NodeInfo?)
        fun displayImage(img: BufferedImage, allNodeRect: HashSet<Rectangle>)
        fun updateTree(hierarchy: DefaultMutableTreeNode?)
        fun updateMatchCodeTF(code: String)
    }

    var uiUpdater: UiUpdater? = null

    // 创建属性图（用于管理状态）
    val propertyGraph = PropertyGraph()
    // 控制连接与未连接状态时的界面展示
    val isConnectedProperty = propertyGraph.property(false)
    // 界面空闲时为 true, 控制按钮可用状态，防止重复操作
    val idleProperty = propertyGraph.property(true)
    // 把核心业务状态存在这里
    var selectedDevice: String? = null
    val deviceModel = DefaultComboBoxModel<String>()

    private val deviceAliasConfig: DeviceAliasConfig = DeviceAliasConfig.getInstance(project) ?: DeviceAliasConfig()
    private val codeGenerator = CodeGenerator(project)
    private val connectedDevices = mutableListOf<Device>()
    private var currentDevice: Device? = null
    private var saneCode = false

    // 当前选中的节点
    private var targetNode: DefaultMutableTreeNode? = null

    fun refreshDevices() {
        ApplicationManager.getApplication().executeOnPooledThread {
            val connectedDevicesList = ADBDeviceUtils.getConnectedDevices()
            ApplicationManager.getApplication().invokeLater {
                deviceModel.removeAllElements()
                connectedDevicesList.forEach { deviceModel.addElement(it) }
                if (connectedDevicesList.isNotEmpty() && selectedDevice == null) {
                    deviceModel.selectedItem = connectedDevicesList[0]
                }
            }
        }
    }

    fun connectDevice(serial: String?) {
        if (serial.isNullOrBlank()) {
            onDeviceConnected(false)
            return
        }

        if (!ADBDeviceUtils.isDeviceOnline(serial)) {
            onDeviceConnected(false)
            return
        }

        idleProperty.set(false)
        val pythonSdkPath = codeGenerator.getPythonSdkPath(deviceAliasConfig)
        println("pythonSdkPath=$pythonSdkPath")

        val connectingDevice = Device(serial, deviceAliasConfig)
        ApplicationManager.getApplication().executeOnPooledThread {
            if (connectingDevice.init(pythonSdkPath ?: "", codeGenerator.getFilePath())) {
                connectingDevice.setAliasByIndex(connectedDevices.size)
                connectedDevices.add(connectingDevice)
                currentDevice = connectingDevice
                onDeviceConnected(true)
            } else {
                onDeviceConnected(false)
                val failMsg = connectingDevice.u2.getInitFailMsg() ?: "Unknown error"
                sendNotification("Error", failMsg, NotificationType.ERROR)
            }
            idleProperty.set(true)
        }
    }

    fun disconnectDevice() {
        currentDevice?.close()
        currentDevice?.let { connectedDevices.remove(it) }
        isConnectedProperty.set(false)
    }

    private fun onDeviceConnected(success: Boolean) {
        if (success) {
            isConnectedProperty.set(true)
            dumpUI()
        } else {
            isConnectedProperty.set(false)
        }
    }

    fun dumpUI(delay:Long = 0) {
        ApplicationManager.getApplication().executeOnPooledThread {
            idleProperty.set(false)
            Thread.sleep(delay)
            try {
                val device = currentDevice ?: return@executeOnPooledThread
                device.dumpUI()

                // Wait for screenshot and hierarchy files to be ready
                val latch = java.util.concurrent.CountDownLatch(2)

                ApplicationManager.getApplication().executeOnPooledThread {
                    try {
                        val screenshot = device.getScreenshot()
                        screenshot?.let {
                            try {
                                val img = javax.imageio.ImageIO.read(java.io.ByteArrayInputStream(it))
                                uiUpdater?.displayImage(img, device.allNodeRect)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        println("getScreenshot done")
                        latch.countDown()
                    }
                }

                ApplicationManager.getApplication().executeOnPooledThread {
                    try {
                        val hierarchy = device.getHierarchy()
                        uiUpdater?.updateTree(hierarchy)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        println("getHierarchy done")
                        latch.countDown()
                    }
                }

                try {
                    latch.await(15, java.util.concurrent.TimeUnit.SECONDS)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            } finally {
                idleProperty.set(true)
            }
        }
    }

    fun sendNotification(title: String, message: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Script Assistant")
            .createNotification(title, message, type)
            .notify(project)
    }

    fun setTarget(node: DefaultMutableTreeNode?) {
        targetNode = node
        updateMatchCodeTF()
    }

    fun getDeviceAlias(): String = currentDevice?.alias ?: "d"

    fun generateClickCode() {
        targetNode?.let { node ->
            val info = node.userObject as NodeInfo
            val code = codeGenerator.generateCode(currentDevice!!, info.node!!)
            val clickCode = codeGenerator.clickCode(code)
            codeGenerator.insert(codeGenerator.getCompletedCode(currentDevice!!.alias, clickCode), true)
            execCode(codeGenerator.getCompletedCode(Device.OBJECT_NAME, clickCode))
        }
    }

    fun generateSelectorCode() {
        targetNode?.let { node ->
            val info = node.userObject as NodeInfo
            val code = codeGenerator.generateCode(currentDevice!!, info.node!!, saneCode, false)
            val completedCode = if (!saneCode) {
                codeGenerator.getCompletedCode(currentDevice!!.alias, code)
            } else {
                code
            }
            codeGenerator.insert(completedCode, saneCode)
        }
    }

    fun updateSelectedNode(tree: javax.swing.JTree, imageX: Int, imageY: Int) {
        val root = tree.model.root as? DefaultMutableTreeNode ?: return
        targetNode = root
        searchNode(root, imageX, imageY)
        targetNode?.let { target ->
            val path = javax.swing.tree.TreePath((tree.model as javax.swing.tree.DefaultTreeModel).getPathToRoot(target))
            tree.selectionPath = path
        }
    }

    private fun searchNode(root: DefaultMutableTreeNode, imageX: Int, imageY: Int) {
        val nodeInfo = root.userObject as? NodeInfo ?: return
        if (nodeInfo.isInBounds(imageX, imageY)) {
            val targetInfo = targetNode?.userObject as? NodeInfo
            if (targetInfo == null || targetInfo.area >= nodeInfo.area) {
                targetNode = root
            }
            for (i in 0 until root.childCount) {
                val child = root.getChildAt(i) as? DefaultMutableTreeNode ?: continue
                searchNode(child, imageX, imageY)
            }
        }
    }

    fun generatePercentClickCode(percentX: Double, percentY: Double) {
        val code = codeGenerator.generatePercentClickCode(percentX, percentY)
        codeGenerator.insert(codeGenerator.getCompletedCode(currentDevice!!.alias, code), true)
        execCode(codeGenerator.getCompletedCode(Device.OBJECT_NAME, code))
    }

    fun generateCtrlClickCode(useXpath: Boolean) {
        targetNode?.let { node ->
            val info = node.userObject as NodeInfo
            val code = codeGenerator.generateCode(currentDevice!!, info.node!!, false, useXpath)
            val clickCode = codeGenerator.clickCode(code)
            codeGenerator.insert(codeGenerator.getCompletedCode(currentDevice!!.alias, clickCode), true)
            execCode(codeGenerator.getCompletedCode(Device.OBJECT_NAME, clickCode))
        }
    }

    fun generateCtrlSelectorCode(useXpath: Boolean) {
        targetNode?.let { node ->
            val info = node.userObject as NodeInfo
            val code = codeGenerator.generateCode(currentDevice!!, info.node!!, false, useXpath)
            codeGenerator.insert(code, false)
        }
    }

    fun generateSwipeCode(startX: Double, startY: Double, endX: Double, endY: Double, duration: Double) {
        val code = if (startX > 1) {
            codeGenerator.generateSwipeCode(startX.toInt(), startY.toInt(), endX.toInt(), endY.toInt(), duration)
        } else {
            codeGenerator.generateSwipeCode(startX, startY, endX, endY, duration)
        }
        codeGenerator.insert(codeGenerator.getCompletedCode(currentDevice!!.alias, code), true)
        execCode(codeGenerator.getCompletedCode(Device.OBJECT_NAME, code))
    }

    fun generateCtrlSwipeCode(startX: Double, startY: Double, endX: Double, endY: Double, duration: Double) {
        val code = if (startX > 1) {
            codeGenerator.generateDragCode(startX.toInt(), startY.toInt(), endX.toInt(), endY.toInt(), duration)
        } else {
            codeGenerator.generateDragCode(startX, startY, endX, endY, duration)
        }
        codeGenerator.insert(codeGenerator.getCompletedCode(currentDevice!!.alias, code), true)
        execCode(codeGenerator.getCompletedCode(Device.OBJECT_NAME, code))
    }

    fun generateAddSelectorParamCode(param: String, value: String) {
        codeGenerator.insertSelectorParam(currentDevice!!.alias, param, value)
    }

    private fun execCode(code: String?) {
        idleProperty.set(false)
        val actuallyExecCode = arrayOf<String?>(code)
        // 创建并执行 SwingWorker
        object : SwingWorker<String?, Void?>() {
            @Throws(java.lang.Exception::class)
            override fun doInBackground(): String? {
                // 使用click_exists替代click，避免点击不存在的控件报错和耗时过长
                if (actuallyExecCode[0]!!.endsWith(".click()")) {
                    actuallyExecCode[0] = actuallyExecCode[0]!!.replace(".click()", ".click_exists()")
                }
                // 在后台线程中执行耗时的 Python 代码
                println("startexec code: ${actuallyExecCode[0]}")
                return currentDevice!!.u2.executeCode(actuallyExecCode[0]!!)
            }

            override fun done() {
                try {
                    // 获取执行结果（如果需要）
                    val result = get()
                    // 在 EDT 中更新 UI
                    dumpUI(500)
                } catch (e: java.lang.Exception) {
                    idleProperty.set(true)
                    // 处理异常情况
                    e.printStackTrace()
                    sendNotification("Error", "exec code error: " + e.message, NotificationType.ERROR)
                }
            }
        }.execute() // 启动 SwingWorker
    }

    fun updateDeviceAlias(alias: String) {
        currentDevice?.updateAlias(alias)
    }

    fun getCurrentDevice(): Device? = currentDevice

    fun updateMatchCodeTF(){
        if (targetNode == null) {
            return
        }
        val info = targetNode!!.getUserObject() as NodeInfo
        val code = codeGenerator.generateCode(currentDevice!!, info.node, onlySelector = false, xpath = false)
        val completedCode = codeGenerator.getCompletedCode(currentDevice!!.alias, code)
        uiUpdater?.updateMatchCodeTF(completedCode)
    }

    fun matchNodeByCode(code: String) {
        if (code.isBlank()) {
            return
        }
        // 替换别名，加上括号防止替换到其他代码
        val normalizedCode = code
            .replace(currentDevice!!.alias + "(", Device.OBJECT_NAME + "(")
            .replace(currentDevice!!.alias + ".xpath(", Device.OBJECT_NAME + ".xpath(")

        idleProperty.set(false)
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val nodeBounds = currentDevice?.u2?.getNodeBounds(normalizedCode)
                ApplicationManager.getApplication().invokeLater {
                    idleProperty.set(true)
                    if (nodeBounds == null) {
                        uiUpdater?.paintGreenRect(null)
//                        sendNotification("Error", "Can not find target or invalid code", NotificationType.ERROR)
                        dumpUI()
                    } else {
                        val targetInfo = NodeInfo("target")
                        targetInfo.bounds = nodeBounds
                        targetInfo.area = nodeBounds.width * nodeBounds.height
                        uiUpdater?.paintGreenRect(targetInfo)
                    }
                }
            } catch (e: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    idleProperty.set(true)
                    sendNotification("Error", "Exception: ${e.message}", NotificationType.ERROR)
                }
            }
        }
    }

    fun executeKeyEvent(keyCode: String, isRightClick: Boolean = false) {
        // 映射按钮名称到 uiautomator2 的按键名称
        val keyMap = mapOf(
            "Back" to "back",
            "Home" to "home",
            "RecentApps" to "recent",
            "volume_up" to "volume_up",
            "volume_down" to "volume_down",
            "power" to "power"
        )

        val key = keyMap[keyCode] ?: return
        val code = codeGenerator.generateKeyEventCode(key)
        val completedCode = codeGenerator.getCompletedCode(currentDevice!!.alias, code)
        execCode(codeGenerator.getCompletedCode(Device.OBJECT_NAME, code))
        if (isRightClick) {
            codeGenerator.insert(completedCode, true)
        }
    }
}
