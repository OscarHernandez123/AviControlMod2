async function request(path, { signal } = {}) {
  const response = await fetch(`/api/inventario${path}`, { signal });
  const data = await response.json().catch(() => null);
  if (!response.ok) {
    throw new Error(data?.detail || `El inventario respondió con HTTP ${response.status}`);
  }
  return data;
}

export const getInventory = (options) => request('', options);
export const getInventorySummary = (options) => request('/resumen', options);
export const getFoodRequirement = (id, options) =>
  request(`/alimentos/${encodeURIComponent(id)}/requerimiento`, options);
