package com.cmict.internalpaas.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
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
        } else if (message != null && message.contains("No available ports")) {
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
        } else {
            title = "操作失败";
            userMessage = message != null ? message : "系统发生未知错误";
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
     * 处理AJAX请求的异常响应
     */
    private ResponseEntity<Map<String, Object>> handleAjaxException(Exception ex, HttpStatus status) {
        return handleAjaxException(ex, status, ex.getMessage());
    }

    /**
     * 处理AJAX请求的异常响应（自定义消息）
     */
    private ResponseEntity<Map<String, Object>> handleAjaxException(Exception ex, HttpStatus status, String userMessage) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", userMessage);
        errorResponse.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.status(status).body(errorResponse);
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

    /**
     * 处理带有RedirectAttributes的页面异常
     */
    private String handlePageExceptionWithRedirect(String redirectUrl, String message, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", message);
        return "redirect:" + redirectUrl;
    }
}