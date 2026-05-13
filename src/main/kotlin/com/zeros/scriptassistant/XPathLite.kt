package com.zeros.scriptassistant

import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import javax.xml.parsers.DocumentBuilderFactory

object XPathLite {
    val props = arrayOf("resource-id", "text", "content-desc", "class")

    @JvmStatic
    fun main(args: Array<String>) {
        val xmlFilePath = "ui_xml.xml"
        try {
            // 解析 XML 文件
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val document = builder.parse(xmlFilePath)
            val node = document.documentElement.getElementsByTagName("node")

            val attributeMap = HashMap<String, Map<String, Int>>()
            for (i in 0 until node.length) {
                val item = node.item(i)
                for (prop in props) {
                    val nodeValue = item.attributes.getNamedItem(prop)?.nodeValue
                    if (!nodeValue.isNullOrEmpty()) {
                        val propMap = attributeMap.getOrPut(prop) { HashMap() }
                        val count = propMap.getOrDefault(nodeValue, 0)
                        attributeMap[prop] = propMap + (nodeValue to (count + 1))
                    }
                }
            }

            // 示例：用户指定的列表索引
            val specifiedIndex = 10
            val selectedNode = node.item(specifiedIndex)
            val xpath = getXPath(attributeMap, selectedNode)
            println(xpath)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getAttrCount(map: Map<String, Map<String, Int>>, prop: String, value: String): Int {
        val propMap = map[prop] ?: return 0
        return propMap[value] ?: 0
    }

    private fun getAttrCount(map: Map<String, Map<String, Int>>, prop: String, node: Node): Int {
        return getAttrCount(map, prop, node.attributes.getNamedItem(prop).nodeValue)
    }

    fun getXPath(map: Map<String, Map<String, Int>>, selectedNode: Node?): String {
        if (selectedNode == null) {
            return ""
        }

        val array = mutableListOf<String>()
        var currentNode = selectedNode!!

        while (true) {
            val parentNode = currentNode.parentNode
            if (currentNode.nodeName != "node") {
                break
            }

            if (getAttrCount(map, "resource-id", currentNode) == 1) {
                array.add("*[@resource-id=\"${currentNode.attributes.getNamedItem("resource-id").nodeValue}\"]")
                break
            } else if (getAttrCount(map, "text", currentNode) == 1) {
                val text = NodeInfo.toEscapedString(currentNode.attributes.getNamedItem("text").nodeValue)
                array.add("*[@text=\"$text\"]")
                break
            } else if (getAttrCount(map, "content-desc", currentNode) == 1) {
                val desc = NodeInfo.toEscapedString(currentNode.attributes.getNamedItem("content-desc").nodeValue)
                array.add("*[@content-desc=\"$desc\"]")
                break
            } else {
                var index = 0
                if (parentNode == null) {
                    break
                }
                val type = currentNode.attributes.getNamedItem("class").nodeValue
                for (i in 0 until parentNode.childNodes.length) {
                    val child = parentNode.childNodes.item(i)
                    if (child.nodeType != Node.ELEMENT_NODE) {
                        continue
                    }
                    val childType = child.attributes.getNamedItem("class").nodeValue
                    if (childType == type) {
                        index++
                    }
                    if (child == currentNode) {
                        break
                    }
                }
                array.add("${currentNode.attributes.getNamedItem("class").nodeValue}[$index]")
            }
            currentNode = parentNode
        }

        val builder = StringBuilder("//")
        for (i in array.lastIndex downTo 0) {
            builder.append(array[i])
            if (i > 0) {
                builder.append("/")
            }
        }
        return builder.toString()
    }
}
