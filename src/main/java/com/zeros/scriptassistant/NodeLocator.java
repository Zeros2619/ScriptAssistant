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

public class NodeLocator {
    private static final String[] props = {"resource-id", "text", "content-desc", "class"};
    private static final Map<String, String> propMap = new HashMap<>();
    static {
        propMap.put("resource-id", "resourceId");
        propMap.put("text", "text");
        propMap.put("content-desc", "description");
        propMap.put("class", "className");
    }

    public static void main(String[] args) {
        String xmlFilePath = "ui_xml.xml";
        try {
            // 解析 XML 文件
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(xmlFilePath);
            NodeList node = document.getDocumentElement().getElementsByTagName("node");
//            for (int i = 0; i < node.getLength(); i++) {
//                System.out.println(node.item(i).getAttributes().item(0).getNodeValue());
//            }

            // 示例：用户指定的列表索引
            int specifiedIndex = 30;
            Node node1 = node.item(specifiedIndex);
            // 获取指定索引位置的控件的最少属性组合
            String attributeCombination = getAttributeCombination(node, node1);

            // 输出结果
            if (attributeCombination != null) {
                System.out.println("Selector(" + attributeCombination + ")");
            } else {
                System.out.println("不能唯一定位");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getAttributeCombination(NodeList nodeList, Node node) {
        StringBuilder attributeCombination = new StringBuilder();
        List<String> propsResult = new ArrayList<>();
        if (node.hasAttributes()) {
            for (String prop : props) {
                String value = node.getAttributes().getNamedItem(prop).getNodeValue();
                if (value == null || value.isEmpty()) {
                    // 如果当前属性为空，则跳过
                    continue;
                }
                // 判断当前属性是否能唯一定位, 不唯一则加上下一个属性，组合判断
                propsResult.add(prop);
                if (!findNode(nodeList, node, propsResult)) {
                    // 可以则查找完成
                    break;
                }else{
                    // 不唯一则去掉当前属性
                    propsResult.remove(prop);
                }
            }
        }
        if (propsResult.isEmpty()) {
            return null;
        }
        for (String prop : propsResult) {
            // 对属性值进行转义
            String value = NodeInfo.toEscapedString(node.getAttributes().getNamedItem(prop).getNodeValue());
            attributeCombination.append(propMap.get(prop)).append("=\"").append(value).append("\",");
        }
        // 结尾去掉最后一个逗号
        attributeCombination.deleteCharAt(attributeCombination.length() - 1);
        return attributeCombination.toString().trim();
    }

    public static boolean findNode(NodeList nodeList, Node node, List<String> props) {
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node n = nodeList.item(i);
            if (n == node) {
                continue;
            }
            boolean find = true;
            for (String prop : props) {
                String v1 = node.getAttributes().getNamedItem(prop).getNodeValue();
                String v2 = n.getAttributes().getNamedItem(prop).getNodeValue();
                if (!v1.equals(v2)) {
                    find = false;
                    break;
                }
            }
            if (find) {
                return true;
            }
        }
        return false;
    }
}