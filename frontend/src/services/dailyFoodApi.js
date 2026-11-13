async function request(path, { signal, method = 'GET', body } = {}) {
  const response = await fetch(path, {
    signal,
    method,
    headers: body ? { 'Content-Type': 'application/json' } : undefined,
    body: body ? JSON.stringify(body) : undefined
  });
  const data = await response.json().catch(() => null);
  if (!response.ok) throw new Error(data?.detail || `HTTP ${response.status}`);
  return data;
}

export async function getDailyFoodSummary({ signal } = {}) {
  const ids = [];
  let page = 0;
  let totalPages = 1;

  while (page < totalPages) {
    const barns = await request(`/api/galpones?page=${page}&size=20&sort=nombre,asc`, { signal });
    ids.push(...(barns.elementos || []).map((barn) => barn.id));
    totalPages = barns.totalPaginas || 0;
    page += 1;
  }

  const params = new URLSearchParams();
  ids.forEach((id) => params.append('galponIds', id));
  return request(`/api/alimento-diario/resumen${params.size ? `?${params}` : ''}`, { signal });
}
export const getBarnDailyFood = (id, options) =>
  request(`/api/galpones/${encodeURIComponent(id)}/alimento-diario`, options);
export const registerDailySupply = (id, body) =>
  request(`/api/galpones/${encodeURIComponent(id)}/suministros-alimento`, { method: 'POST', body });
