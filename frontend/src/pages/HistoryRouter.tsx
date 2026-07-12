import { useNavigate } from 'react-router-dom'
import HistoryPageComponent from './HistoryPage.tsx'

// 包装组件，用于传递 onClose prop
export function HistoryPage() {
  const navigate = useNavigate()
  return <HistoryPageComponent onClose={() => navigate('/')} />
}
