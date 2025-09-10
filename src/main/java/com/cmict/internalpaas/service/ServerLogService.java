package com.cmict.internalpaas.service;

import com.cmict.internalpaas.dto.LogSearchRequest;
import com.cmict.internalpaas.dto.ServerLogContentDto;
import com.cmict.internalpaas.dto.ServerLogFileDto;
import com.cmict.internalpaas.model.Server;
import com.cmict.internalpaas.model.ServerLogType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 服务器日志服务
 * 提供服务器日志文件的发现、读取、解析和监控功能
 */
@Service
public class ServerLogService {
    
    private static final Logger logger = LoggerFactory.getLogger(ServerLogService.class);
    
    @Autowired
    private RemoteCommandService remoteCommandService;
    
    @Autowired
    private ServerService serverService;
    
    // 日志文件缓存：serverId -> Map<filePath, ServerLogFileDto>
    private final Map<Long, Map<String, ServerLogFileDto>> logFileCache = new ConcurrentHashMap<>();
    
    // 常用时间戳格式
    private static final List<Pattern> TIMESTAMP_PATTERNS = Arrays.asList(
        Pattern.compile("^(\\d{4}-\\d{2}-\\d{2}[T\\s]\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{3})?)"),  // ISO格式
        Pattern.compile("^(\\w{3}\\s+\\d{1,2}\\s+\\d{2}:\\d{2}:\\d{2})"),                    // syslog格式
        Pattern.compile("^(\\d{2}/\\w{3}/\\d{4}:\\d{2}:\\d{2}:\\d{2})"),                     // Apache格式
        Pattern.compile("^(\\d{2}-\\d{2}-\\d{4}\\s+\\d{2}:\\d{2}:\\d{2})")                   // 自定义格式
    );
    
    // 日志级别模式
    private static final Pattern LOG_LEVEL_PATTERN = Pattern.compile("\\b(TRACE|DEBUG|INFO|WARN|WARNING|ERROR|FATAL|CRITICAL|EMERG|ALERT|CRIT|ERR|NOTICE)\\b", Pattern.CASE_INSENSITIVE);
    
