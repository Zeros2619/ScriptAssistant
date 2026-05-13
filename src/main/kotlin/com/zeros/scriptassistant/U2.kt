package com.zeros.scriptassistant

import cn.hutool.core.util.NumberUtil
import java.awt.Rectangle
import java.io.File
import java.io.IOException

class U2 {
    private var executor: PythonExecutor? = null
    private var failMsg: String? = null

    fun init(path: String?, serial: String?, objectName: String): Boolean {
        return try {
            failMsg = "Init failed, python interpreter path: $path"
            executor = PythonExecutor(path)
            val errorMsg = executor!!.errorMessage
            if (errorMsg != null) {
                failMsg = errorMsg
                return false
            }

            // 清除 reader 缓存
            executor!!.executeCode("")

            val result = executor!!.executeCode("import uiautomator2 as u2")
            if (result.contains("ModuleNotFoundError:")) {
                println(result)
                failMsg = "please install uiautomator2 first, interpreter path: $path"
                println(failMsg)
                return false
            }

            val connectCode = if (serial == null) {
                "$objectName = u2.connect()"
            } else {
                "$objectName = u2.connect('$serial')"
            }
            val result2 = executor!!.executeCode(connectCode)

            if (result2.contains("NameError:") || result2.contains("状态异常")) {
                failMsg = result2
                println(failMsg)
                return false
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    fun getInitFailMsg(): String? = failMsg

    fun executeCode(code: String): String? {
        return executeCode(code, 0)
    }

    fun executeCode(code: String, indentLevel: Int): String? {
        if (executor == null) {
            return null
        }
        return try {
            executor!!.executeCode(code, indentLevel)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun dumpUI(screenShotSaveFile: File, hierarchySaveFile: File) {
        // device.dump_ui(dir)
        executeCode("d.screenshot(r'" + screenShotSaveFile.absolutePath + "')")
        executeCode("with open(r'" + hierarchySaveFile.absolutePath + "', 'w', encoding=\"utf-8\") as f: f.write(d.dump_hierarchy())")
        executeCode("")
        executeCode("")
        executeCode("")
    }

    fun getNodeBounds(code: String): Rectangle? {
        val result = executeCode("print($code.info['bounds'] if $code.exists else None)")
        executeCode("")
        if (result == null || result.trim() == "None") {
            return null
        }
        println(result)
        // {'bottom': 1555, 'left': 296, 'right': 540, 'top': 1278}
        // 判断格式是否正确
        if (!result.contains("'left': ") || !result.contains("'top': ") ||
            !result.contains("'right': ") || !result.contains("'bottom': ")
        ) {
            return null
        }

        // 解析结果
        val left = NumberUtil.parseInt(result.split("'left': ")[1].split(",")[0])
        val top = NumberUtil.parseInt(result.split("'top': ")[1].split("}")[0])
        val right = NumberUtil.parseInt(result.split("'right': ")[1].split(",")[0])
        val bottom = NumberUtil.parseInt(result.split("'bottom': ")[1].split(",")[0])
        return Rectangle(left, top, right - left, bottom - top)
    }

    fun destroy() {
        try {
            executor?.destroy()
            executor = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
