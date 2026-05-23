const IDENTITY_SERVER_URL = import.meta.env.VITE_IDENTITY_SERVER_URL || 'http://192.168.68.111:3007'

export default function Login() {
  const handleSso = () => {
    const redirect = encodeURIComponent(window.location.origin)
    window.location.href = `${IDENTITY_SERVER_URL}/login?redirect=${redirect}`
  }

  return (
    <div style={{
      minHeight: '100vh', background: '#0d1117',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
    }}>
      <div style={{
        background: '#161b22', border: '1px solid #21262d',
        borderRadius: 16, padding: 40, width: '100%', maxWidth: 360, textAlign: 'center',
      }}>
        <div style={{
          width: 56, height: 56, borderRadius: 14, margin: '0 auto 20px',
          background: 'linear-gradient(135deg, #06b6d4, #3b82f6)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 28,
        }}>🔀</div>
        <h1 style={{ fontSize: 22, fontWeight: 700, color: '#e6edf3', marginBottom: 6 }}>
          ModelRouter
        </h1>
        <p style={{ color: '#7d8590', fontSize: 13, marginBottom: 28 }}>
          One endpoint for all your LLMs
        </p>
        <button
          onClick={handleSso}
          style={{
            width: '100%', padding: '13px 16px', borderRadius: 10, border: 'none',
            background: 'linear-gradient(135deg, #06b6d4, #3b82f6)',
            color: '#fff', fontWeight: 700, fontSize: 14, cursor: 'pointer',
            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
          }}
        >
          <span style={{ fontSize: 16 }}>🔐</span> Sign in with NexusLayer SSO
        </button>
      </div>
    </div>
  )
}
