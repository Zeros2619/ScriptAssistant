package com.zeros.scriptassistant;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ToolManager {
    private final Main main;
    private final Project project;
    private boolean saneCode;
    private Device currentDevice;
    private final List<Device> connectedDevices = new ArrayList<>();
    private final CodeGenerator codeGenerator;
    private final DeviceAliasConfig deviceAliasConfig;

    public ToolManager(Main main, Project project) {
        this.main = main;
        this.project = project;
        codeGenerator = new CodeGenerator(project);
        deviceAliasConfig = DeviceAliasConfig.getInstance(project);
    }

    public List<String> getDevices() {
        // 通过adb devices指令解析出连接的设备
        return ADBDeviceUtils.getConnectedDevices();
    }

    public boolean isConnected(String serial) {
        for (Device connectedDevice : connectedDevices) {
            if (connectedDevice.getSerial().equals(serial)) {
                return true;
            }
        }
        return false;
    }

    public void connectDevice(String serial) {
        if (serial == null || serial.trim().isEmpty()) {
            main.onDeviceConnected(false);
            return;
        }
        // 检查设备的连接状态
        if (!ADBDeviceUtils.isDeviceOnline(serial)) {
            main.onDeviceConnected(false);
            return;
        }

        main.setErrorPanel("", false);
        String pythonSdkPath = codeGenerator.getPythonSdkPath(deviceAliasConfig);
        System.out.println("pythonSdkPath=" + pythonSdkPath);
        Device connectingDevice = new Device(serial, deviceAliasConfig);
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(connectingDevice.init(pythonSdkPath,codeGenerator.getFilePath())){
                    connectingDevice.setAlias(connectedDevices.size());
                    main.setAlias(connectingDevice.getAlias());
                    connectedDevices.add(connectingDevice);
                    currentDevice = connectingDevice;
                    main.onDeviceConnected(true);
                    main.setErrorPanel("", false);
                } else {
                    main.onDeviceConnected(false);
                    main.setErrorPanel(connectingDevice.u2.getInitFailMsg(), true);
                    sendNotification("Error", connectingDevice.u2.getInitFailMsg(), NotificationType.ERROR);
                }
            }
        }).start();
    }

    public void sendNotification(String title, String message, NotificationType type) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("Script Assistant")  // 在 plugin.xml 中注册 group
                .createNotification(title, message, type)
                .notify(project);
    }

    public void disconnectDevice() {
        if (currentDevice != null) {
            main.setDisconnectUIState();
            currentDevice.close();
            connectedDevices.remove(currentDevice);
        }
    }

    public void loadCache(String serial) {
        for (Device connectedDevice : connectedDevices) {
            if (connectedDevice.getSerial().equals(serial)) {
                currentDevice = connectedDevice;
            }
        }
        if (currentDevice.screenshot == null) {
            return;
        }
        new Thread(() -> {
            main.setAlias(currentDevice.getAlias());
            main.paintRect(null);
            main.paintGreenRect(null);
            BufferedImage image = null;
            try {
                image = ImageIO.read(new ByteArrayInputStream(currentDevice.screenshot));
            } catch (Exception e) {
                e.printStackTrace();
            }
            DefaultMutableTreeNode hierarchy = currentDevice.treeNode;
            if (hierarchy == null) {
                return;
            }
            main.displayImage(image, currentDevice.allNodeRect);
            SwingUtilities.invokeLater(() -> {
                main.updateTree(hierarchy);
            });
        }).start();
    }

    public void updateScreen() {
        updateScreen(0);
    }

    public void updateScreen(int delay) {
        if (currentDevice == null) {
            return;
        }
        new Thread(() -> {
            main.setAllViewEnabled(false);
            if (delay > 0) {
                ThreadUtil.sleep(delay);
            }
            updateScreenSync();
            main.setAllViewEnabled(true);
        }).start();
    }

    public void updateScreenSync() {
        main.paintRect(null);
        main.paintGreenRect(null);
        CountDownLatch latch = new CountDownLatch(2);
        currentDevice.dumpUI();
        new Thread(() -> {
            try {
                byte[] screenshot = currentDevice.getScreenshot();
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(screenshot));
                //锁定控件后不再刷新，以优化操作体验
                if (main.isNodeUnlock()) {
                    main.displayImage(image, currentDevice.allNodeRect);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        }).start();
        new Thread(() -> {
            try {
                DefaultMutableTreeNode hierarchy = currentDevice.getHierarchy();
                //锁定控件后不再刷新，以优化操作体验
                if (main.isNodeUnlock()) {
                    SwingUtilities.invokeLater(() -> {
                        main.updateTree(hierarchy);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        }).start();
        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void updateDevicesShow() {
        List<String> devices = getDevices();
        main.updateDevicesComboBox(devices);
    }

    private DefaultMutableTreeNode target = null;

    public void setTarget(DefaultMutableTreeNode target) {
        this.target = target;
        updateCodeMatchTF();
    }

    public void updateSelectedNode(JTree tree, int imageX, int imageY) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        target = root;
        searchNode(root, imageX, imageY);
        System.out.println("target: " + target);
        if (target != null) {
            TreeNode[] nodes = ((DefaultTreeModel) tree.getModel()).getPathToRoot(target);  //有节点到根路径数组
            TreePath path = new TreePath(nodes);
            System.out.println(path);   //路径
            tree.setSelectionPath(new TreePath(nodes));
        }
    }

    public void updateCodeMatchTF(){
        if (target != null) {
            NodeInfo info = (NodeInfo) target.getUserObject();
            String code = codeGenerator.generateCode(currentDevice, info.node, false, false);
            main.setMatchCodeTF(codeGenerator.getCompletedCode(currentDevice.getAlias(), code));
        }
    }

    public void saneCode(boolean sane) {
        if (!sane) {
            saneCode = false;
            return;
        }
        saneCode = true;
    }

    public void generateSelectorCode() {
        if (target != null) {
            NodeInfo info = (NodeInfo) target.getUserObject();
            String code = codeGenerator.generateCode(currentDevice, info.node, saneCode, false);
            System.out.println(code);
            // 获取editor对象
            if (!saneCode) {
                code = codeGenerator.getCompletedCode(currentDevice.getAlias(), code);
            }
            codeGenerator.insert(code, saneCode);
        }
    }

    public void generateClickCode() {
        if (target != null) {
            NodeInfo info = (NodeInfo) target.getUserObject();
            String code = codeGenerator.generateCode(currentDevice, info.node);
            System.out.println(code);
            code = codeGenerator.clickCode(code);
            // 获取editor对象写入代码
            codeGenerator.insert(codeGenerator.getCompletedCode(currentDevice.getAlias(), code), true);
            execCode(codeGenerator.getCompletedCode(Device.OBJECT_NAME, code));
        }
    }

    public void generateSwipeCode(double startX, double startY, double endX, double endY, double duration) {
        String code;
        if (startX > 1) {
            code = codeGenerator.generateSwipeCode((int) startX, (int) startY, (int) endX, (int) endY, duration);
        } else {
            code = codeGenerator.generateSwipeCode(startX, startY, endX, endY, duration);
        }
        codeGenerator.insert(codeGenerator.getCompletedCode(currentDevice.getAlias(), code), true);
        execCode(codeGenerator.getCompletedCode(Device.OBJECT_NAME, code));
    }

    public void generatePercentClickCode(double percentX, double percentY) {
        String code = codeGenerator.generatePercentClickCode(percentX, percentY);
        codeGenerator.insert(codeGenerator.getCompletedCode(currentDevice.getAlias(), code), true);
        execCode(codeGenerator.getCompletedCode(Device.OBJECT_NAME, code));
    }

    public void generateCtrlClickCode(boolean useXpath) {
        if (target != null) {
            NodeInfo info = (NodeInfo) target.getUserObject();
            String code = codeGenerator.generateCode(currentDevice, info.node, false, useXpath);
            code = codeGenerator.clickCode(code);
            codeGenerator.insert(codeGenerator.getCompletedCode(currentDevice.getAlias(), code), true);
            execCode(codeGenerator.getCompletedCode(Device.OBJECT_NAME, code));
        }
    }

    public void generateCtrlSelectorCode(boolean useXpath) {
        if (target != null) {
            NodeInfo info = (NodeInfo) target.getUserObject();
            String code = codeGenerator.generateCode(currentDevice, info.node, saneCode, useXpath);
            if (!saneCode) {
                code = codeGenerator.getCompletedCode(currentDevice.getAlias(), code);
            }
            codeGenerator.insert(code, saneCode);
        }
    }

    public void generateCtrlSwipeCode(double startX, double startY, double endX, double endY, double duration) {
        String code;
        if (startX > 1) {
            code = codeGenerator.generateDragCode((int) startX, (int) startY, (int) endX, (int) endY, duration);
        } else {
            code = codeGenerator.generateDragCode(startX, startY, endX, endY, duration);
        }
        codeGenerator.insert(codeGenerator.getCompletedCode(currentDevice.getAlias(), code), true);
        execCode(codeGenerator.getCompletedCode(Device.OBJECT_NAME, code));
    }

    public void generateAddSelectorParamCode(String param, String value) {
        codeGenerator.insertSelectorParam(currentDevice.getAlias(), param, value);
    }

    private void execCode(String code) {
        main.setAllViewEnabled(false);
        final String[] actuallyExecCode = {code};
        // 创建并执行 SwingWorker
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                // 使用click_exists替代click，避免点击不存在的控件报错和耗时过长
                if (actuallyExecCode[0].endsWith(".click()")){
                    actuallyExecCode[0] = actuallyExecCode[0].replace(".click()", ".click_exists()");
                }
                // 在后台线程中执行耗时的 Python 代码
                System.out.println(actuallyExecCode[0]);
                return currentDevice.u2.executeCode(actuallyExecCode[0]);
            }

            @Override
            protected void done() {
                try {
                    // 获取执行结果（如果需要）
                    String result = get();
                    // 在 EDT 中更新 UI
                    updateScreen(1000);
                } catch (Exception e) {
                    // 处理异常情况
                    e.printStackTrace();
                    main.setErrorPanel("exec code error: " + e.getMessage(), true);
                } finally {
                    // 确保按钮状态被恢复
                    main.setAllViewEnabled(true);
                }
            }
        }.execute(); // 启动 SwingWorker
    }

    private void searchNode(DefaultMutableTreeNode root, int imageX, int imageY) {
        //查找鼠标点击位置对应的控件
        NodeInfo nodeInfo = (NodeInfo) root.getUserObject();
        if (nodeInfo.isInBounds(imageX, imageY)) {
            NodeInfo targetInfo = (NodeInfo) target.getUserObject();
            if (targetInfo.area >= nodeInfo.area) {
                target = root;
            }
            int childCount = root.getChildCount();
            for (int i = 0; i < childCount; i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChildAt(i);
                searchNode(child, imageX, imageY);
            }
        }
    }

    public void setCurrentDeviceAlias(String alias) {
        if (currentDevice == null) {
            return;
        }
        currentDevice.setAlias(alias);
    }

    public void matchNodeByCode() {
        String code = main.getMatchCodeTFText();
        if (StrUtil.isBlank(code)) {
            main.setErrorPanel("can not find target or invalid code", true);
            return;
        }
        // 替换别名， 加上括号防止替换到其他代码
        code = code.replace(currentDevice.getAlias() + "(", Device.OBJECT_NAME + "(");
        code = code.replace(currentDevice.getAlias() + ".xpath(", Device.OBJECT_NAME + ".xpath(");
        main.setMatchBtnEnabled(false);
        // 执行匹配代码，获取控件位置
        Rectangle nodeBounds = currentDevice.u2.getNodeBounds(code);
        main.setMatchBtnEnabled(true);
        main.setMatchCodeTFColor(nodeBounds != null);
        if (nodeBounds == null) {
            main.setErrorPanel("can not find target or invalid code", true);
            // 手机界面已更新了，工具界面还没更新，会困惑为什么找不到控件
            updateScreen();
            return;
        }
        System.out.println(nodeBounds);
        NodeInfo target1 = new NodeInfo("target");
        target1.bounds = nodeBounds;
        target1.area = nodeBounds.width * nodeBounds.height;
        main.paintGreenRect(target1);
    }

    public void showSettingsDialog() {
        SettingsDialog dialog = new SettingsDialog(project, deviceAliasConfig);
        dialog.show();
    }
}
