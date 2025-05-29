package com.zeros.scriptassistant;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XPathLite {
    public static final String[] props = {"resource-id", "text", "content-desc", "class"};

    public static void main(String[] args) {
        String xmlFilePath = "ui_xml.xml";
        try {
            // 解析 XML 文件
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(xmlFilePath);
            NodeList node = document.getDocumentElement().getElementsByTagName("node");

            Map<String, Map<String, Integer>> attributeMap = new HashMap<>();
            for (int i = 0; i < node.getLength(); i++) {
                Node item = node.item(i);
                for (String prop : props) {
                    String nodeValue = item.getAttributes().getNamedItem(prop).getNodeValue();
                    if (nodeValue != null && !nodeValue.isEmpty()) {
                        Map<String, Integer> propMap = attributeMap.get(prop);
                        if (propMap == null) {
                            propMap = new HashMap<>();
                            attributeMap.put(prop, propMap);
                        }
                        Integer count = propMap.get(nodeValue);
                        if (count == null) {
                            count = 0;
                        }
                        propMap.put(nodeValue, count + 1);
                    }
                }
            }

            // 示例：用户指定的列表索引
            int specifiedIndex = 10;
            Node selectedNode = node.item(specifiedIndex);
            String xpath = getXPath(attributeMap, selectedNode);
            System.out.println(xpath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int getAttrCount(Map<String, Map<String, Integer>> map, String prop, String value) {
        Map<String, Integer> propMap = map.get(prop);
        if (propMap == null) {
            return 0;
        }
        Integer count = propMap.get(value);
        if (count == null) {
            return 0;
        }
        return count;
    }

    private static int getAttrCount(Map<String, Map<String, Integer>> map, String prop, Node node) {
        return getAttrCount(map, prop, node.getAttributes().getNamedItem(prop).getNodeValue());
    }


    public static String getXPath(Map<String, Map<String, Integer>> map, Node selectedNode) {
        List<String> array = new ArrayList<>();
        while (true) {
            Node parentNode = selectedNode.getParentNode();
            if (getAttrCount(map, "resource-id", selectedNode) == 1) {
                array.add("*[@resource-id=\"" + selectedNode.getAttributes().getNamedItem("resource-id").getNodeValue() + "\"]");
                break;
            } else if (getAttrCount(map, "text", selectedNode) == 1) {
                array.add("*[@text=\"" + selectedNode.getAttributes().getNamedItem("text").getNodeValue() + "\"]");
                break;
            } else if (getAttrCount(map, "content-desc", selectedNode) == 1) {
                array.add("*[@content-desc=\"" + selectedNode.getAttributes().getNamedItem("content-desc").getNodeValue() + "\"]");
                break;
            } else {
                int index = 0;
                if (parentNode == null) {
                    break;
                }
                String type = selectedNode.getAttributes().getNamedItem("class").getNodeValue();
                for (int i = 0; i < parentNode.getChildNodes().getLength(); i++) {
                    Node child = parentNode.getChildNodes().item(i);
                    if (child.getNodeType()!= Node.ELEMENT_NODE) {
                        continue;
                    }
                    String childType = child.getAttributes().getNamedItem("class").getNodeValue();
                    if (childType.equals(type)) {
                        index++;
                    }
                    if (child == selectedNode) {
                        break;
                    }
                }
                array.add(selectedNode.getAttributes().getNamedItem("class").getNodeValue() + "[" + index + "]");
            }
            selectedNode = parentNode;
        }
        StringBuilder builder = new StringBuilder("//");
        for (int i = array.size() - 1; i >= 0; i--) {
            builder.append(array.get(i));
            if(i > 0){
                builder.append("/");
            }
        }
        return builder.toString();
    }
}
