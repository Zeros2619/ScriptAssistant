package com.zeros.scriptassistant

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class MyToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolUI = ToolUI(project)
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(toolUI.contentPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }

}