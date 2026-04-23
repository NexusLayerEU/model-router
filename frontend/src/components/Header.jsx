import { Cpu, GitFork } from 'lucide-react'

const PROVIDER_META = {
  anthropic: { label: 'Anthropic Claude', color: 'text-orange-400 bg-orange-400/10 border-orange-500/30' },
  gemini:    { label: 'Google Gemini',    color: 'text-blue-400 bg-blue-400/10 border-blue-500/30' },
  ollama:    { label: 'Ollama (Local)',   color: 'text-green-400 bg-green-400/10 border-green-500/30' },
}

export default function Header({ activeProvider }) {
  const meta = PROVIDER_META[activeProvider] || {}

  return (
    <header className="border-b border-gray-800 bg-gray-900/80 backdrop-blur-sm sticky top-0 z-10">
      <div className="max-w-5xl mx-auto px-6 py-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-2 bg-brand-500/10 rounded-lg border border-brand-500/20">
            <GitFork className="w-5 h-5 text-brand-500" />
          </div>
          <div>
            <h1 className="text-lg font-bold tracking-tight">ModelRouter</h1>
            <p className="text-xs text-gray-500">LLM Traffic Controller</p>
          </div>
        </div>

        {activeProvider && (
          <div className={`badge border ${meta.color}`}>
            <Cpu className="w-3 h-3" />
            <span>Active: {meta.label || activeProvider}</span>
          </div>
        )}
      </div>
    </header>
  )
}
