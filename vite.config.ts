import { defineConfig } from "vite";
import legacy from "@vitejs/plugin-legacy";
import federation from "@originjs/vite-plugin-federation";
import { resolve } from "node:path";

export default defineConfig({
    root: resolve(__dirname, "src/main/frontend"),
    plugins: [
        legacy({
            targets: ["defaults", "not IE 11"]
        }),
        federation({
            name: "deploy-platform",
            filename: "deploy-platform-remote.js",
            exposes: {
                "./App": resolve(__dirname, "src/main/frontend/react-app/App.tsx"),
                "./bootstrap": resolve(__dirname, "src/main/frontend/react-app/main.tsx")
            },
            runtime: "classic",
            shared: {
                react: {
                    singleton: true,
                    eager: false,
                    requiredVersion: "^19.0.0"
                },
                "react-dom": {
                    singleton: true,
                    eager: false,
                    requiredVersion: "^19.0.0"
                }
            }
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
        target: "esnext",
        rollupOptions: {
            input: {
                main: resolve(__dirname, "src/main/frontend/main.ts"),
                "deploy-platform": resolve(__dirname, "src/main/frontend/react-app/main.tsx")
            },
            output: {
                entryFileNames: "assets/[name].js",
                chunkFileNames: "assets/[name].js",
                assetFileNames: "assets/[name][extname]"
            }
        }
    },
    esbuild: {
        supported: {
            "top-level-await": true
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
        setupFiles: [resolve(__dirname, "src/main/frontend/tests/setup.ts")],
        include: ["tests/**/*.test.{ts,tsx}"],
        coverage: {
            provider: "v8",
            reporter: ["text", "json", "html"],
            reportsDirectory: resolve(__dirname, "target/vitest-coverage")
        }
    }
});
