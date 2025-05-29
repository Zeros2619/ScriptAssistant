package com.zeros.scriptassistant;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;

public class CodeGenerator {

    private final Project project;

    public CodeGenerator(Project project) {
        this.project = project;
    }

    public void insert(String code, boolean endEnter) {
        // 获取当前编辑器
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor != null) {
            // 获取文档对象，以便修改文件内容
            Document document = editor.getDocument();

            // 获取光标所在的位置
            int offset = editor.getCaretModel().getOffset();

            final String line = code + (endEnter ? "\n" : "");
            // 在写操作中执行文本插入
            WriteCommandAction.runWriteCommandAction(project, () -> {
                document.insertString(offset, line);
            });

            // 移动光标到新插入的代码后面
            editor.getCaretModel().moveToOffset(offset + line.length());
        }
    }

    public String getPythonSdkPath() {
        // 获取当前项目的SDK
        Sdk projectSdk = ProjectRootManager.getInstance(project).getProjectSdk();

        // 检查是否有SDK
        if (projectSdk != null) {
            // 获取解释器路径
            return projectSdk.getHomePath();
        }
        return null; // 如果没有关联的SDK，则返回null
    }

    public String getFilePath() {
        return PathManager.getPluginsPath();
    }

}

