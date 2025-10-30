import "@testing-library/jest-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

import {
    __resetDeployPlatformLoader,
    __setRemoteLoaderForTest,
    loadDeployPlatformMfe
} from "../../mfe/deploy-platform-loader";

describe("deploy-platform loader", () => {
    beforeEach(() => {
        __resetDeployPlatformLoader();
        document.body.innerHTML = "";
        __setRemoteLoaderForTest(async () => {
            throw new Error("loader not set");
        });
    });

    it("ignores when container missing", async () => {
        await loadDeployPlatformMfe();
        expect(document.body.children.length).toBe(0);
    });

    it("loads remote module when container present", async () => {
        const mount = vi.fn();
        document.body.innerHTML = '<div id="deploy-platform-root"></div>';

        __setRemoteLoaderForTest(async () => ({
            mount
        }));

        await loadDeployPlatformMfe();

        expect(mount).toHaveBeenCalled();
    });
});
