package org.example.agent.manager;

import org.example.agent.websocket.SocketClientEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Agent重启管理器
 * 负责优雅地重启Agent应用，保留启动参数
 * 支持两种重启模式：
 * 1. 脚本模式 - 返回exit code=1，由外部脚本处理重启
 * 2. 进程模式 - 直接在JVM内部使用ProcessBuilder重新启动
 */
@Component
public class RestartManager {

    private static final Logger log = LoggerFactory.getLogger(RestartManager.class);
    
    private SocketClientEndpoint socketClientEndpoint;

    /**
     * 设置SocketClientEndpoint，用于关闭WebSocket连接
     */
    public void setSocketClientEndpoint(SocketClientEndpoint socketClientEndpoint) {
        this.socketClientEndpoint = socketClientEndpoint;
    }

    /**
     * 异步执行重启
     * 会关闭WebSocket连接，关闭Spring应用，然后重新启动JVM进程
     * 
     * @param applicationContext Spring应用上下文，用于获取环境信息
     * @param delayMs 延迟多久后执行重启（毫秒），用于确保消息发送完成
     */
    public void restartAsync(ApplicationContext applicationContext, long delayMs) {
        new Thread(() -> {
            try {
                log.info("Waiting {}ms before restart to ensure messages are sent", delayMs);
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                log.warn("Interrupted during restart delay", e);
                Thread.currentThread().interrupt();
            }
            
            performRestart(applicationContext);
        }, "AgentRestartThread").start();
    }

    /**
     * 执行重启
     * 支持两种模式：
     * 1. 如果启动了脚本，使用exit code=1方式（脚本模式）
     * 2. 否则直接使用ProcessBuilder重启（进程模式）
     */
    private void performRestart(ApplicationContext applicationContext) {
        try {
            log.info("Performing restart of Agent application");
            
            // 先关闭WebSocket连接
            if (socketClientEndpoint != null) {
                try {
                    socketClientEndpoint.close();
                    log.info("WebSocket connection closed");
                } catch (Exception e) {
                    log.warn("Error closing WebSocket connection", e);
                }
            }
            
            // 尝试使用ProcessBuilder直接重启（进程模式）
            if (tryRestartWithProcessBuilder(applicationContext)) {
                // ProcessBuilder重启成功，当前进程会退出
                return;
            }
            
            // 如果ProcessBuilder重启失败，使用exit code=1方式（脚本模式）
            log.info("ProcessBuilder restart failed, using exit code=1 for script-based restart");
            if (applicationContext != null) {
                SpringApplication.exit(applicationContext, () -> 1);
            } else {
                log.warn("ApplicationContext is null, exiting with code 1");
                System.exit(1);
            }
        } catch (Exception e) {
            log.error("Error during restart", e);
            System.exit(1);
        }
    }

