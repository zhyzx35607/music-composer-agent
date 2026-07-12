import axios from 'axios';
import type { GenerateRequest, GenerateResponse, ReviseRequest, HistoryItem, ComplianceCheckRequest, ComplianceCheckResult, ComplianceHistoryItem, CopyrightRegisterRequest, CopyrightRecord } from '../types/api';
import { ENV } from './env';

// 创建 axios 实例
const apiClient = axios.create({
  baseURL: ENV.API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器 - 仅在开发环境记录
if (import.meta.env.DEV) {
  apiClient.interceptors.request.use(
    (config) => {
      console.log(`[API Request] ${config.method?.toUpperCase()} ${config.url}`);
      return config;
    },
    (error) => {
      console.error('[API Request Error]', error);
      return Promise.reject(error);
    }
  );
}

// 响应拦截器 - 自动提取 data 字段
// 后端响应格式: { code, message, data }
// 拦截器提取 data 字段并返回，调用方直接得到类型化的业务数据
apiClient.interceptors.response.use(
  function (response) {
    if (import.meta.env.DEV) {
      console.log(`[API Response] ${response.status} ${response.config.url}`, response.data);
    }
    // 如果响应数据包含 data 字段，则提取它
    if (response.data && typeof response.data === 'object' && 'data' in response.data) {
      return response.data;
    }
    return response;
  },
  function (error) {
    console.error('[API Response Error]', error);
    if (error.response) {
      const { status, data } = error.response;
      console.error(`[HTTP ${status}]`, data);
    }
    return Promise.reject(error);
  }
);

/**
 * API 服务层
 * 封装所有与后端的交互
 *
 * 注意：由于响应拦截器已经提取了 data 字段，
 * 这里的方法返回的是提取后的数据类型，而不是 AxiosResponse
 */
export const api = {
  /**
   * 首次生成音乐
   */
  generate: async (payload: GenerateRequest): Promise<GenerateResponse> => {
    const response = await apiClient.post<GenerateResponse>('/api/generate', payload);
    // response.data 已经是 GenerateResponse 类型（被拦截器处理过）
    return response.data as GenerateResponse;
  },

  /**
   * 反馈修改
   */
  revise: async (payload: ReviseRequest): Promise<GenerateResponse> => {
    const response = await apiClient.post<GenerateResponse>('/api/revise', payload);
    return response.data as GenerateResponse;
  },

  /**
   * 分页获取历史版本
   */
  getVersions: async (page: number = 0, size: number = 10): Promise<{
    items: HistoryItem[];
    total: number;
    totalPages: number;
    page: number;
    size: number;
  }> => {
    const response = await apiClient.get<{ items: HistoryItem[]; total: number; totalPages: number; page: number; size: number }>('/api/versions', { params: { page, size } });
    return response.data as { items: HistoryItem[]; total: number; totalPages: number; page: number; size: number };
  },

  /**
   * 获取单个版本详情
   */
  getVersion: async (versionId: string): Promise<HistoryItem> => {
    const response = await apiClient.get<HistoryItem>(`/api/version/${versionId}`);
    return response.data as HistoryItem;
  },

  /**
   * 健康检查
   */
  health: async (): Promise<{
    service: string;
    database: string;
    totalVersions: number;
    healthy: boolean;
  }> => {
    const response = await apiClient.get<{ service: string; database: string; totalVersions: number; healthy: boolean }>('/api/health');
    return response.data as { service: string; database: string; totalVersions: number; healthy: boolean };
  },

  /**
   * 合规检测 — 检测 AI 生成音乐是否侵权已有版权
   */
  checkCompliance: async (payload: ComplianceCheckRequest): Promise<ComplianceCheckResult> => {
    const response = await apiClient.post<ComplianceCheckResult>('/api/compliance/check', payload);
    return response.data as ComplianceCheckResult;
  },

  /**
   * 获取合规检测历史
   */
  getComplianceHistory: async (): Promise<ComplianceHistoryItem[]> => {
    const response = await apiClient.get<{ items: ComplianceHistoryItem[]; total: number; totalPages: number; page: number; size: number }>('/api/compliance/history');
    return (response.data as { items: ComplianceHistoryItem[] }).items;
  },

  /**
   * 版权存证 — 提交版权登记申请
   */
  registerCopyright: async (payload: CopyrightRegisterRequest): Promise<CopyrightRecord> => {
    const response = await apiClient.post<CopyrightRecord>('/api/copyright/register', payload);
    return response.data as CopyrightRecord;
  },

  /**
   * 获取版权存证记录列表
   */
  getCopyrightRecords: async (): Promise<CopyrightRecord[]> => {
    const response = await apiClient.get<{ items: CopyrightRecord[]; total: number; totalPages: number; page: number; size: number }>('/api/copyright/records');
    return (response.data as { items: CopyrightRecord[] }).items;
  },

  /**
   * 获取单条版权存证记录详情
   */
  getCopyrightRecord: async (recordId: string): Promise<CopyrightRecord> => {
    const response = await apiClient.get<CopyrightRecord>(`/api/copyright/record/${recordId}`);
    return response.data as CopyrightRecord;
  },
};

export default apiClient;
