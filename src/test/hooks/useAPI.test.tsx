/**
 * @vitest-environment jsdom
 */

import { renderHook, waitFor } from '@testing-library/react'
import { APIProvider, useAPI } from '../../lib/apiContext'

// Mock the API module
vi.mock('../../lib/api', () => ({
  api: {
    getVersions: vi.fn(),
    getVersion: vi.fn(),
    getCopyrightRecords: vi.fn(),
  },
}))

describe('useAPI', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('initializes with mock data when API fails', async () => {
    vi.spyOn(console, 'warn').mockImplementation(() => {})

    ;(api as any).getVersions.mockRejectedValue(new Error('API Error'))

    const { result } = renderHook(
      () => useAPI(),
      { wrapper: APIProvider }
    )

    await waitFor(() => {
      expect(result.current.history.length).toBeGreaterThan(0)
    })

    // Mock data should be loaded when API fails
    expect(result.current.history.length).toBeGreaterThan(0)
    expect(result.current.currentVersion).not.toBeNull()
  })

  it('initializes with real data when API succeeds', async () => {
    vi.spyOn(console, 'warn').mockImplementation(() => {})

    const mockData = {
      items: [
        {
          version_id: 'v1',
          user_prompt: 'Test',
          style: 'pop',
          mood: 'happy',
          caption: 'Test caption',
          midi_url: '/midi/test.mid',
          audio_url: '/audio/test.wav',
          plan: {
            theme: 'Test',
            style: 'pop',
            mood: ['happy'],
            tempo: 120,
            key: 'C',
            instruments: ['piano'],
            structure: ['verse'],
          },
        },
      ],
      total: 1,
      totalPages: 1,
      page: 0,
      size: 10,
    }

    ;(api as any).getVersions.mockResolvedValue(mockData)

    const { result } = renderHook(
      () => useAPI(),
      { wrapper: APIProvider }
    )

    await waitFor(() => {
      expect(result.current.history.length).toBeGreaterThan(0)
    })

    // History should be loaded from API
    expect(result.current.history.length).toBeGreaterThan(0)
    expect(result.current.history[0].version_id).toBe('v1')
  })

  it('provides generate function', async () => {
    const { result } = renderHook(
      () => useAPI(),
      { wrapper: APIProvider }
    )

    // generate function should exist and be a function
    expect(typeof result.current.generate).toBe('function')
  })

  it('provides revise function', async () => {
    const { result } = renderHook(
      () => useAPI(),
      { wrapper: APIProvider }
    )

    // revise function should exist and be a function
    expect(typeof result.current.revise).toBe('function')
  })
})

import { api } from '../../lib/api'
