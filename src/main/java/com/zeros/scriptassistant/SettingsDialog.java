package com.zeros.scriptassistant;

import cn.hutool.core.io.FileUtil;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class SettingsDialog extends DialogWrapper {
    private JPanel contentPane;

    // 1. 使用官方标准组合组件替换原有的 JTextField 和 JButton
    private TextFieldWithBrowseButton pythonInterpreterPathField;

    private final DeviceAliasConfig deviceAliasConfig;

    public SettingsDialog(Project project, DeviceAliasConfig deviceAliasConfig) {
        super(project, true);
        setTitle("Script Assistant Settings");
        this.deviceAliasConfig = deviceAliasConfig;
        setModal(true);

        // 2. 数据回显
        if(deviceAliasConfig != null){
            pythonInterpreterPathField.setText(deviceAliasConfig.getPythonInterpreterPath());
        }

        // 3. 构建选择器描述（注意：去掉了 setForcedToUseIdeaFileChooser，使用系统默认原生选择器更健壮）
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
                .withTitle("Choose Python Interpreter")
                .withDescription("Please choose a python interpreter")
                .withFileFilter(virtualFile -> virtualFile.getName().startsWith("python")
                        && (virtualFile.getExtension() == null || "exe".equalsIgnoreCase(virtualFile.getExtension())));

        // 4. 一键绑定监听器（内部自动处理按钮点击、图标加载、文件选择以及路径回填）
        pythonInterpreterPathField.addBrowseFolderListener(
                "Choose Python Interpreter",
                "Please choose a python interpreter",
                project,
                descriptor,
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
        );

        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        contentPane.setPreferredSize(new Dimension(800, 200));
        return contentPane;
    }

    @Override
    protected void doOKAction() {
        // 5. 获取路径时直接调用 getText()
        String path = pythonInterpreterPathField.getText();
        if (path.isEmpty()) {
            deviceAliasConfig.setPythonInterpreterPath("");
        } else {
            if(!FileUtil.exist(path)){
                Messages.showErrorDialog("Invalid python interpreter path!", "Error");
                return;
            }
            if(FileUtil.isDirectory(path)){
                Messages.showErrorDialog("Python interpreter path must be a file", "Error");
                return;
            }
            deviceAliasConfig.setPythonInterpreterPath(path);
        }
        super.doOKAction();
    }
}