    /**
     * 尝试使用ProcessBuilder重启JVM进程
     * 这种方式不需要外部脚本，直接在JVM内部重启
     * 
     * @param applicationContext Spring应用上下文，用于优雅关闭
     * @return 成功启动新进程返回true，失败返回false
     */
    private boolean tryRestartWithProcessBuilder(ApplicationContext applicationContext) {
        try {
            // 获取Java命令
            String javaHome = System.getProperty("java.home");
            String javaExe = javaHome + File.separator + "bin" + File.separator + "java";
            
            // 获取启动参数
            String bootArgs = getBootArgs();
            
            // 获取Agent JAR路径
            String agentJar = getAgentJarPath();
            if (agentJar == null || agentJar.isEmpty()) {
                log.warn("Cannot determine Agent JAR path, ProcessBuilder restart not available");
                log.warn("Please ensure the agent is packaged as a JAR file");
                log.warn("You can build the JAR with: mvn clean package -DskipTests");
                return false;
            }
            
            // 验证JAR文件是否存在且有效
            File jarFile = new File(agentJar);
            if (!jarFile.exists()) {
                log.warn("Agent JAR file not found at: {}", agentJar);
                return false;
            }
            
            if (!jarFile.isFile() || jarFile.isDirectory()) {
                log.warn("Agent JAR path is not a valid file: {}", agentJar);
                log.warn("Path points to a directory, not a JAR file");
                return false;
            }
            
            // 构建新的启动命令
            List<String> command = new ArrayList<>();
            command.add(javaExe);
            
            // 添加JVM参数
            command.add("-jar");
            command.add(agentJar);
            
            // 添加启动参数
            if (bootArgs != null && !bootArgs.isEmpty()) {
                // 将启动参数按空格分割并添加
                String[] args = bootArgs.split("\\s+");
                for (String arg : args) {
                    if (!arg.isEmpty()) {
                        command.add(arg);
                    }
                }
            }
            
            log.info("Restarting Agent with ProcessBuilder: {}", String.join(" ", command));
            
            // 关键：在启动新进程之前，立即在后台启动关闭流程
            // 这样可以快速释放端口，避免新进程启动时端口被占用
            new Thread(() -> {
                try {
                    // 短延迟确保ProcessBuilder调用已执行
                    Thread.sleep(100);
                    log.info("Gracefully shutting down old Agent process to release port");
                    
                    // 通过SpringApplication.exit优雅关闭，释放资源
                    if (applicationContext != null) {
                        SpringApplication.exit(applicationContext, () -> 0);
                    } else {
                        log.warn("ApplicationContext is null, using System.exit");
                        System.exit(0);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Interrupted during graceful shutdown", e);
                    System.exit(0);
                } catch (Exception e) {
                    log.error("Error during graceful shutdown", e);
                    System.exit(0);
                }
            }, "ProcessExitThread").start();
            
            // 延迟一点时间再启动新进程，给旧进程一些时间释放资源
            // 但这个延迟很短，不会明显影响重启速度
            Thread.sleep(200);
            
            // 启动新进程
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            // 继承标准输入输出
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            
            log.info("New Agent process started with PID: {}", process.pid());
            
            return true;
        } catch (Exception e) {
            log.error("Error restarting with ProcessBuilder: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取Agent JAR文件路径
     * 优先级：
     * 1. 查找target目录中的agent-*-SNAPSHOT.jar文件
     * 2. 在classpath中查找JAR文件
     * 3. 从CodeSource获取
     * 
     * @return JAR文件完整路径，如果找不到返回null
     */
    private String getAgentJarPath() {
        try {
            // 方法1: 尝试从classpath中找到target目录，然后在target下查找JAR
            String classpath = System.getProperty("java.class.path");
            if (classpath != null && !classpath.isEmpty()) {
                String[] paths = classpath.split(File.pathSeparator);
                
                // 在classpath中查找target/classes目录，推断JAR位置
                for (String path : paths) {
                    if (path.contains("target" + File.separator + "classes")) {
                        // 从target/classes路径推断target目录
                        File targetClassesDir = new File(path);
                        File targetDir = targetClassesDir.getParentFile();
                        
                        if (targetDir != null && targetDir.exists()) {
                            // 在target目录中查找agent-*-SNAPSHOT.jar
                            File[] files = targetDir.listFiles((dir, name) -> 
                                name.startsWith("agent-") && name.endsWith("-SNAPSHOT.jar"));
                            
                            if (files != null && files.length > 0) {
                                String jarPath = files[0].getAbsolutePath();
                                log.info("Found Agent JAR from target directory: {}", jarPath);
                                return jarPath;
                            }
                        }
                    }
                }
                
                // 方法2: 直接在classpath中查找agent JAR文件
                for (String path : paths) {
                    if ((path.contains("agent") && path.endsWith(".jar")) || 
                        path.endsWith("agent-1.0-SNAPSHOT.jar")) {
                        File f = new File(path);
                        if (f.exists() && f.isFile()) {
                            log.info("Found Agent JAR from classpath: {}", path);
                            return path;
                        }
                    }
                }
            }
            
            // 方法3: 从CodeSource获取（IDE运行时可能有用）
            try {
                String codeSourcePath = RestartManager.class.getProtectionDomain()
                        .getCodeSource().getLocation().getPath();
                if (codeSourcePath != null) {
                    // 如果是target/classes，尝试找对应的JAR
                    if (codeSourcePath.contains("target" + File.separator + "classes")) {
                        File targetClassesDir = new File(codeSourcePath);
                        File targetDir = targetClassesDir.getParentFile();
                        
                        if (targetDir != null && targetDir.exists()) {
                            File[] files = targetDir.listFiles((dir, name) -> 
                                name.startsWith("agent-") && name.endsWith("-SNAPSHOT.jar"));
                            
                            if (files != null && files.length > 0) {
                                String jarPath = files[0].getAbsolutePath();
                                log.info("Found Agent JAR from CodeSource target: {}", jarPath);
                                return jarPath;
                            }
                        }
                    }
                    
                    // 如果CodeSource本身就是JAR
                    if (codeSourcePath.endsWith(".jar")) {
                        File f = new File(codeSourcePath);
                        if (f.exists()) {
                            log.info("Found Agent JAR from CodeSource: {}", codeSourcePath);
                            return codeSourcePath;
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Error getting Agent JAR from CodeSource: {}", e.getMessage());
            }
            
            log.warn("Could not find Agent JAR in classpath or target directory");
            return null;
        } catch (Exception e) {
            log.error("Error getting Agent JAR path: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取启动参数
     * 从系统属性中读取保存的启动参数
     * 
     * @return 启动参数字符串，如果没有则返回empty string
     */
    public static String getBootArgs() {
        String bootArgs = System.getProperty("AGENT_BOOT_ARGS", "");
        if (!bootArgs.isEmpty()) {
            log.info("Retrieved boot args: {}", bootArgs);
        } else {
            log.info("No boot args found in system properties");
        }
        return bootArgs;
    }

    /**
     * 保存启动参数
     * 
     * @param args 启动参数数组
     */
    public static void saveBootArgs(String[] args) {
        if (args != null && args.length > 0) {
            String bootArgs = String.join(" ", args);
            System.setProperty("AGENT_BOOT_ARGS", bootArgs);
            log.info("Boot args saved: {}", bootArgs);
        }
    }
}
