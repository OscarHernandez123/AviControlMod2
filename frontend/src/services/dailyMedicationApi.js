async function request(path, signal) {
  const response = await fetch(path, { signal });
  const data = await response.json().catch(() => null);
  if (!response.ok) throw new Error(data?.detail || `HTTP ${response.status}`);
  return data;
}

export function getDailyMedicationSummary(barnIds, signal) {
  const params = new URLSearchParams();
  barnIds.forEach((id) => params.append('galponIds', id));
  return request(`/api/medicacion-diaria/resumen?${params}`, signal);
}

export function getBarnDailyMedication(barnId, signal) {
  return request(`/api/galpones/${encodeURIComponent(barnId)}/medicacion-diaria`, signal);
}
