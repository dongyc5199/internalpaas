/**
 * 事件总线工具模块
 * 轻量级发布-订阅模式实现,用于模块间通信
 */

type EventHandler<T = unknown> = (data: T) => void;
type UnsubscribeFn = () => void;

interface EventMap {
    [eventName: string]: EventHandler[];
}

class EventBus {
    private events: EventMap = {};

    /**
     * 订阅事件
     */
    on<T = unknown>(eventName: string, handler: EventHandler<T>): UnsubscribeFn {
        if (!this.events[eventName]) {
            this.events[eventName] = [];
        }

        this.events[eventName].push(handler as EventHandler);

        // 返回取消订阅函数
        return () => this.off(eventName, handler);
    }

    /**
     * 订阅一次性事件(触发后自动取消订阅)
     */
    once<T = unknown>(eventName: string, handler: EventHandler<T>): UnsubscribeFn {
        const wrappedHandler: EventHandler<T> = (data: T) => {
            handler(data);
            this.off(eventName, wrappedHandler);
        };

        return this.on(eventName, wrappedHandler);
    }

    /**
     * 取消订阅
     */
    off<T = unknown>(eventName: string, handler?: EventHandler<T>): void {
        if (!this.events[eventName]) {
            return;
        }

        if (!handler) {
            // 取消所有该事件的订阅
            delete this.events[eventName];
            return;
        }

        const index = this.events[eventName].indexOf(handler as EventHandler);
        if (index > -1) {
            this.events[eventName].splice(index, 1);
        }

        // 如果该事件没有订阅者了,删除该事件
        if (this.events[eventName].length === 0) {
            delete this.events[eventName];
        }
    }

    /**
     * 触发事件
     */
    emit<T = unknown>(eventName: string, data?: T): void {
        const handlers = this.events[eventName];
        if (!handlers || handlers.length === 0) {
            return;
        }

        // 复制handlers数组,避免在执行过程中被修改
        const handlersCopy = [...handlers];
        handlersCopy.forEach((handler) => {
            try {
                handler(data);
            } catch (error) {
                console.error(`[EventBus] Error in handler for "${eventName}":`, error);
            }
        });
    }

    /**
     * 清除所有事件订阅
     */
    clear(): void {
        this.events = {};
    }

    /**
     * 获取事件订阅数量
     */
    listenerCount(eventName: string): number {
        return this.events[eventName]?.length || 0;
    }

    /**
     * 获取所有事件名称
     */
    eventNames(): string[] {
        return Object.keys(this.events);
    }
}

// 创建全局事件总线实例
export const eventBus = new EventBus();

// 也导出类,允许创建独立实例
export { EventBus };

// 类型导出
export type { EventHandler, UnsubscribeFn };
