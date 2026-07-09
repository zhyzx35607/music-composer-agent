// 作曲方案类型
export interface CompositionPlan {
  theme: string
  style: string
  mood: string[]
  tempo: number | string
  key: string
  instruments: string[]
  structure: string[]
}

// 生成请求类型
export interface GenerateRequest {
  user_prompt: string
  style?: string
  mood?: string
  tempo?: string
  instruments?: string[]
}

// 修订请求类型
export interface ReviseRequest {
  version_id: string
  feedback: string
}

// 生成响应类型
export interface GenerateResponse {
  version_id: string
  caption: string
  midi_url: string
  audio_url: string
  plan: CompositionPlan
}

// 历史版本摘要
export interface HistoryItem {
  version_id: string
  parent_version_id: string | null
  user_prompt: string
  style: string
  mood: string
  tempo?: string
  instruments?: string[]
  feedback?: string
  caption?: string
  caption_preview?: string
  created_at: string | number | null
  // 可选的完整信息
  midi_url?: string
  audio_url?: string
  plan?: CompositionPlan
}

// API 统一响应类型
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}
