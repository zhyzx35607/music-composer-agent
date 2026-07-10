/**
 * 统一错误处理工具函数
 * 用于处理 API 调用失败时的降级逻辑
 */

import type { HistoryItem, GenerateResponse } from '../types/api';
import { mockCurrentVersion, mockHistory } from './mockData';

export interface ErrorHandlingResult<T> {
  fallbackUsed: boolean;
  result: T | null;
  error: Error | string;
}

/**
 * 处理 API 错误并返回降级数据
 * @param operation - 操作名称，用于日志
 * @param error - 捕获的错误
 * @param fallbackData - 降级数据生成函数
 * @returns 包含是否使用降级、结果和错误信息的对象
 */
export function handleApiError<T>(
  operation: string,
  error: unknown,
  fallbackData: () => T
): ErrorHandlingResult<T> {
  const errorMessage = error instanceof Error ? error.message : '操作失败，请重试';
  console.warn(`${operation} failed, using fallback data in development mode`, error);

  return {
    fallbackUsed: true,
    result: fallbackData(),
    error: errorMessage,
  };
}

/**
 * 处理 API 错误并抛出，同时设置错误状态
 * 用于 Context 中需要设置错误状态的场景
 * @param setError - 设置错误状态的函数
 * @param setIsLoading - 设置 loading 状态的函数（可选）
 * @param operation - 操作名称
 * @param error - 捕获的错误
 * @returns 抛出错误以便调用方处理
 */
export function throwWithError(
  setError: React.Dispatch<React.SetStateAction<string | null>>,
  setIsLoading?: React.Dispatch<React.SetStateAction<boolean>>,
  operation?: string,
  error?: unknown
): never {
  const errorMessage = error instanceof Error ? error.message : '操作失败，请重试';
  const opName = operation || '操作';
  console.warn(`${opName} failed, using fallback data in development mode`, error);

  setError(errorMessage);
  if (setIsLoading) {
    setIsLoading(false);
  }
  throw error;
}

/**
 * 处理版本加载错误，返回降级的 GenerateResponse
 */
export function handleVersionError(
  setError: React.Dispatch<React.SetStateAction<string | null>>,
  setIsLoading?: React.Dispatch<React.SetStateAction<boolean>>
): GenerateResponse {
  setError('加载版本失败，请重试');
  if (setIsLoading) {
    setIsLoading(false);
  }
  console.warn('Failed to load version, using mock data');
  return mockCurrentVersion;
}

/**
 * 处理历史记录加载错误
 */
export function handleHistoryError(
  setError: React.Dispatch<React.SetStateAction<string | null>>,
  setHistory: React.Dispatch<React.SetStateAction<HistoryItem[]>>
): HistoryItem[] {
  setError('加载历史版本失败，请重试');
  console.warn('Failed to load history, using mock data');
  setHistory(mockHistory);
  return mockHistory;
}
