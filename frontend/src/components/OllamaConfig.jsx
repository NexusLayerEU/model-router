import { useState, useEffect } from 'react'
import { updateConfig, getOllamaModels } from '../services/api'
import toast from 'react-hot-toast'
import { Save, Loader2, Server, RefreshCw, Star, Package, Zap } from 'lucide-react'

// Popular models from https://ollama.com/library — shown when Ollama is unreachable
const POPULAR_MODELS = [
  { id: 'llama3.1', label: 'Llama 3.1 8B', badge: 'Recommended', badgeColor: 'text-green-400 bg-green-400/10', description: 'Meta · fast, great general purpose' },
  { id: 'llama3.1:70b', label: 'Llama 3.1 70B', badge: null, description: 'Meta · high quality, needs 40GB+ RAM' },
  { id: 'llama3.2', label: 'Llama 3.2 3B', badge: 'Tiny', badgeColor: 'text-green-400 bg-green-400/10', description: 'Meta · ultra-fast, 2GB RAM' },
  { id: 'mistral', label: 'Mistral 7B', badge: null, description: 'Mistral AI · great coding & reasoning' },
  { id: 'mixtral', label: 'Mixtral 8x7B', badge: null, description: 'Mistral AI · mixture of experts' },
  { id: 'gemma2', label: 'Gemma 2 9B', badge: null, description: 'Google · efficient and capable' },
  { id: 'qwen2.5', label: 'Qwen 2.5 7B', badge: null, description: 'Alibaba · multilingual, strong coding' },
  { id: 'phi3.5', label: 'Phi 3.5 Mini', badge: null, description: 'Microsoft · small but punches above weight' },
  { id: 'deepseek-coder-v2', label: 'DeepSeek Coder V2', badge: null, description: 'DeepSeek · best for coding tasks' },
  { id: 'codellama', label: 'Code Llama 7B', badge: null, description: 'Meta · code generation specialist' },
]

