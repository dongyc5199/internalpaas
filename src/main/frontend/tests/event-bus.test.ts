/**
 * EventBus事件总线测试套件
 */

import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { EventBus, eventBus } from "../utils/event-bus";

describe("EventBus", () => {
    let bus: EventBus;

    beforeEach(() => {
        bus = new EventBus();
    });

    afterEach(() => {
        bus.clear();
    });

    describe("基本订阅和发布", () => {
        it("应该能够订阅和触发事件", () => {
            const handler = vi.fn();
            bus.on("test-event", handler);
            bus.emit("test-event", { data: "test" });

            expect(handler).toHaveBeenCalledTimes(1);
            expect(handler).toHaveBeenCalledWith({ data: "test" });
        });

        it("应该支持多个订阅者", () => {
            const handler1 = vi.fn();
            const handler2 = vi.fn();
            const handler3 = vi.fn();

            bus.on("test-event", handler1);
            bus.on("test-event", handler2);
            bus.on("test-event", handler3);

            bus.emit("test-event", "data");

            expect(handler1).toHaveBeenCalledWith("data");
            expect(handler2).toHaveBeenCalledWith("data");
            expect(handler3).toHaveBeenCalledWith("data");
        });

        it("应该支持触发没有数据的事件", () => {
            const handler = vi.fn();
            bus.on("test-event", handler);
            bus.emit("test-event");

            expect(handler).toHaveBeenCalledWith(undefined);
        });

        it("应该支持不同类型的数据", () => {
            const handler = vi.fn();
            bus.on("test-event", handler);

            bus.emit("test-event", "string");
            bus.emit("test-event", 123);
            bus.emit("test-event", { obj: true });
            bus.emit("test-event", [1, 2, 3]);

            expect(handler).toHaveBeenCalledTimes(4);
        });
    });

    describe("一次性订阅", () => {
        it("应该只触发一次", () => {
            const handler = vi.fn();
            bus.once("test-event", handler);

            bus.emit("test-event", "data1");
            bus.emit("test-event", "data2");
            bus.emit("test-event", "data3");

            expect(handler).toHaveBeenCalledTimes(1);
            expect(handler).toHaveBeenCalledWith("data1");
        });

        it("应该返回取消订阅函数", () => {
            const handler = vi.fn();
            const unsubscribe = bus.once("test-event", handler);

            unsubscribe();
            bus.emit("test-event", "data");

            expect(handler).not.toHaveBeenCalled();
        });
    });

    describe("取消订阅", () => {
        it("应该能够通过返回的函数取消订阅", () => {
            const handler = vi.fn();
            const unsubscribe = bus.on("test-event", handler);

            bus.emit("test-event", "data1");
            unsubscribe();
            bus.emit("test-event", "data2");

            expect(handler).toHaveBeenCalledTimes(1);
            expect(handler).toHaveBeenCalledWith("data1");
        });

        it("应该能够通过off方法取消订阅", () => {
            const handler = vi.fn();
            bus.on("test-event", handler);

            bus.emit("test-event", "data1");
            bus.off("test-event", handler);
            bus.emit("test-event", "data2");

            expect(handler).toHaveBeenCalledTimes(1);
        });

        it("应该能够取消特定处理器而不影响其他", () => {
            const handler1 = vi.fn();
            const handler2 = vi.fn();

            bus.on("test-event", handler1);
            bus.on("test-event", handler2);

            bus.off("test-event", handler1);
            bus.emit("test-event", "data");

            expect(handler1).not.toHaveBeenCalled();
            expect(handler2).toHaveBeenCalledWith("data");
        });

        it("应该能够取消所有订阅者", () => {
            const handler1 = vi.fn();
            const handler2 = vi.fn();

            bus.on("test-event", handler1);
            bus.on("test-event", handler2);

            bus.off("test-event");
            bus.emit("test-event", "data");

            expect(handler1).not.toHaveBeenCalled();
            expect(handler2).not.toHaveBeenCalled();
        });

        it("应该在取消不存在的事件时不报错", () => {
            expect(() => {
                bus.off("non-existent-event");
            }).not.toThrow();
        });
    });

    describe("错误处理", () => {
        it("应该捕获处理器中的错误并继续执行其他处理器", () => {
            const consoleError = vi.spyOn(console, "error").mockImplementation(() => {});
            const handler1 = vi.fn(() => {
                throw new Error("Handler 1 error");
            });
            const handler2 = vi.fn();

            bus.on("test-event", handler1);
            bus.on("test-event", handler2);

            bus.emit("test-event", "data");

            expect(handler1).toHaveBeenCalled();
            expect(handler2).toHaveBeenCalled();
            expect(consoleError).toHaveBeenCalled();

            consoleError.mockRestore();
        });

        it("应该在没有订阅者时静默处理", () => {
            expect(() => {
                bus.emit("non-existent-event", "data");
            }).not.toThrow();
        });
    });

    describe("清除所有事件", () => {
        it("应该清除所有事件订阅", () => {
            const handler1 = vi.fn();
            const handler2 = vi.fn();

            bus.on("event1", handler1);
            bus.on("event2", handler2);

            bus.clear();

            bus.emit("event1", "data");
            bus.emit("event2", "data");

            expect(handler1).not.toHaveBeenCalled();
            expect(handler2).not.toHaveBeenCalled();
        });
    });

    describe("事件查询", () => {
        it("应该返回正确的订阅者数量", () => {
            const handler1 = vi.fn();
            const handler2 = vi.fn();

            expect(bus.listenerCount("test-event")).toBe(0);

            bus.on("test-event", handler1);
            expect(bus.listenerCount("test-event")).toBe(1);

            bus.on("test-event", handler2);
            expect(bus.listenerCount("test-event")).toBe(2);

            bus.off("test-event", handler1);
            expect(bus.listenerCount("test-event")).toBe(1);

            bus.off("test-event");
            expect(bus.listenerCount("test-event")).toBe(0);
        });

        it("应该返回所有事件名称", () => {
            bus.on("event1", vi.fn());
            bus.on("event2", vi.fn());
            bus.on("event3", vi.fn());

            const eventNames = bus.eventNames();

            expect(eventNames).toHaveLength(3);
            expect(eventNames).toContain("event1");
            expect(eventNames).toContain("event2");
            expect(eventNames).toContain("event3");
        });

        it("应该在没有事件时返回空数组", () => {
            const eventNames = bus.eventNames();
            expect(eventNames).toEqual([]);
        });
    });

    describe("边界情况", () => {
        it("应该处理在事件处理器中取消自己", () => {
            let unsubscribe: (() => void) | null = null;
            const handler = vi.fn(() => {
                if (unsubscribe) unsubscribe();
            });

            unsubscribe = bus.on("test-event", handler);

            bus.emit("test-event");
            bus.emit("test-event");

            expect(handler).toHaveBeenCalledTimes(1);
        });

        it("应该处理在事件处理器中添加新订阅", () => {
            const handler1 = vi.fn(() => {
                bus.on("test-event", handler2);
            });
            const handler2 = vi.fn();

            bus.on("test-event", handler1);

            bus.emit("test-event");
            expect(handler2).not.toHaveBeenCalled(); // 新订阅不应在当前emit中触发

            bus.emit("test-event");
            expect(handler2).toHaveBeenCalledTimes(1); // 在下一次emit中触发
        });

        it("应该处理同一个处理器多次订阅", () => {
            const handler = vi.fn();

            bus.on("test-event", handler);
            bus.on("test-event", handler);

            bus.emit("test-event", "data");

            expect(handler).toHaveBeenCalledTimes(2);
        });

        it("应该处理最后一个订阅者被移除后删除事件", () => {
            const handler = vi.fn();
            bus.on("test-event", handler);

            expect(bus.eventNames()).toContain("test-event");

            bus.off("test-event", handler);

            expect(bus.eventNames()).not.toContain("test-event");
        });
    });

    describe("全局事件总线实例", () => {
        it("应该能够使用导出的全局实例", () => {
            const handler = vi.fn();

            eventBus.on("global-event", handler);
            eventBus.emit("global-event", "data");

            expect(handler).toHaveBeenCalledWith("data");

            // 清理
            eventBus.clear();
        });
    });

    describe("TypeScript类型安全", () => {
        it("应该支持泛型类型", () => {
            interface TestData {
                id: number;
                name: string;
            }

            const handler = vi.fn<[TestData]>();

            bus.on<TestData>("typed-event", handler);
            bus.emit<TestData>("typed-event", { id: 1, name: "test" });

            expect(handler).toHaveBeenCalledWith({ id: 1, name: "test" });
        });
    });
});
