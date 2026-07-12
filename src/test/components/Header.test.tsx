/**
 * @vitest-environment jsdom
 */

import { render, screen, fireEvent } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import Header from '../../components/Header'

describe('Header', () => {
  it('renders logo', () => {
    render(
      <MemoryRouter>
        <Header />
      </MemoryRouter>
    )
    expect(screen.getByText('Composer AI')).toBeInTheDocument()
  })

  it('renders navigation links in desktop nav', () => {
    render(
      <MemoryRouter>
        <Header />
      </MemoryRouter>
    )
    // 桌面端导航使用 nav 标签
    const nav = screen.getByRole('navigation', { name: /主导航/i })
    expect(nav).toBeInTheDocument()
    expect(nav.children.length).toBeGreaterThanOrEqual(3)
  })

  it('has a mobile menu button', () => {
    render(
      <MemoryRouter>
        <Header />
      </MemoryRouter>
    )
    const menuButton = screen.getByRole('button', { name: /打开菜单/i })
    expect(menuButton).toBeInTheDocument()
    expect(menuButton).toHaveAttribute('aria-expanded', 'false')
  })

  it('toggles mobile menu on button click', () => {
    render(
      <MemoryRouter>
        <Header />
      </MemoryRouter>
    )
    const menuButton = screen.getByRole('button', { name: /打开菜单/i })

    fireEvent.click(menuButton)
    expect(menuButton).toHaveAttribute('aria-expanded', 'true')

    fireEvent.click(menuButton)
    expect(menuButton).toHaveAttribute('aria-expanded', 'false')
  })
})
