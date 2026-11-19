export async function getIntegrationCapabilities({ signal } = {}) {
  const response = await fetch('/api/integracion/modulo1/capacidades', { signal });
  if (response.status === 404) return { asincrono: false, sacrificioSanitario: true };
  const data = await response.json();
  if (!response.ok) throw new Error(data?.detail || `HTTP ${response.status}`);
  return data;
}

export async function listIntegrationOperations({ signal } = {}) {
  const response = await fetch('/api/integracion/modulo1/operaciones', { signal });
  const data = await response.json();
  if (!response.ok) throw new Error(data?.detail || `HTTP ${response.status}`);
  return data;
}
