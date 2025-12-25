package com.zeros.scriptassistant;

import cn.hutool.core.util.NumberUtil;
import com.intellij.openapi.util.IconLoader;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class Main {
    private JPanel root;
    private JPanel top;
    private JButton connectBtn;
    private JPanel viewPanel;
    private ImagePanel imagePanel;
    private JSplitPane mainSplit;
    private JSplitPane infoSplit;
    private JScrollPane treePanel;
    private JScrollPane valuePanel;
    private JTree nodeTree;
    private JTable nodeInfoTable;
    private DefaultTableModel tableModel;
    private JLabel mousePositionLabel;
    private JComboBox<String> devicesCombo;
    private JLabel reloadBtn;
    private JButton dumpButton;
    private JCheckBox saneCheckBox;
    private JPanel errorInfoPanel;
    private JLabel infoLabel;
    private JTextField aliasTF;
    private JPanel bottom;
    private JTextField matchCodeTF;
    private JButton matchBtn;
    private JLabel settingsBtn;
    private ToolManager manager;
    private final ImagePanel.ImagePanelListener imagePanelListener = new ImagePanel.ImagePanelListener() {
        @Override
        public void onMousePositionChange(int imageX, int imageY) {
            mousePositionLabel.setText("Mouse Position: (" + imageX + ", " + imageY + ")");
        }

        @Override
        public void onNodeSelected(int imageX, int imageY) {
            manager.updateSelectedNode(nodeTree, imageX, imageY);
        }

        @Override
        public void onLeftDoubleClicked(int imageX, int imageY, boolean isCtrlPressed) {
            if (isCtrlPressed) {
                // Ctrl 键按下时，强制使用 XPath 生成代码
                manager.generateCtrlClickCode(true);
            } else {
                manager.generateClickCode();
            }
        }

        @Override
        public void onLeftClicked(int imageX, int imageY, boolean isCtrlPressed) {
            if (isCtrlPressed) {
                double percentX = NumberUtil.div(imageX, imagePanel.getImage().getWidth(), 2);
                double percentY = NumberUtil.div(imageY, imagePanel.getImage().getHeight(), 2);
                // Ctrl 键按下时，生成百分比坐标点击
                manager.generatePercentClickCode(percentX, percentY);
            }
        }

        @Override
        public void onRightClicked(int imageX, int imageY, boolean isCtrlPressed) {
            if (isCtrlPressed) {
                // Ctrl 键按下时，强制使用 XPath 生成代码
                manager.generateCtrlSelectorCode(true);
            } else {
                manager.generateSelectorCode();
            }
        }

        @Override
        public void onMouseWheel() {
            imagePanel.paintRect(null);
            imagePanel.paintGreenRect(null);
            // 设置为未选取
            nodeTree.setSelectionPath(null);
        }

        @Override
        public void onSwipe(double startX, double startY, double endX, double endY, double duration, boolean isCtrlPressed) {
            if (isCtrlPressed) {
                // Ctrl 键按下时，强制使用 XPath 生成代码
                manager.generateCtrlSwipeCode(startX, startY, endX, endY, duration);
            } else {
                manager.generateSwipeCode(startX, startY, endX, endY, duration);
            }
        }
    };

    public JPanel getRoot(ToolManager manager) {
        this.manager = manager;
        Icon icon = IconLoader.getIcon("/reload.svg", Main.class);
        reloadBtn.setIcon(icon);
        Icon settingsIcon = IconLoader.getIcon("/settings.svg", Main.class);
        settingsBtn.setIcon(settingsIcon);
        imagePanel = new ImagePanel();
        imagePanel.setListener(imagePanelListener);
        viewPanel.add(imagePanel);

        manager.updateDevicesShow();
        reloadBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                manager.updateDevicesShow();
            }
        });
        settingsBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                manager.showSettingsDialog();
            }
        });
        dumpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                manager.updateScreen();
            }
        });
        saneCheckBox.setEnabled(true);
        saneCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                manager.saneCode(saneCheckBox.isSelected());
            }
        });

        connectBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connectBtn.setEnabled(false);
                String statusText = connectBtn.getText();
                if (statusText.equals("connect")) {
                    String item = (String) devicesCombo.getSelectedItem();
                    boolean connected = manager.connectDevice(item);
                    if (connected) {
                        setConnectedUIState();
                        manager.updateScreen();
                    }
                } else {
                    manager.disconnectDevice();
                    manager.updateDevicesShow();
                }
                connectBtn.setEnabled(true);
            }
        });

        devicesCombo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    // 选中不同设备时刷新连接状态和加载界面
                    String serial = (String) e.getItem();
                    boolean connected = manager.isConnected(serial);
                    if (connected) {
                        setConnectedUIState();
                        // 加载界面缓存
                        manager.loadCache(serial);
                    } else {
                        setDisconnectUIState();
                    }
                }
            }
        });

        aliasTF.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                manager.setCurrentDeviceAlias(aliasTF.getText().trim());
            }
        });
        // 输入回车时失去焦点
        aliasTF.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    e.consume(); // 阻止默认行为
                    aliasTF.transferFocus(); // 失去焦点
                }
            }
        });

        mainSplit.setDividerLocation(300);
        infoSplit.setDividerLocation(500);

        tableModel = new DefaultTableModel(new String[][]{}, new String[]{"property", "value"});
        nodeInfoTable.setModel(tableModel);
        nodeInfoTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && nodeInfoTable.getSelectedRow() != -1) {
                int row = nodeInfoTable.getSelectedRow();
                String v = (String) tableModel.getValueAt(row, 1);
                String text = "\"" + v + "\"";
                Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new StringSelection(text), null);
            }
        });
        nodeInfoTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    // Get the row at the mouse click point
                    int row = nodeInfoTable.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < nodeInfoTable.getRowCount()) {
                        // Select the row
                        nodeInfoTable.setRowSelectionInterval(row, row);
                        String p = (String) tableModel.getValueAt(row, 0);
                        String v = (String) tableModel.getValueAt(row, 1);
                        manager.generateAddSelectorParamCode(p, v);
                    }
                }
            }
        });

        matchCodeTF.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                super.focusGained(e);
                setMatchCodeTFColor(true);
            }
        });
        matchBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                manager.matchNodeByCode();
            }
        });
        updateDeviceInfoTable(null);
        return root;
    }

    public void setErrorPanel(String msg, boolean show) {
        setErrorPanel(msg, show, 3000);
    }

    public void setErrorPanel(String msg, boolean show, int timeout) {
        errorInfoPanel.setVisible(show);
        infoLabel.setText(msg);
        if (timeout > 0) {
            Timer timer = new Timer(timeout, e -> dismissErrorPanel());
            timer.setRepeats(false);
            timer.start();
        }
    }

    public void dismissErrorPanel() {
        errorInfoPanel.setVisible(false);
        infoLabel.setText("");
    }

    public void setDumpButtonEnable(boolean enable) {
        if (enable) {
            imagePanel.setListener(imagePanelListener);
        }else{
            imagePanel.setListener(null);
        }
        dumpButton.setEnabled(enable);
    }

    public void setAllViewEnabled(boolean enable) {
        setDumpButtonEnable(enable);
        aliasTF.setEnabled(enable);
        devicesCombo.setEnabled(enable);
        connectBtn.setEnabled(enable);
        matchCodeTF.setEnabled(enable);
        matchBtn.setEnabled(enable);
    }

    public void setConnectedUIState() {
        aliasTF.setVisible(true);
        connectBtn.setSelected(false);
        connectBtn.setText("disconnect");
//        dumpButton.setEnabled(true);
    }

    public void setDisconnectUIState() {
        aliasTF.setVisible(false);
        connectBtn.setSelected(false);
        connectBtn.setText("connect");
        dumpButton.setEnabled(false);
    }

    public void updateDevicesComboBox(List<String> devices) {
        System.out.println("devices:" + devices);
        devicesCombo.removeAllItems();
        for (String device : devices) {
            devicesCombo.addItem(device);
        }
    }

    public void addDeviceToComboBox(String serial) {
        devicesCombo.addItem(serial);
    }

    public void removeDeviceFormComBox(String serial) {
        int itemCount = devicesCombo.getItemCount();
        for (int i = 0; i < itemCount; i++) {
            if (serial.equals(devicesCombo.getItemAt(i))) {
                devicesCombo.removeItemAt(i);
            }
        }
    }

    public void createWindow() {
        JFrame frame = new JFrame("ScriptCopilot");

        frame.setContentPane(getRoot(new ToolManager(this, null)));
        // 更改关闭操作
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
//        frame.setResizable(false);
        frame.setSize(1000, 600);
        // 获取屏幕尺寸
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        // 计算窗口居中时的坐标
        int x = (screenSize.width - frame.getSize().width) / 2;
        int y = (screenSize.height - frame.getSize().height) / 2;
        // 设置窗口位置
        frame.setLocation(x, y);
        frame.setVisible(true);
    }

    private void updateDeviceInfoTable(NodeInfo nodeInfo) {
        // Update table data
        String[][] data = (nodeInfo != null) ? nodeInfo.getTableArray() : new String[][]{};

        // Update existing model instead of creating a new one
        tableModel.setDataVector(data, new String[]{"property", "value"});
        resizeColumnWidth(nodeInfoTable);
    }

    private void resizeColumnWidth(JTable table) {
        final TableColumnModel columnModel = table.getColumnModel();
        for (int column = 0; column < table.getColumnCount(); column++) {
            int width = 50; // Min width
            for (int row = 0; row < table.getRowCount(); row++) {
                TableCellRenderer renderer = table.getCellRenderer(row, column);
                Component comp = table.prepareRenderer(renderer, row, column);
                width = Math.max(comp.getPreferredSize().width + 10, width);
            }
            columnModel.getColumn(column).setPreferredWidth(width);
        }
    }

    public JTree getNodeTree() {
        try {
            DefaultMutableTreeNode root = new DefaultMutableTreeNode("");
            return new JTree(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updateTree(DefaultMutableTreeNode treeNode) {
        if (treeNode == null) {
            return;
        }
        if (nodeTree == null) {
            DefaultMutableTreeNode root = new DefaultMutableTreeNode("");
            nodeTree = new JTree(root);
            nodeTree.addTreeSelectionListener(new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent e) {
                    DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) nodeTree.getLastSelectedPathComponent();
                    if (selectedNode == null)
                        return;
                    NodeInfo nodeInfo = (NodeInfo) selectedNode.getUserObject();
                    // 控件节点被选中时，更新属性表和绘制控件矩形框
                    updateDeviceInfoTable(nodeInfo);
                    manager.setTarget(selectedNode);
                    imagePanel.paintRect(nodeInfo);
                    imagePanel.paintGreenRect(null);
                }
            });
            DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
            renderer.setLeafIcon(null);
            renderer.setClosedIcon(null);
            renderer.setOpenIcon(null);
            nodeTree.setCellRenderer(renderer);
            treePanel.getViewport().add(nodeTree);
            nodeTree.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        int row = nodeTree.getClosestRowForLocation(e.getX(), e.getY());
                        nodeTree.setSelectionRow(row);
                        manager.generateSelectorCode();
                    }
                }
            });
        }
        DefaultTreeModel model = new DefaultTreeModel(treeNode);
        nodeTree.setModel(model);
        model.reload();
        paintRect(null);
        paintGreenRect(null);
    }

    public void displayImage(BufferedImage img, Set<Rectangle> allNodeRect) {
        imagePanel.setAllNodeRect(allNodeRect);
        imagePanel.setImage(img);
    }

    public void paintRect(NodeInfo nodeInfo) {
        imagePanel.paintRect(nodeInfo);
    }

    public void paintGreenRect(NodeInfo nodeInfo) {
        imagePanel.paintGreenRect(nodeInfo);
    }

    public String getMatchCodeTFText() {
        return matchCodeTF.getText();
    }

    public void setMatchCodeTF(String code) {
        setMatchCodeTFColor(true);
        matchCodeTF.setText(code);
    }

    public void setMatchCodeTFColor(boolean status) {
        if (!status) {
            matchCodeTF.setForeground(Color.RED);
        } else {
            matchCodeTF.setForeground(Color.WHITE);
        }
    }

    public void setMatchBtnEnabled(boolean status) {
        matchBtn.setEnabled(status);
    }

    public void displayImage(File file) {
        try {
            BufferedImage img = ImageIO.read(file);
            imagePanel.setImage(img);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isNodeUnlock() {
        return imagePanel.getRect() == null;
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Main()::createWindow);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }

    public void setAlias(String alias) {
        aliasTF.setText(alias);
    }
}
