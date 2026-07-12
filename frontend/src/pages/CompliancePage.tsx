import { useState } from 'react'
import { Shield, Music, FileText, Headphones, AlertTriangle, CheckCircle, AlertCircle, History } from 'lucide-react'
import { useAPI } from '../lib/apiContext'
import { api } from '../lib/api'
import { useToast } from '../components/useToast'
import { truncateText } from '../lib/utils'
import type { ComplianceCheckResult, ComplianceCheckType, ComplianceHistoryItem, RiskLevel } from '../types/api'

// 风险等级样式映射
const riskStyles: Record<RiskLevel, { bg: string; text: string; icon: React.ElementType }> = {
  low: { bg: 'bg-emerald-50 border-emerald-200', text: 'text-emerald-700', icon: CheckCircle },
  medium: { bg: 'bg-amber-50 border-amber-200', text: 'text-amber-700', icon: AlertTriangle },
  high: { bg: 'bg-red-50 border-red-200', text: 'text-red-700', icon: AlertCircle },
}

const riskLabels: Record<RiskLevel, string> = {
  low: '低风险',
  medium: '中风险',
  high: '高风险',
}

function ScoreGauge({ label, score, icon: Icon, color }: { label: string; score: number; icon: React.ElementType; color: string }) {
  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
          <Icon className="h-4 w-4" />
          <span>{label}</span>
        </div>
        <span className={`text-sm font-bold ${color}`}>{score.toFixed(1)}%</span>
      </div>
      <div className="w-full h-2.5 bg-muted rounded-full overflow-hidden">
        <div
          className={`h-full rounded-full transition-all duration-700 ease-out ${color.replace('text-', 'bg-')}`}
          style={{ width: `${score}%` }}
        />
      </div>
    </div>
  )
}

const checkTypeLabels: Record<ComplianceCheckType, string> = {
  melody: '旋律相似度',
  lyrics: '歌词对比',
  timbre: '音色识别',
  all: '全面检测',
}

