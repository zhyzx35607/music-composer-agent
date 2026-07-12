/**
 * Mock 数据配置
 * 仅在开发环境或 API 请求失败时使用
 */

import type { HistoryItem, GenerateResponse, CompositionPlan } from '../types/api';

// 作曲方案 Mock 数据
const mockPlan: CompositionPlan = {
  theme: 'Summer Beach',
  style: 'pop',
  mood: ['happy', 'energetic'],
  tempo: 120,
  key: 'C major',
  instruments: ['piano', 'drums', 'guitar'],
  structure: ['verse', 'chorus', 'bridge'],
};

// Mock 历史数据
export const mockHistory: HistoryItem[] = [
  {
    version_id: 'v1',
    parent_version_id: null,
    user_prompt: '一首欢快的流行歌曲，适合夏日海滩',
    style: 'pop',
    mood: 'happy',
    tempo: 'medium',
    instruments: ['piano', 'drums'],
    caption: 'A cheerful pop song perfect for a summer beach day',
    caption_preview: 'A cheerful pop song perfect for a summer beach day...',
    created_at: new Date(Date.now() - 86400000).toISOString(),
    midi_url: '/outputs/mock/composition-v1.mid',
    audio_url: '/outputs/mock/composition-v1.wav',
    plan: mockPlan,
  },
];

// Mock 当前版本数据
export const mockCurrentVersion: GenerateResponse = {
  version_id: 'v1',
  caption: 'A cheerful pop song perfect for a summer beach day',
  midi_url: '/outputs/mock/composition-v1.mid',
  audio_url: '/outputs/mock/composition-v1.wav',
  plan: mockPlan,
};

// 工具函数：检查是否应使用 mock 模式
export const shouldUseMock = (): boolean => {
  // 通过环境变量控制
  // VITE_MOCK_MODE 可以设置为: "true" | "false" | "auto"
  const mode = import.meta.env.VITE_MOCK_MODE;

  if (mode === 'true') return true;
  if (mode === 'false') return false;

  // auto 模式：开发环境默认使用 mock，生产环境使用真实 API
  return import.meta.env.DEV;
};
