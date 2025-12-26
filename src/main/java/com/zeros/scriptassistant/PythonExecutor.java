package com.zeros.scriptassistant;

import cn.hutool.core.thread.ThreadUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

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
            errorMessage = "Verify python interpreter failed: " + pythonPath;
            return;
        }

        // 启动交互模式进程
        ProcessBuilder builder = new ProcessBuilder(pythonPath, "-i");
        builder.environment().put("PYTHONIOENCODING", "UTF-8");
        builder.redirectErrorStream(true);
        pythonProcess = builder.start();

        reader = new BufferedReader(new InputStreamReader(pythonProcess.getInputStream(), "UTF-8"));
        writer = pythonProcess.getOutputStream();
    }

    /**
     * 检查 python 命令是否能正常执行（不进入交互模式）
     */
    private boolean isPythonExecutableValid(String pythonCmd) {
        String result = CommandUtil.execCmd(pythonCmd + " --version", 3000);
        Pattern pattern = Pattern.compile("^Python\\s+3\\.\\d+\\.\\d+");
        return result.lines().map(String::trim).anyMatch(line1 -> pattern.matcher(line1).matches());
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

    public void destroy() throws IOException {
        if (writer != null) {
            writer.close();
        }
        if (reader != null) {
            reader.close();
        }
        if (pythonProcess != null) {
            pythonProcess.destroy();
        }
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
            executor.destroy();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
