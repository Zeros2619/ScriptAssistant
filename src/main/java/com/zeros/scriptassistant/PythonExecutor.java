package com.zeros.scriptassistant;

import cn.hutool.core.thread.ThreadUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class PythonExecutor {
    private final String pythonPath;
    private final Process pythonProcess;
    private final BufferedReader reader;
    private final OutputStream writer;

    public PythonExecutor(String path) throws IOException {
        if(path == null){
            pythonPath = "python";
        }else{
            pythonPath = path;
        }
        // 启动Python解释器
        ProcessBuilder builder = new ProcessBuilder(pythonPath, "-i"); // 打开交互模式的python进程
        builder.environment().put("PYTHONIOENCODING", "UTF-8");
        builder.redirectErrorStream(true); // 合并标准错误和标准输出
        pythonProcess = builder.start();
        reader = new BufferedReader(new InputStreamReader(pythonProcess.getInputStream()));
        writer = pythonProcess.getOutputStream();
    }

    public String executeCode(String code) throws IOException {
        return executeCode(code, 0);
    }

    public String executeCode(String code, int indentLevel) throws IOException {
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
        while (!reader.ready()){
            ThreadUtil.sleep(10);
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
