/**
 * @vitest-environment jsdom
 */

import { render, screen } from '@testing-library/react'
import Toast from '../../components/Toast'

describe('Toast', () => {
  const mockToasts = [
    {
      id: '1',
      type: 'success',
      message: 'Test success message',
    },
    {
      id: '2',
      type: 'error',
      message: 'Test error message',
    },
  ]

  it('renders toasts correctly', () => {
    render(<Toast toasts={mockToasts} onRemove={() => {}} />)
    expect(screen.getByText('Test success message')).toBeInTheDocument()
    expect(screen.getByText('Test error message')).toBeInTheDocument()
  })

  it('renders info toast by default', () => {
    const infoToasts = [{ id: '1', type: 'info', message: 'Info message' }]
    render(<Toast toasts={infoToasts} onRemove={() => {}} />)
    expect(screen.getByText('Info message')).toBeInTheDocument()
  })

  it('renders toasts container', () => {
    render(<Toast toasts={mockToasts} onRemove={() => {}} />)
    // Toast 容器应该在底部右侧
    const container = document.querySelector('.fixed.bottom-4.right-4')
    expect(container).toBeInTheDocument()
  })
})
