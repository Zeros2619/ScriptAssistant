package com.zeros.scriptassistant;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.*;

public class Device {
    public String serial;
    public U2 u2;
    public String dumpSaveDir;
    public byte[] screenshot;
    public String hierarchy;
    public Document hierarchyDoc;
    public DefaultMutableTreeNode treeNode;
    public Set<Rectangle> allNodeRect = new HashSet<>();
    private Map<String, Map<String, Integer>> attributeMap;
    private static long lastScreenshotModifyTime;
    private static long lastHierarchyModifyTime;
    private File screenShotSaveFile;
    private File hierarchySaveFile;
    private String alias = "d";

    public Device(String serial) {
        this.serial = serial;
        u2 = new U2();
    }

    public boolean init(String pythonPath, String saveDir) {
        dumpSaveDir = saveDir;
        screenShotSaveFile = new File(dumpSaveDir, "screen.png");
        hierarchySaveFile = new File(dumpSaveDir, "hierarchy.xml");
        return u2.init(pythonPath, serial);
    }

    public String getSerial() {
        return serial;
    }

    public byte[] getScreenshot() {
        for (int i = 0; i < 50; i++) {
            ThreadUtil.sleep(100);
            Date date = FileUtil.lastModifiedTime(screenShotSaveFile);
            if (date != null && date.getTime() != lastScreenshotModifyTime) {
                lastScreenshotModifyTime = date.getTime();
                break;
            }
        }
        screenshot = FileUtil.readBytes(screenShotSaveFile);
        return screenshot;
    }

    public DefaultMutableTreeNode getHierarchy() {
        for (int i = 0; i < 50; i++) {
            ThreadUtil.sleep(100);
            Date date = FileUtil.lastModifiedTime(hierarchySaveFile);
            if (date != null && date.getTime() != lastHierarchyModifyTime) {
                lastHierarchyModifyTime = date.getTime();
                break;
            }
        }
        hierarchy = FileUtil.readUtf8String(hierarchySaveFile);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            InputStream is = new ByteArrayInputStream(hierarchy.getBytes("UTF-8"));
            Document doc = dBuilder.parse(is);
            doc.getDocumentElement().normalize();
            hierarchyDoc = doc;
            NodeList nodeList = doc.getDocumentElement().getChildNodes();
            treeNode = new DefaultMutableTreeNode(new NodeInfo(doc.getDocumentElement().getNodeName()));
            addNodes(nodeList, treeNode);
            allNodeRect.clear();
            obtainAllNodeRect(treeNode);
            obtainAttributeMap();
            return treeNode;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void addNodes(NodeList nList, DefaultMutableTreeNode parent) {
        for (int i = 0; i < nList.getLength(); i++) {
            Node node = nList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                DefaultMutableTreeNode child = new DefaultMutableTreeNode(new NodeInfo(node));
                parent.add(child);
                // Recursive call for the current node's children
                if (node.hasChildNodes()) {
                    addNodes(node.getChildNodes(), child);
                }
            }
        }
    }

    public void obtainAllNodeRect(DefaultMutableTreeNode root) {
        NodeInfo nodeInfo = (NodeInfo) root.getUserObject();
        allNodeRect.add(nodeInfo.bounds);
        int childCount = root.getChildCount();
        for (int i = 0; i < childCount; i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChildAt(i);
            obtainAllNodeRect(child);
        }
    }

    public void obtainAttributeMap() {
        NodeList node = hierarchyDoc.getDocumentElement().getElementsByTagName("node");
        attributeMap = new HashMap<>();
        for (int i = 0; i < node.getLength(); i++) {
            Node item = node.item(i);
            for (String prop : XPathLite.props) {
                String nodeValue = item.getAttributes().getNamedItem(prop).getNodeValue();
                if (nodeValue != null && !nodeValue.isEmpty()) {
                    Map<String, Integer> propMap = attributeMap.computeIfAbsent(prop, k -> new HashMap<>());
                    Integer count = propMap.get(nodeValue);
                    if (count == null) {
                        count = 0;
                    }
                    propMap.put(nodeValue, count + 1);
                }
            }
        }
    }

    public String generateSwipeCode(int startX, int startY, int endX, int endY, double duration) {
//        return alias + ".u2.swipe(" + startX + ", " + startY + ", " + endX + ", " + endY + ", " + duration + ")";
        return alias + ".swipe(" + startX + ", " + startY + ", " + endX + ", " + endY + ", " + duration + ")";
    }

    public String generateSwipeCode(double startX, double startY, double endX, double endY, double duration) {
//        return alias + ".u2.swipe(" + startX + ", " + startY + ", " + endX + ", " + endY + ", " + duration + ")";
        return alias + ".swipe(" + startX + ", " + startY + ", " + endX + ", " + endY + ", " + duration + ")";
    }

    public String generateCode(Node node) {
        return generateCode(node, false);
    }

    public String generateCode(Node node, boolean onlySelector) {
        String selector = NodeLocator.getAttributeCombination(hierarchyDoc.getDocumentElement().getElementsByTagName("node"), node);
        if (selector == null) {
            selector = XPathLite.getXPath(attributeMap, node);
            if (onlySelector) {
                return "'" + selector + "'";
            }
//            return alias + ".u2.xpath('" + selector + "')";
            return alias + ".xpath('" + selector + "')";
        } else {
            if (onlySelector) {
                return "Selector(" + selector + ")";
            }
//            return alias + ".u2(Selector(" + selector + "))";
            return alias + "(" + selector + ")";
        }
    }

    public String clickCode(String code) {
        return code + ".click()";
    }

    public void dumpUI() {
        u2.dumpUI(screenShotSaveFile, hierarchySaveFile);
    }

    public void close() {
        u2.close();
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public void setAlias(int index) {
        if (!alias.equals("d")) {
            return;
        }
        if (index > 0) {
            alias = "d" + (index + 1);
        } else {
            alias = "d";
        }
    }

    public String getAlias() {
        return alias;
    }
}
