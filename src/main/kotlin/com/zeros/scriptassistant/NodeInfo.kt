package com.zeros.scriptassistant

import cn.hutool.core.util.NumberUtil
import cn.hutool.core.util.ReUtil
import cn.hutool.core.util.StrUtil
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.awt.Rectangle
import java.util.regex.Pattern

class NodeInfo {
    var id: String = ""
    var text: String = ""
    var desc: String = ""
    var Class: String = ""
    var Package: String = ""
    var checkable: String = ""
    var checked: String = ""
    var clickable: String = ""
    var enabled: String = ""
    var focusable: String = ""
    var focused: String = ""
    var scrollable: String = ""
    var longClickable: String = ""
    var password: String = ""
    var selected: String = ""
    var visible: String = ""
    var boundsStr: String = ""
    var bounds: Rectangle = Rectangle(0, 0, Int.MAX_VALUE, Int.MAX_VALUE)
    var area: Int = Int.MAX_VALUE

    var node: Node? = null

    constructor(name: String) {
        Class = name
    }

    constructor(node: Node) {
        this.node = node
        val element = node as Element
        id = element.getAttribute("resource-id")
        text = toEscapedString(element.getAttribute("text"))
        desc = toEscapedString(element.getAttribute("content-desc"))
        Class = element.getAttribute("class")
        Package = element.getAttribute("package")
        checkable = element.getAttribute("checkable")
        checked = element.getAttribute("checked")
        clickable = element.getAttribute("clickable")
        enabled = element.getAttribute("enabled")
        focusable = element.getAttribute("focusable")
        focused = element.getAttribute("focused")
        scrollable = element.getAttribute("scrollable")
        longClickable = element.getAttribute("long-clickable")
        password = element.getAttribute("password")
        selected = element.getAttribute("selected")
        visible = element.getAttribute("visible-to-user")
        boundsStr = element.getAttribute("bounds")

        val allGroups = ReUtil.findAllGroup0(Pattern.compile("\\d+"), boundsStr)
        if (allGroups.size == 4) {
            val left = NumberUtil.parseInt(allGroups[0])
            val top = NumberUtil.parseInt(allGroups[1])
            val right = NumberUtil.parseInt(allGroups[2])
            val bottom = NumberUtil.parseInt(allGroups[3])
            bounds = Rectangle(left, top, right - left, bottom - top)
            area = bounds.width * bounds.height
        }
    }

    fun getTableArray(): Array<Array<String>> {
        val result = Array(17) { arrayOf("", "") }
        result[0] = arrayOf("resourceId", id)
        result[1] = arrayOf("text", text)
        result[2] = arrayOf("description", desc)
        result[3] = arrayOf("className", Class)
        result[4] = arrayOf("packageName", Package)
        result[5] = arrayOf("checkable", checkable)
        result[6] = arrayOf("checked", checked)
        result[7] = arrayOf("clickable", clickable)
        result[8] = arrayOf("enabled", enabled)
        result[9] = arrayOf("focusable", focusable)
        result[10] = arrayOf("focused", focused)
        result[11] = arrayOf("scrollable", scrollable)
        result[12] = arrayOf("longClickable", longClickable)
        result[13] = arrayOf("password", password)
        result[14] = arrayOf("selected", selected)
        result[15] = arrayOf("visible-to-user", visible)
        result[16] = arrayOf("bounds", boundsStr)
        return result
    }

    fun getProperties(): List<String> {
        val result = mutableListOf<String>()
        if (StrUtil.isNotEmpty(id)) result.add("id")
        if (StrUtil.isNotEmpty(text)) result.add("text")
        if (StrUtil.isNotEmpty(desc)) result.add("desc")
        if (StrUtil.isNotEmpty(Class)) result.add("Class")
        return result
    }

    fun getPropertyValue(prop: String): String? {
        return when (prop) {
            "id" -> id
            "text" -> text
            "desc" -> desc
            "Class" -> Class
            else -> null
        }
    }

    fun getUniqueProperties(other: NodeInfo): List<String>? {
        if (other === this) return null
        val result = mutableListOf<String>()

        if (StrUtil.isNotEmpty(other.id) && other.id != id) {
            result.add("id")
            return result
        }
        if (StrUtil.isNotEmpty(other.text) && other.text != text) {
            result.add("text")
            return result
        }
        if (StrUtil.isNotEmpty(other.desc) && other.desc != desc) {
            result.add("desc")
            return result
        }
        if (StrUtil.isNotEmpty(other.Class) && other.Class != Class) {
            result.add("Class")
            return result
        }
        return result
    }

    fun isInBounds(x: Int, y: Int): Boolean {
        return bounds.x < x && (bounds.x + bounds.width) > x &&
                bounds.y < y && (bounds.y + bounds.height) > y
    }

    override fun toString(): String {
        val result = StringBuilder()
        result.append(
            when {
                Class.startsWith("android.widget.") -> Class.substring(15)
                Class.startsWith("android.view.") -> Class.substring(13)
                else -> Class
            }
        )
        if (StrUtil.isNotEmpty(text)) {
            val showText = if (text.length > 10) text.substring(0, 10) else text
            result.append(":").append(showText)
        }
        if (StrUtil.isNotEmpty(desc)) {
            val showDesc = if (desc.length > 10) desc.substring(0, 10) else desc
            result.append("{").append(showDesc).append("}")
        }
        return result.toString()
    }

    companion object {
        fun toEscapedString(input: String): String {
            return input
                .replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\"", "\\\"")
        }
    }
}
