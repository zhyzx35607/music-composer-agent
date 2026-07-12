import { useState, useEffect, useCallback } from 'react'
import { Shield, FileText, Hash, Activity, Stamp, Clock, User, Music } from 'lucide-react'
import { useAPI } from '../lib/apiContext'
import { api } from '../lib/api'
import { useToast } from '../components/useToast'
import { truncateText } from '../lib/utils'
import type { CopyrightRecord } from '../types/api'

const statusStyles: Record<string, { bg: string; text: string; label: string }> = {
  pending: { bg: 'bg-amber-50 border-amber-200', text: 'text-amber-700', label: '存证中' },
  confirmed: { bg: 'bg-emerald-50 border-emerald-200', text: 'text-emerald-700', label: '已确认' },
  verified: { bg: 'bg-blue-50 border-blue-200', text: 'text-blue-700', label: '已验证' },
}

export default function CopyrightPage() {
  const { history } = useAPI()
  const { success, error: addError, info } = useToast()

  const [selectedVersionId, setSelectedVersionId] = useState('')
  const [creatorName, setCreatorName] = useState('')
  const [isRegistering, setIsRegistering] = useState(false)
  const [records, setRecords] = useState<CopyrightRecord[]>([])
  const [selectedRecord, setSelectedRecord] = useState<CopyrightRecord | null>(null)

  // 加载存证记录
  const loadRecords = useCallback(async () => {
    try {
      const data = await api.getCopyrightRecords()
      setRecords(data)
    } catch {
      info('暂时无法加载存证记录')
    }
  }, [info])

  // 处理存证申请
  const handleRegister = async () => {
    if (!selectedVersionId) {
      addError('请先选择一个版本')
      return
    }
    if (!creatorName.trim()) {
      addError('请输入创作者姓名')
      return
    }

    setIsRegistering(true)
    try {
      const record = await api.registerCopyright({
        version_id: selectedVersionId,
        creator_name: creatorName,
      })
      setRecords(prev => [record, ...prev])
      success('版权存证提交成功！')
      setSelectedVersionId('')
      setCreatorName('')
      setSelectedRecord(record)
    } catch {
      addError('存证失败，请重试')
    } finally {
      setIsRegistering(false)
    }
  }

  // 加载存证记录
  useEffect(() => {
    loadRecords()
  }, [loadRecords])

  return (
    <div className="max-w-5xl mx-auto px-4 py-8 space-y-8">
      {/* 页面标题 */}
      <div className="text-center space-y-4 animate-in fade-in slide-in-from-bottom-4 duration-500">
        <div className="inline-flex items-center justify-center h-16 w-16 rounded-2xl bg-gradient-to-br from-purple-600 to-pink-500 shadow-lg shadow-purple-500/20 mb-2">
          <Stamp className="h-8 w-8 text-white" />
        </div>
        <h1 className="text-4xl md:text-5xl font-bold text-foreground tracking-tight">
          AI 音乐版权存证
        </h1>
        <p className="text-xl text-muted-foreground max-w-2xl mx-auto">
          记录创作全流程，为版权归属提供区块链存证证据链
        </p>
      </div>

      {/* 存证申请区 */}
      <div className="bg-card rounded-3xl p-6 sm:p-8 border border-border shadow-xl shadow-purple-500/10 space-y-6 animate-in fade-in slide-in-from-bottom-6 duration-500 delay-100">
        <h2 className="text-xl font-bold text-foreground flex items-center gap-3">
          <Stamp className="h-6 w-6 text-primary" />
          提交版权存证
        </h2>

        {/* 版本选择 */}
        <div className="space-y-3">
          <label className="flex items-center gap-2 text-sm font-semibold text-foreground">
            <Music className="h-4 w-4 text-primary" />
            <span>选择音乐版本</span>
          </label>
          <select
            value={selectedVersionId}
            onChange={(e) => setSelectedVersionId(e.target.value)}
            className="w-full px-4 py-3.5 bg-muted border border-border rounded-xl text-sm focus:ring-2 focus:ring-primary focus:border-primary transition-all"
            disabled={isRegistering}
          >
            <option value="">请选择要存证的音乐版本</option>
            {history.map((v) => (
              <option key={v.version_id} value={v.version_id}>
                {v.version_id} - {truncateText(v.caption_preview || v.caption || '无描述', 40)}
              </option>
            ))}
          </select>
          {selectedVersionId && (
            <div className="flex items-center gap-2 text-xs text-muted-foreground bg-purple-50 px-3 py-2 rounded-lg">
              <Clock className="h-3 w-3" />
              <span>提示词交互过程和修改稿历史将一并存证</span>
            </div>
          )}
        </div>

        {/* 创作者信息 */}
        <div className="space-y-3">
          <label className="flex items-center gap-2 text-sm font-semibold text-foreground">
            <User className="h-4 w-4 text-primary" />
            <span>创作者姓名</span>
          </label>
          <input
            type="text"
            value={creatorName}
            onChange={(e) => setCreatorName(e.target.value)}
            placeholder="输入创作者姓名（与身份证一致）"
            className="w-full px-4 py-3.5 bg-muted border border-border rounded-xl text-sm focus:ring-2 focus:ring-primary focus:border-primary transition-all disabled:opacity-50"
            disabled={isRegistering}
          />
        </div>

        {/* 提示框 */}
        <div className="bg-gradient-to-r from-purple-50 to-pink-50 rounded-xl p-4 border border-purple-100">
          <div className="flex items-start gap-3">
            <FileText className="h-5 w-5 text-primary mt-0.5 flex-shrink-0" />
            <div className="space-y-1">
              <p className="text-sm font-medium text-foreground">存证信息说明</p>
              <p className="text-xs text-muted-foreground">
                根据《区块链版权存证规范》要求，本次存证将记录：用户输入的中文提示词、选择的音乐参数、
                生成的作曲方案（Plan）、英文描述（Caption）、修改反馈记录，以及最终的 MIDI/WAV 文件哈希值。
                所有数据将通过区块链存证，为版权归属提供可信证据链。
              </p>
            </div>
          </div>
        </div>

        {/* 提交按钮 */}
        <button
          onClick={handleRegister}
          disabled={!selectedVersionId || !creatorName.trim() || isRegistering}
          className="w-full py-4 bg-gradient-to-r from-primary via-purple-600 to-accent text-on-primary rounded-xl font-semibold text-lg shadow-xl shadow-purple-500/30 hover:shadow-purple-500/40 hover:scale-[1.02] active:scale-[0.98] transition-all duration-200 disabled:opacity-60 disabled:scale-100 disabled:cursor-not-allowed flex items-center justify-center gap-3"
        >
          {isRegistering ? (
            <>
              <div className="h-5 w-5 animate-spin rounded-full border-2 border-on-primary border-t-transparent" />
              <span>提交存证中...</span>
            </>
          ) : (
            <>
              <Stamp className="h-5 w-5" />
              <span>提交版权存证</span>
            </>
          )}
        </button>
      </div>

      {/* 存证记录 */}
      <div className="bg-card rounded-3xl p-6 sm:p-8 border border-border shadow-xl shadow-purple-500/10 space-y-6 animate-in fade-in slide-in-from-bottom-8 duration-700">
        <h2 className="text-xl font-bold text-foreground flex items-center gap-3">
          <Shield className="h-6 w-6 text-primary" />
          存证记录
        </h2>

        {records.length === 0 && (
          <div className="text-center py-12">
            <div className="inline-flex items-center justify-center h-20 w-20 rounded-2xl bg-muted mb-4">
              <Stamp className="h-10 w-10 text-muted-foreground/50" />
            </div>
            <p className="text-muted-foreground">暂无存证记录</p>
            <p className="text-xs text-muted-foreground mt-1">选择版本并提交存证后，记录将显示在这里</p>
          </div>
        )}

        {records.map((record) => (
          <div
            key={record.record_id}
            className={`rounded-2xl p-6 border transition-all duration-200 cursor-pointer hover:shadow-md ${
              selectedRecord?.record_id === record.record_id
                ? 'border-primary ring-1 ring-primary/20 bg-gradient-to-br from-primary/5 to-transparent'
                : 'border-border hover:border-primary/30'
            }`}
            onClick={() => setSelectedRecord(selectedRecord?.record_id === record.record_id ? null : record)}
          >
            <div className="flex items-start justify-between mb-4">
              <div className="flex items-center gap-4">
                <div className={`h-12 w-12 rounded-xl flex items-center justify-center ${statusStyles[record.status].bg}`}>
                  <Shield className={`h-6 w-6 ${statusStyles[record.status].text}`} />
                </div>
                <div>
                  <div className="flex items-center gap-3">
                    <h3 className="font-semibold text-foreground">存证 #{record.record_id}</h3>
                    <span className={`text-xs px-2.5 py-1 rounded-full font-medium ${statusStyles[record.status].bg} ${statusStyles[record.status].text}`}>
                      {statusStyles[record.status].label}
                    </span>
                  </div>
                  <p className="text-sm text-muted-foreground mt-1">
                    {record.creator_name} · 版本 {record.version_id}
                  </p>
                </div>
              </div>
              <span className="text-xs text-muted-foreground">
                {new Date(record.created_at).toLocaleString('zh-CN')}
              </span>
            </div>

            {selectedRecord?.record_id === record.record_id && (
              <div className="space-y-4 pt-4 border-t border-border animate-in fade-in slide-in-from-top-2">
                {/* 区块链信息 */}
                <div className="grid grid-cols-2 gap-4">
                  <div className="p-4 bg-muted/50 rounded-xl border border-border">
                    <div className="flex items-center gap-2 text-sm text-muted-foreground mb-1">
                      <Hash className="h-4 w-4" />
                      <span>区块高度</span>
                    </div>
                    <p className="font-mono text-sm font-medium text-foreground">
                      {record.block_height.toLocaleString()}
                    </p>
                  </div>
                  <div className="p-4 bg-muted/50 rounded-xl border border-border">
                    <div className="flex items-center gap-2 text-sm text-muted-foreground mb-1">
                      <Activity className="h-4 w-4" />
                      <span>证书哈希</span>
                    </div>
                    <p className="font-mono text-xs text-foreground break-all">
                      {record.certificate_hash}
                    </p>
                  </div>
                </div>

                {/* 交互过程 */}
                <div className="p-4 bg-muted/50 rounded-xl border border-border">
                  <div className="flex items-center gap-2 text-sm text-muted-foreground mb-2">
                    <FileText className="h-4 w-4" />
                    <span>提示词交互过程</span>
                  </div>
                  {record.prompt_history.length > 0 ? (
                    <div className="space-y-2">
                      {record.prompt_history.map((prompt, i) => (
                        <p key={i} className="text-sm text-foreground">
                          <span className="text-muted-foreground">#{i + 1}:</span> {prompt}
                        </p>
                      ))}
                    </div>
                  ) : (
                    <p className="text-sm text-muted-foreground italic">暂无交互记录</p>
                  )}
                </div>

                {/* 修改稿历史 */}
                <div className="p-4 bg-muted/50 rounded-xl border border-border">
                  <div className="flex items-center gap-2 text-sm text-muted-foreground mb-2">
                    <FileText className="h-4 w-4" />
                    <span>修改稿历史</span>
                  </div>
                  {record.revision_history.length > 0 ? (
                    <div className="space-y-2">
                      {record.revision_history.map((revision, i) => (
                        <p key={i} className="text-sm text-foreground">
                          <span className="text-muted-foreground">#{i + 1}:</span> {revision}
                        </p>
                      ))}
                    </div>
                  ) : (
                    <p className="text-sm text-muted-foreground italic">暂无修改记录</p>
                  )}
                </div>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}
