/**
 * HTTP工具模块测试套件
 */

import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { http } from "../utils/http";

// Mock fetch
let mockFetch: typeof global.fetch;

describe("HTTP Client", () => {
    beforeEach(() => {
        // 保存原始fetch
        mockFetch = global.fetch;

        // Mock CSRF meta标签
        document.head.innerHTML = `
            <meta name="_csrf" content="test-csrf-token">
            <meta name="_csrf_header" content="X-CSRF-TOKEN">
        `;

        // Mock fetch
        global.fetch = vi.fn();
    });

    afterEach(() => {
        // 恢复fetch
        global.fetch = mockFetch;
        vi.clearAllTimers();
    });

    describe("GET请求", () => {
        it("应该发送基本的GET请求", async () => {
            const mockData = { id: 1, name: "test" };
            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                status: 200,
                statusText: "OK",
                headers: new Headers({ "Content-Type": "application/json" }),
                json: async () => mockData
            });

            const response = await http.get("/api/test");

            expect(global.fetch).toHaveBeenCalledWith(
                "/api/test",
                expect.objectContaining({
                    method: "GET"
                })
            );
            expect(response.data).toEqual(mockData);
            expect(response.status).toBe(200);
        });

        it("应该正确处理查询参数", async () => {
            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                status: 200,
                statusText: "OK",
                headers: new Headers({ "Content-Type": "application/json" }),
                json: async () => ({})
            });

            await http.get("/api/test", {
                params: { id: 1, name: "test", active: true }
            });

            expect(global.fetch).toHaveBeenCalledWith(
                expect.stringContaining("id=1"),
                expect.anything()
            );
            expect(global.fetch).toHaveBeenCalledWith(
                expect.stringContaining("name=test"),
                expect.anything()
            );
            expect(global.fetch).toHaveBeenCalledWith(
                expect.stringContaining("active=true"),
                expect.anything()
            );
        });

        it("应该正确处理已有查询参数的URL", async () => {
            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                status: 200,
                statusText: "OK",
                headers: new Headers({ "Content-Type": "application/json" }),
                json: async () => ({})
            });

            await http.get("/api/test?existing=param", {
                params: { new: "param" }
            });

            expect(global.fetch).toHaveBeenCalledWith(
                "/api/test?existing=param&new=param",
                expect.anything()
            );
        });

        it("应该处理文本响应", async () => {
            const mockText = "Hello World";
            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                status: 200,
                statusText: "OK",
                headers: new Headers({ "Content-Type": "text/plain" }),
                text: async () => mockText
            });

            const response = await http.get("/api/text");

            expect(response.data).toBe(mockText);
        });

        it("应该处理Blob响应", async () => {
            const mockBlob = new Blob(["test"]);
            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                status: 200,
                statusText: "OK",
                headers: new Headers({ "Content-Type": "application/pdf" }),
                blob: async () => mockBlob
            });

            const response = await http.get("/api/file");

            expect(response.data).toBeInstanceOf(Blob);
        });
    });

    describe("POST请求", () => {
        it("应该发送POST请求并自动添加CSRF令牌", async () => {
            const mockData = { success: true };
            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                status: 200,
                statusText: "OK",
                headers: new Headers({ "Content-Type": "application/json" }),
                json: async () => mockData
            });

            const postData = { name: "test" };
            await http.post("/api/create", postData);

            expect(global.fetch).toHaveBeenCalledWith(
                "/api/create",
                expect.objectContaining({
                    method: "POST",
                    body: JSON.stringify(postData)
                })
            );

            // 验证CSRF token被添加
            const callArgs = (global.fetch as any).mock.calls[0];
            const headers = callArgs[1].headers;
            expect(headers.get("X-CSRF-TOKEN")).toBe("test-csrf-token");
        });

        it("应该自动设置Content-Type为application/json", async () => {
            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                status: 200,
                statusText: "OK",
                headers: new Headers({ "Content-Type": "application/json" }),
                json: async () => ({})
            });

            await http.post("/api/create", { name: "test" });

            const callArgs = (global.fetch as any).mock.calls[0];
            const headers = callArgs[1].headers;
            expect(headers.get("Content-Type")).toBe("application/json");
        });

        it("应该处理没有请求体的POST请求", async () => {
            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                status: 200,
                statusText: "OK",
                headers: new Headers({ "Content-Type": "application/json" }),
                json: async () => ({})
            });

            await http.post("/api/action");

            const callArgs = (global.fetch as any).mock.calls[0];
            expect(callArgs[1].body).toBeUndefined();
        });
    });

    describe("PUT请求", () => {
        it("应该发送PUT请求", async () => {
            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                status: 200,
                statusText: "OK",
                headers: new Headers({ "Content-Type": "application/json" }),
                json: async () => ({})
            });

            const putData = { id: 1, name: "updated" };
            await http.put("/api/update/1", putData);

            expect(global.fetch).toHaveBeenCalledWith(
                "/api/update/1",
                expect.objectContaining({
                    method: "PUT",
                    body: JSON.stringify(putData)
                })
            );
        });

        it("应该为PUT请求添加CSRF令牌", async () => {
            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                status: 200,
                statusText: "OK",
                headers: new Headers({ "Content-Type": "application/json" }),
                json: async () => ({})
            });

            await http.put("/api/update/1", {});

            const callArgs = (global.fetch as any).mock.calls[0];
            const headers = callArgs[1].headers;
            expect(headers.get("X-CSRF-TOKEN")).toBe("test-csrf-token");
        });
    });

    describe("DELETE请求", () => {
        it("应该发送DELETE请求", async () => {
            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                status: 204,
                statusText: "No Content",
                headers: new Headers(),
                json: async () => ({}),
                text: async () => "",
                blob: async () => new Blob()
            });

            await http.delete("/api/delete/1");

            expect(global.fetch).toHaveBeenCalledWith(
                "/api/delete/1",
                expect.objectContaining({
                    method: "DELETE"
                })
            );
        });

        it("应该为DELETE请求添加CSRF令牌", async () => {
            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                status: 204,
                statusText: "No Content",
                headers: new Headers(),
                json: async () => ({}),
                text: async () => "",
                blob: async () => new Blob()
            });

            await http.delete("/api/delete/1");

            const callArgs = (global.fetch as any).mock.calls[0];
            const headers = callArgs[1].headers;
            expect(headers.get("X-CSRF-TOKEN")).toBe("test-csrf-token");
        });
    });

    describe("PATCH请求", () => {
        it("应该发送PATCH请求", async () => {
            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                status: 200,
                statusText: "OK",
                headers: new Headers({ "Content-Type": "application/json" }),
                json: async () => ({})
            });

            const patchData = { name: "patched" };
            await http.patch("/api/patch/1", patchData);

            expect(global.fetch).toHaveBeenCalledWith(
                "/api/patch/1",
                expect.objectContaining({
                    method: "PATCH",
                    body: JSON.stringify(patchData)
                })
            );
        });
    });

    describe("错误处理", () => {
        it("应该处理HTTP错误状态码", async () => {
            (global.fetch as any).mockResolvedValueOnce({
                ok: false,
                status: 404,
                statusText: "Not Found"
            });

            await expect(http.get("/api/notfound")).rejects.toThrow(
                "HTTP Error 404"
            );
        });

        it("应该处理401未授权错误", async () => {
            (global.fetch as any).mockResolvedValueOnce({
                ok: false,
                status: 401,
                statusText: "Unauthorized"
            });

            await expect(http.get("/api/protected")).rejects.toThrow();
        });

        it("应该处理500服务器错误", async () => {
            (global.fetch as any).mockResolvedValueOnce({
                ok: false,
                status: 500,
                statusText: "Internal Server Error"
            });

            await expect(http.get("/api/error")).rejects.toThrow();
        });

        it("应该处理网络错误", async () => {
            (global.fetch as any).mockRejectedValueOnce(
                new Error("Network error")
            );

            await expect(http.get("/api/test")).rejects.toThrow(
                "Network error"
            );
        });

        it("应该处理超时错误", async () => {
            // Mock fetch to throw AbortError
            (global.fetch as any).mockRejectedValueOnce(
                Object.assign(new Error("The operation was aborted"), {
                    name: "AbortError"
                })
            );

            await expect(http.get("/api/slow", { timeout: 1000 })).rejects.toThrow(
                "请求超时"
            );
        });
    });

    describe("CSRF令牌处理", () => {
        it("应该从meta标签获取CSRF令牌", async () => {
            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                status: 200,
                statusText: "OK",
                headers: new Headers({ "Content-Type": "application/json" }),
                json: async () => ({})
            });

            await http.post("/api/test", {});

            const callArgs = (global.fetch as any).mock.calls[0];
            const headers = callArgs[1].headers;
            expect(headers.get("X-CSRF-TOKEN")).toBe("test-csrf-token");
        });

        it("应该在CSRF令牌不存在时仍能正常工作", async () => {
            // 移除CSRF meta标签
            document.head.innerHTML = "";

            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                status: 200,
                statusText: "OK",
                headers: new Headers({ "Content-Type": "application/json" }),
                json: async () => ({})
            });

            await expect(http.post("/api/test", {})).resolves.toBeDefined();
        });

        it("GET请求不应该添加CSRF令牌", async () => {
            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                status: 200,
                statusText: "OK",
                headers: new Headers({ "Content-Type": "application/json" }),
                json: async () => ({})
            });

            await http.get("/api/test");

            const callArgs = (global.fetch as any).mock.calls[0];
            const headers = callArgs[1].headers;
            expect(headers.has("X-CSRF-TOKEN")).toBe(false);
        });
    });

    describe("自定义配置", () => {
        it("应该支持自定义请求头", async () => {
            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                status: 200,
                statusText: "OK",
                headers: new Headers({ "Content-Type": "application/json" }),
                json: async () => ({})
            });

            await http.get("/api/test", {
                headers: {
                    "X-Custom-Header": "custom-value"
                }
            });

            const callArgs = (global.fetch as any).mock.calls[0];
            const headers = callArgs[1].headers;
            expect(headers.get("X-Custom-Header")).toBe("custom-value");
        });

        it("应该支持自定义超时时间", async () => {
            // Mock fetch to throw AbortError
            (global.fetch as any).mockRejectedValueOnce(
                Object.assign(new Error("The operation was aborted"), {
                    name: "AbortError"
                })
            );

            await expect(http.get("/api/test", { timeout: 5000 })).rejects.toThrow();
        });

        it("应该支持禁用超时", async () => {
            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                status: 200,
                statusText: "OK",
                headers: new Headers({ "Content-Type": "application/json" }),
                json: async () => ({})
            });

            await http.get("/api/test", { timeout: 0 });

            const callArgs = (global.fetch as any).mock.calls[0];
            expect(callArgs[1].signal).toBeUndefined();
        });
    });

    describe("响应类型处理", () => {
        it("应该正确解析JSON响应", async () => {
            const mockData = { id: 1, name: "test", items: [1, 2, 3] };
            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                status: 200,
                statusText: "OK",
                headers: new Headers({ "Content-Type": "application/json" }),
                json: async () => mockData
            });

            const response = await http.get("/api/test");

            expect(response.data).toEqual(mockData);
        });

        it("应该正确解析纯文本响应", async () => {
            const mockText = "Plain text response";
            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                status: 200,
                statusText: "OK",
                headers: new Headers({ "Content-Type": "text/plain" }),
                text: async () => mockText
            });

            const response = await http.get("/api/text");

            expect(response.data).toBe(mockText);
        });

        it("应该正确解析HTML响应", async () => {
            const mockHtml = "<html><body>Test</body></html>";
            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                status: 200,
                statusText: "OK",
                headers: new Headers({ "Content-Type": "text/html" }),
                text: async () => mockHtml
            });

            const response = await http.get("/api/html");

            expect(response.data).toBe(mockHtml);
        });

        it("应该正确处理二进制响应", async () => {
            const mockBlob = new Blob(["binary data"], {
                type: "application/octet-stream"
            });
            (global.fetch as any).mockResolvedValueOnce({
                ok: true,
                status: 200,
                statusText: "OK",
                headers: new Headers({
                    "Content-Type": "application/octet-stream"
                }),
                blob: async () => mockBlob
            });

            const response = await http.get("/api/binary");

            expect(response.data).toBeInstanceOf(Blob);
        });
    });
});
