export default function PlanBanner() {
  const raw = localStorage.getItem('modelrouter_token') || localStorage.getItem('ids_token') || '';
  try {
    const payload = raw.split('.')[1];
    const claims = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
    const tier = claims?.tier || 'FREE';
    const trialStartedAt = claims?.trialStartedAt || 0;
    const trialEndMs = trialStartedAt + 7 * 24 * 60 * 60 * 1000;
    const now = Date.now();
    const trialActive = now < trialEndMs;
    const daysLeft = Math.ceil((trialEndMs - now) / (24 * 60 * 60 * 1000));

    if (tier === 'PRO') return null;
    if (trialActive) return (
      <div style={{ background: '#22c55e11', borderBottom: '1px solid #22c55e33', padding: '8px 24px', fontSize: 13, color: '#86efac', display: 'flex', alignItems: 'center', gap: 8 }}>
        <span>Free trial — <strong>{daysLeft} day{daysLeft !== 1 ? 's' : ''}</strong> remaining</span>
        <span style={{ color: '#64748b' }}>·</span>
        <span style={{ color: '#64748b' }}>Upgrade: <a href="mailto:admin@nexuslayer.eu" style={{ color: '#22c55e' }}>admin@nexuslayer.eu</a></span>
      </div>
    );
    return (
      <div style={{ background: '#ef444411', borderBottom: '1px solid #ef444433', padding: '8px 24px', fontSize: 13, color: '#fca5a5', display: 'flex', alignItems: 'center', gap: 8 }}>
        <span>Trial expired — <strong>5 actions/day</strong> limit active</span>
        <span style={{ color: '#64748b' }}>·</span>
        <span>Upgrade: <a href="mailto:admin@nexuslayer.eu" style={{ color: '#ef4444' }}>admin@nexuslayer.eu</a></span>
      </div>
    );
  } catch { return null; }
}
