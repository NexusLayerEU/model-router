import { useState } from 'react'
import { switchProvider } from '../services/api'
import toast from 'react-hot-toast'
import { CheckCircle2, Loader2 } from 'lucide-react'

const PROVIDERS = [
  {
    id: 'anthropic',
    label: 'Anthropic Claude',
    subtitle: 'claude-sonnet-4-20250514',
    emoji: '🟠',
    description: 'State-of-the-art reasoning and coding from Anthropic.',
    accent: 'border-orange-500/50 bg-orange-500/5',
    activeAccent: 'border-orange-500 bg-orange-500/10 ring-1 ring-orange-500/30',
    dot: 'bg-orange-400',
  },
  {
    id: 'gemini',
    label: 'Google Gemini',
    subtitle: 'gemini-2.5-flash',
    emoji: '🔵',
    description: 'Fast multimodal model from Google DeepMind.',
    accent: 'border-blue-500/50 bg-blue-500/5',
    activeAccent: 'border-blue-500 bg-blue-500/10 ring-1 ring-blue-500/30',
    dot: 'bg-blue-400',
  },
  {
    id: 'ollama',
    label: 'Ollama (Local)',
    subtitle: 'user-configured endpoint',
    emoji: '🟢',
    description: 'Run open-source models locally via Ollama.',
    accent: 'border-green-500/50 bg-green-500/5',
    activeAccent: 'border-green-500 bg-green-500/10 ring-1 ring-green-500/30',
    dot: 'bg-green-400',
  },
]

export default function ProviderSelector({ activeProvider, onProviderChange }) {
  const [loading, setLoading] = useState(null)

  const handleSelect = async (id) => {
    if (id === activeProvider) return
    setLoading(id)
    try {
      const updated = await switchProvider(id)
      onProviderChange(updated.provider)
      toast.success(`Switched to ${id}`)
    } catch (err) {
      toast.error(`Failed to switch: ${err.message}`)
    } finally {
      setLoading(null)
    }
  }

  return (
    <div className="card">
      <h2 className="text-sm font-semibold text-gray-300 mb-4">Active Provider</h2>
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        {PROVIDERS.map((p) => {
          const isActive = activeProvider === p.id
          const isLoading = loading === p.id
          return (
            <button
              key={p.id}
              onClick={() => handleSelect(p.id)}
              disabled={isLoading}
              className={`relative text-left rounded-xl border p-4 transition-all duration-200
                cursor-pointer hover:scale-[1.02] active:scale-[0.98]
                ${isActive ? p.activeAccent : p.accent + ' hover:border-gray-600'}
                disabled:opacity-60 disabled:cursor-not-allowed`}
            >
              <div className="flex items-start justify-between mb-2">
                <span className="text-xl">{p.emoji}</span>
                {isLoading ? (
                  <Loader2 className="w-4 h-4 text-gray-400 animate-spin" />
                ) : isActive ? (
                  <CheckCircle2 className="w-4 h-4 text-green-400" />
                ) : null}
              </div>
              <p className="font-semibold text-sm text-gray-100">{p.label}</p>
              <p className="text-xs text-gray-500 mt-0.5 font-mono">{p.subtitle}</p>
              <p className="text-xs text-gray-600 mt-2">{p.description}</p>

              {isActive && (
                <span className="absolute top-3 right-3 flex h-2 w-2">
                  <span className={`animate-ping absolute inline-flex h-full w-full rounded-full ${p.dot} opacity-75`} />
                  <span className={`relative inline-flex rounded-full h-2 w-2 ${p.dot}`} />
                </span>
              )}
            </button>
          )
        })}
      </div>
    </div>
  )
}
