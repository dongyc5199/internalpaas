/**
 * AI助手快捷命令处理器
 * 支持特殊命令如 /help, /clear, /model 等
 */

export interface CommandResult {
  /** 是否为命令 */
  isCommand: boolean;
  /** 命令响应消息 */
  response?: string;
  /** 命令执行的副作用 */
  action?: 'clear' | 'export' | 'model-change' | 'show-context';
  /** 命令参数 */
  payload?: any;
}

/**
 * 快捷命令定义
 */
const COMMANDS = {
  '/help': {
    description: '显示帮助信息',
    handler: (): CommandResult => ({
      isCommand: true,
      response: `## 🤖 AI助手快捷命令

### 可用命令

- **/help** - 显示此帮助信息
- **/clear** - 清空当前会话消息
- **/model <名称>** - 切换AI模型
- **/export** - 导出当前聊天记录
- **/context** - 显示终端上下文（最后100行）
- **/new** - 创建新会话

### 使用技巧

- 输入消息前加 \`/\` 即可使用命令
- 使用 **Ctrl+Shift+A** 快速打开/关闭AI助手
- 使用 **Enter** 发送消息，**Shift+Enter** 换行

### 示例

\`\`\`
/model moonshot-v1-8k
/export
/context
\`\`\``,
    }),
  },

  '/clear': {
    description: '清空当前会话',
    handler: (): CommandResult => ({
      isCommand: true,
      action: 'clear',
      response: '✅ 会话已清空',
    }),
  },

  '/export': {
    description: '导出聊天记录',
    handler: (): CommandResult => ({
      isCommand: true,
      action: 'export',
      response: '📥 正在导出聊天记录...',
    }),
  },

  '/new': {
    description: '创建新会话',
    handler: (): CommandResult => ({
      isCommand: true,
      action: 'clear',
      response: '✅ 已创建新会话',
    }),
  },

  '/context': {
    description: '显示终端上下文',
    handler: (): CommandResult => ({
      isCommand: true,
      action: 'show-context',
      response: '📋 正在获取终端上下文...',
    }),
  },

  '/model': {
    description: '切换AI模型',
    handler: (args?: string[]): CommandResult => {
      if (!args || args.length === 0) {
        return {
          isCommand: true,
          response: '❌ 请指定模型名称。例如: `/model moonshot-v1-8k`',
        };
      }

      const modelName = args.join(' ');
      return {
        isCommand: true,
        action: 'model-change',
        payload: modelName,
        response: `✅ 已切换到模型: **${modelName}**`,
      };
    },
  },
};

/**
 * 处理用户输入的命令
 *
 * @param input - 用户输入
 * @returns 命令处理结果
 */
export function handleCommand(input: string): CommandResult {
  const trimmed = input.trim();

  // 不是以 / 开头，不是命令
  if (!trimmed.startsWith('/')) {
    return { isCommand: false };
  }

  // 解析命令和参数
  const parts = trimmed.split(/\s+/);
  const command = parts[0]?.toLowerCase();
  if (!command) {
    return { isCommand: false };
  }
  const args = parts.slice(1);

  // 查找命令处理器
  const commandDef = COMMANDS[command as keyof typeof COMMANDS];

  if (!commandDef) {
    return {
      isCommand: true,
      response: `❌ 未知命令: \`${command}\`\n\n输入 \`/help\` 查看可用命令`,
    };
  }

  // 执行命令
  return commandDef.handler(args);
}

/**
 * 获取所有可用命令列表
 */
export function getAvailableCommands(): Array<{
  command: string;
  description: string;
}> {
  return Object.entries(COMMANDS).map(([command, def]) => ({
    command,
    description: def.description,
  }));
}

/**
 * 检查输入是否为命令
 */
export function isCommand(input: string): boolean {
  return input.trim().startsWith('/');
}
