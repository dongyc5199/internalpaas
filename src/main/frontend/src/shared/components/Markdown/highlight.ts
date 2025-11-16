/**
 * Code Syntax Highlighting
 * 轻量级代码语法高亮实现
 */

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
 * 语法高亮规则定义
 */
interface SyntaxRule {
  pattern: RegExp;
  className: string;
}

/**
 * JavaScript/TypeScript 关键字
 */
const jsKeywords = [
  'abstract', 'arguments', 'await', 'boolean', 'break', 'byte', 'case', 'catch',
  'char', 'class', 'const', 'continue', 'debugger', 'default', 'delete', 'do',
  'double', 'else', 'enum', 'eval', 'export', 'extends', 'false', 'final',
  'finally', 'float', 'for', 'function', 'goto', 'if', 'implements', 'import',
  'in', 'instanceof', 'int', 'interface', 'let', 'long', 'native', 'new',
  'null', 'package', 'private', 'protected', 'public', 'return', 'short',
  'static', 'super', 'switch', 'synchronized', 'this', 'throw', 'throws',
  'transient', 'true', 'try', 'typeof', 'var', 'void', 'volatile', 'while',
  'with', 'yield', 'async', 'as', 'from', 'type', 'namespace', 'readonly',
];

/**
 * Python 关键字
 */
const pythonKeywords = [
  'and', 'as', 'assert', 'async', 'await', 'break', 'class', 'continue', 'def',
  'del', 'elif', 'else', 'except', 'False', 'finally', 'for', 'from', 'global',
  'if', 'import', 'in', 'is', 'lambda', 'None', 'nonlocal', 'not', 'or', 'pass',
  'raise', 'return', 'True', 'try', 'while', 'with', 'yield',
];

/**
 * Java 关键字
 */
const javaKeywords = [
  'abstract', 'assert', 'boolean', 'break', 'byte', 'case', 'catch', 'char',
  'class', 'const', 'continue', 'default', 'do', 'double', 'else', 'enum',
  'extends', 'final', 'finally', 'float', 'for', 'goto', 'if', 'implements',
  'import', 'instanceof', 'int', 'interface', 'long', 'native', 'new', 'package',
  'private', 'protected', 'public', 'return', 'short', 'static', 'strictfp',
  'super', 'switch', 'synchronized', 'this', 'throw', 'throws', 'transient',
  'try', 'void', 'volatile', 'while', 'true', 'false', 'null',
];

/**
 * SQL 关键字
 */
const sqlKeywords = [
  'SELECT', 'FROM', 'WHERE', 'INSERT', 'UPDATE', 'DELETE', 'CREATE', 'DROP',
  'ALTER', 'TABLE', 'DATABASE', 'INDEX', 'VIEW', 'TRIGGER', 'PROCEDURE',
  'FUNCTION', 'AND', 'OR', 'NOT', 'NULL', 'TRUE', 'FALSE', 'JOIN', 'INNER',
  'LEFT', 'RIGHT', 'OUTER', 'ON', 'AS', 'ORDER', 'BY', 'GROUP', 'HAVING',
  'LIMIT', 'OFFSET', 'UNION', 'ALL', 'DISTINCT', 'COUNT', 'SUM', 'AVG',
  'MIN', 'MAX', 'IN', 'LIKE', 'BETWEEN', 'IS', 'EXISTS',
];

/**
 * Bash/Shell 关键字
 */
const bashKeywords = [
  'if', 'then', 'else', 'elif', 'fi', 'case', 'esac', 'for', 'while', 'until',
  'do', 'done', 'in', 'function', 'select', 'time', 'until', 'return', 'exit',
  'break', 'continue', 'shift', 'export', 'unset', 'readonly', 'local', 'declare',
  'typeset', 'echo', 'printf', 'read', 'cd', 'pwd', 'ls', 'cat', 'grep', 'sed',
  'awk', 'find', 'sort', 'uniq', 'head', 'tail', 'chmod', 'chown', 'mkdir',
  'rm', 'cp', 'mv', 'touch', 'tar', 'gzip', 'curl', 'wget',
];

/**
 * 获取语言对应的关键字列表
 */
