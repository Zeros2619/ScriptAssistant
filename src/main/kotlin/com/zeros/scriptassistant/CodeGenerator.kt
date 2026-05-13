package com.zeros.scriptassistant

import cn.hutool.core.util.StrUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.TextRange
import org.w3c.dom.Node
import org.w3c.dom.NodeList

class CodeGenerator(private val project: Project) {

    fun getCompletedCode(objectName: String, code: String): String {
        return objectName + code
    }

    fun generateSwipeCode(startX: Int, startY: Int, endX: Int, endY: Int, duration: Double): String {
        return ".swipe($startX, $startY, $endX, $endY, $duration)"
    }

    fun generateSwipeCode(startX: Double, startY: Double, endX: Double, endY: Double, duration: Double): String {
        return ".swipe($startX, $startY, $endX, $endY, $duration)"
    }

    fun generateDragCode(startX: Double, startY: Double, endX: Double, endY: Double, duration: Double): String {
        return ".drag($startX, $startY, $endX, $endY, $duration)"
    }

    fun generateDragCode(startX: Int, startY: Int, endX: Int, endY: Int, duration: Double): String {
        return ".drag($startX, $startY, $endX, $endY, $duration)"
    }

    fun generateCode(device: Device, node: Node): String {
        return generateCode(device, node, false, false)
    }

    fun generatePercentClickCode(percentX: Double, percentY: Double): String {
        return ".click($percentX, $percentY)"
    }

    fun generateKeyEventCode(key: String): String {
        return ".press(\"$key\")"
    }

    fun generateCode(device: Device, node: Node?, onlySelector: Boolean, xpath: Boolean): String {
        val nodeList = device.hierarchyDoc!!.documentElement.getElementsByTagName("node")
        var selector = NodeLocator.getAttributeCombination(nodeList, node)
        if (selector == null || xpath) {
            selector = XPathLite.getXPath(device.attributeMap!!, node)
            return if (onlySelector) {
                "'$selector'"
            } else {
                ".xpath('$selector')"
            }
        } else {
            return if (onlySelector) {
                "Selector($selector)"
            } else {
                "($selector)"
            }
        }
    }

    fun clickCode(code: String): String {
        return "$code.click()"
    }

    fun insert(code: String, endEnter: Boolean) {
        // 使用 invokeLater 确保在正确的上下文中执行
        ApplicationManager.getApplication().invokeLater {
            // 获取当前编辑器
            val editor = FileEditorManager.getInstance(project).selectedTextEditor
            if (editor != null) {
                // 获取文档对象，以便修改文件内容
                val document = editor.document

                // 获取光标所在的位置
                val offset = editor.caretModel.offset

                val line = code + if (endEnter) "\n" else ""
                // 在写操作中执行文本插入
                WriteCommandAction.runWriteCommandAction(project) {
                    document.insertString(offset, line)
                }

                if (endEnter) {
                    // 获取当前行号
                    val lineNumber = document.getLineNumber(offset)
                    // 获取当前行的起始偏移量
                    val lineStartOffset = document.getLineStartOffset(lineNumber)
                    // 获取当前行的缩进字符串
                    val lineText = document.charsSequence.subSequence(lineStartOffset, offset)
                    var indentLength = 0
                    while (indentLength < lineText.length && lineText[indentLength].isWhitespace()) {
                        indentLength++
                    }
                    val indent = lineText.subSequence(0, indentLength).toString()

                    // 在新行插入缩进
                    val newOffset = offset + line.length
                    WriteCommandAction.runWriteCommandAction(project) {
                        document.insertString(newOffset, indent)
                    }
                    // 移动光标到新插入的缩进后面
                    editor.caretModel.moveToOffset(newOffset + indent.length)
                } else {
                    // 移动光标到新插入的代码后面
                    editor.caretModel.moveToOffset(offset + line.length)
                }
            }
        }
    }

