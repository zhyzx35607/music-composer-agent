import { useState, useEffect } from 'react'
import type { GenerateResponse, HistoryItem } from '../types/api'

// Mock 数据，开发阶段使用
const mockVersions: HistoryItem[] = [
  {
    version_id: 'v1',
    parent_version_id: null,
    user_prompt: '我想要一首毕业季的歌，抒情一点',
    style: 'pop ballad',
    mood: 'nostalgic',
    tempo: 'medium',
    instruments: ['piano', 'strings'],
    caption: 'A warm nostalgic pop ballad instrumental about graduation farewell',
    caption_preview: 'A warm nostalgic pop ballad instrumental...',
    created_at: new Date().toISOString(),
    midi_url: '/outputs/v1/v1.mid',
    audio_url: '/outputs/v1/v1.wav',
    plan: {
      theme: 'graduation farewell',
      style: 'pop ballad',
      mood: ['nostalgic', 'warm'],
      tempo: 88,
      key: 'C major',
      instruments: ['piano', 'strings'],
      structure: ['intro', 'verse', 'chorus', 'outro']
    }
  }
]

// 模拟 API 调用
const mockGenerate = async (request: {
  user_prompt: string
  style: string
  mood: string
  tempo: string
  instruments: string[]
}): Promise<GenerateResponse> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        version_id: 'v1',
        caption: `A ${request.mood} ${request.style} instrumental piece featuring ${request.instruments.join(', ')}.`,
        midi_url: '/outputs/v1/v1.mid',
        audio_url: '/outputs/v1/v1.wav',
        plan: {
          theme: request.user_prompt.substring(0, 20),
          style: request.style,
          mood: [request.mood],
          tempo: request.tempo === 'slow' ? 72 : request.tempo === 'fast' ? 120 : 88,
          key: 'C major',
          instruments: request.instruments,
          structure: ['intro', 'verse', 'chorus', 'outro']
        }
      })
    }, 1500)
  })
}

export function useMusicGeneration() {
  const [isLoading, setIsLoading] = useState(false)
  const [currentVersion, setCurrentVersion] = useState<GenerateResponse | null>(null)
  const [history, setHistory] = useState<HistoryItem[]>([])

  // 开发模式下使用 mock 数据初始化
  useEffect(() => {
    if (window.location.href.includes('localhost') && history.length === 0) {
      setHistory(mockVersions)
      // Set currentVersion with required fields
      const firstVersion = mockVersions[0]
      if (firstVersion.caption && firstVersion.midi_url && firstVersion.audio_url && firstVersion.plan) {
        setCurrentVersion({
          version_id: firstVersion.version_id,
          caption: firstVersion.caption,
          midi_url: firstVersion.midi_url,
          audio_url: firstVersion.audio_url,
          plan: firstVersion.plan
        })
      }
    }
  }, [])

  const generate = async (request: {
    user_prompt: string
    style: string
    mood: string
    tempo: string
    instruments: string[]
  }) => {
    setIsLoading(true)
    try {
      // 使用 mock 数据代替真实 API 调用
      const result = await mockGenerate(request)
      setCurrentVersion(result)
      // Convert GenerateResponse to HistoryItem for history
      const historyItem: HistoryItem = {
        version_id: result.version_id,
        parent_version_id: null,
        user_prompt: request.user_prompt,
        style: request.style,
        mood: request.mood,
        tempo: request.tempo,
        instruments: request.instruments,
        caption: result.caption,
        caption_preview: result.caption.length > 50 ? result.caption.substring(0, 50) + '...' : result.caption,
        created_at: new Date().toISOString(),
        midi_url: result.midi_url,
        audio_url: result.audio_url,
        plan: result.plan
      }
      setHistory(prev => [historyItem, ...prev])
      return result
    } catch (error) {
      console.error('Generation failed:', error)
      throw error
    } finally {
      setIsLoading(false)
    }
  }

  const resetCurrentVersion = () => {
    setCurrentVersion(null)
  }

  return {
    isLoading,
    currentVersion,
    setCurrentVersion,
    history,
    generate,
    resetCurrentVersion
  }
}
