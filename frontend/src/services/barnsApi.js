async function request(path, { signal } = {}) {
  const response = await fetch(`/api/galpones${path}`, { signal });
  const data = await response.json().catch(() => null);
  if (!response.ok) throw new Error(data?.detail || `HTTP ${response.status}`);
  return data;
}

export const listBarns = (page = 0, options) =>
  request(`?page=${page}&size=20&sort=nombre,asc`, options);
export const getBarnSummary = (options) => request('/resumen', options);
export const getBarn = (id, options) => request(`/${encodeURIComponent(id)}`, options);
export const getBarnAge = (id, options) => request(`/${encodeURIComponent(id)}/edad-lote`, options);
