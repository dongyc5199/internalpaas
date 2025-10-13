import { defineConfig } from "vite";
import legacy from "@vitejs/plugin-legacy";
import { resolve } from "node:path";

export default defineConfig({
    root: resolve(__dirname, "src/main/frontend"),
    plugins: [
        legacy({
            targets: ["defaults", "not IE 11"]
        })
    ],
    resolve: {
        alias: {
            "@": resolve(__dirname, "src/main/frontend")
        }
    },
    build: {
        outDir: resolve(__dirname, "src/main/resources/static/dist"),
        emptyOutDir: false,
        sourcemap: true,
        manifest: true,
        rollupOptions: {
            input: {
                main: resolve(__dirname, "src/main/frontend/main.ts")
            },
            output: {
                entryFileNames: "assets/[name].js",
                chunkFileNames: "assets/[name].js",
                assetFileNames: "assets/[name][extname]"
            }
        }
    },
    server: {
        port: 5173,
        strictPort: true,
        open: false
    },
    test: {
        globals: true,
        environment: "jsdom",
        include: ["tests/**/*.test.ts"],
        coverage: {
            provider: "v8",
            reporter: ["text", "json", "html"],
            reportsDirectory: resolve(__dirname, "target/vitest-coverage")
        }
    }
});
