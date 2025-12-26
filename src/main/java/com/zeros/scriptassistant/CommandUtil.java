package com.zeros.scriptassistant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class CommandUtil {

    private final static int BUFFER_SIZE = 1024;

    private final static String DEFAULT_ENCODING = "gbk";

    public static class ProcessWorker extends Thread {
        public final Process process;
        public volatile int exitCode = -99;
        public volatile boolean completed = false;
        public volatile StringBuilder output;
        public volatile boolean needOutput = true;
        public OutputListener listener;

        private ProcessWorker(Process process) {
            this.process = process;
        }

        private ProcessWorker(Process process, boolean needOutput) {
            this.process = process;
            this.needOutput = needOutput;
        }

        public void setOutputListener(OutputListener listener){
            this.listener = listener;
        }

        @Override
        public void run() {
            try (InputStreamReader reader = new InputStreamReader(
                    process.getInputStream(), DEFAULT_ENCODING)) {
                BufferedReader bufferedReader = new BufferedReader(reader);
                output = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    if(listener!=null){
                        listener.onReadLine(line);
                    }
                    if(needOutput) {
                        output.append(line).append("\r\n");
                    }
                }
                exitCode = process.waitFor();
                completed = true;
            } catch (InterruptedException | IOException e) {
                Thread.currentThread().interrupt();
            }
        }

        public int getExitCode() {
            return exitCode;
        }

        public String getOutput() {
            if(output == null){
                return "";
            }
            return output.toString();
        }

        public boolean isCompleted() {
            return completed;
        }
    }

    public static class ProcessHandler{
        public Process process;
        public ProcessWorker processWorker;
        public OutputListener listener;

        public boolean shutdown(){
            if(process != null && processWorker != null){
                process.destroy();
                processWorker.interrupt();
                return true;
            }else{
                return false;
            }
        }
    }

    public interface OutputListener{
        void onReadLine(String line);
    }

    public static int execCmd(String command, StringBuilder log, int timeoutSecond) throws IOException, TimeoutException {
        ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
        // 合并错误输出流
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        ProcessWorker processWorker = new ProcessWorker(process);
        int exitCode = processWorker.getExitCode();
        processWorker.start();
        try {
            processWorker.join(timeoutSecond * 1000L);
            if (processWorker.isCompleted()) {
                log.append(processWorker.getOutput());
                exitCode = processWorker.getExitCode();
            } else {
                process.destroy();
                log.append(processWorker.getOutput());
                processWorker.interrupt();
                throw new TimeoutException("进程执行超时, timeoutSecond:" + timeoutSecond);
            }
        } catch (InterruptedException e) {
            processWorker.interrupt();
        }
        return exitCode;
    }

    public static String execCmd(String command,long timeout) {
        return execCmd(command, true, timeout, null);
    }

    public static String execCmd(String[] command,long timeout) {
        return execCmd(command, true, timeout, null);
    }

    public static String execCmd(List<String> command,long timeout) {
        return execCmd(command, true, timeout, null);
    }

    public static String execCmd(String command, boolean needOutput, long timeout, ProcessHandler handler) {
        return execCmd(command.split(" "), needOutput, timeout, handler);
    }
    public static String execCmd(String[] command, boolean needOutput,long timeout, ProcessHandler handler) {
        return execCmd(Arrays.asList(command), needOutput, timeout, handler);
    }

    public static String execCmd(List<String> command, boolean needOutput,long timeout, ProcessHandler handler) {
        StringBuilder result = new StringBuilder();
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        // 合并错误输出流
        processBuilder.redirectErrorStream(true);
        Process process = null;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        ProcessWorker processWorker = new ProcessWorker(process, needOutput);
        if(handler != null){
            handler.process = process;
            handler.processWorker = processWorker;
            processWorker.setOutputListener(handler.listener);
        }
        int exitCode = processWorker.getExitCode();
        processWorker.start();
        try {
            processWorker.join(timeout);
            if (processWorker.isCompleted()) {
                result.append(processWorker.getOutput());
                exitCode = processWorker.getExitCode();
            } else {
                result.append(processWorker.getOutput());
                process.destroy();
                processWorker.interrupt();
                System.out.println("进程执行超时, max timeout:" + timeout);
            }
        } catch (InterruptedException e) {
            processWorker.interrupt();
        }
        return result.toString();
    }
}

