export async function getBackendHealth({ signal } = {}) {
  const response = await fetch('/actuator/health', { signal });
  if (!response.ok) {
    throw new Error(`El backend respondió con HTTP ${response.status}`);
  }

  const health = await response.json();
  if (health.status !== 'UP') {
    throw new Error(`Estado del backend: ${health.status || 'desconocido'}`);
  }

  return health;
}
