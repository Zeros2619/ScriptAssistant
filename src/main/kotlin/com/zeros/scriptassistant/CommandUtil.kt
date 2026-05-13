package com.zeros.scriptassistant

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.concurrent.TimeoutException

object CommandUtil {

    private const val BUFFER_SIZE = 1024
    private const val DEFAULT_ENCODING = "gbk"

    interface OutputListener {
        fun onReadLine(line: String)
    }

    class ProcessHandler {
        @JvmField var process: Process? = null
        @JvmField var processWorker: ProcessWorker? = null
        @JvmField var listener: OutputListener? = null

        fun shutdown(): Boolean {
            return if (process != null && processWorker != null) {
                process?.destroy()
                processWorker?.interrupt()
                true
            } else {
                false
            }
        }
    }

    class ProcessWorker : Thread {
        @JvmField val process: Process
        @Volatile var exitCode = -99
        @Volatile var completed = false
        @Volatile var output: java.lang.StringBuilder? = null
        @Volatile var needOutput = true
        var listener: OutputListener? = null

        // 保持与 Java 一致的 private 构造器
        private constructor(process: Process) {
            this.process = process
        }

        private constructor(process: Process, needOutput: Boolean) {
            this.process = process
            this.needOutput = needOutput
        }

        companion object {
            // 提供内部创建实例的方法
            internal fun create(process: Process) = ProcessWorker(process)
            internal fun create(process: Process, needOutput: Boolean) = ProcessWorker(process, needOutput)
        }

        fun setOutputListener(listener: OutputListener?) {
            this.listener = listener
        }

        override fun run() {
            try {
                // 使用 use 块自动管理 IO 资源
                InputStreamReader(process.inputStream, Charset.forName(DEFAULT_ENCODING)).use { reader ->
                    BufferedReader(reader).use { bufferedReader ->
                        output = java.lang.StringBuilder()
                        var line: String?
                        while (bufferedReader.readLine().also { line = it } != null) {
                            val currentLine = line!!
                            listener?.onReadLine(currentLine)
                            if (needOutput) {
                                output?.append(currentLine)?.append("\r\n")
                            }
                        }
                        exitCode = process.waitFor()
                        completed = true
                    }
                }
            } catch (e: InterruptedException) {
                currentThread().interrupt()
            } catch (e: IOException) {
                currentThread().interrupt()
            }
        }

        fun getOutput(): String = output?.toString() ?: ""

        fun isCompleted(): Boolean = completed
    }

    @JvmStatic
    @Throws(IOException::class, TimeoutException::class)
    fun execCmd(command: String, log: java.lang.StringBuilder, timeoutSecond: Int): Int {
        val processBuilder = ProcessBuilder(*command.split(" ").toTypedArray())
        processBuilder.redirectErrorStream(true) // 合并错误输出流
        
        val process = processBuilder.start()
        val processWorker = ProcessWorker.create(process)
        var exitCode = processWorker.exitCode
        
        processWorker.start()
        try {
            processWorker.join(timeoutSecond * 1000L)
            if (processWorker.isCompleted()) {
                log.append(processWorker.getOutput())
                exitCode = processWorker.exitCode
            } else {
                process.destroy()
                log.append(processWorker.getOutput())
                processWorker.interrupt()
                throw TimeoutException("进程执行超时, timeoutSecond:$timeoutSecond")
            }
        } catch (e: InterruptedException) {
            processWorker.interrupt()
        }
        return exitCode
    }

    @JvmStatic
    fun execCmd(command: String, timeout: Long): String? = 
        execCmd(command, true, timeout, null)

    @JvmStatic
    fun execCmd(command: Array<String>, timeout: Long): String? = 
        execCmd(command, true, timeout, null)

    @JvmStatic
    fun execCmd(command: List<String>, timeout: Long): String? = 
        execCmd(command, true, timeout, null)

    @JvmStatic
    fun execCmd(command: String, needOutput: Boolean, timeout: Long, handler: ProcessHandler?): String? =
        execCmd(command.split(" "), needOutput, timeout, handler)

    @JvmStatic
    fun execCmd(command: Array<String>, needOutput: Boolean, timeout: Long, handler: ProcessHandler?): String? =
        execCmd(command.toList(), needOutput, timeout, handler)

    @JvmStatic
    fun execCmd(command: List<String>, needOutput: Boolean, timeout: Long, handler: ProcessHandler?): String? {
        val result = java.lang.StringBuilder()
        val processBuilder = ProcessBuilder(command)
        processBuilder.redirectErrorStream(true) // 合并错误输出流
        
        val process: Process
        try {
            process = processBuilder.start()
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }

        val processWorker = ProcessWorker.create(process, needOutput)
        if (handler != null) {
            handler.process = process
            handler.processWorker = processWorker
            processWorker.setOutputListener(handler.listener)
        }
        
        @Suppress("UNUSED_VARIABLE")
        var exitCode = processWorker.exitCode
        
        processWorker.start()
        try {
            processWorker.join(timeout)
            if (processWorker.isCompleted()) {
                result.append(processWorker.getOutput())
                exitCode = processWorker.exitCode
            } else {
                result.append(processWorker.getOutput())
                process.destroy()
                processWorker.interrupt()
                println("进程执行超时, max timeout:$timeout")
            }
        } catch (e: InterruptedException) {
            processWorker.interrupt()
        }
        return result.toString()
    }
}