export default function CompliancePage() {
  const { history } = useAPI()
  const { success, error: addError } = useToast()

  const [selectedVersionId, setSelectedVersionId] = useState('')
  const [checkType, setCheckType] = useState<ComplianceCheckType>('all')
  const [isChecking, setIsChecking] = useState(false)
  const [result, setResult] = useState<ComplianceCheckResult | null>(null)
  const [historyList] = useState<ComplianceHistoryItem[]>([])

  const handleCheck = async () => {
    if (!selectedVersionId) {
      addError('请先选择一个版本')
      return
    }

    setIsChecking(true)
    setResult(null)

    try {
      const data = await api.checkCompliance({
        version_id: selectedVersionId,
        check_type: checkType,
      })
      setResult(data)
      success('检测完成')
    } catch {
      addError('检测失败，请重试')
    } finally {
      setIsChecking(false)
    }
  }

  return (
    <div className="max-w-5xl mx-auto px-4 py-8 space-y-8">
      {/* 页面标题 */}
      <div className="text-center space-y-4 animate-in fade-in slide-in-from-bottom-4 duration-500">
        <div className="inline-flex items-center justify-center h-16 w-16 rounded-2xl bg-gradient-to-br from-purple-600 to-pink-500 shadow-lg shadow-purple-500/20 mb-2">
          <Shield className="h-8 w-8 text-white" />
        </div>
        <h1 className="text-4xl md:text-5xl font-bold text-foreground tracking-tight">
          AI 音乐版权合规检测
        </h1>
        <p className="text-xl text-muted-foreground max-w-2xl mx-auto">
          检测 AI 生成音乐是否与已有版权作品存在相似，保障创作合规
        </p>
      </div>

      {/* 检测配置区 */}
      <div className="bg-card rounded-3xl p-6 sm:p-8 border border-border shadow-xl shadow-purple-500/10 space-y-6 animate-in fade-in slide-in-from-bottom-6 duration-500 delay-100">
        {/* 版本选择 */}
        <div className="space-y-3">
          <label className="flex items-center gap-3 text-sm font-semibold text-foreground">
            <Music className="h-5 w-5 text-primary" />
            <span>选择版本</span>
          </label>
          <select
            value={selectedVersionId}
            onChange={(e) => setSelectedVersionId(e.target.value)}
            className="w-full px-4 py-3.5 bg-muted border border-border rounded-xl text-sm focus:ring-2 focus:ring-primary focus:border-primary transition-all"
            disabled={isChecking}
          >
            <option value="">请选择一个音乐版本</option>
            {history.map((v) => (
              <option key={v.version_id} value={v.version_id}>
                {v.version_id} - {truncateText(v.caption_preview || v.caption || '无描述', 40)}
              </option>
            ))}
          </select>
        </div>

        {/* 检测类型 */}
        <div className="space-y-3">
          <label className="flex items-center gap-3 text-sm font-semibold text-foreground">
            <FileText className="h-5 w-5 text-primary" />
            <span>检测类型</span>
          </label>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            {(Object.entries(checkTypeLabels) as [ComplianceCheckType, string][]).map(([key, label]) => (
              <button
                key={key}
                onClick={() => setCheckType(key)}
                disabled={isChecking}
                className={`px-4 py-3 rounded-xl text-sm font-medium transition-all duration-200 ${
                  checkType === key
                    ? 'bg-primary text-on-primary shadow-lg shadow-primary/30 scale-105'
                    : 'bg-muted text-muted-foreground hover:bg-primary hover:text-on-primary'
                } disabled:opacity-50 disabled:cursor-not-allowed`}
              >
                {label}
              </button>
            ))}
          </div>
        </div>

        {/* 检测按钮 */}
        <button
          onClick={handleCheck}
          disabled={!selectedVersionId || isChecking}
          className="w-full py-4 bg-gradient-to-r from-primary via-purple-600 to-accent text-on-primary rounded-xl font-semibold text-lg shadow-xl shadow-purple-500/30 hover:shadow-purple-500/40 hover:scale-[1.02] active:scale-[0.98] transition-all duration-200 disabled:opacity-60 disabled:scale-100 disabled:cursor-not-allowed flex items-center justify-center gap-3"
        >
          {isChecking ? (
            <>
              <div className="h-5 w-5 animate-spin rounded-full border-2 border-on-primary border-t-transparent" />
              <span>检测中...</span>
            </>
          ) : (
            <>
              <Shield className="h-5 w-5" />
              <span>开始检测</span>
            </>
          )}
        </button>
      </div>

      {/* 检测结果 */}
      {result && (
        <div className="space-y-6 animate-in fade-in slide-in-from-bottom-8 duration-700">
          {/* 总体风险 */}
          <div className={`rounded-3xl p-8 border-2 shadow-xl ${riskStyles[result.risk_level].bg} ${riskStyles[result.risk_level].text}`}>
            <div className="flex items-center justify-between mb-6">
              <div className="flex items-center gap-3">
                {(() => {
                  const Icon = riskStyles[result.risk_level].icon
                  return <Icon className="h-8 w-8" />
                })()}
                <div>
                  <h3 className="text-xl font-bold">风险等级：{riskLabels[result.risk_level]}</h3>
                  <p className="text-sm opacity-80 mt-1">
                    检测时间：{new Date(result.checked_at).toLocaleString('zh-CN')}
                  </p>
                </div>
              </div>
              <div className="text-right">
                <p className="text-3xl font-bold">{result.overall_score.toFixed(1)}%</p>
                <p className="text-sm opacity-80">总体相似度</p>
              </div>
            </div>

            {/* 三维度评分 */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
              <ScoreGauge
                label="旋律相似度"
                score={result.details.melody_similarity}
                icon={Music}
                color="text-primary"
              />
              <ScoreGauge
                label="歌词对比"
                score={result.details.lyric_similarity}
                icon={FileText}
                color="text-accent"
              />
              <ScoreGauge
                label="音色识别"
                score={result.details.timbre_similarity}
                icon={Headphones}
                color="text-secondary"
              />
            </div>
          </div>

          {/* 匹配作品列表 */}
          {result.matched_works.length > 0 && (
            <div className="bg-card rounded-3xl p-6 sm:p-8 border border-border shadow-xl shadow-purple-500/10">
              <h3 className="text-lg font-semibold text-foreground mb-6 flex items-center gap-2">
                <AlertTriangle className="h-5 w-5 text-amber-500" />
                匹配作品列表
              </h3>
              <div className="space-y-4">
                {result.matched_works.map((work, index) => (
                  <div
                    key={index}
                    className="flex items-center justify-between p-4 bg-muted/50 rounded-xl border border-border"
                  >
                    <div>
                      <p className="font-medium text-foreground">{work.title}</p>
                      <p className="text-sm text-muted-foreground">{work.artist}</p>
                      <p className="text-xs text-muted-foreground mt-1">匹配段落：{work.section}</p>
                    </div>
                    <div className="text-right">
                      <span className={`text-lg font-bold ${work.similarity > 70 ? 'text-red-500' : work.similarity > 40 ? 'text-amber-500' : 'text-emerald-500'}`}>
                        {work.similarity.toFixed(1)}%
                      </span>
                      <p className="text-xs text-muted-foreground">相似度</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* 无匹配 */}
          {result.matched_works.length === 0 && (
            <div className="bg-card rounded-3xl p-8 border border-border shadow-xl shadow-purple-500/10 text-center">
              <CheckCircle className="h-12 w-12 mx-auto text-emerald-500 mb-4" />
              <h3 className="text-lg font-semibold text-foreground mb-2">未发现相似作品</h3>
              <p className="text-muted-foreground">该作品与已有版权作品的相似度较低，合规风险低</p>
            </div>
          )}
        </div>
      )}

      {/* 检测历史 */}
      {historyList.length > 0 && (
        <div className="bg-card rounded-3xl p-6 sm:p-8 border border-border shadow-xl shadow-purple-500/10 space-y-4 animate-in fade-in duration-500">
          <div className="flex items-center gap-3 mb-4">
            <History className="h-5 w-5 text-primary" />
            <h3 className="text-lg font-semibold text-foreground">检测历史</h3>
          </div>
          <div className="space-y-3">
            {historyList.map((item) => (
              <div key={item.id} className="flex items-center justify-between p-4 bg-muted/50 rounded-xl border border-border">
                <div className="flex items-center gap-3">
                  <div className={`h-8 w-8 rounded-lg flex items-center justify-center ${
                    item.risk_level === 'low' ? 'bg-emerald-100 text-emerald-600' :
                    item.risk_level === 'medium' ? 'bg-amber-100 text-amber-600' :
                    'bg-red-100 text-red-600'
                  }`}>
                    <Shield className="h-4 w-4" />
                  </div>
                  <div>
                    <p className="text-sm font-medium text-foreground">
                      {checkTypeLabels[item.check_type]} - {item.version_id}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {new Date(item.checked_at).toLocaleString('zh-CN')}
                    </p>
                  </div>
                </div>
                <span className={`text-sm font-bold ${
                  item.risk_level === 'low' ? 'text-emerald-600' :
                  item.risk_level === 'medium' ? 'text-amber-600' :
                  'text-red-600'
                }`}>
                  {item.overall_score.toFixed(1)}% - {riskLabels[item.risk_level]}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
