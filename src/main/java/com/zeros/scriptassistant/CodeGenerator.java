package com.zeros.scriptassistant;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class CodeGenerator {

    private final Project project;

    public CodeGenerator(Project project) {
        this.project = project;
    }

    public String getCompletedCode(String objectName, String code) {
        return objectName + code;
    }

    public String generateSwipeCode(int startX, int startY, int endX, int endY, double duration) {
        return ".swipe(" + startX + ", " + startY + ", " + endX + ", " + endY + ", " + duration + ")";
    }

    public String generateSwipeCode(double startX, double startY, double endX, double endY, double duration) {
        return ".swipe(" + startX + ", " + startY + ", " + endX + ", " + endY + ", " + duration + ")";
    }

    public String generateDragCode(double startX, double startY, double endX, double endY, double duration) {
        return ".drag(" + startX + ", " + startY + ", " + endX + ", " + endY + ", " + duration + ")";
    }

    public String generateDragCode(int startX, int startY, int endX, int endY, double duration) {
        return ".drag(" + startX + ", " + startY + ", " + endX + ", " + endY + ", " + duration + ")";
    }

    public String generateCode(Device device, Node node) {
        return generateCode(device, node, false, false);
    }

    public String generatePercentClickCode(double percentX, double percentY) {
        return ".click(" + percentX + ", " + percentY + ")";
    }

    public String generateCode(Device device, Node node, boolean onlySelector, boolean xpath) {
        NodeList nodeList = device.hierarchyDoc.getDocumentElement().getElementsByTagName("node");
        String selector = NodeLocator.getAttributeCombination(nodeList, node);
        if (selector == null || xpath) {
            selector = XPathLite.getXPath(device.attributeMap, node);
            if (onlySelector) {
                return "'" + selector + "'";
            }
            return ".xpath('" + selector + "')";
        } else {
            if (onlySelector) {
                return "Selector(" + selector + ")";
            }
            return "(" + selector + ")";
        }
    }

    public String clickCode(String code) {
        return code + ".click()";
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
            WriteCommandAction.runWriteCommandAction(project, () -> document.insertString(offset, line));

            if (endEnter) {
                // 获取当前行号
                int lineNumber = document.getLineNumber(offset);
                // 获取当前行的起始偏移量
                int lineStartOffset = document.getLineStartOffset(lineNumber);
                // 获取当前行的缩进字符串
                CharSequence lineText = document.getCharsSequence().subSequence(lineStartOffset, offset);
                int indentLength = 0;
                while (indentLength < lineText.length() && Character.isWhitespace(lineText.charAt(indentLength))) {
                    indentLength++;
                }
                String indent = lineText.subSequence(0, indentLength).toString();

                // 在新行插入缩进
                int newOffset = offset + line.length();
                WriteCommandAction.runWriteCommandAction(project, () -> document.insertString(newOffset, indent));
                // 移动光标到新插入的缩进后面
                editor.getCaretModel().moveToOffset(newOffset + indent.length());
            } else {
                // 移动光标到新插入的代码后面
                editor.getCaretModel().moveToOffset(offset + line.length());
            }
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

