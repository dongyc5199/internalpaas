package com.cmict.internalpaas.controller.deploy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Root-level controller for direct URL access to React application routes
 * Serves the deploy-platform-content template for client-side routing
 */
@Controller
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class DeployPlatformRootController {

    private static final Logger log = LoggerFactory.getLogger(DeployPlatformRootController.class);

    /**
     * Catch-all route for /overview and sub-routes
     * Supports deep linking and direct URL access to React app
     */
    @GetMapping({"/overview", "/overview/**"})
    public String overview() {
        log.info("Direct access to /overview - serving React app container");
        return "admin/deploy-platform-content";
    }

    /**
     * Catch-all route for /releases and sub-routes
     * Supports deep linking and direct URL access to React app
     */
    @GetMapping({"/releases", "/releases/**"})
    public String releases() {
        log.info("Direct access to /releases - serving React app container");
        return "admin/deploy-platform-content";
    }

    /**
     * Catch-all route for /settings/policies and sub-routes
     * Supports deep linking and direct URL access to React app
     */
    @GetMapping({"/settings/policies", "/settings/policies/**"})
    public String policies() {
        log.info("Direct access to /settings/policies - serving React app container");
        return "admin/deploy-platform-content";
    }
}
