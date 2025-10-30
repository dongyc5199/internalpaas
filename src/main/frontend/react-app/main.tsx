import React from "react";
import ReactDOM from "react-dom/client";

import { App } from "./App";
import { initTokenBridge } from "./token-bridge";

declare global {
    interface Window {
        __DEPLOY_PLATFORM_DEFER_AUTO_MOUNT__?: boolean;
    }
}

const containerId = "deploy-platform-root";
let root: ReactDOM.Root | null = null;
let autoMounted = false;

export const mountDeployPlatform = (): void => {
    const container = document.getElementById(containerId);
    if (!container) {
        if (import.meta.env.DEV) {
            console.warn(`[deploy-platform] missing mount node #${containerId}`);
        }
        return;
    }

    if (!root) {
        root = ReactDOM.createRoot(container);
    }

    if (!container.hasAttribute("data-react-mounted")) {
        root.render(
            <React.StrictMode>
                <App />
            </React.StrictMode>
        );
        container.setAttribute("data-react-mounted", "true");
    }

    autoMounted = true;
};

const bootstrap = (): void => {
    initTokenBridge({
        onTokenError: (reason) => {
            if (import.meta.env.DEV) {
                console.warn("[deploy-platform] token bridge error", reason);
            }
        }
    });

    if (window.__DEPLOY_PLATFORM_DEFER_AUTO_MOUNT__) {
        return;
    }

    if (document.readyState === "loading") {
        document.addEventListener(
            "DOMContentLoaded",
            () => {
                mountDeployPlatform();
            },
            { once: true }
        );
    } else {
        mountDeployPlatform();
    }
};

if (typeof window !== "undefined" && !autoMounted) {
    bootstrap();
}

export default mountDeployPlatform;
export { mountDeployPlatform as mount };

