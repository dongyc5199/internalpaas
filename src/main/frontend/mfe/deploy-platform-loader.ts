type DeployPlatformModule = {
    mountDeployPlatform?: () => void;
    default?: () => void;
    mount?: () => void;
};

const ENTRY_URL = "/dist/assets/deploy-platform.js";

let loadingPromise: Promise<void> | null = null;

const runMount = (mod: DeployPlatformModule): void => {
    const candidate = mod.mountDeployPlatform || mod.mount || mod.default;
    if (typeof candidate === "function") {
        candidate();
    } else {
        console.warn("[deploy-platform] module loaded but no mount function found");
    }
};

export const loadDeployPlatformMfe = async (): Promise<void> => {
    const container = document.getElementById("deploy-platform-root");
    if (!container) {
        return;
    }

    if (container.hasAttribute("data-react-mounted")) {
        return;
    }

    if (!loadingPromise) {
        loadingPromise = import(/* @vite-ignore */ ENTRY_URL)
            .then((mod) => {
                runMount(mod as DeployPlatformModule);
            })
            .catch((error) => {
                console.error("[deploy-platform] failed to load entry", error);
            })
            .finally(() => {
                loadingPromise = null;
            });
    }

    await loadingPromise;
};

export const __resetDeployPlatformLoader = (): void => {
    loadingPromise = null;
};

export const __setRemoteLoaderForTest = (_: () => Promise<DeployPlatformModule>): void => {
    console.warn("__setRemoteLoaderForTest is deprecated with direct entry loading");
};
