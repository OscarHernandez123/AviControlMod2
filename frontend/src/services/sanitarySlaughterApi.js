async function request(path, options = {}) {
  const response = await fetch(path, options);
  const data = await response.json().catch(() => null);
  if (!response.ok) throw new Error(data?.detail || `HTTP ${response.status}`);
  return data;
}

export const listSanitarySlaughterOrders = ({ signal } = {}) =>
  request('/api/ordenes-sacrificio-sanitario', { signal });

export const issueSanitarySlaughterOrder = (barnId) =>
  request(`/api/galpones/${encodeURIComponent(barnId)}/ordenes-sacrificio-sanitario`, {
    method: 'POST'
  });

export const executeSanitarySlaughterOrder = (orderId) =>
  request(`/api/ordenes-sacrificio-sanitario/${encodeURIComponent(orderId)}/ejecucion`, {
    method: 'POST'
  });
