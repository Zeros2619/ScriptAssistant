package com.zeros.scriptassistant;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import org.jetbrains.annotations.NotNull;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

public class MainToolWindow implements ToolWindowFactory, DumbAware {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        Main main = new Main();
        ToolManager toolManager = new ToolManager(main, project);
//        Content content = ContentFactory.getInstance().createContent(main.getRoot(toolManager), "", false);
        Content content = ContentFactory.SERVICE.getInstance().createContent(main.getRoot(toolManager), "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
