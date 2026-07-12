/**
 * 应用配置
 * 统一管理应用的常量和配置
 */

// Mock 数据配置
export const MOCK_URL_PREFIX = '/outputs/mock/';

/**
 * 检查是否为 Mock URL
 */
export const isMockUrl = (url: string): boolean => {
  return !url || url.startsWith(MOCK_URL_PREFIX);
};

// API 配置
export const API_CONFIG = {
  timeout: 30000,
  mockMode: import.meta.env.VITE_MOCK_MODE !== 'false', // 默认启用 mock 模式
};

// UI 配置
export const UI_CONFIG = {
  toast: {
    defaultDuration: 3000,
    maxToasts: 5,
  },
  pagination: {
    defaultPageSize: 10,
    maxPageSize: 100,
  },
};

// 本地存储键名
export const STORAGE_KEYS = {
  HISTORY: 'composer_history',
  THEME: 'composer_theme',
  USER_PREFERENCES: 'composer_preferences',
};
