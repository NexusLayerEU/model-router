import { useState } from 'react'
import { updateConfig } from '../services/api'
import toast from 'react-hot-toast'
import { Eye, EyeOff, Save, Loader2, Star } from 'lucide-react'

// Source: https://ai.google.dev/gemini-api/docs/models/gemini
const GEMINI_MODELS = [
  {
    id: 'gemini-2.5-flash',
    label: 'Gemini 2.5 Flash',
    badge: 'Latest · Recommended',
    badgeColor: 'text-blue-400 bg-blue-400/10',
    description: 'Best performance/speed ratio, available to all accounts',
  },
  {
    id: 'gemini-2.5-pro',
    label: 'Gemini 2.5 Pro',
    badge: 'Most capable',
    badgeColor: 'text-purple-400 bg-purple-400/10',
    description: 'Highest intelligence for complex tasks',
  },
  {
    id: 'gemini-2.0-flash',
    label: 'Gemini 2.0 Flash',
    badge: 'Legacy accounts',
    badgeColor: 'text-yellow-500 bg-yellow-500/10',
    description: 'Not available on new API accounts',
  },
  {
    id: 'gemini-2.0-flash-thinking-exp',
    label: 'Gemini 2.0 Flash Thinking',
    badge: 'Experimental',
    badgeColor: 'text-yellow-400 bg-yellow-400/10',
    description: 'Enhanced reasoning with chain-of-thought',
  },
  {
    id: 'gemini-1.5-pro',
    label: 'Gemini 1.5 Pro',
    badge: null,
    description: 'Long context (2M tokens), highly capable',
  },
  {
    id: 'gemini-1.5-flash-8b',
    label: 'Gemini 1.5 Flash 8B',
    badge: 'Smallest',
    badgeColor: 'text-green-400 bg-green-400/10',
    description: 'Lowest latency, high-volume tasks',
  },
]

export default function GeminiConfig({ currentModel, masked }) {
  const [apiKey, setApiKey] = useState('')
  const [model, setModel] = useState(currentModel || 'gemini-2.5-flash')
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
        ...(apiKey.trim() ? { geminiApiKey: apiKey.trim() } : {}),
        geminiModel: model,
      })
      toast.success('Gemini config saved')
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
            placeholder={masked ? masked : 'AIza…'}
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
          <a href="https://aistudio.google.com/app/apikey" target="_blank" rel="noreferrer"
             className="text-brand-500 hover:underline">Google AI Studio</a>
        </p>
      </div>

      <div>
        <label className="label">Model</label>
        <div className="space-y-1.5">
          {GEMINI_MODELS.map((m) => (
            <label
              key={m.id}
              className={`flex items-start gap-3 p-2.5 rounded-lg border cursor-pointer transition-all ${
                model === m.id
                  ? 'border-blue-500/50 bg-blue-500/5'
                  : 'border-gray-800 hover:border-gray-700'
              }`}
            >
              <input
                type="radio"
                name="gemini-model"
                value={m.id}
                checked={model === m.id}
                onChange={() => setModel(m.id)}
                className="mt-0.5 accent-blue-400"
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
                <Star className="w-3.5 h-3.5 text-blue-400 mt-0.5 shrink-0" />
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
