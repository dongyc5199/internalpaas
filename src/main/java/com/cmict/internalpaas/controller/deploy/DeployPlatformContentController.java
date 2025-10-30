package com.cmict.internalpaas.controller.deploy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/deploy-platform")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class DeployPlatformContentController {

    private static final Logger log = LoggerFactory.getLogger(DeployPlatformContentController.class);

    @GetMapping("/content")
    public String content() {
        log.info("Loading deployment platform content page");
        return "admin/deploy-platform-content";
    }
}
