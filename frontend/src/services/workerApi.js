import { getBarn, listBarns } from './barnsApi';

export async function getProductiveBarns({ signal } = {}) {
  const listed = [];
  let page = 0;
  let totalPages = 1;

  while (page < totalPages) {
    const result = await listBarns(page, { signal });
    listed.push(...(result.elementos || []));
    totalPages = result.totalPaginas || 0;
    page += 1;
  }

  const productive = listed.filter((barn) => barn.estado === 'PRODUCTIVO');
  const details = await Promise.all(productive.map((barn) => getBarn(barn.id, { signal })));
  return details.filter((barn) => barn.loteActual);
}

export async function registerMortality(barnId, body, idempotencyKey) {
  const response = await fetch(`/api/galpones/${encodeURIComponent(barnId)}/mortalidad`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Idempotency-Key': idempotencyKey
    },
    body: JSON.stringify(body)
  });
  const data = await response.json().catch(() => null);
  if (!response.ok) {
    const error = new Error(data?.detail || `HTTP ${response.status}`);
    error.status = response.status;
    throw error;
  }
  return data;
}

export async function reportAbnormalMortality(barnId, body, idempotencyKey) {
  const response = await fetch(`/api/galpones/${encodeURIComponent(barnId)}/mortalidad-anormal`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Idempotency-Key': idempotencyKey
    },
    body: JSON.stringify(body)
  });
  const data = await response.json().catch(() => null);
  if (!response.ok) {
    const error = new Error(data?.detail || `HTTP ${response.status}`);
    error.status = response.status;
    throw error;
  }
  return data;
}