    /**
     * 获取服务器的可用日志文件列表
     */
    public List<ServerLogFileDto> getAvailableLogFiles(Long serverId) {
        logger.info("获取服务器 {} 的日志文件列表", serverId);
        
        Optional<Server> serverOpt = serverService.getServerById(serverId);
        if (serverOpt.isEmpty()) {
            logger.warn("服务器不存在: {}", serverId);
            return Collections.emptyList();
        }
        
        Server server = serverOpt.get();
        
        // 检查缓存
        Map<String, ServerLogFileDto> cached = logFileCache.get(serverId);
        if (cached != null && !cached.isEmpty()) {
            logger.debug("从缓存返回服务器 {} 的日志文件列表", serverId);
            return new ArrayList<>(cached.values());
        }
        
        List<ServerLogFileDto> logFiles = new ArrayList<>();
        
        try {
            // 扫描系统日志目录
            logFiles.addAll(scanSystemLogFiles(server));
            
            // 添加预定义的日志类型
            logFiles.addAll(scanPredefinedLogFiles(server));
            
            // 缓存结果
            Map<String, ServerLogFileDto> fileMap = new HashMap<>();
            for (ServerLogFileDto file : logFiles) {
                fileMap.put(file.getPath(), file);
            }
            logFileCache.put(serverId, fileMap);
            
            logger.info("服务器 {} 发现 {} 个日志文件", serverId, logFiles.size());
            return logFiles;
            
        } catch (Exception e) {
            logger.error("获取服务器 {} 日志文件列表失败", serverId, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 扫描系统日志目录
     */
    private List<ServerLogFileDto> scanSystemLogFiles(Server server) {
        List<ServerLogFileDto> files = new ArrayList<>();
        
        try {
            // 扫描 /var/log 目录
            String command = "find /var/log -maxdepth 2 -name \"*.log\" -type f 2>/dev/null | head -50";
            RemoteCommandService.CommandResult result = remoteCommandService.executeCommand(server, command);
            
            if (result.isSuccess()) {
                String[] logPaths = result.getOutput().split("\\n");
                for (String path : logPaths) {
                    if (path.trim().isEmpty()) continue;
                    
                    ServerLogFileDto fileDto = createLogFileDto(server, path.trim());
                    if (fileDto != null) {
                        files.add(fileDto);
                    }
                }
            }
            
            // 添加常见的无扩展名日志文件
            String[] commonFiles = {"/var/log/syslog", "/var/log/messages", "/var/log/secure", "/var/log/auth.log"};
            for (String filePath : commonFiles) {
                if (checkFileExists(server, filePath)) {
                    ServerLogFileDto fileDto = createLogFileDto(server, filePath);
                    if (fileDto != null) {
                        files.add(fileDto);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.warn("扫描系统日志文件失败: {}", e.getMessage());
        }
        
        return files;
    }
    
    /**
     * 扫描预定义日志类型
     */
    private List<ServerLogFileDto> scanPredefinedLogFiles(Server server) {
        List<ServerLogFileDto> files = new ArrayList<>();
        
        for (ServerLogType logType : ServerLogType.values()) {
            if (logType == ServerLogType.CUSTOM) continue;
            
            String path = logType.getDefaultPath();
            if (checkFileExists(server, path)) {
                ServerLogFileDto fileDto = new ServerLogFileDto(
                    path, 
                    extractFileName(path),
                    logType.name(),
                    logType.getDescription()
                );
                
                // 获取文件详细信息
                enrichFileDto(server, fileDto);
                files.add(fileDto);
            }
        }
        
        return files;
    }
    
    /**
     * 创建日志文件DTO
     */
    private ServerLogFileDto createLogFileDto(Server server, String path) {
        if (path == null || path.trim().isEmpty()) return null;
        
        try {
            String fileName = extractFileName(path);
            ServerLogType logType = ServerLogType.inferFromPath(path);
            
            ServerLogFileDto fileDto = new ServerLogFileDto(
                path,
                fileName,
                logType.name(),
                logType.getDescription()
            );
            
            enrichFileDto(server, fileDto);
            return fileDto;
            
        } catch (Exception e) {
            logger.warn("创建日志文件DTO失败: {}", path, e);
            return null;
        }
    }
    
    /**
     * 丰富文件DTO信息
     */
    private void enrichFileDto(Server server, ServerLogFileDto fileDto) {
        try {
            // 获取文件信息: 大小、权限、修改时间
            String command = String.format("ls -la '%s' 2>/dev/null | awk '{print $1\" \"$5\" \"$6\" \"$7\" \"$8}'", fileDto.getPath());
            RemoteCommandService.CommandResult result = remoteCommandService.executeCommand(server, command);
            
            if (result.isSuccess()) {
                String output = result.getOutput().trim();
                String[] parts = output.split("\\s+");
                if (parts.length >= 5) {
                    fileDto.setPermission(parts[0]);
                    try {
                        fileDto.setSize(Long.parseLong(parts[1]));
                    } catch (NumberFormatException e) {
                        fileDto.setSize(0L);
                    }
                    
                    // 设置可读性
                    fileDto.setReadable(parts[0].contains("r"));
                    
                    // 估算行数
                    estimateLineCount(server, fileDto);
                }
            }
            
        } catch (Exception e) {
            logger.warn("丰富文件信息失败: {}", fileDto.getPath(), e);
            fileDto.setReadable(false);
        }
    }
    
    /**
     * 估算文件行数
     */
    private void estimateLineCount(Server server, ServerLogFileDto fileDto) {
        try {
            String command = String.format("wc -l '%s' 2>/dev/null | awk '{print $1}'", fileDto.getPath());
            RemoteCommandService.CommandResult result = remoteCommandService.executeCommand(server, command, 5000);
            
            if (result.isSuccess()) {
                try {
                    fileDto.setEstimatedLines(Integer.parseInt(result.getOutput().trim()));
                } catch (NumberFormatException e) {
                    fileDto.setEstimatedLines(0);
                }
            }
        } catch (Exception e) {
            logger.debug("估算行数失败: {}", fileDto.getPath());
            fileDto.setEstimatedLines(0);
        }
    }
    
    /**
     * 检查文件是否存在
     */
    private boolean checkFileExists(Server server, String filePath) {
        try {
            String command = String.format("test -f '%s' && echo 'exists' || echo 'not found'", filePath);
            RemoteCommandService.CommandResult result = remoteCommandService.executeCommand(server, command, 3000);
            return result.isSuccess() && "exists".equals(result.getOutput().trim());
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获取日志文件内容
     */
    public ServerLogContentDto getLogContent(Long serverId, String filePath, int lines, boolean fromEnd) {
        logger.info("获取服务器 {} 日志文件 {} 的内容，行数: {}, 从末尾: {}", serverId, filePath, lines, fromEnd);
        
        Optional<Server> serverOpt = serverService.getServerById(serverId);
        if (serverOpt.isEmpty()) {
            logger.warn("服务器不存在: {}", serverId);
            return null;
        }
        
        Server server = serverOpt.get();
        ServerLogContentDto contentDto = new ServerLogContentDto(filePath, extractFileName(filePath));
        
        try {
            String command;
            if (fromEnd) {
                command = String.format("tail -n %d '%s' 2>/dev/null", lines, filePath);
            } else {
                command = String.format("head -n %d '%s' 2>/dev/null", lines, filePath);
            }
            
            RemoteCommandService.CommandResult result = remoteCommandService.executeCommand(server, command, 15000);
            
            if (result.isSuccess()) {
                // Set raw content for frontend compatibility
                contentDto.setContent(result.getOutput());
                
                List<ServerLogContentDto.LogLineDto> logLines = parseLogLines(result.getOutput());
                contentDto.setLines(logLines);
                contentDto.setDisplayedLines(logLines.size());
                contentDto.setTotalLines(getTotalLineCount(server, filePath));
                
                if (fromEnd) {
                    contentDto.setStartLine(Math.max(1, contentDto.getTotalLines() - lines + 1));
                    contentDto.setEndLine(contentDto.getTotalLines());
                } else {
                    contentDto.setStartLine(1);
                    contentDto.setEndLine(Math.min(lines, contentDto.getTotalLines()));
                }
                
                contentDto.setHasMore(contentDto.getTotalLines() > lines);
                
                logger.info("成功获取日志内容: {} 行", logLines.size());
                
            } else {
                logger.error("读取日志文件失败: {}", result.getError());
                contentDto.setContent("");
                contentDto.setLines(Collections.emptyList());
            }
            
        } catch (Exception e) {
            logger.error("获取日志内容异常", e);
            contentDto.setContent("");
            contentDto.setLines(Collections.emptyList());
        }
        
        return contentDto;
    }
    
    /**
     * 解析日志行
     */
    private List<ServerLogContentDto.LogLineDto> parseLogLines(String content) {
        List<ServerLogContentDto.LogLineDto> lines = new ArrayList<>();
        
        if (content == null || content.trim().isEmpty()) {
            return lines;
        }
        
        String[] rawLines = content.split("\\n");
        long lineNumber = 1;
        
        for (String rawLine : rawLines) {
            if (rawLine.isEmpty()) continue;
            
            ServerLogContentDto.LogLineDto lineDto = new ServerLogContentDto.LogLineDto(lineNumber++, rawLine);
            
            // 解析时间戳
            parseTimestamp(lineDto);
            
            // 解析日志级别
            parseLogLevel(lineDto);
            
            lines.add(lineDto);
        }
        
        return lines;
    }
    
    /**
     * 解析时间戳
     */
    private void parseTimestamp(ServerLogContentDto.LogLineDto lineDto) {
        for (Pattern pattern : TIMESTAMP_PATTERNS) {
            Matcher matcher = pattern.matcher(lineDto.getContent());
            if (matcher.find()) {
                try {
                    String timestampStr = matcher.group(1);
                    // 这里可以添加更复杂的时间戳解析逻辑
                    // 目前简化处理，实际应用中需要根据不同格式进行解析
                    lineDto.setSource("timestamp_parsed");
                    break;
                } catch (Exception e) {
                    // 解析失败，忽略
                }
            }
        }
    }
    
    /**
     * 解析日志级别
     */
    private void parseLogLevel(ServerLogContentDto.LogLineDto lineDto) {
        Matcher matcher = LOG_LEVEL_PATTERN.matcher(lineDto.getContent());
        if (matcher.find()) {
            String level = matcher.group(1).toUpperCase();
            lineDto.setLevel(level);
        } else {
            lineDto.setLevel("UNKNOWN");
        }
    }
    
    /**
     * 获取文件总行数
     */
    private int getTotalLineCount(Server server, String filePath) {
        try {
            String command = String.format("wc -l '%s' 2>/dev/null | awk '{print $1}'", filePath);
            RemoteCommandService.CommandResult result = remoteCommandService.executeCommand(server, command, 5000);
            
            if (result.isSuccess()) {
                return Integer.parseInt(result.getOutput().trim());
            }
        } catch (Exception e) {
            logger.warn("获取文件行数失败: {}", filePath, e);
        }
        return 0;
    }
    
    /**
     * 提取文件名
     */
    private String extractFileName(String path) {
        if (path == null || path.isEmpty()) return "";
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }
    
    /**
     * 清除服务器的日志文件缓存
     */
    public void clearLogFileCache(Long serverId) {
        logFileCache.remove(serverId);
        logger.info("清除服务器 {} 的日志文件缓存", serverId);
    }
    
    /**
     * 搜索日志内容
     */
    public ServerLogContentDto searchLogContent(Long serverId, String filePath, LogSearchRequest searchRequest) {
        logger.info("搜索服务器 {} 日志文件 {} 内容: {}", serverId, filePath, searchRequest);
        
        Optional<Server> serverOpt = serverService.getServerById(serverId);
        if (serverOpt.isEmpty()) {
            logger.warn("服务器不存在: {}", serverId);
            return null;
        }
        
        Server server = serverOpt.get();
        ServerLogContentDto contentDto = new ServerLogContentDto(filePath, extractFileName(filePath));
        
        try {
            String command = buildSearchCommand(filePath, searchRequest);
            logger.debug("执行搜索命令: {}", command);
            
            RemoteCommandService.CommandResult result = remoteCommandService.executeCommand(server, command, 30000);
            
            if (result.isSuccess()) {
                List<ServerLogContentDto.LogLineDto> logLines = parseSearchResults(result.getOutput(), searchRequest);
                contentDto.setLines(logLines);
                contentDto.setDisplayedLines(logLines.size());
                contentDto.setTotalLines(logLines.size());
                
                // 标记匹配的行
                highlightSearchMatches(logLines, searchRequest);
                
                logger.info("搜索完成: 找到 {} 个匹配结果", logLines.size());
                
            } else {
                logger.error("搜索日志文件失败: {}", result.getError());
                contentDto.setLines(Collections.emptyList());
            }
            
        } catch (Exception e) {
            logger.error("搜索日志内容异常", e);
            contentDto.setLines(Collections.emptyList());
        }
        
        return contentDto;
    }
    
    /**
     * 构建搜索命令
     */
    private String buildSearchCommand(String filePath, LogSearchRequest searchRequest) {
        StringBuilder command = new StringBuilder();
        
        // 基础grep命令
        command.append("grep");
        
        // 正则表达式支持
        if (searchRequest.isRegex()) {
            command.append(" -E");
        } else {
            command.append(" -F"); // 固定字符串搜索
        }
        
        // 大小写敏感性
        if (!searchRequest.isCaseSensitive()) {
            command.append(" -i");
        }
        
        // 显示行号
        command.append(" -n");
        
        // 上下文行数
        if (searchRequest.isIncludeContext() && searchRequest.getContextLines() > 0) {
            command.append(" -C ").append(searchRequest.getContextLines());
        }
        
        // 最大结果数限制
        command.append(" | head -").append(searchRequest.getMaxResults());
        
        // 搜索关键词 - 需要转义
        String keyword = searchRequest.getKeyword().replace("'", "'\"'\"'");
        command.append(" '").append(keyword).append("'");
        
        // 文件路径 - 需要转义
        command.append(" '").append(filePath).append("'");
        
        // 错误处理
        command.append(" 2>/dev/null || echo 'NO_MATCHES'");
        
        return command.toString();
    }
    
    /**
     * 解析搜索结果
     */
    private List<ServerLogContentDto.LogLineDto> parseSearchResults(String output, LogSearchRequest searchRequest) {
        List<ServerLogContentDto.LogLineDto> lines = new ArrayList<>();
        
        if (output == null || output.trim().isEmpty() || "NO_MATCHES".equals(output.trim())) {
            return lines;
        }
        
        String[] rawLines = output.split("\\n");
        
        for (String rawLine : rawLines) {
            if (rawLine.isEmpty()) continue;
            
            // 解析grep输出格式 (行号:内容)
            ServerLogContentDto.LogLineDto lineDto = parseGrepLine(rawLine);
            if (lineDto != null) {
                // 解析时间戳和日志级别
                parseTimestamp(lineDto);
                parseLogLevel(lineDto);
                
                // 检查日志级别过滤
                if (shouldIncludeLine(lineDto, searchRequest)) {
                    lines.add(lineDto);
                }
            }
        }
        
        // 排序
        if ("asc".equalsIgnoreCase(searchRequest.getSortOrder())) {
            lines.sort((a, b) -> Long.compare(a.getLineNumber(), b.getLineNumber()));
        } else {
            lines.sort((a, b) -> Long.compare(b.getLineNumber(), a.getLineNumber()));
        }
        
        return lines;
    }
    
    /**
     * 解析grep输出的单行
     */
    private ServerLogContentDto.LogLineDto parseGrepLine(String grepLine) {
        // grep -n 输出格式: "行号:内容" 或 "行号-内容" (上下文行)
        int colonIndex = grepLine.indexOf(':');
        int dashIndex = grepLine.indexOf('-');
        
        int separatorIndex = -1;
        boolean isContextLine = false;
        
        if (colonIndex > 0 && (dashIndex < 0 || colonIndex < dashIndex)) {
            separatorIndex = colonIndex;
        } else if (dashIndex > 0) {
            separatorIndex = dashIndex;
            isContextLine = true;
        }
        
        if (separatorIndex > 0) {
            try {
                long lineNumber = Long.parseLong(grepLine.substring(0, separatorIndex));
                String content = grepLine.substring(separatorIndex + 1);
                
                ServerLogContentDto.LogLineDto lineDto = new ServerLogContentDto.LogLineDto(lineNumber, content);
                lineDto.setSource(isContextLine ? "context" : "match");
                
                return lineDto;
            } catch (NumberFormatException e) {
                logger.warn("解析grep行号失败: {}", grepLine);
            }
        }
        
        return null;
    }
    
    /**
     * 检查是否应该包含该行
     */
    private boolean shouldIncludeLine(ServerLogContentDto.LogLineDto lineDto, LogSearchRequest searchRequest) {
        // 日志级别过滤
        if (searchRequest.getLogLevel() != null && !searchRequest.getLogLevel().trim().isEmpty()) {
            String requiredLevel = searchRequest.getLogLevel().toUpperCase();
            String lineLevel = lineDto.getLevel();
            
            if (lineLevel != null && !lineLevel.equals(requiredLevel)) {
                return false;
            }
        }
        
        // TODO: 时间范围过滤 (需要更复杂的时间戳解析)
        
        return true;
    }
    
    /**
     * 高亮搜索匹配项
     */
    private void highlightSearchMatches(List<ServerLogContentDto.LogLineDto> lines, LogSearchRequest searchRequest) {
        for (ServerLogContentDto.LogLineDto line : lines) {
            if ("match".equals(line.getSource())) {
                line.setHighlighted(true);
            }
        }
    }
    
    /**
     * 获取日志文件统计信息
     */
    public Map<String, Object> getLogFileStats(Long serverId, String filePath) {
        logger.info("获取服务器 {} 日志文件 {} 统计信息", serverId, filePath);
        
        Optional<Server> serverOpt = serverService.getServerById(serverId);
        if (serverOpt.isEmpty()) {
            logger.warn("服务器不存在: {}", serverId);
            return Collections.emptyMap();
        }
        
        Server server = serverOpt.get();
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 文件基本信息
            String basicInfoCommand = String.format("ls -la '%s' 2>/dev/null", filePath);
            RemoteCommandService.CommandResult basicResult = remoteCommandService.executeCommand(server, basicInfoCommand);
            
            if (basicResult.isSuccess()) {
                String[] parts = basicResult.getOutput().trim().split("\\s+");
                if (parts.length >= 5) {
                    stats.put("permissions", parts[0]);
                    stats.put("size", parts[4]);
                    stats.put("lastModified", String.join(" ", Arrays.copyOfRange(parts, 5, parts.length)));
                }
            }
            
            // 行数统计
            String lineCountCommand = String.format("wc -l '%s' 2>/dev/null | awk '{print $1}'", filePath);
            RemoteCommandService.CommandResult lineResult = remoteCommandService.executeCommand(server, lineCountCommand);
            
            if (lineResult.isSuccess()) {
                stats.put("totalLines", lineResult.getOutput().trim());
            }
            
            // 日志级别统计
            String levelStatsCommand = String.format(
                "grep -oiE '\\b(ERROR|WARN|INFO|DEBUG|TRACE)\\b' '%s' 2>/dev/null | sort | uniq -c | sort -nr",
                filePath
            );
            RemoteCommandService.CommandResult levelResult = remoteCommandService.executeCommand(server, levelStatsCommand);
            
            if (levelResult.isSuccess()) {
                Map<String, Integer> levelStats = new HashMap<>();
                String[] levelLines = levelResult.getOutput().split("\\n");
                
                for (String levelLine : levelLines) {
                    if (!levelLine.trim().isEmpty()) {
                        String[] parts = levelLine.trim().split("\\s+");
                        if (parts.length == 2) {
                            try {
                                int count = Integer.parseInt(parts[0]);
                                String level = parts[1].toUpperCase();
                                levelStats.put(level, count);
                            } catch (NumberFormatException e) {
                                // 忽略解析错误
                            }
                        }
                    }
                }
                
                stats.put("logLevelStats", levelStats);
            }
            
            stats.put("success", true);
            
        } catch (Exception e) {
            logger.error("获取日志文件统计信息失败", e);
            stats.put("success", false);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * 清除所有缓存
     */
    public void clearAllCache() {
        logFileCache.clear();
        logger.info("清除所有日志文件缓存");
    }
}