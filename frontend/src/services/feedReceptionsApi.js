const BASE_PATH = '/api/recepciones/alimentos';

async function requestUrl(url, options = {}) {
  const response = await fetch(url, {
    ...options,
    headers: {
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...options.headers
    }
  });
  const data = await response.json().catch(() => null);

  if (!response.ok) {
    const fields = data?.fieldErrors?.map((error) => error.message || error.defaultMessage).filter(Boolean);
    throw new Error(fields?.length ? fields.join('. ') : data?.detail || `HTTP ${response.status}`);
  }

  return data;
}

const request = (path, options) => requestUrl(`${BASE_PATH}${path}`, options);

export function listAvailableFoods(type, { signal } = {}) {
  const query = new URLSearchParams({ tipoAlimento: type });
  return requestUrl(`/api/nutricion/alimentos-compatibles?${query}`, { signal });
}

export function listFeedReceptions(filters = {}, { signal } = {}) {
  const query = new URLSearchParams({ pagina: String(filters.page || 0), tamano: '20' });
  if (filters.search?.trim()) query.set('busqueda', filters.search.trim());
  if (filters.status) query.set('estado', filters.status);
  if (filters.from) query.set('desde', filters.from);
  if (filters.to) query.set('hasta', filters.to);
  return request(`?${query}`, { signal });
}

export function createFeedReception(data) {
  return request('', { method: 'POST', body: JSON.stringify(data) });
}

export function getFeedReception(id) {
  return request(`/${encodeURIComponent(id)}`);
}

export function getFeedReceptionEditForm(id) {
  return request(`/${encodeURIComponent(id)}/edicion`);
}

export function updateFeedReception(id, data) {
  return request(`/${encodeURIComponent(id)}`, { method: 'PUT', body: JSON.stringify(data) });
}
