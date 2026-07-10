import { FileAudio, FileMusic } from 'lucide-react'
import { useState, useEffect } from 'react'
import { isMockUrl } from '../lib/config'

interface DownloadButtonsProps {
  midiUrl: string
  audioUrl: string
}

export default function DownloadButtons({ midiUrl, audioUrl }: DownloadButtonsProps) {
  const [isDownloading, setIsDownloading] = useState<string | null>(null)

  useEffect(() => {
    // 清理下载状态，防止长时间处于下载中
    if (isDownloading) {
      const timer = setTimeout(() => {
        setIsDownloading(null)
      }, 5000)
      return () => clearTimeout(timer)
    }
    return undefined
  }, [isDownloading])

  const handleDownload = async (url: string, filename: string) => {
    if (!url) {
      console.error('Download URL is empty')
      return
    }

    // 检查 URL 是否为 mock URL
    if (isMockUrl(url)) {
      alert('预览文件暂不可用，请等待正式文件生成')
      return
    }

    setIsDownloading(filename)

    try {
      // 尝试先获取文件头信息，检查 URL 是否有效
      const response = await fetch(url, { method: 'HEAD' })
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      // 创建 blob URL 用于下载
      const responseBlob = await fetch(url)
      const blob = await responseBlob.blob()
      const blobUrl = URL.createObjectURL(blob)

      const link = document.createElement('a')
      link.href = blobUrl
      link.download = filename
      link.style.display = 'none'
      document.body.appendChild(link)

      // 触发下载
      await new Promise((resolve, reject) => {
        link.addEventListener('click', resolve)
        link.addEventListener('error', reject)
        link.click()
      })

      document.body.removeChild(link)

      // 清理 blob URL
      setTimeout(() => {
        URL.revokeObjectURL(blobUrl)
      }, 100)
    } catch (error) {
      console.error(`Download failed for ${filename}:`, error)
      alert(`下载 ${filename} 失败，请检查网络连接或文件是否存在`)
    } finally {
      setIsDownloading(null)
    }
  }

  return (
    <div className="flex flex-wrap gap-3">
      <button
        onClick={() => handleDownload(midiUrl, 'composition.mid')}
        disabled={isDownloading !== null}
        className="flex items-center gap-2 px-5 py-3 bg-muted hover:bg-muted/80 rounded-xl font-medium transition-all duration-200 text-foreground border border-border hover:border-primary/30 disabled:opacity-50 disabled:cursor-not-allowed"
      >
        <FileMusic className="h-5 w-5 text-muted-foreground" />
        <span>{isDownloading === 'composition.mid' ? '下载中...' : '下载 MIDI'}</span>
      </button>

      <button
        onClick={() => handleDownload(audioUrl, 'composition.wav')}
        disabled={isDownloading !== null}
        className="flex items-center gap-2 px-5 py-3 bg-gradient-to-r from-primary to-secondary hover:from-secondary hover:to-primary rounded-xl font-medium transition-all duration-200 text-on-primary shadow-lg shadow-primary/20 disabled:opacity-50 disabled:cursor-not-allowed"
      >
        <FileAudio className="h-5 w-5" />
        <span>{isDownloading === 'composition.wav' ? '下载中...' : '下载 WAV'}</span>
      </button>
    </div>
  )
}
