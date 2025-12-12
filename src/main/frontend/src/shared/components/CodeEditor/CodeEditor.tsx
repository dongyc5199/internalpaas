import React, { useRef, useCallback } from 'react';
import Editor, { Monaco } from '@monaco-editor/react';
import type { editor } from 'monaco-editor';
import styles from './CodeEditor.module.css';

export interface CodeEditorProps {
  value: string;
  onChange: (value: string) => void;
  language: 'yaml' | 'json' | 'properties' | 'javascript' | 'typescript';
  height?: string;
  readOnly?: boolean;
  theme?: 'vs-dark' | 'light';
  onValidate?: (markers: editor.IMarker[]) => void;
  minimap?: boolean;
  lineNumbers?: 'on' | 'off' | 'relative';
}

export const CodeEditor: React.FC<CodeEditorProps> = ({
  value,
  onChange,
  language,
  height = '400px',
  readOnly = false,
  theme = 'vs-dark',
  onValidate,
  minimap = true,
  lineNumbers = 'on',
}) => {
  const editorRef = useRef<editor.IStandaloneCodeEditor | null>(null);
  const monacoRef = useRef<Monaco | null>(null);

  const handleEditorDidMount = useCallback(
    (editor: editor.IStandaloneCodeEditor, monaco: Monaco) => {
      editorRef.current = editor;
      monacoRef.current = monaco;

      // 配置编辑器选项
      editor.updateOptions({
        readOnly,
        minimap: { enabled: minimap },
        lineNumbers,
        fontSize: 14,
        tabSize: 2,
        wordWrap: 'on',
        automaticLayout: true,
        scrollBeyondLastLine: false,
        formatOnPaste: true,
        formatOnType: true,
      });

      // 注册Properties语言支持
      if (language === 'properties') {
        monaco.languages.register({ id: 'properties' });

        monaco.languages.setMonarchTokensProvider('properties', {
          tokenizer: {
            root: [
              [/^#.*$/, 'comment'],
              [/^!.*$/, 'comment'],
              [/[a-zA-Z_][\w.-]*(?=\s*[=:])/, 'key'],
              [/[=:]/, 'delimiter'],
              [/.*$/, 'value'],
            ],
          },
        });

        monaco.languages.setLanguageConfiguration('properties', {
          comments: {
            lineComment: '#',
          },
          brackets: [['[', ']']],
          autoClosingPairs: [
            { open: '[', close: ']' },
            { open: '"', close: '"' },
            { open: "'", close: "'" },
          ],
        });
      }

      // 监听验证标记
      if (onValidate) {
        const model = editor.getModel();
        if (model) {
          monaco.editor.onDidChangeMarkers(() => {
            const markers = monaco.editor.getModelMarkers({ resource: model.uri });
            onValidate(markers);
          });
        }
      }
    },
    [language, readOnly, minimap, lineNumbers, onValidate]
  );

  const handleEditorChange = useCallback(
    (value: string | undefined) => {
      if (value !== undefined) {
        onChange(value);
      }
    },
    [onChange]
  );

  return (
    <div className={styles.editorContainer}>
      <Editor
        height={height}
        language={language === 'properties' ? 'properties' : language}
        value={value}
        theme={theme}
        onChange={handleEditorChange}
        onMount={handleEditorDidMount}
        options={{
          readOnly,
          minimap: { enabled: minimap },
          lineNumbers,
          fontSize: 14,
          tabSize: 2,
          wordWrap: 'on',
          automaticLayout: true,
          scrollBeyondLastLine: false,
        }}
      />
    </div>
  );
};

export default CodeEditor;
