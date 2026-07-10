/**
 * @vitest-environment jsdom
 */

import { render, screen } from '@testing-library/react'
import AudioPlayer from '../../components/AudioPlayer'

describe('AudioPlayer', () => {
  it('renders with correct title', () => {
    render(<AudioPlayer audioUrl="test.mp3" />)
    expect(screen.getByText('音频播放器')).toBeInTheDocument()
  })

  it('renders audio controls', () => {
    render(<AudioPlayer audioUrl="test.mp3" />)
    // 应该有音量按钮
    const volumeButton = screen.getAllByRole('button')[0]
    expect(volumeButton).toBeInTheDocument()
  })

  it('renders progress bar', () => {
    render(<AudioPlayer audioUrl="test.mp3" />)
    // 应该有一个 input type="range" 作为进度条
    const progressBar = screen.getByRole('slider')
    expect(progressBar).toBeInTheDocument()
  })

  it('renders play/pause button', () => {
    render(<AudioPlayer audioUrl="test.mp3" />)
    // 播放按钮是中心的大按钮
    const playButtons = screen.getAllByRole('button').filter(btn =>
      btn.classList.contains('rounded-full')
    )
    expect(playButtons.length).toBeGreaterThan(0)
  })
})
