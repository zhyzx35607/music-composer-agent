import { createContext, useContext, useEffect, useState, useRef, ReactNode } from 'react';
import type { GenerateResponse, HistoryItem } from '../types/api';
import { api } from '../lib/api';
import { mockHistory, mockCurrentVersion, shouldUseMock } from './mockData';
import { throwWithError, handleVersionError, handleHistoryError } from './errorHandler';

interface APIContextType {
  isLoading: boolean;
  currentVersion: GenerateResponse | null;
  history: HistoryItem[];
  generate: (request: {
    user_prompt: string;
    style: string;
    mood: string;
    tempo: string;
    instruments: string[];
  }) => Promise<GenerateResponse>;
  revise: (versionId: string, feedback: string) => Promise<GenerateResponse>;
  loadVersion: (versionId: string) => Promise<GenerateResponse>;
  resetCurrentVersion: () => void;
  setCurrentVersion: (version: GenerateResponse | null) => void;
  error: string | null;
}

const APIContext = createContext<APIContextType | undefined>(undefined);

interface APIProviderProps {
  children: ReactNode;
}

export function APIProvider({ children }: APIProviderProps) {
  const [isLoading, setIsLoading] = useState(false);
  const [currentVersion, setCurrentVersion] = useState<GenerateResponse | null>(null);
  const [history, setHistory] = useState<HistoryItem[]>([]);
  const [error, setError] = useState<string | null>(null);

  // 使用 useRef 来跟踪是否已加载，避免依赖状态导致的循环
  const hasLoadedRef = useRef(false);

  // 记录最近一次生成请求的参数，供 revise 构建历史记录时使用
  const lastGenerateRequestRef = useRef<{
    user_prompt: string;
    style: string;
    mood: string;
    tempo: string;
    instruments: string[];
  }>({
    user_prompt: '',
    style: '',
    mood: '',
    tempo: '',
    instruments: [],
  });

  // 初始化：加载历史版本 - 只触发一次
  useEffect(() => {
    // 如果已经加载过，跳过
    if (hasLoadedRef.current) return;

    // 如果配置使用 mock 数据，直接使用
    if (shouldUseMock()) {
      setHistory(mockHistory);
      setCurrentVersion(mockCurrentVersion);
      hasLoadedRef.current = true;
      return;
    }

    const loadHistory = async () => {
      try {
        const data = await api.getVersions(0, 10);
        if (data.items.length > 0) {
          setHistory(data.items);
          const latest = data.items[0];
          // 优先使用列表数据，如果列表数据不完整则通过详情 API 获取
          if (latest.caption && latest.midi_url && latest.audio_url && latest.plan) {
            setCurrentVersion({
              version_id: latest.version_id,
              caption: latest.caption,
              midi_url: latest.midi_url,
              audio_url: latest.audio_url,
              plan: latest.plan,
            });
            setError(null);
          } else {
            // 列表数据不完整，尝试获取完整详情
            api.getVersion(latest.version_id).then(fullVersion => {
              if (fullVersion.caption && fullVersion.midi_url && fullVersion.audio_url && fullVersion.plan) {
                setCurrentVersion({
                  version_id: fullVersion.version_id,
                  caption: fullVersion.caption,
                  midi_url: fullVersion.midi_url,
                  audio_url: fullVersion.audio_url,
                  plan: fullVersion.plan,
                });
              }
            }).catch(() => {
              // 静默失败，用户生成音乐后自然能看到结果
            });
          }
        }
        hasLoadedRef.current = true;
      } catch {
        handleHistoryError(setError, setHistory);
        hasLoadedRef.current = true;
      }
    };

    loadHistory();
  }, []);

  // 通用的 generate 处理函数
  const handleGenerate = async (request: {
    user_prompt: string;
    style: string;
    mood: string;
    tempo: string;
    instruments: string[];
  }): Promise<GenerateResponse> => {
    // 保存请求参数，供 revise 构建历史记录时使用
    lastGenerateRequestRef.current = { ...request };
    setIsLoading(true);
    try {
      const result = await api.generate({
        user_prompt: request.user_prompt,
        style: request.style,
        mood: request.mood,
        tempo: request.tempo,
        instruments: request.instruments,
      });

      setCurrentVersion(result);

      // 构建 history item
      const historyItem: HistoryItem = {
        version_id: result.version_id,
        parent_version_id: null,
        user_prompt: request.user_prompt,
        style: request.style,
        mood: request.mood,
        tempo: request.tempo,
        instruments: request.instruments,
        caption: result.caption,
        caption_preview: result.caption.length > 50
          ? result.caption.substring(0, 50) + '...'
          : result.caption,
        created_at: new Date().toISOString(),
        midi_url: result.midi_url,
        audio_url: result.audio_url,
        plan: result.plan,
      };
      setHistory(prev => [historyItem, ...prev]);
      setError(null);
      return result;
    } catch (error) {
      throwWithError(setError, setIsLoading, 'Generation', error);
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  const revise = async (versionId: string, feedback: string) => {
    setIsLoading(true);
    try {
      const result = await api.revise({
        version_id: versionId,
        feedback: feedback,
      });

      setCurrentVersion(result);

      const historyItem: HistoryItem = {
        version_id: result.version_id,
        parent_version_id: versionId,
        user_prompt: lastGenerateRequestRef.current.user_prompt,
        style: lastGenerateRequestRef.current.style,
        mood: lastGenerateRequestRef.current.mood,
        tempo: lastGenerateRequestRef.current.tempo,
        instruments: lastGenerateRequestRef.current.instruments,
        caption: result.caption,
        caption_preview: result.caption.length > 50
          ? result.caption.substring(0, 50) + '...'
          : result.caption,
        created_at: new Date().toISOString(),
        midi_url: result.midi_url,
        audio_url: result.audio_url,
        plan: result.plan,
      };
      setHistory(prev => [historyItem, ...prev]);
      setError(null);
      return result;
    } catch (error) {
      throwWithError(setError, setIsLoading, 'Revise', error);
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  const resetCurrentVersion = () => {
    setCurrentVersion(null);
  };

  const loadVersion = async (versionId: string): Promise<GenerateResponse> => {
    setIsLoading(true);
    try {
      const version = await api.getVersion(versionId);

      if (version.caption && version.midi_url && version.audio_url && version.plan) {
        const loadedVersion: GenerateResponse = {
          version_id: version.version_id,
          caption: version.caption,
          midi_url: version.midi_url,
          audio_url: version.audio_url,
          plan: version.plan,
        };
        setCurrentVersion(loadedVersion);
        setError(null);
        return loadedVersion;
      }

      // 如果版本数据不完整
      throw new Error('版本数据不完整');
    } catch (error) {
      handleVersionError(setError, setIsLoading);
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <APIContext.Provider
      value={{
        isLoading,
        currentVersion,
        history,
        generate: handleGenerate,
        revise,
        loadVersion,
        resetCurrentVersion,
        setCurrentVersion,
        error,
      }}
    >
      {children}
    </APIContext.Provider>
  );
}

/* eslint-disable react/only-export-components */
export function useAPI() {
  const context = useContext(APIContext);
  if (context === undefined) {
    throw new Error('useAPI must be used within an APIProvider');
  }
  return context;
}
