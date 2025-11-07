package com.cmict.internalpaas.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * React 应用控制器
 *
 * 处理 React 单页应用的路由
 * 所有 /app/** 路径都会返回 React 应用的入口页面
 * React Router 会在客户端处理具体的路由
 */
@Controller
@RequestMapping("/app")
public class ReactAppController {

    /**
     * React 应用入口
     *
     * 处理所有 /app/** 的路由，返回 React 应用的 index.html
     * 这样 React Router 可以在客户端接管路由
     */
    @GetMapping(value = {"", "/", "/**"})
    public String reactApp() {
        return "forward:/dist/index.html";
    }
}
