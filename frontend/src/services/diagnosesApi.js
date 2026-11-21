async function request(path, options = {}) {
  const response = await fetch(path, {
    ...options,
    headers: options.body ? { 'Content-Type': 'application/json' } : undefined,
    body: options.body ? JSON.stringify(options.body) : undefined
  });
  const data = await response.json().catch(() => null);
  if (!response.ok) throw new Error(data?.detail || `HTTP ${response.status}`);
  return data;
}

export const listDiagnoses = (signal) => request('/api/diagnosticos', { signal });
export const getDiagnosis = (id) => request(`/api/diagnosticos/${encodeURIComponent(id)}`);
export const createDiagnosis = (barnId, body) =>
  request(`/api/galpones/${encodeURIComponent(barnId)}/diagnosticos`, { method: 'POST', body });
