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

export async function getIsolatedBarns({ signal } = {}) {
  const listed = [];
  let page = 0;
  let totalPages = 1;
  while (page < totalPages) {
    const result = await request(`/api/galpones?page=${page}&size=20&sort=nombre,asc`, { signal });
    listed.push(...(result.elementos || []));
    totalPages = result.totalPaginas || 0;
    page += 1;
  }
  const isolated = listed.filter((barn) => barn.estado === 'AISLAMIENTO');
  return Promise.all(isolated.map((barn) => request(`/api/galpones/${encodeURIComponent(barn.id)}`, { signal })));
}

export const createIsolationRequest = (barnId, body) =>
  request(`/api/galpones/${encodeURIComponent(barnId)}/solicitudes-aislamiento`, {
    method: 'POST', body
  });

export const getPendingIsolationRequests = (options) =>
  request('/api/solicitudes-aislamiento/pendientes', options);

export const getIsolationRequest = (requestId, options) =>
  request(`/api/solicitudes-aislamiento/${encodeURIComponent(requestId)}`, options);

export const validateIsolationRequest = (requestId, body) =>
  request(`/api/solicitudes-aislamiento/${encodeURIComponent(requestId)}/validacion`, {
    method: 'POST', body
  });
