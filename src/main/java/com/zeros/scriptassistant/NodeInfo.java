package com.zeros.scriptassistant;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class NodeInfo {
    public String id;
    public String text;
    public String desc;
    public String Class;
    public String Package;
    public String checkable;
    public String checked;
    public String clickable;
    public String enabled;
    public String focusable;
    public String focused;
    public String scrollable;
    public String longClickable;
    public String password;
    public String selected;
    public String visible;
    public String boundsStr;
    public Rectangle bounds;

    public Node node;
    public int area;
    public NodeInfo(String name) {
        Class = name;
        bounds = new Rectangle(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
        area = Integer.MAX_VALUE;
    }
    public NodeInfo(Node node) {
        this.node = node;
        Element element = (Element) node;
        id = element.getAttribute("resource-id");
        text = element.getAttribute("text");
        desc = element.getAttribute("content-desc");
        Class = element.getAttribute("class");
        Package = element.getAttribute("package");
        checkable = element.getAttribute("checkable");
        checked = element.getAttribute("checked");
        clickable = element.getAttribute("clickable");
        enabled = element.getAttribute("enabled");
        focusable = element.getAttribute("focusable");
        focused = element.getAttribute("focused");
        scrollable = element.getAttribute("scrollable");
        longClickable = element.getAttribute("long-clickable");
        password = element.getAttribute("password");
        selected = element.getAttribute("selected");
        visible = element.getAttribute("visible-to-user");
        boundsStr = element.getAttribute("bounds");
        List<String> allGroups = ReUtil.findAllGroup0(Pattern.compile("\\d+"), boundsStr);
        if(allGroups.size() == 4){
            int left = NumberUtil.parseInt(allGroups.get(0));
            int top = NumberUtil.parseInt(allGroups.get(1));
            int right = NumberUtil.parseInt(allGroups.get(2));
            int bottom = NumberUtil.parseInt(allGroups.get(3));
            bounds = new Rectangle(left, top, right-left, bottom-top);
            area = bounds.width * bounds.height;
        }else{
            bounds = new Rectangle();
        }
    }

    public String[][] getTableArray(){
        String[][] result = new String[17][2];
        result[0][0] = "resourceId";
        result[1][0] = "text";
        result[2][0] = "description";
        result[3][0] = "className";
        result[4][0] = "packageName";
        result[5][0] = "checkable";
        result[6][0] = "checked";
        result[7][0] = "clickable";
        result[8][0] = "enabled";
        result[9][0] = "focusable";
        result[10][0] = "focused";
        result[11][0] = "scrollable";
        result[12][0] = "longClickable";
        result[13][0] = "password";
        result[14][0] = "selected";
        result[15][0] = "visible-to-user";
        result[16][0] = "bounds";

        result[0][1] = id;
        result[1][1] = text;
        result[2][1] = desc;
        result[3][1] = Class;
        result[4][1] = Package;
        result[5][1] = checkable;
        result[6][1] = checked;
        result[7][1] = clickable;
        result[8][1] = enabled;
        result[9][1] = focusable;
        result[10][1] = focused;
        result[11][1] = scrollable;
        result[12][1] = longClickable;
        result[13][1] = password;
        result[14][1] = selected;
        result[15][1] = visible;
        result[16][1] = boundsStr;
        return result;
    }

    public List<String> getProperties(){
        List<String> result = new ArrayList<>();
        if(!StrUtil.isEmpty(id)){
            result.add("id");
        }
        if(!StrUtil.isEmpty(text)){
            result.add("text");
        }
        if(!StrUtil.isEmpty(desc)){
            result.add("desc");
        }
        if(!StrUtil.isEmpty(Class)){
            result.add("Class");
        }
        return result;
    }

    public String getPropertyValue(String prop){
        return (String) ReflectUtil.getFieldValue(this, prop);
    }

    public List<String> getUniqueProperties(NodeInfo node){
        if(node == this){
            return null;
        }
        List<String> result = new ArrayList<>();
        if(!StrUtil.isEmpty(node.id)){
            if (!node.id.equals(id)) {
                result.add("id");
                return result;
            }
        }
        if(!StrUtil.isEmpty(node.text)){
            if (!node.text.equals(text)) {
                result.add("text");
                return result;
            }
        }
        if(!StrUtil.isEmpty(node.desc)){
            if (!node.desc.equals(desc)) {
                result.add("desc");
                return result;
            }
        }
        if(!StrUtil.isEmpty(node.Class)){
            if (!node.Class.equals(Class)) {
                result.add("Class");
                return result;
            }
        }
        return result;
    }


    public boolean isInBounds(int x, int y){
        return bounds.x < x && (bounds.x+bounds.width) > x && bounds.y < y && (bounds.y+bounds.height) > y;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        if(Class != null && Class.startsWith("android.widget.")){
            result.append(Class.substring(15));
        }else if (Class != null && Class.startsWith("android.view.")){
            result.append(Class.substring(13));
        } else{
            result.append(Class);
        }
        if(!StrUtil.isEmpty(text)){
            // 截取前10个字符
            String showText = text.length() > 10? text.substring(0, 10) : text;
            result.append(":").append(showText);
        }
        if(!StrUtil.isEmpty(desc)){
            // 截取前10个字符
            String showDesc = desc.length() > 10? desc.substring(0, 10) : desc;
            result.append("{").append(showDesc).append("}");
        }
        return result.toString();
    }
}
