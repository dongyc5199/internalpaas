/**
 * CSS Modules Mock for Testing
 *
 * 此Mock用于解决jsdom环境中CSS变量解析问题
 * 为所有CSS Module导入返回类名到类名的映射
 */

export default new Proxy(
  {},
  {
    get(_target, prop) {
      // 返回原始属性名作为类名
      // 例如: styles['input-wrapper'] 返回 'input-wrapper'
      if (typeof prop === 'string') {
        return prop;
      }
      return undefined;
    },
  }
);
