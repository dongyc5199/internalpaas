/**
 * StorageManager存储管理测试套件
 */

import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { StorageManager, localStorageManager, sessionStorageManager } from "../utils/storage";

describe("StorageManager", () => {
    let storage: StorageManager;
    let mockStorage: Storage;

    beforeEach(() => {
        // 创建一个模拟的Storage对象
        const store: Record<string, string> = {};
        mockStorage = {
            getItem: (key: string) => store[key] || null,
            setItem: (key: string, value: string) => {
                store[key] = value;
            },
            removeItem: (key: string) => {
                delete store[key];
            },
            clear: () => {
                for (const key in store) {
                    delete store[key];
                }
            },
            key: (index: number) => {
                const keys = Object.keys(store);
                return keys[index] || null;
            },
            length: 0
        };
        Object.defineProperty(mockStorage, "length", {
            get: () => Object.keys(store).length
        });

        storage = new StorageManager(mockStorage);
    });

    afterEach(() => {
        storage.clear();
    });

    describe("基本设置和获取", () => {
        it("应该能够设置和获取字符串", () => {
            storage.set("test-key", "test-value");
            const value = storage.get("test-key");

            expect(value).toBe("test-value");
        });

        it("应该能够设置和获取数字", () => {
            storage.set("number", 123);
            const value = storage.get("number");

            expect(value).toBe(123);
        });

        it("应该能够设置和获取对象", () => {
            const obj = { id: 1, name: "test" };
            storage.set("object", obj);
            const value = storage.get("object");

            expect(value).toEqual(obj);
        });

        it("应该能够设置和获取数组", () => {
            const arr = [1, 2, 3];
            storage.set("array", arr);
            const value = storage.get("array");

            expect(value).toEqual(arr);
        });

        it("应该能够设置和获取布尔值", () => {
            storage.set("bool", true);
            const value = storage.get("bool");

            expect(value).toBe(true);
        });

        it("应该在键不存在时返回null", () => {
            const value = storage.get("non-existent");
            expect(value).toBeNull();
        });

        it("应该在键不存在时返回默认值", () => {
            const value = storage.get("non-existent", "default");
            expect(value).toBe("default");
        });

        it("应该在设置成功时返回true", () => {
            const result = storage.set("test", "value");
            expect(result).toBe(true);
        });
    });

    describe("过期时间", () => {
        it("应该支持设置过期时间", () => {
            vi.useFakeTimers();

            storage.set("expiring", "value", { expires: 1000 });

            // 立即获取应该有值
            expect(storage.get("expiring")).toBe("value");

            // 推进时间到过期后
            vi.advanceTimersByTime(1001);

            // 过期后应该返回null
            expect(storage.get("expiring")).toBeNull();

            vi.useRealTimers();
        });

        it("应该在过期后自动删除项", () => {
            vi.useFakeTimers();

            storage.set("expiring", "value", { expires: 1000 });
            expect(storage.has("expiring")).toBe(true);

            vi.advanceTimersByTime(1001);

            // 获取过期项会自动删除
            storage.get("expiring");
            expect(storage.has("expiring")).toBe(false);

            vi.useRealTimers();
        });

        it("应该在不设置过期时间时永久有效", () => {
            vi.useFakeTimers();

            storage.set("permanent", "value");

            vi.advanceTimersByTime(100000000);

            expect(storage.get("permanent")).toBe("value");

            vi.useRealTimers();
        });
    });

    describe("移除和清空", () => {
        it("应该能够移除指定键", () => {
            storage.set("test", "value");
            expect(storage.has("test")).toBe(true);

            storage.remove("test");
            expect(storage.has("test")).toBe(false);
        });

        it("应该能够清空所有存储", () => {
            storage.set("key1", "value1");
            storage.set("key2", "value2");
            storage.set("key3", "value3");

            storage.clear();

            expect(storage.has("key1")).toBe(false);
            expect(storage.has("key2")).toBe(false);
            expect(storage.has("key3")).toBe(false);
        });
    });

    describe("存储查询", () => {
        it("应该能够检查键是否存在", () => {
            expect(storage.has("test")).toBe(false);

            storage.set("test", "value");
            expect(storage.has("test")).toBe(true);

            storage.remove("test");
            expect(storage.has("test")).toBe(false);
        });

        it("应该能够获取所有键", () => {
            storage.set("key1", "value1");
            storage.set("key2", "value2");
            storage.set("key3", "value3");

            const keys = storage.keys();

            expect(keys).toHaveLength(3);
            expect(keys).toContain("key1");
            expect(keys).toContain("key2");
            expect(keys).toContain("key3");
        });

        it("应该在空存储时返回空数组", () => {
            const keys = storage.keys();
            expect(keys).toEqual([]);
        });

        it("应该能够计算存储大小", () => {
            storage.set("test", "value");

            const size = storage.size();
            expect(size).toBeGreaterThan(0);
        });

        it("应该在空存储时返回0大小", () => {
            const size = storage.size();
            expect(size).toBe(0);
        });
    });

    describe("批量操作", () => {
        it("应该能够批量设置", () => {
            storage.setMultiple({
                key1: "value1",
                key2: "value2",
                key3: "value3"
            });

            expect(storage.get("key1")).toBe("value1");
            expect(storage.get("key2")).toBe("value2");
            expect(storage.get("key3")).toBe("value3");
        });

        it("应该能够批量获取", () => {
            storage.set("key1", "value1");
            storage.set("key2", "value2");
            storage.set("key3", "value3");

            const values = storage.getMultiple(["key1", "key2", "key3"]);

            expect(values).toEqual({
                key1: "value1",
                key2: "value2",
                key3: "value3"
            });
        });

        it("应该在批量获取不存在的键时返回null", () => {
            storage.set("key1", "value1");

            const values = storage.getMultiple(["key1", "key2", "key3"]);

            expect(values).toEqual({
                key1: "value1",
                key2: null,
                key3: null
            });
        });

        it("应该能够批量移除", () => {
            storage.set("key1", "value1");
            storage.set("key2", "value2");
            storage.set("key3", "value3");

            storage.removeMultiple(["key1", "key3"]);

            expect(storage.has("key1")).toBe(false);
            expect(storage.has("key2")).toBe(true);
            expect(storage.has("key3")).toBe(false);
        });
    });

    describe("错误处理", () => {
        it("应该在设置失败时返回false并记录错误", () => {
            const consoleError = vi.spyOn(console, "error").mockImplementation(() => {});

            // 创建一个会抛出错误的storage
            const badStorage: Storage = {
                ...mockStorage,
                setItem: () => {
                    throw new Error("Storage full");
                }
            };

            const badStorageManager = new StorageManager(badStorage);
            const result = badStorageManager.set("test", "value");

            expect(result).toBe(false);
            expect(consoleError).toHaveBeenCalled();

            consoleError.mockRestore();
        });

        it("应该在获取失败时返回null并记录错误", () => {
            const consoleError = vi.spyOn(console, "error").mockImplementation(() => {});

            // 创建一个会抛出错误的storage
            const badStorage: Storage = {
                ...mockStorage,
                getItem: () => {
                    throw new Error("Storage error");
                }
            };

            const badStorageManager = new StorageManager(badStorage);
            const result = badStorageManager.get("test");

            expect(result).toBeNull();
            expect(consoleError).toHaveBeenCalled();

            consoleError.mockRestore();
        });

        it("应该在解析JSON失败时返回默认值", () => {
            const consoleError = vi.spyOn(console, "error").mockImplementation(() => {});

            // 直接设置一个无效的JSON字符串
            mockStorage.setItem("invalid", "not a valid json");

            const result = storage.get("invalid", "default");

            expect(result).toBe("default");
            expect(consoleError).toHaveBeenCalled();

            consoleError.mockRestore();
        });

        it("应该在移除失败时捕获错误", () => {
            const consoleError = vi.spyOn(console, "error").mockImplementation(() => {});

            const badStorage: Storage = {
                ...mockStorage,
                removeItem: () => {
                    throw new Error("Remove error");
                }
            };

            const badStorageManager = new StorageManager(badStorage);

            expect(() => {
                badStorageManager.remove("test");
            }).not.toThrow();

            expect(consoleError).toHaveBeenCalled();
            consoleError.mockRestore();
        });

        it("应该在清空失败时捕获错误", () => {
            const consoleError = vi.spyOn(console, "error").mockImplementation(() => {});

            const badStorage: Storage = {
                ...mockStorage,
                clear: () => {
                    throw new Error("Clear error");
                }
            };

            const badStorageManager = new StorageManager(badStorage);

            expect(() => {
                badStorageManager.clear();
            }).not.toThrow();

            expect(consoleError).toHaveBeenCalled();
            consoleError.mockRestore();
        });
    });

    describe("全局实例", () => {
        it("应该能够使用localStorage实例", () => {
            localStorageManager.set("test", "value");
            const value = localStorageManager.get("test");

            expect(value).toBe("value");

            // 清理
            localStorageManager.remove("test");
        });

        it("应该能够使用sessionStorage实例", () => {
            sessionStorageManager.set("test", "value");
            const value = sessionStorageManager.get("test");

            expect(value).toBe("value");

            // 清理
            sessionStorageManager.remove("test");
        });
    });

    describe("TypeScript类型安全", () => {
        it("应该支持泛型类型", () => {
            interface User {
                id: number;
                name: string;
            }

            const user: User = { id: 1, name: "test" };

            storage.set<User>("user", user);
            const retrieved = storage.get<User>("user");

            expect(retrieved).toEqual(user);
        });
    });

    describe("边界情况", () => {
        it("应该处理null值", () => {
            storage.set("null-value", null);
            const value = storage.get("null-value");

            expect(value).toBeNull();
        });

        it("应该处理undefined值", () => {
            storage.set("undefined-value", undefined);
            const value = storage.get("undefined-value");

            expect(value).toBeUndefined();
        });

        it("应该处理空字符串", () => {
            storage.set("empty", "");
            const value = storage.get("empty");

            expect(value).toBe("");
        });

        it("应该处理零值", () => {
            storage.set("zero", 0);
            const value = storage.get("zero");

            expect(value).toBe(0);
        });

        it("应该处理false值", () => {
            storage.set("false", false);
            const value = storage.get("false");

            expect(value).toBe(false);
        });

        it("应该处理空对象", () => {
            storage.set("empty-obj", {});
            const value = storage.get("empty-obj");

            expect(value).toEqual({});
        });

        it("应该处理空数组", () => {
            storage.set("empty-arr", []);
            const value = storage.get("empty-arr");

            expect(value).toEqual([]);
        });
    });
});
