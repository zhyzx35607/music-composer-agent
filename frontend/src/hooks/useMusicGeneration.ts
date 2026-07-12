import { useCallback } from 'react';
import { api } from '../lib/api';
import type { GenerateRequest, GenerateResponse, HistoryItem } from '../types/api';

/**
 * 将 GenerateRequest 转换为 API 需要的格式
 * 处理可选参数，移除未定义的属性
 */
const transformRequest = (request: {
  user_prompt: string;
  style: string;
  mood: string;
  tempo: string;
  instruments: string[];
}): GenerateRequest => {
  const result: GenerateRequest = {
    user_prompt: request.user_prompt,
  };

  // 只添加有值的可选参数
  if (request.style) {
    result.style = request.style;
  }
  if (request.mood) {
    result.mood = request.mood;
  }
  if (request.tempo) {
    result.tempo = request.tempo;
  }
  if (request.instruments.length > 0) {
    result.instruments = request.instruments;
  }

  return result;
};

/**
 * 构建 HistoryItem 用于更新本地状态
 */
const buildHistoryItem = (
  result: GenerateResponse,
  request: {
    user_prompt: string;
    style: string;
    mood: string;
    tempo: string;
    instruments: string[];
  },
  parentVersionId?: string
): HistoryItem => {
  return {
    version_id: result.version_id,
    parent_version_id: parentVersionId || null,
    user_prompt: request.user_prompt,
    style: request.style || '',
    mood: request.mood || '',
    tempo: request.tempo || '',
    instruments: request.instruments.length > 0 ? request.instruments : ['piano'],
    caption: result.caption,
    caption_preview: result.caption.length > 50
      ? result.caption.substring(0, 50) + '...'
      : result.caption,
    created_at: new Date().toISOString(),
    midi_url: result.midi_url,
    audio_url: result.audio_url,
    plan: result.plan,
  };
};

/**
 * 自定义 Hook：处理音乐生成相关的异步操作
 * 将生成/修订逻辑从 Context 中抽离，提高可维护性
 */
export function useMusicGeneration() {
  /**
   * 生成音乐
   */
  const generate = useCallback(
    async (request: {
      user_prompt: string;
      style: string;
      mood: string;
      tempo: string;
      instruments: string[];
    }): Promise<GenerateResponse> => {
      const transformedRequest = transformRequest(request);
      const result = await api.generate(transformedRequest);

      return result;
    },
    []
  );

  /**
   * 修订音乐
   */
  const revise = useCallback(
    async (
      versionId: string,
      feedback: string
    ): Promise<GenerateResponse> => {
      const result = await api.revise({
        version_id: versionId,
        feedback: feedback,
      });

      return result;
    },
    []
  );

  /**
   * 加载指定版本
   */
  const loadVersion = useCallback(
    async (versionId: string): Promise<GenerateResponse> => {
      const version = await api.getVersion(versionId);

      if (!version.caption || !version.midi_url || !version.audio_url || !version.plan) {
        throw new Error('版本数据不完整');
      }

      return {
        version_id: version.version_id,
        caption: version.caption,
        midi_url: version.midi_url,
        audio_url: version.audio_url,
        plan: version.plan,
      };
    },
    []
  );

  /**
   * 构建 HistoryItem（不调用 API，仅用于本地状态更新）
   */
  const buildGenerateHistoryItem = useCallback(
    (
      result: GenerateResponse,
      request: {
        user_prompt: string;
        style: string;
        mood: string;
        tempo: string;
        instruments: string[];
      }
    ): HistoryItem => {
      return buildHistoryItem(result, request);
    },
    []
  );

  const buildReviseHistoryItem = useCallback(
    (
      result: GenerateResponse,
      parentVersionId: string,
      request: {
        user_prompt: string;
        style: string;
        mood: string;
        tempo: string;
        instruments: string[];
      }
    ): HistoryItem => {
      return buildHistoryItem(result, request, parentVersionId);
    },
    []
  );

  return {
    generate,
    revise,
    loadVersion,
    buildGenerateHistoryItem,
    buildReviseHistoryItem,
  };
}

export default useMusicGeneration;