    fun insertSelectorParam(objectName: String, param: String, value: String) {
        // 使用 invokeLater 确保在正确的上下文中执行
        ApplicationManager.getApplication().invokeLater {
            // 获取当前编辑器
            val editor = FileEditorManager.getInstance(project).selectedTextEditor
            if (editor != null) {
                // 获取文档对象，以便修改文件内容
                val document = editor.document

                // 获取光标所在的位置
                val offset = editor.caretModel.offset

                // 获取当前行号
                val lineNumber = document.getLineNumber(offset)
                val lineStartOffset = document.getLineStartOffset(lineNumber)
                val lineEndOffset = document.getLineEndOffset(lineNumber)

                // 获取当前行内容
                val lineText = document.getText(TextRange(lineStartOffset, lineEndOffset)).trim()

                WriteCommandAction.runWriteCommandAction(project) {
                    // Case 1: 光标在括号内
                    if (isCursorInParentheses(document, offset)) {
                        insertParamInParentheses(document, offset, param, value)
                        return@runWriteCommandAction
                    }

                    // Case 2: 当前行匹配 objectName(.*) 模式
                    if (lineText.matches(objectName.replace(".", "\\.").toRegex())) {
                        insertParamInLine(document, lineStartOffset, lineEndOffset, objectName, param, value)
                        return@runWriteCommandAction
                    }

                    // Case 3: 当前行为空行，检查上一行
                    if (lineText.isEmpty() && lineNumber > 0) {
                        val prevLineNumber = lineNumber - 1
                        val prevLineStart = document.getLineStartOffset(prevLineNumber)
                        val prevLineEnd = document.getLineEndOffset(prevLineNumber)
                        val prevLineText = document.getText(TextRange(prevLineStart, prevLineEnd)).trim()

                        if (prevLineText.matches(objectName.replace(".", "\\.").toRegex())) {
                            insertParamInLine(document, prevLineStart, prevLineEnd, objectName, param, value)
                        }
                    }
                }
            }
        }
    }

    private fun isCursorInParentheses(document: Document, offset: Int): Boolean {
        val text = document.text
        val leftParen = text.lastIndexOf("(", offset)
        val rightParen = text.indexOf(")", leftParen - 1)
        return leftParen != -1 && rightParen != -1 && leftParen < offset && offset <= rightParen
    }

    private fun insertParamInLine(
        document: Document,
        lineStart: Int,
        lineEnd: Int,
        objectName: String,
        param: String,
        value: String
    ) {
        val lineText = document.getText(TextRange(lineStart, lineEnd))
        val leftParenIndex = lineText.indexOf(objectName + "(") + objectName.length
        val rightParenIndex = lineText.indexOf(")", leftParenIndex - 1)

        if (leftParenIndex != -1 && rightParenIndex != -1) {
            val params = lineText.substring(leftParenIndex + 1, rightParenIndex).trim()
            val newParam = "$param=\"$value\""

            // 检查是否已存在该参数
            if (params.contains("$param=")) {
                return // 参数已存在，不插入
            }

            // 构造新参数字符串
            val newParams = if (params.isEmpty()) newParam else "$params, $newParam"

            // 替换括号内的内容
            document.replaceString(lineStart + leftParenIndex + 1, lineStart + rightParenIndex, newParams)
        }
    }

    private fun insertParamInParentheses(
        document: Document,
        offset: Int,
        param: String,
        value: String
    ) {
        // 查找括号范围
        val text = document.text
        val leftParen = text.lastIndexOf("(", offset)
        val rightParen = text.indexOf(")", leftParen - 1)

        if (leftParen != -1 && rightParen != -1) {
            val params = document.getText(TextRange(leftParen + 1, rightParen)).trim()
            val newParam = "$param=\"$value\""

            // 检查是否已存在该参数
            if (params.contains("$param=")) {
                return // 参数已存在，不插入
            }

            // 构造新参数字符串
            val newParams = if (params.isEmpty()) newParam else "$params, $newParam"

            // 替换括号内的内容
            document.replaceString(leftParen + 1, rightParen, newParams)
        }
    }

    fun getPythonSdkPath(deviceAliasConfig: DeviceAliasConfig): String? {
        val interpreterPath = deviceAliasConfig.pythonInterpreterPath
        if (StrUtil.isNotEmpty(interpreterPath)) {
            return interpreterPath
        }
        // 获取当前项目的 SDK
        val projectSdk = ProjectRootManager.getInstance(project).projectSdk

        // 检查是否有 SDK
        if (projectSdk != null) {
            // 获取解释器路径
            return projectSdk.homePath
        }
        return null // 如果没有关联的 SDK，则返回 null
    }

    fun getFilePath(): String {
        return PathManager.getPluginsPath()
    }
}
