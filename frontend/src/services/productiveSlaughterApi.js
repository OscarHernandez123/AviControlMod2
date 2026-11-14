async function request(path, { signal, method = 'GET', body } = {}) {
  const response = await fetch(path, {
    signal,
    method,
    headers: body ? { 'Content-Type': 'application/json' } : undefined,
    body: body ? JSON.stringify(body) : undefined
  });
  const data = await response.json().catch(() => null);
  if (!response.ok) {
    const error = new Error(data?.detail || `HTTP ${response.status}`);
    error.status = response.status;
    throw error;
  }
  return data;
}

export const listProductiveSlaughterOrders = (signal) => request('/api/ordenes-sacrificio', { signal });
export const getProductiveSlaughterSummary = (signal) => request('/api/ordenes-sacrificio/resumen', { signal });
export const validateSlaughterFitness = (barnId) =>
  request(`/api/galpones/${encodeURIComponent(barnId)}/validacion-aptitud-sacrificio`, { method: 'POST' });
export const scheduleSlaughter = (barnId, fechaHoraProgramada) =>
  request(`/api/galpones/${encodeURIComponent(barnId)}/ordenes-sacrificio`, {
    method: 'POST', body: { fechaHoraProgramada }
  });
export const rescheduleSlaughter = (orderId, fechaHoraProgramada) =>
  request(`/api/ordenes-sacrificio/${encodeURIComponent(orderId)}/reprogramacion`, {
    method: 'PUT', body: { fechaHoraProgramada }
  });
export const cancelSlaughter = (orderId) =>
  request(`/api/ordenes-sacrificio/${encodeURIComponent(orderId)}/cancelacion`, { method: 'POST' });
export const getSlaughterResult = (orderId, signal) =>
  request(`/api/ordenes-sacrificio/${encodeURIComponent(orderId)}/resultados`, { signal });
export const saveSlaughterResult = (orderId, body, correction = false) =>
  request(`/api/ordenes-sacrificio/${encodeURIComponent(orderId)}/resultados`, {
    method: correction ? 'PUT' : 'POST', body
  });
