package com.zeros.scriptassistant

import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import javax.xml.parsers.DocumentBuilderFactory

object NodeLocator {
    private val props = arrayOf("resource-id", "text", "content-desc", "class")
    private val propMap = mapOf(
        "resource-id" to "resourceId",
        "text" to "text",
        "content-desc" to "description",
        "class" to "className"
    )

    @JvmStatic
    fun main(args: Array<String>) {
        val xmlFilePath = "ui_xml.xml"
        try {
            // 解析 XML 文件
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val document = builder.parse(xmlFilePath)
            val node = document.documentElement.getElementsByTagName("node")

            // 示例：用户指定的列表索引
            val specifiedIndex = 30
            val node1 = node.item(specifiedIndex)
            // 获取指定索引位置的控件的最少属性组合
            val attributeCombination = getAttributeCombination(node, node1)

            // 输出结果
            if (attributeCombination != null) {
                println("Selector($attributeCombination)")
            } else {
                println("不能唯一定位")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getAttributeCombination(nodeList: NodeList, node: Node?): String? {
        if (node == null) {
            return null
        }

        val attributeCombination = StringBuilder()
        val propsResult = mutableListOf<String>()

        if (node.hasAttributes()) {
            for (prop in props) {
                val value = node.attributes.getNamedItem(prop)?.nodeValue
                if (value == null || value.isEmpty()) {
                    // 如果当前属性为空，则跳过
                    continue
                }
                // 判断当前属性是否能唯一定位，不唯一则加上下一个属性，组合判断
                propsResult.add(prop)
                if (!findNode(nodeList, node, propsResult)) {
                    // 可以则查找完成
                    break
                } else {
                    // 不唯一则去掉当前属性
                    propsResult.remove(prop)
                }
            }
        }

        if (propsResult.isEmpty()) {
            return null
        }

        for (prop in propsResult) {
            // 对属性值进行转义
            val value = NodeInfo.toEscapedString(node.attributes.getNamedItem(prop).nodeValue)
            attributeCombination.append(propMap[prop]).append("=\"").append(value).append("\",")
        }

        // 结尾去掉最后一个逗号
        attributeCombination.deleteCharAt(attributeCombination.length - 1)
        return attributeCombination.toString().trim()
    }

    fun findNode(nodeList: NodeList, node: Node, props: List<String>): Boolean {
        for (i in 0 until nodeList.length) {
            val n = nodeList.item(i)
            if (n == node) {
                continue
            }
            var find = true
            for (prop in props) {
                val v1 = node.attributes.getNamedItem(prop)?.nodeValue
                val v2 = n.attributes.getNamedItem(prop)?.nodeValue
                if (v1 != v2) {
                    find = false
                    break
                }
            }
            if (find) {
                return true
            }
        }
        return false
    }
}
