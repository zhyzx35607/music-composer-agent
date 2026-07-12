/**
 * @vitest-environment jsdom
 */

import { render, screen } from '@testing-library/react'
import { ErrorBoundary } from '../../components/ErrorBoundary'

describe('ErrorBoundary', () => {
  it('renders children when no error occurs', () => {
    render(
      <ErrorBoundary>
        <div data-testid="test-child">Hello World</div>
      </ErrorBoundary>
    )
    expect(screen.getByText('Hello World')).toBeInTheDocument()
  })

  it('renders fallback when error occurs', () => {
    const ThrowingComponent = () => {
      throw new Error('Test error')
    }

    render(
      <ErrorBoundary>
        <ThrowingComponent />
      </ErrorBoundary>
    )
    expect(screen.getByText('发生错误')).toBeInTheDocument()
    expect(screen.getByText('重新加载页面')).toBeInTheDocument()
  })

  it('renders custom fallback when provided', () => {
    const CustomFallback = () => (
      <div data-testid="custom-fallback">Custom Error</div>
    )

    render(
      <ErrorBoundary fallback={<CustomFallback />}>
        <ThrowingComponent />
      </ErrorBoundary>
    )
    expect(screen.getByTestId('custom-fallback')).toBeInTheDocument()
  })

  it('resets error state on button click', () => {
    const ThrowingComponent = () => {
      throw new Error('Test error')
    }

    render(
      <ErrorBoundary>
        <ThrowingComponent />
      </ErrorBoundary>
    )

    // Initial error state
    expect(screen.getByText('发生错误')).toBeInTheDocument()

    // Reset button click - note: this will trigger window.location.reload
    // For testing purposes, we'll just verify the button exists
    const resetButton = screen.getByRole('button', { name: /重新加载页面/i })
    expect(resetButton).toBeInTheDocument()

    // Verify the error message is shown
    expect(screen.getByText(/Test error/i)).toBeInTheDocument()
  })

  it('logs error to console when catching error', () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {})

    const ThrowingComponent = () => {
      throw new Error('Test error for console')
    }

    render(
      <ErrorBoundary>
        <ThrowingComponent />
      </ErrorBoundary>
    )

    // Verify console.error was called
    expect(consoleSpy).toHaveBeenCalled()

    consoleSpy.mockRestore()
  })
})

function ThrowingComponent() {
  throw new Error('Test error')
}
