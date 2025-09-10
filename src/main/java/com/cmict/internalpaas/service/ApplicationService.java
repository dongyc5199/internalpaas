package com.cmict.internalpaas.service;

import com.cmict.internalpaas.model.Application;
import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.repository.ApplicationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ApplicationRepository applicationRepository;
    
    @Autowired
    private PortManagerService portManagerService;
    
    /**
     * 上传JAR文件
     */
    public Application uploadApplication(User user, MultipartFile file, String appName) throws IOException {
        // 获取或创建用户工作目录
        String workDir = getOrCreateUserWorkDirectory(user);
        Path appsDir = Paths.get(workDir, "applications");
        
        // 创建应用目录
        if (!Files.exists(appsDir)) {
            Files.createDirectories(appsDir);
        }
        
        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String uniqueFilename = appName + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
        Path filePath = appsDir.resolve(uniqueFilename);
        
        // 保存文件
        file.transferTo(filePath.toFile());
        
        // 创建应用记录
        Application app = new Application();
        app.setUser(user);
        app.setName(appName);
        app.setJarFileName(originalFilename);
        app.setJarFilePath(filePath.toString());
        
        return applicationRepository.save(app);
    }

    private String getOrCreateUserWorkDirectory(User user) throws IOException {
        String workDir = user.getWorkDirectory();
        if (workDir == null || workDir.trim().isEmpty()) {
            // 如果没有工作目录，使用默认路径
            workDir = "./workspaces/" + user.getUsername();
            user.setWorkDirectory(workDir);
        }
        
        Path workPath = Paths.get(workDir);
        if (!Files.exists(workPath)) {
            Files.createDirectories(workPath);
        }
        
        return workDir;
    }

    public String getUserWorkDirectory(User user) {
        try {
            return getOrCreateUserWorkDirectory(user);
        } catch (IOException e) {
            throw new RuntimeException("Failed to get user work directory", e);
        }
    }
    
    /**
     * 保存应用配置
     */
    public Application saveApplication(Application app) {
        return applicationRepository.save(app);
    }
    
    /**
     * 启动应用
     */
    public Application startApplication(Long appId) throws IOException {
        Application app = applicationRepository.findById(appId)
            .orElseThrow(() -> new RuntimeException("Application not found"));
        
        if ("RUNNING".equals(app.getStatus())) {
            throw new RuntimeException("Application is already running");
        }
        
        // 分配端口
        PortManagerService.PortAllocation ports = portManagerService.allocatePorts(app.getUser());
        app.setPort(ports.getApplicationPort());
        app.setDebugPort(ports.getDebugPort());
        
        // 创建日志文件
        String logFilePath = createLogFile(app);
        app.setLogFilePath(logFilePath);
        
        // 启动进程（支持JVM参数配置）
        try {
            List<String> command = buildJavaCommand(app, ports);
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            
            // 设置环境变量
            setEnvironmentVariables(processBuilder, app);
            
            processBuilder.redirectErrorStream(true);
            processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(new java.io.File(logFilePath)));
            
            Process process = processBuilder.start();
            app.setProcessId(String.valueOf(process.pid()));
            app.setStatus("RUNNING");
            app.setLastStartedAt(java.time.LocalDateTime.now());
            
            logger.info("应用启动成功: {} (PID: {}, 端口: {}, 调试端口: {})", 
                app.getName(), app.getProcessId(), app.getPort(), app.getDebugPort());
            
        } catch (Exception e) {
            app.setStatus("ERROR");
            logger.error("应用启动失败: {}", app.getName(), e);
            throw new RuntimeException("Failed to start application: " + e.getMessage(), e);
        }
        
        return applicationRepository.save(app);
    }
    
    /**
     * 停止应用
     */
    public Application stopApplication(Long appId) {
        Application app = applicationRepository.findById(appId)
            .orElseThrow(() -> new RuntimeException("Application not found"));
        
        if (!"RUNNING".equals(app.getStatus())) {
            return app;
        }
        
        try {
            if (app.getProcessId() != null) {
                ProcessHandle.of(Long.parseLong(app.getProcessId()))
                    .ifPresent(ProcessHandle::destroy);
            }
        } catch (Exception e) {
            // 忽略停止错误
        }
        
        app.setStatus("STOPPED");
        app.setProcessId(null);
        
        return applicationRepository.save(app);
    }
    
    /**
     * 重启应用
     * 原子性操作：先停止应用，然后重新启动
     */
    public Application restartApplication(Long appId) throws IOException {
        Application app = applicationRepository.findById(appId)
            .orElseThrow(() -> new RuntimeException("Application not found"));
        
        // 记录原始状态
        String originalStatus = app.getStatus();
        
        try {
            // 如果应用正在运行，先停止它
            if ("RUNNING".equals(app.getStatus())) {
                app = stopApplication(appId);
                
                // 等待进程完全停止（最多等待5秒）
                int waitTime = 0;
                while (waitTime < 5000 && isProcessRunning(app.getProcessId())) {
                    Thread.sleep(500);
                    waitTime += 500;
                }
            }
            
            // 重新分配端口
            PortManagerService.PortAllocation ports = portManagerService.allocatePorts(app.getUser());
            app.setPort(ports.getApplicationPort());
            app.setDebugPort(ports.getDebugPort());
            
            // 创建新的日志文件（带有重启时间戳）
            String logFilePath = createRestartLogFile(app);
            app.setLogFilePath(logFilePath);
            
            // 启动进程
            List<String> command = buildJavaCommand(app, ports);
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            
            // 设置环境变量
            setEnvironmentVariables(processBuilder, app);
            
            processBuilder.redirectErrorStream(true);
            processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(new java.io.File(logFilePath)));
            
            Process process = processBuilder.start();
            app.setProcessId(String.valueOf(process.pid()));
            app.setStatus("RUNNING");
            app.setLastStartedAt(java.time.LocalDateTime.now());
            
            logger.info("应用重启成功: {} (PID: {}, 端口: {}, 调试端口: {})", 
                app.getName(), app.getProcessId(), app.getPort(), app.getDebugPort());
            
            return applicationRepository.save(app);
            
        } catch (Exception e) {
            app.setStatus("ERROR");
            applicationRepository.save(app);
            throw new RuntimeException("Failed to restart application: " + e.getMessage(), e);
        }
    }
    
    /**
     * 检查进程是否仍在运行
     */
    private boolean isProcessRunning(String processId) {
        if (processId == null || processId.trim().isEmpty()) {
            return false;
        }
        
        try {
            return ProcessHandle.of(Long.parseLong(processId))
                .map(ProcessHandle::isAlive)
                .orElse(false);
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 为重启创建新的日志文件
     */
    private String createRestartLogFile(Application app) throws IOException {
        String workDir = app.getUser().getWorkDirectory();
        Path logsDir = Paths.get(workDir, "logs");
        
        if (!Files.exists(logsDir)) {
            Files.createDirectories(logsDir);
        }
        
        // 使用时间戳区分重启后的日志文件
        String timestamp = String.valueOf(System.currentTimeMillis());
        String logFilename = app.getName() + "_" + app.getId() + "_restart_" + timestamp + ".log";
        Path logFile = logsDir.resolve(logFilename);
        
        // 添加重启标记到日志文件
        String restartMarker = "\n=== Application Restarted at " + 
            java.time.LocalDateTime.now() + " ===\n";
        Files.writeString(logFile, restartMarker);
        
        return logFile.toString();
    }
    
    /**
     * 构建 Java 启动命令
     */
    private List<String> buildJavaCommand(Application app, PortManagerService.PortAllocation ports) {
        List<String> command = new ArrayList<>();
        
        // 基本 Java 命令
        command.add("java");
        
        // 添加 JVM 参数
        addJvmOptions(command, app);
        
        // 添加 GC 参数
        addGcOptions(command, app);
        
        // 添加系统属性
        addSystemProperties(command, app, ports);
        
        // 添加调试参数
        addDebugOptions(command, app, ports);
        
        // 添加 JMX 参数
        addJmxOptions(command, app);
        
        // JAR 文件或主类
        if (app.getMainClass() != null && !app.getMainClass().trim().isEmpty()) {
            // 使用主类方式启动
            command.add("-cp");
            command.add(app.getJarFilePath());
            command.add(app.getMainClass());
        } else {
            // 使用 JAR 方式启动
            command.add("-jar");
            command.add(app.getJarFilePath());
        }
        
        // 添加程序参数
        addProgramArguments(command, app);
        
        logger.info("构建的Java启动命令: {}", String.join(" ", command));
        return command;
    }
    
    /**
     * 添加 JVM 参数
     */
    private void addJvmOptions(List<String> command, Application app) {
        if (app.getJvmOptions() != null && !app.getJvmOptions().trim().isEmpty()) {
            String[] options = app.getJvmOptions().trim().split("\\s+");
            for (String option : options) {
                if (!option.trim().isEmpty()) {
                    command.add(option.trim());
                }
            }
            logger.debug("添加JVM参数: {}", app.getJvmOptions());
        } else {
            // 默认 JVM 参数
            command.add("-Xms256m");
            command.add("-Xmx512m");
            logger.debug("使用默认JVM参数: -Xms256m -Xmx512m");
        }
    }
    
    /**
     * 添加 GC 参数
     */
    private void addGcOptions(List<String> command, Application app) {
        if (app.getGcOptions() != null && !app.getGcOptions().trim().isEmpty()) {
            String[] options = app.getGcOptions().trim().split("\\s+");
            for (String option : options) {
                if (!option.trim().isEmpty()) {
                    command.add(option.trim());
                }
            }
            logger.debug("添加GC参数: {}", app.getGcOptions());
        }
    }
    
    /**
     * 添加系统属性
     */
    private void addSystemProperties(List<String> command, Application app, PortManagerService.PortAllocation ports) {
        // 服务端口
        command.add("-Dserver.port=" + ports.getApplicationPort());
        
        // 添加环境变量作为系统属性
        if (app.getEnvironmentVariables() != null && !app.getEnvironmentVariables().trim().isEmpty()) {
            try {
                Map<String, String> envVars = objectMapper.readValue(
                    app.getEnvironmentVariables(), 
                    new TypeReference<Map<String, String>>() {}
                );
                
                for (Map.Entry<String, String> entry : envVars.entrySet()) {
                    command.add("-D" + entry.getKey() + "=" + entry.getValue());
                }
                logger.debug("添加环境变量: {}", app.getEnvironmentVariables());
            } catch (Exception e) {
                logger.warn("解析环境变量失败: {}", app.getEnvironmentVariables(), e);
            }
        }
    }
    
    /**
     * 添加调试参数
     */
    private void addDebugOptions(List<String> command, Application app, PortManagerService.PortAllocation ports) {
        command.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=" + ports.getDebugPort());
        logger.debug("添加调试参数: 端口{}", ports.getDebugPort());
    }
    
    /**
     * 添加 JMX 参数
     */
    private void addJmxOptions(List<String> command, Application app) {
        if (app.getEnableJmx() != null && app.getEnableJmx()) {
            int jmxPort = app.getJmxPort() != null ? app.getJmxPort() : 9999;
            
            command.add("-Dcom.sun.management.jmxremote");
            command.add("-Dcom.sun.management.jmxremote.port=" + jmxPort);
            command.add("-Dcom.sun.management.jmxremote.authenticate=false");
            command.add("-Dcom.sun.management.jmxremote.ssl=false");
            
            logger.debug("启用JMX监控: 端口{}", jmxPort);
        }
    }
    
    /**
     * 添加程序参数
     */
    private void addProgramArguments(List<String> command, Application app) {
        if (app.getProgramArguments() != null && !app.getProgramArguments().trim().isEmpty()) {
            String[] args = app.getProgramArguments().trim().split("\\s+");
            for (String arg : args) {
                if (!arg.trim().isEmpty()) {
                    command.add(arg.trim());
                }
            }
            logger.debug("添加程序参数: {}", app.getProgramArguments());
        }
    }
    
    /**
     * 设置环境变量
     */
    private void setEnvironmentVariables(ProcessBuilder processBuilder, Application app) {
        if (app.getEnvironmentVariables() != null && !app.getEnvironmentVariables().trim().isEmpty()) {
            try {
                Map<String, String> envVars = objectMapper.readValue(
                    app.getEnvironmentVariables(), 
                    new TypeReference<Map<String, String>>() {}
                );
                
                Map<String, String> env = processBuilder.environment();
                env.putAll(envVars);
                
                logger.debug("设置环境变量: {}", envVars.keySet());
            } catch (Exception e) {
                logger.warn("设置环境变量失败: {}", app.getEnvironmentVariables(), e);
            }
        }
    }
    
    /**
     * 获取用户的所有应用
     * 使用优化查询避免N+1问题
     */
    public List<Application> getUserApplications(User user) {
        return applicationRepository.findByUserWithUserOrderByCreatedAtDesc(user);
    }
    
    /**
     * 获取用户的所有应用（包含配置信息）
     * 适用于需要显示配置详情的页面
     */
    public List<Application> getUserApplicationsWithConfigs(User user) {
        return applicationRepository.findByUserWithConfigsOrderByCreatedAtDesc(user);
    }
    
    /**
     * 根据ID获取应用
     */
    public Application getApplicationById(Long id) {
        return applicationRepository.findById(id).orElse(null);
    }
    
    /**
     * 删除应用
     */
    public void deleteApplication(Long appId) throws IOException {
        Application app = applicationRepository.findById(appId)
            .orElseThrow(() -> new RuntimeException("Application not found"));
        
        // 先停止应用
        if ("RUNNING".equals(app.getStatus())) {
            stopApplication(appId);
        }
        
        // 删除文件
        if (app.getJarFilePath() != null) {
            Files.deleteIfExists(Paths.get(app.getJarFilePath()));
        }
        
        // 删除日志文件
        if (app.getLogFilePath() != null) {
            Files.deleteIfExists(Paths.get(app.getLogFilePath()));
        }
        
        applicationRepository.delete(app);
    }
    
    private String createLogFile(Application app) throws IOException {
        String workDir = app.getUser().getWorkDirectory();
        Path logsDir = Paths.get(workDir, "logs");
        
        if (!Files.exists(logsDir)) {
            Files.createDirectories(logsDir);
        }
        
        String logFilename = app.getName() + "_" + app.getId() + ".log";
        Path logFile = logsDir.resolve(logFilename);
        
        return logFile.toString();
    }
}