package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.test.ServerManagementTester;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 测试控制器
 * 提供服务器管理功能的测试接口
 */
@Controller
@RequestMapping("/test")
public class TestController {

    @Autowired
    private ServerManagementTester serverManagementTester;

    /**
     * 显示服务器管理测试页面
     */
    @GetMapping("/server-management-ui")
    public String serverManagementTestPage() {
        return "server-management-test";
    }

    /**
     * 测试服务器管理功能
     */
    @GetMapping("/server-management")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testServerManagement() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 捕获控制台输出
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(baos));
            
            // 运行测试
            serverManagementTester.runAllTests();
            
            // 恢复原始输出流
            System.setOut(originalOut);
            
            // 获取测试输出
            String testOutput = baos.toString("UTF-8");
            
            response.put("success", true);
            response.put("message", "服务器管理功能测试完成");
            response.put("testOutput", testOutput);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "测试过程中发生错误: " + e.getMessage());
            response.put("error", e.toString());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        }
    }
}