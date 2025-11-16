/**
 * Markdown Component
 * 简单的Markdown渲染器，支持常用语法和代码高亮
 */

import { useMemo, useEffect, useRef } from 'react';
import styles from './Markdown.module.css';
import { highlightCode } from './highlight';

export interface MarkdownProps {
  content: string;
  className?: string;
}

/**
 * 转义HTML特殊字符
 */
function escapeHtml(text: string): string {
  const map: Record<string, string> = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#039;',
  };
  return text.replace(/[&<>"']/g, (char) => map[char] ?? char);
}

/**
 * 解析Markdown为HTML
 */
function parseMarkdown(markdown: string): string {
  let html = markdown;

  // 代码块 ```language\ncode\n```
  html = html.replace(
    /```(\w+)?\n([\s\S]*?)```/g,
    (_, lang, code) => {
      const language = lang || 'text';
      return `<pre><code class="language-${escapeHtml(language)}">${escapeHtml(code.trim())}</code></pre>`;
    }
  );

  // 行内代码 `code`
  html = html.replace(/`([^`]+)`/g, (_, code) => `<code>${escapeHtml(code)}</code>`);

  // 标题 # H1, ## H2, etc.
  html = html.replace(/^### (.*$)/gim, '<h3>$1</h3>');
  html = html.replace(/^## (.*$)/gim, '<h2>$1</h2>');
  html = html.replace(/^# (.*$)/gim, '<h1>$1</h1>');

  // 粗体 **text** 或 __text__
  html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
  html = html.replace(/__([^_]+)__/g, '<strong>$1</strong>');

  // 斜体 *text* 或 _text_
  html = html.replace(/\*([^*]+)\*/g, '<em>$1</em>');
  html = html.replace(/_([^_]+)_/g, '<em>$1</em>');

  // 链接 [text](url)
  html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>');

  // 无序列表 - item 或 * item
  html = html.replace(/^\s*[-*]\s+(.*)$/gim, '<li>$1</li>');
  html = html.replace(/(<li>.*<\/li>)/s, '<ul>$1</ul>');

  // 有序列表 1. item
  html = html.replace(/^\s*\d+\.\s+(.*)$/gim, '<li>$1</li>');

  // 引用 > text
  html = html.replace(/^>\s*(.*)$/gim, '<blockquote>$1</blockquote>');

  // 水平线 --- or ***
  html = html.replace(/^(---|\*\*\*)$/gim, '<hr/>');

  // 段落 (连续的非空行)
  const lines = html.split('\n');
  const paragraphs: string[] = [];
  let currentParagraph: string[] = [];

  for (const line of lines) {
    const trimmed = line.trim();

    // 跳过已经是HTML标签的行
    if (trimmed.startsWith('<') || trimmed === '') {
      if (currentParagraph.length > 0) {
        paragraphs.push(`<p>${currentParagraph.join(' ')}</p>`);
        currentParagraph = [];
      }
      if (trimmed !== '') {
        paragraphs.push(trimmed);
      }
    } else {
      currentParagraph.push(trimmed);
    }
  }

  if (currentParagraph.length > 0) {
    paragraphs.push(`<p>${currentParagraph.join(' ')}</p>`);
  }

  return paragraphs.join('\n');
}

/**
 * Markdown渲染组件
 */
export function Markdown({ content, className = '' }: MarkdownProps): JSX.Element {
  const html = useMemo(() => parseMarkdown(content), [content]);
  const containerRef = useRef<HTMLDivElement>(null);

  // 在内容更新后高亮代码块
  useEffect(() => {
    if (containerRef.current) {
      const blocks = containerRef.current.querySelectorAll('pre code[class*="language-"]');

      blocks.forEach((block) => {
        const codeElement = block as HTMLElement;
        const preElement = codeElement.parentElement;
        if (!preElement) return;

        const languageMatch = codeElement.className.match(/language-(\w+)/);
        const language = (languageMatch && languageMatch[1]) || 'text';
        const code = codeElement.textContent || '';

        // 高亮代码
        const highlighted = highlightCode(code, language);
        codeElement.innerHTML = highlighted;

        // 确保pre元素有relative定位,用于绝对定位按钮
        preElement.style.position = 'relative';

        // 创建复制按钮 (不使用React Portal,直接DOM操作)
        const existingButton = preElement.querySelector('.copy-button');
        if (!existingButton) {
          const button = document.createElement('button');
          button.className = 'copy-button';
          button.textContent = '复制';

          button.addEventListener('click', () => {
            navigator.clipboard.writeText(code).then(() => {
              button.textContent = '已复制!';
              button.classList.add('copied');
              setTimeout(() => {
                button.textContent = '复制';
                button.classList.remove('copied');
              }, 2000);
            }).catch(() => {
              button.textContent = '复制失败';
              button.classList.add('error');
              setTimeout(() => {
                button.textContent = '复制';
                button.classList.remove('error');
              }, 2000);
            });
          });

          preElement.appendChild(button);
        }
      });
    }
  }, [html]);

  return (
    <div className={`${styles.markdown} ${className}`}>
      <div
        ref={containerRef}
        dangerouslySetInnerHTML={{ __html: html }}
      />
    </div>
  );
}

