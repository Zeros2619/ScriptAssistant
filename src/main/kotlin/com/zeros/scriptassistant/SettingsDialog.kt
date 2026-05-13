package com.zeros.scriptassistant

import cn.hutool.core.io.FileUtil
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TextFieldWithBrowseButton // 需要导入该类
import com.intellij.ui.dsl.builder.*
import org.jetbrains.annotations.Nullable
import java.awt.Dimension
import javax.swing.JComponent

class SettingsDialog(
    private val project: Project,
    private val deviceAliasConfig: DeviceAliasConfig
) : DialogWrapper(project, true) {

    private var pythonInterpreterPath: String = ""

    // 💡 新增：用于在点击 OK 时直接获取界面上的真实路径
    private lateinit var pathInputField: TextFieldWithBrowseButton

    init {
        title = "Script Assistant Settings"
        isModal = true
        pythonInterpreterPath = deviceAliasConfig.pythonInterpreterPath.orEmpty()
        println("Init pythonInterpreterPath: $pythonInterpreterPath")
        init()
    }

    @Nullable
    override fun createCenterPanel(): JComponent {
        val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
            .withFileFilter {
                it.name.startsWith("python") && (it.extension == null || "exe" == it.extension)
            }
            .withTitle("Choose Python Interpreter")

        return panel {
            row("Python interpreter:") {
                val textField = textFieldWithBrowseButton(descriptor, project) { file ->
                    file.path
                }

                textField
                    .align(AlignX.FILL) // 💡 修复问题2：让组件填满所在列的宽度 (替代旧版的 horizontalAlign)
                    .comment("Path to the Python interpreter with uiautomator2 installed")
                    .applyToComponent {
                        text = pythonInterpreterPath // 初始回显
                        pathInputField = this        // 保存该组件的引用
                    }
            }
        }.apply {
            preferredSize = Dimension(800, 150)
        }
    }

    override fun doOKAction() {
        // 💡 修复问题1：不再依赖 Listener，直接在用户点击 OK 时读取界面上最新的值
        val currentSelectedPath = pathInputField.text

        if (currentSelectedPath.isEmpty()) {
            println("pythonInterpreterPath is empty")
            deviceAliasConfig.pythonInterpreterPath = ""
            super.doOKAction()
            return
        }

        if (!FileUtil.exist(currentSelectedPath)) {
            Messages.showErrorDialog(project, "Invalid python interpreter path!", "Error")
            return
        }

        if (FileUtil.isDirectory(currentSelectedPath)) {
            Messages.showErrorDialog(project, "Python interpreter path must be a file", "Error")
            return
        }

        println("Final pythonInterpreterPath: $currentSelectedPath")
        deviceAliasConfig.pythonInterpreterPath = currentSelectedPath
        super.doOKAction()
    }
}