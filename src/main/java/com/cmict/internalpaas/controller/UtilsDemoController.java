package com.cmict.internalpaas.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 工具函数演示控制器
 * 用于展示前端重构后的工具模块功能
 */
@Controller
@RequestMapping("/demo")
public class UtilsDemoController {

    /**
     * 工具函数演示页面
     * @return 演示页面模板
     */
    @GetMapping("/utils")
    public String utilsDemo() {
        return "utils-demo";
    }
}
