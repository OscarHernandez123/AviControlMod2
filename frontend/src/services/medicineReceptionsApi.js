const BASE_PATH = '/api/recepciones/medicamentos';

async function request(path, options = {}) {
  const response = await fetch(`${BASE_PATH}${path}`, {
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

export const listMedicinesForReception = (options = {}) => request('/disponibles', options);

export function listMedicineReceptions(filters = {}, { signal } = {}) {
  const query = new URLSearchParams({ pagina: String(filters.page || 0), tamano: '20' });
  if (filters.search?.trim()) query.set('busqueda', filters.search.trim());
  if (filters.presentation) query.set('presentacion', filters.presentation);
  if (filters.from) query.set('desde', filters.from);
  if (filters.to) query.set('hasta', filters.to);
  return request(`?${query}`, { signal });
}

export const createMedicineReception = (data) =>
  request('', { method: 'POST', body: JSON.stringify(data) });
export const getMedicineReception = (id) => request(`/${encodeURIComponent(id)}`);
export const getMedicineReceptionEditForm = (id) => request(`/${encodeURIComponent(id)}/edicion`);
export const updateMedicineReception = (id, data) =>
  request(`/${encodeURIComponent(id)}`, { method: 'PUT', body: JSON.stringify(data) });
