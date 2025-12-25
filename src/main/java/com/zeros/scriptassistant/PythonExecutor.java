package com.zeros.scriptassistant;

import cn.hutool.core.thread.ThreadUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class PythonExecutor {
    private final String pythonPath;
    private Process pythonProcess;
    private BufferedReader reader;
    private OutputStream writer;
    private String errorMessage;

    public PythonExecutor(String path) throws IOException {
        if (path == null || path.trim().isEmpty()) {
            pythonPath = "python";
        } else {
            pythonPath = path.trim();
        }

        // 先尝试简单命令行检查 python 是否能正常运行（不进入交互模式）
        if (!isPythonExecutableValid(pythonPath)) {
            errorMessage = "Invalid python interpreter: " + pythonPath;
            return;
        }

        // 启动交互模式进程
        ProcessBuilder builder = new ProcessBuilder(pythonPath, "-i");
        builder.environment().put("PYTHONIOENCODING", "UTF-8");
        builder.redirectErrorStream(true);
        pythonProcess = builder.start();

        reader = new BufferedReader(new InputStreamReader(pythonProcess.getInputStream(), "UTF-8"));
        writer = pythonProcess.getOutputStream();

        // 启动后进一步验证交互模式是否正常
        if (!verifyInteractiveMode()) {
            pythonProcess.destroyForcibly();
            errorMessage = "Invalid python interpreter: " + pythonPath;
            return;
        }
    }

    /**
     * 检查 python 命令是否能正常执行（不进入交互模式）
     */
    private boolean isPythonExecutableValid(String pythonCmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder(pythonCmd, "--version");
            Process p = pb.start();
            boolean finished = p.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                return false;
            }
            int exitCode = p.exitValue();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    /**
     * 验证交互模式是否能正常读写
     * 发送一个简单命令并期待特定输出
     */
    private boolean verifyInteractiveMode() throws IOException {
        // 清空可能的启动输出（Python 交互模式启动时可能有欢迎信息）
        drainOutput(1000); // 最多等1秒

        // 发送测试命令
        writer.write("import sys; print('PYTHON_EXECUTOR_READY')\\n".getBytes("UTF-8"));
        writer.flush();

        // 等待并读取输出，寻找我们的标记
        long deadline = System.currentTimeMillis() + 5000; // 最多等5秒
        StringBuilder output = new StringBuilder();
        while (System.currentTimeMillis() < deadline) {
            if (reader.ready()) {
                char[] buf = new char[1024];
                int len = reader.read(buf);
                if (len > 0) {
                    output.append(buf, 0, len);
                    if (output.toString().contains("PYTHON_EXECUTOR_READY")) {
                        return true;
                    }
                }
            } else {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {}
            }
        }
        return false;
    }

    /**
     * 尽量读取并丢弃已有输出（用于清除启动时的 Python 欢迎信息）
     */
    private void drainOutput(long timeoutMs) throws IOException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline && reader.ready()) {
            reader.readLine();
        }
    }

    public String executeCode(String code) throws IOException {
        return executeCode(code, 0);
    }

    public synchronized String executeCode(String code, int indentLevel) throws IOException {
        StringBuilder indentedCode = new StringBuilder();
        for (int i = 0; i < indentLevel; i++) {
            indentedCode.append("    "); // 每次缩进是4个空格
        }
        indentedCode.append(code).append("\n");

        // 发送代码到python进程
        writer.write(indentedCode.toString().getBytes(StandardCharsets.UTF_8));
        writer.flush();

        // 读取输出
        StringBuilder output = new StringBuilder();
        int timeout = 10000;
        while (!reader.ready()){
            ThreadUtil.sleep(10);
            timeout -= 10;
            if(timeout <= 0){
                break;
            }
            if(code.equals("exit()")){
                break;
            }
        }
        while (reader.ready()) {
            output.append((char) reader.read());
        }
        String trim = output.toString().trim();
        System.out.println(trim);
        return trim;
    }

    public void close() throws IOException {
        executeCode("exit()", 0);
        writer.close();
        reader.close();
        pythonProcess.destroy();
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    // 示例用法
    public static void main(String[] args) {
        try {
            PythonExecutor executor = new PythonExecutor(null);
            // 执行Python代码示例
            System.out.println(executor.executeCode("print('Hello from Python!')"));
            System.out.println(executor.executeCode("x = 5"));
            System.out.println(executor.executeCode("for i in range(5):"));
            System.out.println(executor.executeCode("print(x * 2)", 1));
            System.out.println(executor.executeCode(""));
            System.out.println(executor.executeCode(""));
            // 退出Python解释器
            ThreadUtil.sleep(1000);
            executor.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
