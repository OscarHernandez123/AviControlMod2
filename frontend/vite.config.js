import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const backend = process.env.AVICONTROL_BACKEND_URL || 'http://localhost:8080';
const adminUser = process.env.AVICONTROL_ADMIN_USERNAME || 'admin';
const adminPassword = process.env.AVICONTROL_ADMIN_PASSWORD || 'admin123';
const adminAuthorization = `Basic ${Buffer.from(`${adminUser}:${adminPassword}`).toString('base64')}`;
const nutritionistUser = process.env.AVICONTROL_NUTRICIONISTA_USERNAME || 'nutricionista';
const nutritionistPassword = process.env.AVICONTROL_NUTRICIONISTA_PASSWORD || 'nutricionista123';
const nutritionistAuthorization = `Basic ${Buffer.from(`${nutritionistUser}:${nutritionistPassword}`).toString('base64')}`;
const workerUser = process.env.AVICONTROL_TRABAJADOR_USERNAME || 'trabajador';
const workerPassword = process.env.AVICONTROL_TRABAJADOR_PASSWORD || 'trabajador123';
const workerAuthorization = `Basic ${Buffer.from(`${workerUser}:${workerPassword}`).toString('base64')}`;
const veterinarianUser = process.env.AVICONTROL_VETERINARIO_USERNAME || 'veterinario';
const veterinarianPassword = process.env.AVICONTROL_VETERINARIO_PASSWORD || 'veterinario123';
const veterinarianAuthorization = `Basic ${Buffer.from(`${veterinarianUser}:${veterinarianPassword}`).toString('base64')}`;

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    open: false,
    proxy: {
      '/api/enfermedades': {
        target: backend,
        changeOrigin: true,
        headers: { Authorization: veterinarianAuthorization }
      },
      '/api/medicaciones': {
        target: backend,
        changeOrigin: true,
        headers: { Authorization: veterinarianAuthorization }
      },
      '/api/diagnosticos': {
        target: backend,
        changeOrigin: true,
        headers: { Authorization: veterinarianAuthorization }
      },
      '/api/medicacion-diaria': {
        target: backend,
        changeOrigin: true,
        headers: { Authorization: workerAuthorization }
      },
      '/api/recepciones/alimentos': {
        target: backend,
        changeOrigin: true,
        headers: { Authorization: adminAuthorization }
      },
      '/api/recepciones/medicamentos': {
        target: backend,
        changeOrigin: true,
        headers: { Authorization: adminAuthorization }
      },
      '/api/nutricion/alimentos-compatibles': {
        target: backend,
        changeOrigin: true,
        headers: { Authorization: adminAuthorization }
      },
      '/api/nutricion/plantillas': {
        target: backend,
        changeOrigin: true,
        headers: { Authorization: nutritionistAuthorization }
      },
      '/api/nutricion/alimentos': {
        target: backend,
        changeOrigin: true,
        headers: { Authorization: nutritionistAuthorization }
      },
      '/api/alimento-diario': {
        target: backend,
        changeOrigin: true,
        headers: { Authorization: workerAuthorization }
      },
      '/api/alertas-sanitarias': {
        target: backend,
        changeOrigin: true,
        headers: { Authorization: veterinarianAuthorization }
      },
      '/api/solicitudes-aislamiento': {
        target: backend,
        changeOrigin: true,
        headers: { Authorization: veterinarianAuthorization }
      },
      '/api/galpones': {
        target: backend,
        changeOrigin: true,
        configure: (proxy) => proxy.on('proxyReq', (proxyRequest, request) => {
          proxyRequest.setHeader('Authorization', request.url.includes('/diagnosticos')
            ? veterinarianAuthorization : request.url.includes('/medicacion-diaria')
            ? workerAuthorization : request.url.includes('/plan-nutricional')
            ? nutritionistAuthorization : request.url.includes('/alimento-diario') || request.url.includes('/suministros-alimento') || request.url.includes('/mortalidad') || request.url.includes('/solicitudes-aislamiento')
            ? workerAuthorization : adminAuthorization);
        })
      },
      '/api/lotes': {
        target: backend,
        changeOrigin: true,
        headers: { Authorization: nutritionistAuthorization }
      },
      '/api/administracion/balance-abastecimiento': {
        target: backend,
        changeOrigin: true,
        headers: { Authorization: nutritionistAuthorization }
      },
      '/api/inventario': {
        target: backend,
        changeOrigin: true,
        headers: { Authorization: adminAuthorization }
      },
      '/api': backend,
      '/actuator': backend
    }
  }
});
