package com.zeros.scriptassistant;

import cn.hutool.core.io.FileUtil;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class SettingsDialog extends DialogWrapper {
    private JPanel contentPane;
    private JTextField pythonInterpreterPathTF;
    private JButton selectorButton;
    private final DeviceAliasConfig deviceAliasConfig;

    @Override
    protected @Nullable JComponent createCenterPanel() {
        contentPane.setPreferredSize(new Dimension(800, 200));
        return contentPane;
    }

    public SettingsDialog(Project project, DeviceAliasConfig deviceAliasConfig) {
        super(project, true);
        setTitle("Script Assistant Settings");
        this.deviceAliasConfig = deviceAliasConfig;
        setModal(true);
        if(deviceAliasConfig != null){
            pythonInterpreterPathTF.setText(deviceAliasConfig.getPythonInterpreterPath());
        }

        // 文件选择器
        selectorButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor();
                descriptor.setTitle("Choose Python Interpreter");
                descriptor.setDescription("Please choose a python interpreter");
                VirtualFile preselectFile = LocalFileSystem.getInstance().findFileByPath(pythonInterpreterPathTF.getText());

                FileChooser.chooseFiles(descriptor, project, preselectFile, selectedFiles -> {
                    if (!selectedFiles.isEmpty()) {
                        VirtualFile selected = selectedFiles.get(0);
                        pythonInterpreterPathTF.setText(selected.getPath());
                    }
                });
            }
        });
        init();
    }

    @Override
    protected void doOKAction() {
        String path = pythonInterpreterPathTF.getText();
        if (path.isEmpty()) {
            deviceAliasConfig.setPythonInterpreterPath("");
        }else{
            if(!FileUtil.exist(path)){
                Messages.showErrorDialog("Invalid python interpreter path!", "Error");
                return;
            }
            deviceAliasConfig.setPythonInterpreterPath(path);
        }
        super.doOKAction();
    }
}
