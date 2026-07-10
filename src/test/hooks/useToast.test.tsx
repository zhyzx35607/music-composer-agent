/**
 * @vitest-environment jsdom
 */

import { render, screen, fireEvent, act } from '@testing-library/react'
import { useToast } from '../../components/useToast'

describe('useToast', () => {
  it('adds a toast message', () => {
    function TestComponent() {
      const { addToast, toasts } = useToast()
      return (
        <div>
          <button onClick={() => addToast('Test message', 'success')}>Add Toast</button>
          <span data-testid="toast-count">{toasts.length}</span>
        </div>
      )
    }

    render(<TestComponent />)
    expect(screen.getByTestId('toast-count')).toHaveTextContent('0')

    fireEvent.click(screen.getByRole('button', { name: /Add Toast/i }))
    expect(screen.getByTestId('toast-count')).toHaveTextContent('1')
  })

  it('removes a toast after duration', () => {
    vi.useFakeTimers()

    function TestComponent() {
      const { addToast, toasts } = useToast()
      return (
        <div>
          <button onClick={() => addToast('Test message', 'info', 1000)}>Add Toast</button>
          <span data-testid="toast-count">{toasts.length}</span>
        </div>
      )
    }

    render(<TestComponent />)
    fireEvent.click(screen.getByRole('button', { name: /Add Toast/i }))
    expect(screen.getByTestId('toast-count')).toHaveTextContent('1')

    act(() => {
      vi.advanceTimersByTime(1000)
    })
    expect(screen.getByTestId('toast-count')).toHaveTextContent('0')

    vi.useRealTimers()
  })

  it('provides success, error, warning, info methods', () => {
    function TestComponent() {
      const { success, error, warning, info, toasts } = useToast()
      return (
        <div>
          <button onClick={() => success('Success message')}>Success</button>
          <button onClick={() => error('Error message')}>Error</button>
          <button onClick={() => warning('Warning message')}>Warning</button>
          <button onClick={() => info('Info message')}>Info</button>
          <span data-testid="toast-count">{toasts.length}</span>
        </div>
      )
    }

    render(<TestComponent />)

    fireEvent.click(screen.getByRole('button', { name: /Success/i }))
    expect(screen.getByTestId('toast-count')).toHaveTextContent('1')

    fireEvent.click(screen.getByRole('button', { name: /Error/i }))
    fireEvent.click(screen.getByRole('button', { name: /Warning/i }))
    fireEvent.click(screen.getByRole('button', { name: /Info/i }))
    expect(screen.getByTestId('toast-count')).toHaveTextContent('4')
  })
})
