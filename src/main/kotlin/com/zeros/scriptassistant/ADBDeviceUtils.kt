package com.zeros.scriptassistant

object ADBDeviceUtils {
    // ADB命令路径，可根据实际情况修改
    private const val ADB_PATH = "adb"

    // 匹配设备列表输出的正则表达式
    private val DEVICE_PATTERN = Regex("^([^\\t]+)\\t(device|offline|unauthorized|bootloader)")

    // 设置合理的默认超时时间（例如 10 秒）
    private const val ADB_TIMEOUT = 10000L

    /**
     * 获取已连接的Android设备序列号列表
     * @return 设备序列号列表，如果发生错误则返回空列表
     */
    @JvmStatic
    fun getConnectedDevices(): List<String> {
        val deviceSerials = mutableListOf<String>()

        try {
            // 直接委托 CommandUtil 执行命令，摆脱冗长的流读写和线程等待逻辑
            val output = CommandUtil.execCmd("$ADB_PATH devices", ADB_TIMEOUT)
                ?: return deviceSerials

            // 利用 lineSequence 内存友好地按行处理返回的结果
            output.lineSequence()
                .map { it.trim() }
                .dropWhile { !it.contains("List of devices attached") } // 跳过找到标题行之前的所有行
                .drop(1) // 跳过标题行本身
                .filter { it.isNotEmpty() } // 忽略空行
                .forEach { line ->
                    DEVICE_PATTERN.find(line)?.destructured?.let { (serial, state) ->
                        if (state == "device") {
                            deviceSerials.add(serial)
                        }
                    }
                }

        } catch (e: Exception) {
            System.err.println("获取设备列表时发生错误: ${e.message}")
            e.printStackTrace()
        }

        return deviceSerials
    }

    /**
     * 检查指定序列号的设备是否在线
     * @param serial 设备序列号
     * @return 如果设备在线返回true，否则返回false
     */
    @JvmStatic
    fun isDeviceOnline(serial: String?): Boolean {
        if (serial.isNullOrBlank()) {
            return false
        }

        try {
            // 执行adb -s <serial> get-state命令
            val output = CommandUtil.execCmd("$ADB_PATH -s $serial get-state", ADB_TIMEOUT)
                ?: return false

            // 取第一行非空的有效返回判断状态
            val firstLine = output.lineSequence()
                .firstOrNull { it.isNotBlank() }
                ?.trim()

            return firstLine == "device"

        } catch (e: Exception) {
            System.err.println("检查设备状态时发生错误: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

}