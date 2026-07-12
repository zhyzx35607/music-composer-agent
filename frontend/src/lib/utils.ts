/**
 * 工具函数库
 */

/**
 * 截断文本
 * @param text - 要截断的文本
 * @param maxLen - 最大长度
 * @returns 截断后的文本
 */
export const truncateText = (text: string, maxLen: number = 50): string => {
  if (!text) return '';
  return text.length > maxLen ? `${text.substring(0, maxLen)}...` : text;
};

/**
 * 生成唯一的 Toast ID
 * 使用时间戳 + 递增计数器避免冲突
 */
let toastCounter = 0;
export const generateToastId = (): string => {
  return `toast-${Date.now()}-${++toastCounter}`;
};

/**
 * 检查是否为 Mock URL
 */
export const isMockUrl = (url: string): boolean => {
  return !url || url.startsWith('/outputs/mock/');
};
