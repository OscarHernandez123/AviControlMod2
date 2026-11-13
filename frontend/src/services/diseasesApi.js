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

export const listDiseases = (signal) => request('/api/enfermedades', { signal });
export const createDisease = (body) => request('/api/enfermedades', { method: 'POST', body });
export const updateDisease = (id, body) =>
  request(`/api/enfermedades/${encodeURIComponent(id)}`, { method: 'PUT', body });
