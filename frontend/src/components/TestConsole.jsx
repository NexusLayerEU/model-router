import { useState, useRef, useEffect } from 'react'
import { testMessage } from '../services/api'
import toast from 'react-hot-toast'
import { Send, Loader2, Terminal, Trash2 } from 'lucide-react'

export default function TestConsole({ activeProvider }) {
  const [prompt, setPrompt] = useState('')
  const [messages, setMessages] = useState([])
  const [loading, setLoading] = useState(false)
  const bottomRef = useRef(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const handleSend = async () => {
    const text = prompt.trim()
    if (!text) return
    setPrompt('')
    const userMsg = { role: 'user', content: text, ts: Date.now() }
    setMessages((m) => [...m, userMsg])
    setLoading(true)
    try {
      const res = await testMessage(text)
      const assistantText = res.content?.[0]?.text || '(empty response)'
      setMessages((m) => [
        ...m,
        { role: 'assistant', content: assistantText, model: res.model, ts: Date.now() },
      ])
    } catch (err) {
      toast.error(err.message)
      setMessages((m) => [
        ...m,
        { role: 'error', content: err.message, ts: Date.now() },
      ])
    } finally {
      setLoading(false)
    }
  }

  const PROVIDER_COLORS = {
    anthropic: 'text-orange-400',
    gemini: 'text-blue-400',
    ollama: 'text-green-400',
  }

  return (
    <div className="card flex flex-col h-[480px]">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <Terminal className="w-4 h-4 text-gray-400" />
          <h2 className="text-sm font-semibold text-gray-300">Test Console</h2>
          <span className={`text-xs font-mono ${PROVIDER_COLORS[activeProvider] || 'text-gray-400'}`}>
            → {activeProvider}
          </span>
        </div>
        {messages.length > 0 && (
          <button
            onClick={() => setMessages([])}
            className="text-gray-600 hover:text-gray-400 transition-colors"
            title="Clear console"
          >
            <Trash2 className="w-4 h-4" />
          </button>
        )}
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto space-y-3 mb-4 pr-1 scrollbar-thin">
        {messages.length === 0 && (
          <div className="flex items-center justify-center h-full text-gray-700 text-sm">
            Send a message to test the active provider
          </div>
        )}
        {messages.map((msg, i) => (
          <div key={i} className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
            <div
              className={`max-w-[85%] rounded-xl px-4 py-2.5 text-sm ${
                msg.role === 'user'
                  ? 'bg-brand-500/20 text-gray-100 border border-brand-500/30'
                  : msg.role === 'error'
                  ? 'bg-red-500/10 text-red-400 border border-red-500/20'
                  : 'bg-gray-800 text-gray-200 border border-gray-700'
              }`}
            >
              <p className="whitespace-pre-wrap">{msg.content}</p>
              {msg.model && (
                <p className="text-xs text-gray-600 mt-1 font-mono">{msg.model}</p>
              )}
            </div>
          </div>
        ))}
        {loading && (
          <div className="flex justify-start">
            <div className="bg-gray-800 border border-gray-700 rounded-xl px-4 py-2.5">
              <Loader2 className="w-4 h-4 text-gray-400 animate-spin" />
            </div>
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      {/* Input */}
      <div className="flex gap-2">
        <input
          className="input flex-1"
          type="text"
          value={prompt}
          onChange={(e) => setPrompt(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && handleSend()}
          placeholder="Type a message and press Enter…"
          disabled={loading}
        />
        <button
          onClick={handleSend}
          disabled={loading || !prompt.trim()}
          className="btn-primary px-3"
        >
          {loading ? (
            <Loader2 className="w-4 h-4 animate-spin" />
          ) : (
            <Send className="w-4 h-4" />
          )}
        </button>
      </div>
    </div>
  )
}
