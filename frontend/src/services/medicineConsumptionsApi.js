async function request(path, { method = 'GET', body, idempotencyKey, signal } = {}) {
  const response = await fetch(path, {
    method,
    signal,
    headers: {
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...(idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : {})
    },
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

export const getLotMedicineConsumptions = (lotId, signal) =>
  request(`/api/lotes/${encodeURIComponent(lotId)}/consumos-medicamento`, { signal });

export const getMedicineConsumption = (id, signal) =>
  request(`/api/consumos-medicamento/${encodeURIComponent(id)}`, { signal });

export const createMedicineConsumption = (diagnosisId, body, idempotencyKey) =>
  request(`/api/diagnosticos/${encodeURIComponent(diagnosisId)}/consumos-medicamento`, {
    method: 'POST', body, idempotencyKey
  });
