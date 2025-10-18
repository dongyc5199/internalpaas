package com.cmict.internalpaas.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 统一处理应用中的异常，提供用户友好的错误信息
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理静态资源未找到异常
     * 忽略浏览器开发工具的特定资源请求（如Chrome DevTools）
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public Object handleNoResourceFoundException(NoResourceFoundException ex, HttpServletRequest request) {
        String resourcePath = ex.getResourcePath();
        
        // 忽略浏览器开发工具相关的资源请求，不记录错误日志
        if (resourcePath != null && 
            (resourcePath.contains(".well-known/") || 
             resourcePath.contains("com.chrome.devtools") ||
             resourcePath.contains("favicon.ico"))) {
            logger.debug("忽略浏览器工具资源请求: {}", resourcePath);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        
        // 其他资源未找到的情况记录警告日志
        logger.warn("静态资源未找到: {}", resourcePath);
        
        if (isAjaxRequest(request)) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "请求的资源不存在");
            errorResponse.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorResponse);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * 统一处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public Object handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        logger.error("运行时异常: {}", ex.getMessage(), ex);
        
        String message = ex.getMessage();
        String userMessage;
        String title;
        
        // 根据异常消息类型进行分类处理
        if (message != null && message.contains("Failed to start application")) {
            title = "应用启动失败";
            userMessage = "应用启动失败，请检查JAR文件是否正确或端口是否被占用";
        } else if (message != null && message.contains("Failed to restart application")) {
            title = "应用重启失败";
            userMessage = "应用重启失败，请尝试手动停止后重新启动";
        } else if (message != null && (message.contains("No available ports") || message.contains("端口范围"))) {
            title = "端口分配失败";
            userMessage = "无可用端口，请停止一些应用后重试或联系管理员扩展端口范围";
        } else if (message != null && message.contains("User not found")) {
            title = "用户认证失效";
            userMessage = "用户认证失效，请重新登录";
            
            if (isAjaxRequest(request)) {
                return handleAjaxException(ex, HttpStatus.UNAUTHORIZED, userMessage);
            } else {
                return "redirect:/login?error=session_expired";
            }
        } else if (message != null && message.contains("Application not found")) {
            title = "应用不存在";
            userMessage = "应用不存在或已被删除";
        } else if (message != null && message.contains("认证失败")) {
            title = "SSH认证失败";
            userMessage = "SSH认证失败，请检查用户名和密码";
        } else if (message != null && message.contains("连接超时")) {
            title = "连接超时";
            userMessage = "SSH连接超时，请检查网络连接和服务器状态";
        } else if (message != null && message.contains("权限不足")) {
            title = "权限不足";
            userMessage = message;
        } else if (message != null && message.contains("文件上传失败")) {
            title = "文件上传失败";
            userMessage = "文件上传失败，请检查文件大小和格式";
        } else if (message != null && message.contains("端口分配失败")) {
            title = "端口分配失败";
            userMessage = message;
        } else if (message != null && (message.contains("SSH配置") || message.contains("SSH config"))) {
            title = "SSH配置解析失败";
            userMessage = message;
        } else if (message != null && message.contains("服务器已存在")) {
            title = "服务器导入失败";
            userMessage = message;
        } else if (message != null && message.contains("验证失败")) {
            title = "数据验证失败";
            userMessage = message;
        } else if (message != null && message.contains("数据库")) {
            title = "数据库错误";
            userMessage = "数据库操作失败，请稍后重试";
        } else {
            title = "操作失败";
            userMessage = message != null && !message.trim().isEmpty() ? message : "系统发生未知错误，请稍后重试";
        }
        
        if (isAjaxRequest(request)) {
            return handleAjaxException(ex, HttpStatus.INTERNAL_SERVER_ERROR, userMessage);
        } else {
            return handlePageException(ex, title, userMessage, request);
        }
    }

    /**
     * 处理IO异常（文件操作相关）
     */
    @ExceptionHandler(IOException.class)
    public Object handleIOException(IOException ex, HttpServletRequest request) {
        logger.error("IO异常: {}", ex.getMessage(), ex);
        
        String userMessage = "文件操作失败，请检查文件权限或磁盘空间";
        
        if (isAjaxRequest(request)) {
            return handleAjaxException(ex, HttpStatus.INTERNAL_SERVER_ERROR, userMessage);
        } else {
            return handlePageException(ex, "文件操作失败", userMessage, request);
        }
    }
    
    /**
     * 处理网络连接异常（SSH、端口检查等）
     */
    @ExceptionHandler({java.net.ConnectException.class, java.net.SocketTimeoutException.class})
    public Object handleNetworkException(Exception ex, HttpServletRequest request) {
        logger.error("网络连接异常: {}", ex.getMessage(), ex);
        
        String userMessage;
        if (ex instanceof java.net.SocketTimeoutException) {
            userMessage = "网络连接超时，请检查网络连接或服务器状态";
        } else {
            userMessage = "无法连接到目标服务器，请检查网络配置和服务器状态";
        }
        
        if (isAjaxRequest(request)) {
            return handleAjaxException(ex, HttpStatus.BAD_GATEWAY, userMessage);
        } else {
            return handlePageException(ex, "网络连接错误", userMessage, request);
        }
    }
    
    /**
     * 处理安全相关异常
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public Object handleSecurityException(org.springframework.security.access.AccessDeniedException ex, HttpServletRequest request) {
        logger.warn("访问被拒绝: {}", ex.getMessage());

        if (isAjaxRequest(request)) {
            // 为API请求返回标准的access_denied错误
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "access_denied");
            errorResponse.put("message", "访问被拒绝，权限不足");
            errorResponse.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorResponse);
        } else {
            return "redirect:/login?error=access_denied";
        }
    }
    
    /**
     * 处理数据验证异常
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public Object handleValidationException(RuntimeException ex, HttpServletRequest request) {
        logger.warn("数据验证错误: {}", ex.getMessage(), ex);
        
        String message = ex.getMessage();
        String userMessage;
        
        if (message != null && message.contains("端口")) {
            userMessage = "端口配置错误: " + message;
        } else if (message != null && message.contains("应用")) {
            userMessage = "应用状态错误: " + message;
        } else {
            userMessage = "输入参数错误: " + (message != null ? message : "请检查输入内容");
        }
        
        if (isAjaxRequest(request)) {
            return handleAjaxException(ex, HttpStatus.BAD_REQUEST, userMessage);
        } else {
            return handlePageException(ex, "参数错误", userMessage, request);
        }
    }

    /**
     * 处理文件上传大小超限异常
     * SSH配置文件上传时会触发此异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Object handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        logger.warn("文件上传大小超限: {}", ex.getMessage());

        long maxSize = ex.getMaxUploadSize();
        String userMessage;

        if (maxSize > 0) {
            // 转换为MB
            long maxSizeMB = maxSize / (1024 * 1024);
            userMessage = String.format("文件大小超过限制，最大允许 %d MB", maxSizeMB);
        } else {
            userMessage = "文件大小超过限制，请选择较小的文件";
        }

        if (isAjaxRequest(request)) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", true);
            errorResponse.put("message", userMessage);
            errorResponse.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorResponse);
        } else {
            return handlePageException(ex, "文件上传失败", userMessage, request);
        }
    }

    /**
     * 处理所有其他未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public Object handleAllException(Exception ex, HttpServletRequest request) {
        logger.error("未处理的异常: {}", ex.getMessage(), ex);

        String userMessage = "系统发生未知错误，请稍后重试或联系管理员";

        if (isAjaxRequest(request)) {
            return handleAjaxException(ex, HttpStatus.INTERNAL_SERVER_ERROR, userMessage);
        } else {
            return handlePageException(ex, "系统错误", userMessage, request);
        }
    }

    /**
     * 判断是否是AJAX请求
     */
    private boolean isAjaxRequest(HttpServletRequest request) {
        String ajaxHeader = request.getHeader("X-Requested-With");
        String contentType = request.getContentType();
        String accept = request.getHeader("Accept");
        
        return "XMLHttpRequest".equals(ajaxHeader) ||
               (contentType != null && contentType.contains("application/json")) ||
               (accept != null && accept.contains("application/json"));
    }

    /**
     * 处理AJAX请求的异常响应（自定义消息）
     */
    private ResponseEntity<Map<String, Object>> handleAjaxException(Exception ex, HttpStatus status, String userMessage) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", userMessage);
        errorResponse.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    /**
     * 处理页面请求的异常响应
     */
    private ModelAndView handlePageException(Exception ex, String title, String message, HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        String redirectUrl = referer != null ? referer : "/";
        
        // 检查是否已经包含参数
        String separator = redirectUrl.contains("?") ? "&" : "?";
        redirectUrl += separator + "error=" + java.net.URLEncoder.encode(message, java.nio.charset.StandardCharsets.UTF_8);
        
        ModelAndView modelAndView = new ModelAndView("redirect:" + redirectUrl);
        
        return modelAndView;
    }

}