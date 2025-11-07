package com.cmict.internalpaas.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 *
 * 配置静态资源处理和视图控制器
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 配置静态资源处理
     *
     * 确保 React 应用的静态资源可以正确访问
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // React 应用的静态资源
        registry.addResourceHandler("/dist/**")
                .addResourceLocations("classpath:/static/dist/");

        // React 应用的 assets 目录（Vite 生成的 JS/CSS 文件）
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/dist/assets/");

        // 其他静态资源（如果需要）
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }
}
