import { useEffect, useState } from 'react'
import { Toaster } from 'react-hot-toast'
import { getConfig } from './services/api'
import Header from './components/Header'
import PlanBanner from './components/PlanBanner'
import ProviderSelector from './components/ProviderSelector'
import AnthropicConfig from './components/AnthropicConfig'
import GeminiConfig from './components/GeminiConfig'
import OllamaConfig from './components/OllamaConfig'
import TestConsole from './components/TestConsole'
import Login from './pages/Login'
import { RefreshCw, AlertTriangle } from 'lucide-react'

const TOKEN_KEY = 'modelrouter_token'

export default function App() {
  const [authed, setAuthed] = useState(!!localStorage.getItem(TOKEN_KEY))
  const [config, setConfig] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  if (!authed) {
    return <Login />
  }

  const loadConfig = async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await getConfig()
      setConfig(data)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadConfig() }, [])

  const handleProviderChange = (newProvider) => {
    setConfig((prev) => ({ ...prev, provider: newProvider }))
  }

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="flex items-center gap-3 text-gray-400">
          <RefreshCw className="w-5 h-5 animate-spin" />
          <span>Connecting to router…</span>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center px-4">
        <div className="card max-w-md w-full text-center">
          <AlertTriangle className="w-10 h-10 text-red-400 mx-auto mb-3" />
          <h2 className="text-lg font-semibold mb-2">Cannot reach ModelRouter backend</h2>
          <p className="text-sm text-gray-500 mb-4">{error}</p>
          <p className="text-xs text-gray-600 mb-4">
            Make sure the Spring Boot backend is running on{' '}
            <span className="font-mono text-gray-400">http://localhost:8080</span>
          </p>
          <button onClick={loadConfig} className="btn-primary">
            Retry
          </button>
        </div>
      </div>
    )
  }

  const activeProvider = config?.provider || 'anthropic'

  return (
    <div className="min-h-screen">
      <Toaster
        position="top-right"
        toastOptions={{
          style: { background: '#1f2937', color: '#f3f4f6', border: '1px solid #374151' },
        }}
      />
      <Header activeProvider={activeProvider} />
      <PlanBanner />

      <main className="max-w-5xl mx-auto px-6 py-8 space-y-6">
        {/* Provider switcher */}
        <ProviderSelector
          activeProvider={activeProvider}
          onProviderChange={handleProviderChange}
        />

        {/* Config panels */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
          {/* Anthropic */}
          <div className={`card transition-all duration-300 ${
            activeProvider === 'anthropic'
              ? 'border-orange-500/40 ring-1 ring-orange-500/20'
              : 'opacity-70'
          }`}>
            <div className="flex items-center gap-2 mb-4">
              <span className="text-lg">🟠</span>
              <h2 className="text-sm font-semibold text-gray-300">Anthropic</h2>
              {activeProvider === 'anthropic' && (
                <span className="ml-auto badge text-orange-400 bg-orange-400/10 border border-orange-500/20">
                  Active
                </span>
              )}
            </div>
            <AnthropicConfig masked={config?.anthropicApiKey} currentModel={config?.anthropicModel} />
          </div>

          {/* Gemini */}
          <div className={`card transition-all duration-300 ${
            activeProvider === 'gemini'
              ? 'border-blue-500/40 ring-1 ring-blue-500/20'
              : 'opacity-70'
          }`}>
            <div className="flex items-center gap-2 mb-4">
              <span className="text-lg">🔵</span>
              <h2 className="text-sm font-semibold text-gray-300">Gemini</h2>
              {activeProvider === 'gemini' && (
                <span className="ml-auto badge text-blue-400 bg-blue-400/10 border border-blue-500/20">
                  Active
                </span>
              )}
            </div>
            <GeminiConfig
              currentModel={config?.geminiModel}
              masked={config?.geminiApiKey}
            />
          </div>

          {/* Ollama */}
          <div className={`card transition-all duration-300 ${
            activeProvider === 'ollama'
              ? 'border-green-500/40 ring-1 ring-green-500/20'
              : 'opacity-70'
          }`}>
            <div className="flex items-center gap-2 mb-4">
              <span className="text-lg">🟢</span>
              <h2 className="text-sm font-semibold text-gray-300">Ollama</h2>
              {activeProvider === 'ollama' && (
                <span className="ml-auto badge text-green-400 bg-green-400/10 border border-green-500/20">
                  Active
                </span>
              )}
            </div>
            <OllamaConfig
              currentBaseUrl={config?.ollamaBaseUrl}
              currentModel={config?.ollamaModel}
            />
          </div>
        </div>

        {/* Test console */}
        <TestConsole activeProvider={activeProvider} />

        {/* Footer info */}
        <div className="text-center text-xs text-gray-700 pb-4">
          <p>
            All products should point to{' '}
            <span className="font-mono text-gray-500">http://localhost:8080/v1/messages</span>
          </p>
          <p className="mt-1">
            Use any{' '}
            <span className="font-mono text-gray-500">x-api-key</span> header value — the router uses its own stored keys.
          </p>
        </div>
      </main>
    </div>
  )
}