export default function OllamaConfig({ currentBaseUrl, currentModel }) {
  const [baseUrl, setBaseUrl] = useState(currentBaseUrl || 'http://localhost:11434')
  const [model, setModel] = useState(currentModel || 'llama3.1')
  const [customModel, setCustomModel] = useState('')
  const [loading, setLoading] = useState(false)
  const [installedModels, setInstalledModels] = useState(null) // null = not fetched yet
  const [fetching, setFetching] = useState(false)

  // Fetch installed models on mount
  useEffect(() => { fetchInstalled() }, [])

  const fetchInstalled = async () => {
    setFetching(true)
    try {
      const models = await getOllamaModels()
      setInstalledModels(models)
      if (models.length > 0 && !currentModel) setModel(models[0])
    } catch {
      setInstalledModels([])
    } finally {
      setFetching(false)
    }
  }

  const detectModels = async () => {
    const url = baseUrl.trim()
    if (!url) return
    setFetching(true)
    try {
      // Save the current URL first so the backend queries it
      await updateConfig({ ollamaBaseUrl: url })
      const models = await getOllamaModels()
      setInstalledModels(models)
      if (models.length > 0) {
        setModel(models[0])
        setCustomModel('')
        toast.success(`Detected ${models.length} model${models.length !== 1 ? 's' : ''}`)
      } else {
        toast.error('No models found at that URL')
      }
    } catch {
      setInstalledModels([])
      toast.error('Could not reach Ollama at that URL')
    } finally {
      setFetching(false)
    }
  }

  const effectiveModel = customModel.trim() || model

  const handleSave = async () => {
    setLoading(true)
    try {
      await updateConfig({ ollamaBaseUrl: baseUrl.trim(), ollamaModel: effectiveModel })
      toast.success('Ollama config saved')
      if (customModel.trim()) setCustomModel('')
    } catch (err) {
      toast.error(err.message)
    } finally {
      setLoading(false)
    }
  }

  // Installed models take precedence over the popular list
  const showInstalled = installedModels && installedModels.length > 0
  const displayModels = showInstalled
    ? installedModels.map((id) => ({ id, label: id, badge: 'Installed', badgeColor: 'text-green-400 bg-green-400/10', description: 'Available on your Ollama instance' }))
    : POPULAR_MODELS

  return (
    <div className="space-y-4">
      <div>
        <label className="label">Ollama Base URL</label>
        <div className="flex gap-2">
          <div className="relative flex-1">
            <Server className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" />
            <input
              className="input pl-9"
              type="url"
              value={baseUrl}
              onChange={(e) => setBaseUrl(e.target.value)}
              placeholder="http://localhost:11434"
            />
          </div>
          <button
            type="button"
            onClick={detectModels}
            disabled={fetching || !baseUrl.trim()}
            title="Save URL and detect installed models"
            className="flex items-center gap-1.5 px-3 py-2 rounded-lg border border-green-600/40 bg-green-600/10 text-green-400 text-xs font-medium hover:bg-green-600/20 hover:border-green-500/60 disabled:opacity-40 disabled:cursor-not-allowed transition-all whitespace-nowrap"
          >
            {fetching
              ? <Loader2 className="w-3.5 h-3.5 animate-spin" />
              : <Zap className="w-3.5 h-3.5" />}
            {fetching ? 'Detecting…' : 'Detect'}
          </button>
        </div>
      </div>

      <div>
        <div className="flex items-center justify-between mb-1.5">
          <label className="label mb-0">Model</label>
          <button
            type="button"
            onClick={fetchInstalled}
            disabled={fetching}
            className="flex items-center gap-1.5 text-xs text-gray-500 hover:text-gray-300 transition-colors"
            title="Refresh installed models from Ollama"
          >
            <RefreshCw className={`w-3 h-3 ${fetching ? 'animate-spin' : ''}`} />
            {showInstalled ? `${installedModels.length} installed` : fetching ? 'Scanning…' : 'Scan installed'}
          </button>
        </div>

        {!showInstalled && !fetching && (
          <div className="flex items-center gap-1.5 text-xs text-yellow-600 mb-2 bg-yellow-500/5 border border-yellow-500/20 rounded px-2.5 py-1.5">
            <Package className="w-3.5 h-3.5 shrink-0" />
            <span>Ollama unreachable — showing popular models. Pull with <span className="font-mono">ollama pull &lt;model&gt;</span></span>
          </div>
        )}

        <div className="space-y-1.5 max-h-56 overflow-y-auto pr-0.5">
          {displayModels.map((m) => (
            <label
              key={m.id}
              className={`flex items-start gap-3 p-2.5 rounded-lg border cursor-pointer transition-all ${
                effectiveModel === m.id && !customModel
                  ? 'border-green-500/50 bg-green-500/5'
                  : 'border-gray-800 hover:border-gray-700'
              }`}
            >
              <input
                type="radio"
                name="ollama-model"
                value={m.id}
                checked={effectiveModel === m.id && !customModel}
                onChange={() => { setModel(m.id); setCustomModel('') }}
                className="mt-0.5 accent-green-400"
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
                <p className="text-xs text-gray-500 mt-0.5">{m.description}</p>
              </div>
              {effectiveModel === m.id && !customModel && m.badge === 'Recommended' && (
                <Star className="w-3.5 h-3.5 text-green-400 mt-0.5 shrink-0" />
              )}
            </label>
          ))}
        </div>

        <div className="mt-2">
          <input
            className="input"
            type="text"
            value={customModel}
            onChange={(e) => setCustomModel(e.target.value)}
            placeholder="Or type custom model (e.g. llama3.1:8b-instruct-q4_K_M)"
          />
          {customModel.trim() && (
            <p className="mt-1 text-xs text-gray-500">
              Will use: <span className="font-mono text-green-400">{customModel.trim()}</span>
            </p>
          )}
        </div>
      </div>

      <button
        onClick={handleSave}
        disabled={loading || !baseUrl.trim()}
        className="btn-primary flex items-center gap-2"
      >
        {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
        Save Config
      </button>
    </div>
  )
}
