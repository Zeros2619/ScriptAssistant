package com.zeros.scriptassistant;

import java.io.File;
import java.io.IOException;

public class U2 {
    private PythonExecutor executor;
    private String failMsg;

    public boolean init(String path, String serial, String objectName) {
        try {
            failMsg = "init failed";
            executor = new PythonExecutor(path);
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
