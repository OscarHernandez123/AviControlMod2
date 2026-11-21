export const demoRolesEnabled = import.meta.env.VITE_DEMO_ROLE_SELECTOR === 'true';
export const demoRoles = [
  ['administrador', 'Administrador'], ['nutricionista', 'Nutricionista'],
  ['veterinario', 'Veterinario'], ['trabajador', 'Trabajador']
];
export function roleForPath(pathname) {
  const role = pathname.split('/')[1];
  return demoRoles.some(([value]) => value === role) ? role : null;
}
export function installDemoRoleRequests() {
  if (!demoRolesEnabled) return;
  const originalFetch = window.fetch.bind(window);
  window.fetch = (input, init = {}) => {
    const request = new Request(input, init);
    const url = new URL(request.url);
    if (url.origin === window.location.origin && url.pathname.startsWith('/api/')) {
      const headers = new Headers(request.headers);
      const role = roleForPath(window.location.pathname);
      if (role) headers.set('X-Demo-Role', role);
      headers.delete('Authorization');
      return originalFetch(new Request(request, { headers, credentials: 'omit' }));
    }
    return originalFetch(request);
  };
}