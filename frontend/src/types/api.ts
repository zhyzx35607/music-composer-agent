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
  mock?: boolean
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
  created_at: string | number
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

// ============================
// 合规检测相关类型
// ============================

export type ComplianceCheckType = 'melody' | 'lyrics' | 'timbre' | 'all'

// 合规检测请求
export interface ComplianceCheckRequest {
  version_id: string
  check_type: ComplianceCheckType
}

// 合规检测结果 - 相似度详情
export interface ComplianceDetail {
  melody_similarity: number
  lyric_similarity: number
  timbre_similarity: number
}

// 匹配的作品
export interface MatchedWork {
  title: string
  artist: string
  similarity: number
  section: string
}

export type RiskLevel = 'low' | 'medium' | 'high'

// 合规检测结果
export interface ComplianceCheckResult {
  overall_score: number
  details: ComplianceDetail
  matched_works: MatchedWork[]
  risk_level: RiskLevel
  checked_at: string
}

// 合规检测历史记录
export interface ComplianceHistoryItem {
  id: string
  version_id: string
  check_type: ComplianceCheckType
  risk_level: RiskLevel
  overall_score: number
  checked_at: string
}

// ============================
// 版权存证相关类型
// ============================

// 版权存证请求
export interface CopyrightRegisterRequest {
  version_id: string
  creator_name: string
  creator_id?: string
}

// 版权存证记录
export interface CopyrightRecord {
  record_id: string
  version_id: string
  creator_name: string
  certificate_hash: string
  block_height: number
  created_at: string
  prompt_history: string[]
  revision_history: string[]
  status: 'pending' | 'confirmed' | 'verified'
}
