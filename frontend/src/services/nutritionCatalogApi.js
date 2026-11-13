async function request(path, options = {}) {
  const response = await fetch(path, {
    ...options,
    headers: {
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...options.headers
    }
  });
  const data = await response.json().catch(() => null);
  if (!response.ok) {
    const fields = data?.fieldErrors?.map((item) => item.message || item.defaultMessage).filter(Boolean);
    throw new Error(fields?.length ? fields.join('. ') : data?.detail || `HTTP ${response.status}`);
  }
  return data;
}

export const listTemplates = ({ signal } = {}) => request('/api/nutricion/plantillas', { signal });
export const getTemplate = (id) => request(`/api/nutricion/plantillas/${encodeURIComponent(id)}`);
export const createTemplate = (data) => request('/api/nutricion/plantillas', {
  method: 'POST', body: JSON.stringify(data)
});
export const updateTemplate = (id, data) => request(`/api/nutricion/plantillas/${encodeURIComponent(id)}`, {
  method: 'PUT', body: JSON.stringify(data)
});
export const listCompatibleFoods = (type, { signal } = {}) => request(
  `/api/nutricion/alimentos-compatibles?tipoAlimento=${encodeURIComponent(type)}`, { signal }
);
export const createCommercialFood = (data) => request('/api/nutricion/alimentos', {
  method: 'POST', body: JSON.stringify(data)
});

export const localBarnId = import.meta.env.VITE_AVICONTROL_GALPON_ID || '550e8400-e29b-41d4-a716-446655440101';
export const getBarnNutritionPlan = (barnId = localBarnId, { signal } = {}) =>
  request(`/api/galpones/${encodeURIComponent(barnId)}/plan-nutricional`, { signal });
export const substituteBarnNutritionPlan = (barnId, data) =>
  request(`/api/galpones/${encodeURIComponent(barnId)}/plan-nutricional/sustituir`, {
    method: 'PUT', body: JSON.stringify(data)
  });

export const localLotId = import.meta.env.VITE_AVICONTROL_LOTE_ID || '550e8400-e29b-41d4-a716-446655440102';
export const getLotFoodRequirement = (lotId = localLotId, { signal } = {}) =>
  request(`/api/lotes/${encodeURIComponent(lotId)}/alimento-requerido`, { signal });
export const getSupplyBalance = ({ signal } = {}) =>
  request('/api/administracion/balance-abastecimiento', { signal });
