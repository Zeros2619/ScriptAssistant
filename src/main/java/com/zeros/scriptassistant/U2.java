package com.zeros.scriptassistant;

import cn.hutool.core.util.NumberUtil;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class U2 {
    private PythonExecutor executor;
    private String failMsg;

    public boolean init(String path, String serial, String objectName) {
        try {
            failMsg = "Init failed, python interpreter path: " + path;
            executor = new PythonExecutor(path);
            String errorMsg = executor.getErrorMessage();
            if (errorMsg != null) {
                failMsg = errorMsg;
                return false;
            }
            String result = executor.executeCode("import uiautomator2 as u2");
            if (result.contains("ModuleNotFoundError:")) {
                System.out.println(result);
                failMsg = "please install uiautomator2 first";
                System.out.println(failMsg);
                return false;
            }
            if (serial == null) {
                result = executor.executeCode(objectName + " = u2.connect()");
            } else {
                result = executor.executeCode(objectName + " = u2.connect('" + serial + "')");
            }
            if (result.contains("NameError:") || result.contains("状态异常")) {
                failMsg = result;
                System.out.println(failMsg);
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public String getInitFailMsg() {
        return failMsg;
    }

    public String executeCode(String code) {
        return executeCode(code, 0);
    }

    public String executeCode(String code, int indentLevel) {
        if (executor == null) {
            return null;
        }
        try {
            return executor.executeCode(code, indentLevel);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void dumpUI(File screenShotSaveFile, File hierarchySaveFile) {
        // device.dump_ui(dir)
        executeCode("d.screenshot(r'" + screenShotSaveFile.getAbsolutePath() + "')");
        executeCode("with open(r'" + hierarchySaveFile.getAbsolutePath() + "', 'w', encoding=\"utf-8\") as f: f.write(d.dump_hierarchy())");
        executeCode("");
        executeCode("");
        executeCode("");
    }

    public Rectangle getNodeBounds(String code) {
        String result = executeCode("print(" + code + ".info['bounds'])");
        executeCode("");
        if (result == null) {
            return null;
        }
        System.out.println(result);
        // {'bottom': 1555, 'left': 296, 'right': 540, 'top': 1278}
        // 判断格式是否正确
        if (!result.contains("'left': ") || !result.contains("'top': ") || !result.contains("'right': ") || !result.contains("'bottom': ")) {
            return null;
        }
        // 解析结果
        int left = NumberUtil.parseInt(result.split("'left': ")[1].split(",")[0]);
        int top = NumberUtil.parseInt(result.split("'top': ")[1].split("}")[0]);
        int right = NumberUtil.parseInt(result.split("'right': ")[1].split(",")[0]);
        int bottom = NumberUtil.parseInt(result.split("'bottom': ")[1].split(",")[0]);
        return new Rectangle(left, top, right-left, bottom-top);
    }

    public void close() {
        try {
            executor.close();
            executor = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new U2().init("F:\\PycharmProjects\\U2Test\\venv\\Scripts\\python.exe", null, "d");
    }
}
