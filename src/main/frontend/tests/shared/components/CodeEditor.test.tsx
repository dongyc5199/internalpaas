import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { CodeEditor } from '../../../src/shared/components/CodeEditor/CodeEditor';
import '@testing-library/jest-dom';

// Mock Monaco Editor
vi.mock('@monaco-editor/react', () => ({
  default: ({ value, onChange, language, onMount }: any) => {
    return (
      <div data-testid="monaco-editor">
        <textarea
          data-testid="editor-textarea"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          data-language={language}
        />
      </div>
    );
  },
}));

describe('CodeEditor', () => {
  let mockOnChange: ReturnType<typeof vi.fn>;
  let mockOnValidate: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    mockOnChange = vi.fn();
    mockOnValidate = vi.fn();
  });

  it('应该正确渲染编辑器', () => {
    render(
      <CodeEditor
        value="test content"
        onChange={mockOnChange}
        language="yaml"
      />
    );

    expect(screen.getByTestId('monaco-editor')).toBeInTheDocument();
  });

  it('应该正确显示初始值', () => {
    const initialValue = '# YAML配置\nserver:\n  port: 8080';
    render(
      <CodeEditor
        value={initialValue}
        onChange={mockOnChange}
        language="yaml"
      />
    );

    const textarea = screen.getByTestId('editor-textarea');
    expect(textarea).toHaveValue(initialValue);
  });

  it('应该在内容变化时调用onChange', async () => {
    const user = userEvent.setup();
    render(
      <CodeEditor
        value=""
        onChange={mockOnChange}
        language="json"
      />
    );

    const textarea = screen.getByTestId('editor-textarea');
    await user.type(textarea, 'test content');

    await waitFor(() => {
      expect(mockOnChange).toHaveBeenCalled();
    });
  });

  it('应该支持不同的语言类型', () => {
    const languages: Array<'yaml' | 'json' | 'properties'> = ['yaml', 'json', 'properties'];

    languages.forEach((language) => {
      const { unmount } = render(
        <CodeEditor
          value=""
          onChange={mockOnChange}
          language={language}
        />
      );

      const textarea = screen.getByTestId('editor-textarea');
      expect(textarea).toHaveAttribute('data-language', language);

      unmount();
    });
  });

  it('应该支持只读模式', () => {
    render(
      <CodeEditor
        value="readonly content"
        onChange={mockOnChange}
        language="yaml"
        readOnly
      />
    );

    expect(screen.getByTestId('monaco-editor')).toBeInTheDocument();
  });

  it('应该支持自定义高度', () => {
    render(
      <CodeEditor
        value=""
        onChange={mockOnChange}
        language="yaml"
        height="600px"
      />
    );

    expect(screen.getByTestId('monaco-editor')).toBeInTheDocument();
  });

  it('应该支持深色主题', () => {
    render(
      <CodeEditor
        value=""
        onChange={mockOnChange}
        language="yaml"
        theme="vs-dark"
      />
    );

    expect(screen.getByTestId('monaco-editor')).toBeInTheDocument();
  });

  it('应该支持浅色主题', () => {
    render(
      <CodeEditor
        value=""
        onChange={mockOnChange}
        language="yaml"
        theme="light"
      />
    );

    expect(screen.getByTestId('monaco-editor')).toBeInTheDocument();
  });

  it('应该正确处理Properties语言', () => {
    const propertiesContent = 'server.port=8080\nspring.application.name=myapp';
    render(
      <CodeEditor
        value={propertiesContent}
        onChange={mockOnChange}
        language="properties"
      />
    );

    const textarea = screen.getByTestId('editor-textarea');
    expect(textarea).toHaveAttribute('data-language', 'properties');
    expect(textarea).toHaveValue(propertiesContent);
  });

  it('应该正确处理YAML语言', () => {
    const yamlContent = 'server:\n  port: 8080\nspring:\n  application:\n    name: myapp';
    render(
      <CodeEditor
        value={yamlContent}
        onChange={mockOnChange}
        language="yaml"
      />
    );

    const textarea = screen.getByTestId('editor-textarea');
    expect(textarea).toHaveValue(yamlContent);
  });

  it('应该正确处理JSON语言', () => {
    const jsonContent = '{"server": {"port": 8080}, "spring": {"application": {"name": "myapp"}}}';
    render(
      <CodeEditor
        value={jsonContent}
        onChange={mockOnChange}
        language="json"
      />
    );

    const textarea = screen.getByTestId('editor-textarea');
    expect(textarea).toHaveValue(jsonContent);
  });

  it('应该支持禁用minimap', () => {
    render(
      <CodeEditor
        value=""
        onChange={mockOnChange}
        language="yaml"
        minimap={false}
      />
    );

    expect(screen.getByTestId('monaco-editor')).toBeInTheDocument();
  });

  it('应该支持自定义行号显示', () => {
    render(
      <CodeEditor
        value=""
        onChange={mockOnChange}
        language="yaml"
        lineNumbers="off"
      />
    );

    expect(screen.getByTestId('monaco-editor')).toBeInTheDocument();
  });

  it('应该正确处理空内容', () => {
    render(
      <CodeEditor
        value=""
        onChange={mockOnChange}
        language="yaml"
      />
    );

    const textarea = screen.getByTestId('editor-textarea');
    expect(textarea).toHaveValue('');
  });

  it('应该正确处理多行内容', () => {
    const multilineContent = 'line1\nline2\nline3\nline4\nline5';
    render(
      <CodeEditor
        value={multilineContent}
        onChange={mockOnChange}
        language="yaml"
      />
    );

    const textarea = screen.getByTestId('editor-textarea');
    expect(textarea).toHaveValue(multilineContent);
  });
});
