export async function getPendingHealthAlerts({ signal } = {}) {
  const response = await fetch('/api/alertas-sanitarias?page=0&size=20', { signal });
  const data = await response.json().catch(() => null);
  if (!response.ok) throw new Error(data?.detail || `HTTP ${response.status}`);
  return data;
}
