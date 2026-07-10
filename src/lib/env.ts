/**
 * 环境配置
 * 用于统一管理应用的环境变量
 */

export const ENV = {
  // 环境标识
  isDev: import.meta.env.DEV,
  isProd: import.meta.env.PROD,

  // API 配置
  API_BASE_URL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',

  // 应用配置
  APP_NAME: import.meta.env.VITE_APP_NAME || 'Composer AI',
  APP_VERSION: import.meta.env.VITE_APP_VERSION || '1.0.0',
};
