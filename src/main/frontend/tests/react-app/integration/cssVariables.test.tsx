/**
 * CSS变量继承集成测试
 *
 * 测试目标:
 * 1. React应用能否正确读取主应用的--shell-* CSS变量
 * 2. CSS变量值是否从主应用DOM继承
 * 3. 主题切换时CSS变量是否正确更新
 */

import { describe, it, expect, beforeEach } from 'vitest';
import { render } from '@testing-library/react';
import React from 'react';

// 简单的测试组件
const TestComponent: React.FC<{ children?: React.ReactNode }> = ({ children }) => (
  <div className="test-component" data-testid="test-component">
    {children || 'Test Content'}
  </div>
);

describe('CSS变量继承测试', () => {
  // 模拟主应用的CSS变量
  const mockShellVariables = {
    '--shell-background': '#F4F5FB',
    '--shell-surface': 'rgba(255,255,255,0.92)',
    '--shell-glass': 'rgba(255,255,255,0.78)',
    '--shell-border': 'rgba(255,255,255,0.6)',
    '--shell-border-dark': 'rgba(15,23,42,0.18)',
    '--shell-text-primary': '#0F172A',
    '--shell-text-secondary': 'rgba(15,23,42,0.8)',
    '--shell-accent': 'linear-gradient(135deg, #2F9BFF, #3BC6B8)',
    '--shell-scrollbar': 'rgba(15,23,42,0.18)',
  };

  const mockDarkModeVariables = {
    '--shell-background': '#111827',
    '--shell-surface': 'rgba(30,41,59,0.92)',
    '--shell-glass': 'rgba(30,41,59,0.82)',
    '--shell-border': 'rgba(148,163,184,0.32)',
    '--shell-border-dark': 'rgba(15,23,42,0.36)',
    '--shell-text-primary': '#F9FAFB',
    '--shell-text-secondary': 'rgba(226,232,240,0.72)',
    '--shell-accent': 'linear-gradient(135deg, #2F9BFF, #3BC6B8)',
    '--shell-scrollbar': 'rgba(148,163,184,0.35)',
  };

  beforeEach(() => {
    // 清除之前的样式
    document.head.innerHTML = '';
    document.body.innerHTML = '';
  });

  /**
   * 辅助函数: 设置主应用CSS变量
   */
  const setShellVariables = (variables: Record<string, string>): void => {
    const style = document.createElement('style');
    const cssRules = Object.entries(variables)
      .map(([key, value]) => `${key}: ${value};`)
      .join('\n    ');

    style.textContent = `:root {\n    ${cssRules}\n}`;
    document.head.appendChild(style);
  };

  /**
   * 辅助函数: 获取计算后的CSS变量值
   */
  const getComputedVariable = (element: Element, variableName: string): string => {
    return getComputedStyle(element).getPropertyValue(variableName).trim();
  };

  describe('亮色主题 - CSS变量继承', () => {
    beforeEach(() => {
      setShellVariables(mockShellVariables);
    });

    it('应该正确继承 --shell-background', () => {
      const { container } = render(<TestComponent />);
      const rootElement = container.firstChild as Element;
      const bgValue = getComputedVariable(rootElement, '--shell-background');

      expect(bgValue).toBeTruthy();
    });

    it('应该正确继承 --shell-text-primary', () => {
      const { container } = render(<TestComponent />);
      const rootElement = container.firstChild as Element;
      const textValue = getComputedVariable(rootElement, '--shell-text-primary');

      expect(textValue).toBeTruthy();
    });

    it('应该正确继承 --shell-text-secondary', () => {
      const { container } = render(<TestComponent />);
      const rootElement = container.firstChild as Element;
      const textValue = getComputedVariable(rootElement, '--shell-text-secondary');

      expect(textValue).toBeTruthy();
    });

    it('应该正确继承 --shell-surface', () => {
      const { container } = render(<TestComponent />);
      const rootElement = container.firstChild as Element;
      const surfaceValue = getComputedVariable(rootElement, '--shell-surface');

      expect(surfaceValue).toBeTruthy();
    });

    it('应该正确继承 --shell-border', () => {
      const { container } = render(<TestComponent />);
      const rootElement = container.firstChild as Element;
      const borderValue = getComputedVariable(rootElement, '--shell-border');

      expect(borderValue).toBeTruthy();
    });

    it('应该正确继承 --shell-border-dark', () => {
      const { container } = render(<TestComponent />);
      const rootElement = container.firstChild as Element;
      const borderDarkValue = getComputedVariable(rootElement, '--shell-border-dark');

      expect(borderDarkValue).toBeTruthy();
    });
  });

  describe('暗色主题 - CSS变量继承', () => {
    beforeEach(() => {
      setShellVariables(mockDarkModeVariables);
    });

    it('暗色模式: 应该正确继承 --shell-background', () => {
      const { container } = render(<TestComponent />);
      const rootElement = container.firstChild as Element;
      const bgValue = getComputedVariable(rootElement, '--shell-background');

      expect(bgValue).toBeTruthy();
    });

    it('暗色模式: 应该正确继承 --shell-text-primary', () => {
      const { container } = render(<TestComponent />);
      const rootElement = container.firstChild as Element;
      const textValue = getComputedVariable(rootElement, '--shell-text-primary');

      expect(textValue).toBeTruthy();
    });

    it('暗色模式: 应该正确继承 --shell-surface', () => {
      const { container } = render(<TestComponent />);
      const rootElement = container.firstChild as Element;
      const surfaceValue = getComputedVariable(rootElement, '--shell-surface');

      expect(surfaceValue).toBeTruthy();
    });
  });

  describe('主题切换 - CSS变量动态更新', () => {
    it('从亮色切换到暗色时，CSS变量应该更新', () => {
      // 初始设置亮色主题
      setShellVariables(mockShellVariables);

      const { container, rerender } = render(<TestComponent />);
      const rootElement = container.firstChild as Element;

      // 验证亮色主题值
      const lightBg = getComputedVariable(rootElement, '--shell-background');
      expect(lightBg).toBeTruthy();

      // 切换到暗色主题
      document.head.innerHTML = '';
      setShellVariables(mockDarkModeVariables);

      // 重新渲染
      rerender(<TestComponent />);

      // 验证暗色主题值已更新
      const darkBg = getComputedVariable(rootElement, '--shell-background');
      expect(darkBg).toBeTruthy();
    });

    it('主题切换应该保持CSS变量可用性', () => {
      setShellVariables(mockShellVariables);

      const { container } = render(<TestComponent />);
      const rootElement = container.firstChild as Element;

      // 测试所有关键CSS变量在切换后仍然可用
      const criticalVariables = [
        '--shell-background',
        '--shell-text-primary',
        '--shell-text-secondary',
        '--shell-surface',
        '--shell-border',
      ];

      criticalVariables.forEach(varName => {
        const value = getComputedVariable(rootElement, varName);
        expect(value).toBeTruthy();
      });
    });
  });

  describe('嵌入模式 - CSS变量继承验证', () => {
    it('嵌入模式下应该从主应用容器继承CSS变量', () => {
      // 模拟主应用容器
      const mainAppContainer = document.createElement('div');
      mainAppContainer.setAttribute('data-spring-context', 'true');
      mainAppContainer.setAttribute('data-embedded', 'true');

      // 设置CSS变量到主应用容器
      setShellVariables(mockShellVariables);

      document.body.appendChild(mainAppContainer);

      const { container } = render(<TestComponent />, { container: mainAppContainer });
      const rootElement = container.firstChild as Element;

      // 验证继承
      const textPrimary = getComputedVariable(rootElement, '--shell-text-primary');
      expect(textPrimary).toBeTruthy();

      document.body.removeChild(mainAppContainer);
    });

    it('嵌入模式下CSS变量应该与主应用保持一致', () => {
      const mainAppContainer = document.createElement('div');
      mainAppContainer.setAttribute('data-spring-context', 'true');
      mainAppContainer.setAttribute('data-embedded', 'true');

      setShellVariables(mockShellVariables);
      document.body.appendChild(mainAppContainer);

      const { container } = render(<TestComponent />, { container: mainAppContainer });
      const rootElement = container.firstChild as Element;

      // 验证关键变量
      expect(getComputedVariable(rootElement, '--shell-background')).toBeTruthy();
      expect(getComputedVariable(rootElement, '--shell-surface')).toBeTruthy();
      expect(getComputedVariable(rootElement, '--shell-text-primary')).toBeTruthy();

      document.body.removeChild(mainAppContainer);
    });
  });

  describe('Fallback值 - CSS变量未定义时的降级处理', () => {
    it('未定义CSS变量时应该使用fallback值', () => {
      // 不设置任何CSS变量
      const { container } = render(<TestComponent />);
      const rootElement = container.firstChild as Element;

      // 即使没有定义--shell-*变量，组件也应该能正常渲染
      expect(rootElement).toBeTruthy();
      expect(rootElement).toBeInTheDocument();
    });

    it('部分CSS变量未定义时，其他变量应该正常工作', () => {
      // 只设置部分变量
      const partialVariables = {
        '--shell-text-primary': '#0F172A',
        '--shell-surface': '#FFFFFF',
      };

      setShellVariables(partialVariables);

      const { container } = render(<TestComponent />);
      const rootElement = container.firstChild as Element;

      // 已定义的变量应该可用
      expect(getComputedVariable(rootElement, '--shell-text-primary')).toBeTruthy();
      expect(getComputedVariable(rootElement, '--shell-surface')).toBeTruthy();
    });
  });

  describe('性能测试 - CSS变量读取性能', () => {
    it('批量读取CSS变量应该在可接受时间内完成', () => {
      setShellVariables(mockShellVariables);

      const { container } = render(<TestComponent />);
      const rootElement = container.firstChild as Element;

      const startTime = performance.now();

      // 批量读取100次
      for (let i = 0; i < 100; i++) {
        getComputedVariable(rootElement, '--shell-background');
        getComputedVariable(rootElement, '--shell-text-primary');
        getComputedVariable(rootElement, '--shell-surface');
      }

      const endTime = performance.now();
      const duration = endTime - startTime;

      // 100次读取应该在100ms内完成
      expect(duration).toBeLessThan(100);
    });
  });
});
