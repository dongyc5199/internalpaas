/**
 * 存储封装工具模块
 * 统一localStorage/sessionStorage操作,支持过期时间、序列化等
 */

interface StorageOptions {
    /** 过期时间(毫秒),不设置则永久有效 */
    expires?: number;
    /** 存储引擎: localStorage(默认) 或 sessionStorage */
    engine?: Storage;
}

interface StorageItem<T> {
    value: T;
    expires?: number;
}

class StorageManager {
    private engine: Storage;

    constructor(engine: Storage = localStorage) {
        this.engine = engine;
    }

    /**
     * 设置存储项
     */
    set<T = unknown>(key: string, value: T, options: StorageOptions = {}): boolean {
        try {
            const { expires } = options;

            const item: StorageItem<T> = {
                value,
                expires: expires ? Date.now() + expires : undefined
            };

            this.engine.setItem(key, JSON.stringify(item));
            return true;
        } catch (error) {
            console.error(`[Storage] Failed to set "${key}":`, error);
            return false;
        }
    }

    /**
     * 获取存储项
     */
    get<T = unknown>(key: string, defaultValue?: T): T | null {
        try {
            const itemStr = this.engine.getItem(key);
            if (!itemStr) {
                return defaultValue ?? null;
            }

            const item: StorageItem<T> = JSON.parse(itemStr);

            // 检查是否过期
            if (item.expires && Date.now() > item.expires) {
                this.remove(key);
                return defaultValue ?? null;
            }

            return item.value;
        } catch (error) {
            console.error(`[Storage] Failed to get "${key}":`, error);
            return defaultValue ?? null;
        }
    }

    /**
     * 移除存储项
     */
    remove(key: string): void {
        try {
            this.engine.removeItem(key);
        } catch (error) {
            console.error(`[Storage] Failed to remove "${key}":`, error);
        }
    }

    /**
     * 清空所有存储
     */
    clear(): void {
        try {
            this.engine.clear();
        } catch (error) {
            console.error("[Storage] Failed to clear storage:", error);
        }
    }

    /**
     * 检查键是否存在
     */
    has(key: string): boolean {
        return this.engine.getItem(key) !== null;
    }

    /**
     * 获取所有键
     */
    keys(): string[] {
        const keys: string[] = [];
        for (let i = 0; i < this.engine.length; i++) {
            const key = this.engine.key(i);
            if (key) {
                keys.push(key);
            }
        }
        return keys;
    }

    /**
     * 获取存储大小(字节)
     */
    size(): number {
        let totalSize = 0;
        this.keys().forEach((key) => {
            const value = this.engine.getItem(key);
            if (value) {
                totalSize += key.length + value.length;
            }
        });
        return totalSize;
    }

    /**
     * 批量设置
     */
    setMultiple(items: Record<string, unknown>, options: StorageOptions = {}): void {
        Object.entries(items).forEach(([key, value]) => {
            this.set(key, value, options);
        });
    }

    /**
     * 批量获取
     */
    getMultiple<T = unknown>(keys: string[]): Record<string, T | null> {
        const result: Record<string, T | null> = {};
        keys.forEach((key) => {
            result[key] = this.get<T>(key);
        });
        return result;
    }

    /**
     * 批量移除
     */
    removeMultiple(keys: string[]): void {
        keys.forEach((key) => {
            this.remove(key);
        });
    }
}

// 创建localStorage和sessionStorage实例
export const localStorage = new StorageManager(window.localStorage);
export const sessionStorage = new StorageManager(window.sessionStorage);

// 也导出类,允许创建自定义实例
export { StorageManager };

// 类型导出
export type { StorageOptions, StorageItem };
