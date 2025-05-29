package com.zeros.scriptassistant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ADBDeviceUtils {
    // ADB命令路径，可根据实际情况修改或通过构造函数传入
    private static final String ADB_PATH = "adb";

    // 匹配设备列表输出的正则表达式
    private static final Pattern DEVICE_PATTERN = Pattern.compile("^([^\t]+)\t(device|offline|unauthorized|bootloader)");

    /**
     * 获取已连接的Android设备序列号列表
     * @return 设备序列号列表，如果发生错误则返回空列表
     */
    public static List<String> getConnectedDevices() {
        List<String> deviceSerials = new ArrayList<>();
        Process process = null;
        BufferedReader reader = null;

        try {
            // 执行adb devices命令
            process = Runtime.getRuntime().exec(ADB_PATH + " devices");
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            boolean isHeaderSkipped = false;

            // 逐行读取输出并解析设备信息
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // 跳过标题行
                if (!isHeaderSkipped) {
                    if (line.contains("List of devices attached")) {
                        isHeaderSkipped = true;
                    }
                    continue;
                }

                // 跳过空行
                if (line.isEmpty()) {
                    continue;
                }

                // 使用正则表达式匹配设备信息
                Matcher matcher = DEVICE_PATTERN.matcher(line);
                if (matcher.find()) {
                    String serial = matcher.group(1);
                    String state = matcher.group(2);
                    
                    // 只添加状态为"device"的设备
                    if ("device".equals(state)) {
                        deviceSerials.add(serial);
                    }
                }
            }

            // 等待命令执行完成并检查返回码
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("ADB命令执行失败，返回码: " + exitCode);
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("获取设备列表时发生错误: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 关闭资源
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (process != null) {
                process.destroy();
            }
        }

        return deviceSerials;
    }

    /**
     * 检查指定序列号的设备是否在线
     * @param serial 设备序列号
     * @return 如果设备在线返回true，否则返回false
     */
    public static boolean isDeviceOnline(String serial) {
        if (serial == null || serial.trim().isEmpty()) {
            return false;
        }

        Process process = null;
        BufferedReader reader = null;
        boolean isOnline = false;

        try {
            // 执行adb -s <serial> get-state命令
            process = Runtime.getRuntime().exec(ADB_PATH + " -s " + serial + " get-state");
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            // 读取输出的第一行
            if ((line = reader.readLine()) != null) {
                line = line.trim();
                // 如果状态为"device"，则设备在线
                isOnline = "device".equals(line);
            }

            // 等待命令执行完成并检查返回码
            int exitCode = process.waitFor();
            if (exitCode != 0 && isOnline) {
                // 如果返回码非零但状态为device，可能存在冲突，重置状态
                isOnline = false;
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("检查设备状态时发生错误: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 关闭资源
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (process != null) {
                process.destroy();
            }
        }

        return isOnline;
    }

    // 简单的测试示例
    public static void main(String[] args) {
        // 获取已连接的设备列表
        List<String> devices = getConnectedDevices();
        System.out.println("已连接的设备: " + devices);

        // 检查每个设备是否在线
        for (String device : devices) {
            boolean online = isDeviceOnline(device);
            System.out.println("设备 " + device + " 是否在线: " + online);
        }
    }
}    