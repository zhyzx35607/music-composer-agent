import type { HistoryItem } from '../types/api'
import { useAPI } from '../lib/apiContext'

/**
 * 自定义 Hook：用于处理历史版本选择的逻辑
 * 提取 CreatePage 和 HistoryPage 中的重复逻辑
 */
export function useHistorySelector() {
  const { setCurrentVersion } = useAPI()

  const handleSelect = (v: HistoryItem) => {
    if (v.plan && v.midi_url && v.audio_url && v.caption) {
      setCurrentVersion({
        version_id: v.version_id,
        caption: v.caption,
        midi_url: v.midi_url,
        audio_url: v.audio_url,
        plan: v.plan
      })
    }
  }

  const loadVersionData = (v: HistoryItem) => {
    if (v.plan && v.midi_url && v.audio_url && v.caption) {
      return {
        version_id: v.version_id,
        caption: v.caption,
        midi_url: v.midi_url,
        audio_url: v.audio_url,
        plan: v.plan
      }
    }
    return null
  }

  return { handleSelect, loadVersionData }
}
