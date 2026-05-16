export function formatBytes(n: number | null | undefined): string {
  if (!n) return '—';
  if (n < 1024) return n + ' B';
  if (n < 1024 * 1024) return (n / 1024).toFixed(1) + ' KB';
  if (n < 1024 * 1024 * 1024) return (n / (1024 * 1024)).toFixed(1) + ' MB';
  if (n < 1024 * 1024 * 1024 * 1024) return (n / (1024 * 1024 * 1024)).toFixed(2) + ' GB';
  return (n / (1024 * 1024 * 1024 * 1024)).toFixed(2) + ' TB';
}

export function formatDuration(s: number | null | undefined): string {
  if (s == null) return '—';
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  const sec = Math.floor(s % 60);
  if (h > 0) return `${h}h${m}m${sec}s`;
  return `${m}m${sec}s`;
}

export function formatDurationCompact(s: number | null | undefined): string {
  if (s == null) return '—';
  const d = Math.floor(s / 86400);
  const h = Math.floor((s % 86400) / 3600);
  if (d > 0) return `${d} 天 ${h} 小时`;
  const m = Math.floor((s % 3600) / 60);
  if (h > 0) return `${h} 时 ${m} 分`;
  return `${m} 分`;
}

export function formatDateTime(s: string | null | undefined): string {
  if (!s) return '—';
  const trimmed = s.trim();
  const matched = trimmed.match(/^(\d{4}-\d{2}-\d{2})[T\s](\d{2}:\d{2}:\d{2})/);
  if (matched) return `${matched[1]} ${matched[2]}`;
  if (/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) return `${trimmed} 00:00:00`;
  const parsed = new Date(trimmed);
  if (!Number.isNaN(parsed.getTime())) {
    const p = (n: number) => String(n).padStart(2, '0');
    return `${parsed.getFullYear()}-${p(parsed.getMonth() + 1)}-${p(parsed.getDate())} ${p(parsed.getHours())}:${p(parsed.getMinutes())}:${p(parsed.getSeconds())}`;
  }
  return trimmed.replace('T', ' ').slice(0, 19);
}

export function statusColor(s: string | null | undefined): string {
  if (!s) return 'gray';
  const upper = s.toUpperCase();
  if (upper === 'COMPLETED') return 'green';
  if (upper === 'FAILED') return 'red';
  if (upper === 'PENDING') return 'orange';
  if (upper === 'RECORDING' || upper === 'UPLOADING') return 'blue';
  return 'arcoblue';
}
