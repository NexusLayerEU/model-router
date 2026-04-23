import { useState } from 'react'
import { updateConfig } from '../services/api'
import toast from 'react-hot-toast'
import { Eye, EyeOff, Save, Loader2, Star } from 'lucide-react'

// Source: https://docs.anthropic.com/en/docs/about-claude/models
const CLAUDE_MODELS = [
  {
    id: 'claude-sonnet-4-20250514',
    label: 'Claude Sonnet 4',
    badge: 'Latest · Recommended',
    badgeColor: 'text-orange-400 bg-orange-400/10',
    description: 'Best balance of intelligence and speed',
  },
  {
    id: 'claude-opus-4-20250514',
    label: 'Claude Opus 4',
    badge: 'Most powerful',
    badgeColor: 'text-purple-400 bg-purple-400/10',
    description: 'Highest capability for complex tasks',
  },
  {
    id: 'claude-haiku-4-20250514',
    label: 'Claude Haiku 4',
    badge: 'Fastest',
    badgeColor: 'text-green-400 bg-green-400/10',
    description: 'Instant responses, lowest cost',
  },
  {
    id: 'claude-3-5-sonnet-20241022',
    label: 'Claude 3.5 Sonnet',
    badge: null,
    description: 'Previous generation flagship',
  },
  {
    id: 'claude-3-5-haiku-20241022',
    label: 'Claude 3.5 Haiku',
    badge: null,
    description: 'Previous generation fast model',
  },
  {
    id: 'claude-3-opus-20240229',
    label: 'Claude 3 Opus',
    badge: null,
    description: 'Claude 3 most capable',
  },
  {
    id: 'claude-3-haiku-20240307',
    label: 'Claude 3 Haiku',
    badge: null,
    description: 'Claude 3 fastest',
  },
]

export default function AnthropicConfig({ currentModel, masked }) {
  const [apiKey, setApiKey] = useState('')
  const [model, setModel] = useState(currentModel || 'claude-sonnet-4-20250514')
  const [show, setShow] = useState(false)
  const [loading, setLoading] = useState(false)

  const handleSave = async () => {
    if (!apiKey.trim() && model === currentModel) {
      toast('Nothing to save', { icon: 'ℹ️' })
      return
    }
    setLoading(true)
    try {
      await updateConfig({
        ...(apiKey.trim() ? { anthropicApiKey: apiKey.trim() } : {}),
        anthropicModel: model,
      })
      toast.success('Anthropic config saved')
      setApiKey('')
    } catch (err) {
      toast.error(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="space-y-4">
      <div>
        <label className="label">API Key</label>
        <div className="relative">
          <input
            className="input pr-10"
            type={show ? 'text' : 'password'}
            value={apiKey}
            onChange={(e) => setApiKey(e.target.value)}
            placeholder={masked ? masked : 'sk-ant-…'}
            onKeyDown={(e) => e.key === 'Enter' && handleSave()}
          />
          <button
            type="button"
            onClick={() => setShow(!show)}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-300"
          >
            {show ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
          </button>
        </div>
        <p className="mt-1 text-xs text-gray-600">
          <a href="https://console.anthropic.com" target="_blank" rel="noreferrer"
             className="text-brand-500 hover:underline">console.anthropic.com</a>
        </p>
      </div>

      <div>
        <label className="label">Model</label>
        <div className="space-y-1.5">
          {CLAUDE_MODELS.map((m) => (
            <label
              key={m.id}
              className={`flex items-start gap-3 p-2.5 rounded-lg border cursor-pointer transition-all ${
                model === m.id
                  ? 'border-orange-500/50 bg-orange-500/5'
                  : 'border-gray-800 hover:border-gray-700'
              }`}
            >
              <input
                type="radio"
                name="anthropic-model"
                value={m.id}
                checked={model === m.id}
                onChange={() => setModel(m.id)}
                className="mt-0.5 accent-orange-400"
              />
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="text-sm font-medium text-gray-200">{m.label}</span>
                  {m.badge && (
                    <span className={`text-xs px-1.5 py-0.5 rounded font-medium ${m.badgeColor}`}>
                      {m.badge}
                    </span>
                  )}
                </div>
                <p className="text-xs text-gray-600 font-mono mt-0.5">{m.id}</p>
                <p className="text-xs text-gray-500 mt-0.5">{m.description}</p>
              </div>
              {model === m.id && m.badge?.includes('Recommended') && (
                <Star className="w-3.5 h-3.5 text-orange-400 mt-0.5 shrink-0" />
              )}
            </label>
          ))}
        </div>
      </div>

      <button
        onClick={handleSave}
        disabled={loading}
        className="btn-primary flex items-center gap-2"
      >
        {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
        Save Config
      </button>
    </div>
  )
}
