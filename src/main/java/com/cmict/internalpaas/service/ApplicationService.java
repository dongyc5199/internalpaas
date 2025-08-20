package com.cmict.internalpaas.service;

import com.cmict.internalpaas.model.Application;
import com.cmict.internalpaas.model.User;
import com.cmict.internalpaas.repository.ApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class ApplicationService {

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
        
        // 启动进程（简化版，实际生产环境需要更复杂的进程管理）
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                "java",
                "-jar",
                "-Dserver.port=" + ports.getApplicationPort(),
                "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=" + ports.getDebugPort(),
                app.getJarFilePath()
            );
            
            processBuilder.redirectErrorStream(true);
            processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(new java.io.File(logFilePath)));
            
            Process process = processBuilder.start();
            app.setProcessId(String.valueOf(process.pid()));
            app.setStatus("RUNNING");
            
        } catch (Exception e) {
            app.setStatus("ERROR");
            throw new RuntimeException("Failed to start application", e);
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
     * 获取用户的所有应用
     */
    public List<Application> getUserApplications(User user) {
        return applicationRepository.findByUserOrderByCreatedAtDesc(user);
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