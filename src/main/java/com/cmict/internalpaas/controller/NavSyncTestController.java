package com.cmict.internalpaas.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 导航同步测试页面控制器
 * 提供E2E测试页面用于验证主应用与React应用之间的导航同步
 */
@Controller
@RequestMapping("/debug")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class NavSyncTestController {

    private static final Logger log = LoggerFactory.getLogger(NavSyncTestController.class);

    /**
     * 导航同步 E2E 测试页面
     * @return 测试页面模板路径
     */
    @GetMapping("/nav-sync-e2e-test")
    public String navSyncE2ETest() {
        log.info("Loading navigation sync E2E test page");
        return "debug/nav-sync-e2e-test";
    }
}