function getKeywords(language: string): string[] {
  const lang = language.toLowerCase();

  if (lang === 'javascript' || lang === 'js' || lang === 'typescript' || lang === 'ts' || lang === 'jsx' || lang === 'tsx') {
    return jsKeywords;
  }

  if (lang === 'python' || lang === 'py') {
    return pythonKeywords;
  }

  if (lang === 'java') {
    return javaKeywords;
  }

  if (lang === 'sql') {
    return sqlKeywords;
  }

  if (lang === 'bash' || lang === 'sh' || lang === 'shell') {
    return bashKeywords;
  }

  return jsKeywords; // 默认使用JS关键字
}

/**
 * 语法高亮主函数
 */
export function highlightCode(code: string, language: string = 'text'): string {
  if (language === 'text' || language === 'plain') {
    return escapeHtml(code);
  }

  // 先转义HTML
  let result = escapeHtml(code);

  // 存储已高亮的部分(避免重复处理)
  const placeholders: Map<string, string> = new Map();
  let placeholderIndex = 0;

  // 辅助函数: 创建占位符
  const createPlaceholder = (content: string): string => {
    const placeholder = `__PLACEHOLDER_${placeholderIndex++}__`;
    placeholders.set(placeholder, content);
    return placeholder;
  };

  // 1. 先处理字符串和注释(避免内部内容被误高亮)
  const stringCommentRules: SyntaxRule[] = [
    { pattern: /"(?:[^"\\]|\\.)*"/g, className: 'string' },
    { pattern: /'(?:[^'\\]|\\.)*'/g, className: 'string' },
    { pattern: /`(?:[^`\\]|\\.)*`/g, className: 'string' },
    { pattern: /\/\*[\s\S]*?\*\//g, className: 'comment' },
    { pattern: /\/\/.*/g, className: 'comment' },
  ];

  // 根据语言选择注释规则
  if (language.toLowerCase() === 'python' || language.toLowerCase() === 'py' ||
      language.toLowerCase() === 'bash' || language.toLowerCase() === 'sh' || language.toLowerCase() === 'shell') {
    // Python/Bash使用#注释
    stringCommentRules.push({ pattern: /#.*/g, className: 'comment' });
  }

  stringCommentRules.forEach((rule) => {
    result = result.replace(rule.pattern, (match) => {
      const highlighted = `<span class="token ${rule.className}">${match}</span>`;
      return createPlaceholder(highlighted);
    });
  });

  // 2. 处理关键字
  const keywords = getKeywords(language);
  const keywordPattern = new RegExp(`\\b(${keywords.join('|')})\\b`, 'g');
  result = result.replace(keywordPattern, (match) => {
    // 检查是否在占位符中(已被处理)
    if (match.includes('__PLACEHOLDER_')) {
      return match;
    }
    const highlighted = `<span class="token keyword">${match}</span>`;
    return createPlaceholder(highlighted);
  });

  // 3. 处理函数调用
  result = result.replace(/\b(\w+)\s*(?=\()/g, (match, funcName) => {
    if (match.includes('__PLACEHOLDER_')) {
      return match;
    }
    const highlighted = `<span class="token function">${funcName}</span>`;
    return createPlaceholder(highlighted);
  });

  // 4. 处理数字
  result = result.replace(/\b\d+\.?\d*([eE][+-]?\d+)?\b/g, (match) => {
    if (match.includes('__PLACEHOLDER_')) {
      return match;
    }
    const highlighted = `<span class="token number">${match}</span>`;
    return createPlaceholder(highlighted);
  });

  // 5. 处理运算符(常见的)
  const operators = ['+', '-', '*', '/', '%', '=', '==', '===', '!=', '!==', '<', '>', '<=', '>=', '&&', '||', '!', '&', '|', '^', '~', '<<', '>>', '?', ':'];
  operators.forEach((op) => {
    const escaped = op.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    const pattern = new RegExp(`(${escaped})`, 'g');
    result = result.replace(pattern, (match) => {
      if (match.includes('__PLACEHOLDER_') || match.includes('<span')) {
        return match;
      }
      return `<span class="token operator">${match}</span>`;
    });
  });

  // 6. 恢复所有占位符
  placeholders.forEach((value, key) => {
    result = result.replace(key, value);
  });

  return result;
}

/**
 * 获取支持的语言列表
 */
export function getSupportedLanguages(): string[] {
  return [
    'javascript', 'js', 'typescript', 'ts', 'jsx', 'tsx',
    'python', 'py',
    'java',
    'sql',
    'bash', 'sh', 'shell',
    'json',
    'html', 'xml',
    'css', 'scss', 'less',
    'markdown', 'md',
    'yaml', 'yml',
    'text', 'plain',
  ];
}
