package com.zeros.scriptassistant

import cn.hutool.core.thread.ThreadUtil
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

class PythonExecutor(path: String?) {
    private val pythonPath: String
    private var pythonProcess: Process? = null
    private var reader: BufferedReader? = null
    private var writer: OutputStream? = null
    internal var errorMessage: String? = null

    init {
        pythonPath = if (path.isNullOrBlank()) "python" else path.trim()

        // 先尝试简单命令行检查 python 是否能正常运行
        if (!isPythonExecutableValid()) {
            errorMessage = "Verify python interpreter failed: $pythonPath"
        } else {
            // 启动交互模式进程
            val builder = ProcessBuilder(pythonPath, "-i")
            builder.environment()["PYTHONIOENCODING"] = "UTF-8"
            builder.redirectErrorStream(true)
            pythonProcess = builder.start()

            reader = BufferedReader(InputStreamReader(pythonProcess!!.inputStream, Charsets.UTF_8))
            writer = pythonProcess!!.outputStream
        }
    }

    /**
     * 检查 python 命令是否能正常执行
     */
    private fun isPythonExecutableValid(): Boolean {
        val result = CommandUtil.execCmd(arrayOf(pythonPath, "--version"), 3000)
        val pattern = Pattern.compile("^Python\\s+3\\.\\d+\\.\\d+")
        return result?.lineSequence()?.any { line ->
            pattern.matcher(line.trim()).matches()
        } == true
    }

    @Throws(IOException::class)
    fun executeCode(code: String): String {
        return executeCode(code, 0)
    }

    @Synchronized
    @Throws(IOException::class)
    fun executeCode(code: String, indentLevel: Int): String {
        val indentedCode = buildString {
            repeat(indentLevel) {
                append("    ")
            }
            append(code).append("\n")
        }

        // 发送代码到 python 进程
        writer?.write(indentedCode.toByteArray(StandardCharsets.UTF_8))
        writer?.flush()

        // 读取输出
        val output = StringBuilder()
        var timeout = 10000
        while (!reader!!.ready()) {
            ThreadUtil.sleep(10)
            timeout -= 10
            if (timeout <= 0) {
                break
            }
            if (code == "exit()") {
                break
            }
        }
        while (reader!!.ready()) {
            output.append(reader!!.read().toChar())
        }
        val trim = output.toString().trim()
        println(trim)
        return trim
    }

    @Throws(IOException::class)
    fun destroy() {
        writer?.close()
        reader?.close()
        pythonProcess?.destroy()
    }

    fun getErrorMessage(): String? = errorMessage
}
