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

export const listMedications = (signal) => request('/api/medicaciones', { signal });
export const createMedication = (body) => request('/api/medicaciones', { method: 'POST', body });
export const updateMedication = (id, body) =>
  request(`/api/medicaciones/${encodeURIComponent(id)}`, { method: 'PUT', body });
