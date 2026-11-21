async function request(path, { signal } = {}) {
  const response = await fetch(`/api/galpones${path}`, { signal });
  const data = await response.json().catch(() => null);
  if (!response.ok) throw new Error(data?.detail || `HTTP ${response.status}`);
  return data;
}

export const listBarns = (page = 0, options) =>
  request(`?page=${page}&size=20&sort=nombre,asc`, options);
export async function listAllBarns(options) {
  const first = await listBarns(0, options);
  const pages = await Promise.all(
    Array.from({ length: Math.max(0, first.totalPaginas - 1) }, (_, index) => listBarns(index + 1, options))
  );
  return [first, ...pages].flatMap((page) => page.elementos || []);
}
export const getBarnSummary = (options) => request('/resumen', options);
export const getBarn = (id, options) => request(`/${encodeURIComponent(id)}`, options);
export const getBarnAge = (id, options) => request(`/${encodeURIComponent(id)}/edad-lote`, options);
