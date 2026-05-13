package com.zeros.scriptassistant

import cn.hutool.core.io.FileUtil
import cn.hutool.core.thread.ThreadUtil
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.awt.Rectangle
import java.io.ByteArrayInputStream
import java.io.File
import java.util.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.xml.parsers.DocumentBuilderFactory

class Device(val serial: String, private val deviceAliasConfig: DeviceAliasConfig) {
    companion object {
        const val OBJECT_NAME = "d"
    }

    var u2 = U2()
    var dumpSaveDir: String? = null
    private var screenshotBytes: ByteArray? = null
    var hierarchy: String? = null
    var hierarchyDoc: Document? = null
    var treeNode: DefaultMutableTreeNode? = null
    val allNodeRect = HashSet<Rectangle>()
    var attributeMap: MutableMap<String, MutableMap<String, Int>>? = null

    private var lastScreenshotModifyTime: Long = 0
    private var lastHierarchyModifyTime: Long = 0
    private lateinit var screenShotSaveFile: File
    private lateinit var hierarchySaveFile: File
    var alias: String = "d"

    fun init(pythonPath: String, saveDir: String): Boolean {
        dumpSaveDir = saveDir
        screenShotSaveFile = File(dumpSaveDir, "screen.png")
        hierarchySaveFile = File(dumpSaveDir, "hierarchy.xml")
        alias = deviceAliasConfig.getAlias(serial) ?: "d"

        val success = u2.init(pythonPath, serial, OBJECT_NAME)
        if (!success) {
            u2.destroy()
        }
        return success
    }

    fun getScreenshot(): ByteArray? {
        for (i in 0 until 50) {
            ThreadUtil.sleep(100)
            val date = FileUtil.lastModifiedTime(screenShotSaveFile)
            if (date != null && date.time != lastScreenshotModifyTime) {
                lastScreenshotModifyTime = date.time
                break
            }
        }
        screenshotBytes = FileUtil.readBytes(screenShotSaveFile)
        return screenshotBytes
    }

    fun getHierarchy(): DefaultMutableTreeNode? {
        for (i in 0 until 50) {
            ThreadUtil.sleep(100)
            val date = FileUtil.lastModifiedTime(hierarchySaveFile)
            if (date != null && date.time != lastHierarchyModifyTime) {
                lastHierarchyModifyTime = date.time
                break
            }
        }
        hierarchy = FileUtil.readUtf8String(hierarchySaveFile)

        return try {
            val dbFactory = DocumentBuilderFactory.newInstance()
            val dBuilder = dbFactory.newDocumentBuilder()
            val `is` = ByteArrayInputStream(hierarchy!!.toByteArray(Charsets.UTF_8))
            val doc = dBuilder.parse(`is`)
            doc.documentElement.normalize()
            hierarchyDoc = doc

            val nodeList = doc.documentElement.childNodes
            treeNode = DefaultMutableTreeNode(NodeInfo(doc.documentElement.nodeName))
            addNodes(nodeList, treeNode!!)
            allNodeRect.clear()
            obtainAllNodeRect(treeNode!!)
            obtainAttributeMap()
            treeNode
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun addNodes(nList: NodeList, parent: DefaultMutableTreeNode) {
        for (i in 0 until nList.length) {
            val node = nList.item(i)
            if (node.nodeType == Node.ELEMENT_NODE) {
                val child = DefaultMutableTreeNode(NodeInfo(node))
                parent.add(child)
                if (node.hasChildNodes()) {
                    addNodes(node.childNodes, child)
                }
            }
        }
    }

    private fun obtainAllNodeRect(root: DefaultMutableTreeNode) {
        val nodeInfo = root.userObject as NodeInfo
        allNodeRect.add(nodeInfo.bounds)
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i) as DefaultMutableTreeNode
            obtainAllNodeRect(child)
        }
    }

    fun obtainAttributeMap() {
        val node = hierarchyDoc!!.documentElement.getElementsByTagName("node")
        val map = mutableMapOf<String, MutableMap<String, Int>>()
        for (i in 0 until node.length) {
            val item = node.item(i)
            for (prop in XPathLite.props) {
                val nodeValue = item.attributes.getNamedItem(prop)?.nodeValue
                if (!nodeValue.isNullOrEmpty()) {
                    val propMap = map.getOrPut(prop) { mutableMapOf() }
                    val count = propMap.getOrDefault(nodeValue, 0)
                    propMap[nodeValue] = count + 1
                }
            }
        }
        attributeMap = map
    }

    fun dumpUI() {
        u2.dumpUI(screenShotSaveFile, hierarchySaveFile)
    }

    fun close() {
        u2.destroy()
    }

    fun updateAlias(alias: String) {
        this.alias = alias
        deviceAliasConfig.setAlias(serial, alias)
    }

    fun setAliasByIndex(index: Int) {
        if (alias != "d") return
        alias = if (index > 0) "d${index + 1}" else "d"
        deviceAliasConfig.setAlias(serial, alias)
    }
}
