import '@testing-library/jest-dom';
import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';

// Cleanup after each test
afterEach(() => {
  cleanup();
});

// Mock window.matchMedia for tests
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: (query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {}, // deprecated
    removeListener: () => {}, // deprecated
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => true,
  }),
});

// Mock IntersectionObserver
global.IntersectionObserver = class IntersectionObserver {
  constructor() {}
  disconnect() {}
  observe() {}
  takeRecords() {
    return [];
  }
  unobserve() {}
} as any;

// Mock scrollTo
window.scrollTo = () => {};

// Mock BroadcastChannel for testing
class MockBroadcastChannel {
  name: string;
  onmessage: ((event: MessageEvent) => void) | null = null;
  onmessageerror: ((event: MessageEvent) => void) | null = null;

  constructor(name: string) {
    this.name = name;
  }

  postMessage(message: any) {
    // Simulate message event
    setTimeout(() => {
      if (this.onmessage) {
        this.onmessage(new MessageEvent('message', { data: message }));
      }
    }, 0);
  }

  close() {}

  addEventListener() {}
  removeEventListener() {}
  dispatchEvent() {
    return true;
  }
}

global.BroadcastChannel = MockBroadcastChannel as any;

// Mock CSS.supports for CSS variable tests
if (!CSS || !CSS.supports) {
  global.CSS = {
    supports: (property: string, value?: string) => {
      // Mock支持CSS变量
      if (property.startsWith('--') || property === 'color' || property === 'background-color') {
        return true;
      }
      return false;
    },
    escape: (value: string) => value,
  } as any;
}

// Suppress console errors for CSS parsing issues in tests
const originalError = console.error;
console.error = (...args: any[]) => {
  // Suppress specific CSS-related errors that are expected in jsdom
  const message = args[0]?.toString() || '';
  if (
    message.includes("Cannot create property 'border-width'") ||
    message.includes('cssstyle') ||
    message.includes('CSS parsing')
  ) {
    return;
  }
  originalError.apply(console, args);
};
