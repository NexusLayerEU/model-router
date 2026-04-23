import axios from 'axios'

const BASE = '/api/admin'

const IDENTITY_SERVER_URL =
  import.meta.env.VITE_IDENTITY_SERVER_URL || 'http://192.168.68.111:3007'
const TOKEN_KEY = 'modelrouter_token'

// Store sso_token from URL param when returning from identity server
;(function captureSsoToken() {
  const params = new URLSearchParams(window.location.search)
  const ssoToken = params.get('sso_token')
  if (ssoToken) {
    localStorage.setItem(TOKEN_KEY, ssoToken)
    params.delete('sso_token')
    const newSearch = params.toString()
    const newUrl = window.location.pathname + (newSearch ? `?${newSearch}` : '')
    window.history.replaceState({}, '', newUrl)
  }
})()

const api = axios.create({
  baseURL: '',
  headers: { 'Content-Type': 'application/json' },
})

// Attach JWT token to every request if present
api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`
  }
  return config
})

/** Interceptor: surface error messages from the backend; redirect to SSO on 401 */
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      const returnUrl = encodeURIComponent(window.location.href)
      window.location.href = `${IDENTITY_SERVER_URL}/login?redirect=${returnUrl}`
      return new Promise(() => {})
    }
    const message =
      err.response?.data?.error?.message ||
      err.response?.data?.message ||
      err.message ||
      'Unknown error'
    return Promise.reject(new Error(message))
  }
)

// ── Admin API ──────────────────────────────────────────────────────────────

/** Fetch current router config (keys are masked). */
export const getConfig = () =>
  api.get(`${BASE}/config`).then((r) => r.data)

/**
 * Update router config. Pass only the fields you wish to change.
 * @param {object} payload - Partial ProviderConfigDto
 */
export const updateConfig = (payload) =>
  api.put(`${BASE}/config`, payload).then((r) => r.data)

/**
 * Switch the active provider.
 * @param {string} provider - "anthropic" | "gemini" | "ollama"
 */
export const switchProvider = (provider) =>
  api.post(`${BASE}/provider/switch`, { provider }).then((r) => r.data)

/** Returns list of all registered provider names. */
export const getProviders = () =>
  api.get(`${BASE}/providers`).then((r) => r.data)

/**
 * Returns locally installed Ollama model names (from the configured Ollama instance).
 * Returns [] if Ollama is unreachable.
 */
export const getOllamaModels = () =>
  api.get(`${BASE}/ollama/models`).then((r) => r.data)

// ── Proxy API (for the in-UI test console) ────────────────────────────────

/**
 * Send a test message through the router's /v1/messages endpoint.
 * @param {string} content - User message text
 * @param {number} [maxTokens=512]
 */
export const testMessage = (content, maxTokens = 512) =>
  api
    .post('/v1/messages', {
      model: 'claude-sonnet-4-20250514', // router ignores this; uses active provider
      max_tokens: maxTokens,
      messages: [{ role: 'user', content }],
    })
    .then((r) => r.data)

export default api
