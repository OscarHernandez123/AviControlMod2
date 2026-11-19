import { useEffect, useRef, useState } from 'react';
import { getIntegrationCapabilities, listIntegrationOperations } from '../services/integrationApi';

const labels = { MORTALIDAD: 'Registro de mortalidad', AISLAMIENTO: 'Aislamiento', REANUDACION: 'Reanudación', VACIADO: 'Vaciado sanitario' };
const states = { PENDIENTE: 'Pendiente de envío', PUBLICADO: 'Enviado; aceptación pendiente', EXPIRADO: 'No entregado: requiere revisión' };

export default function IntegrationOperations({ onDelivered, tipos }) {
  const [operations, setOperations] = useState([]);
  const [error, setError] = useState('');
  const delivered = useRef(new Map());
  const callback = useRef(onDelivered);
  callback.current = onDelivered;
  useEffect(() => {
    const controller = new AbortController();
    let timer;
    let refresh;
    let stopped = false;
    async function poll() {
      try {
        const capabilities = await getIntegrationCapabilities({ signal: controller.signal });
        if (!capabilities.asincrono) { stopped = true; return; }
        const items = await listIntegrationOperations({ signal: controller.signal });
        if (stopped) return;
        setOperations(items);
        setError('');
        let changed = false;
        for (const item of items) {
          const state = item.integracion?.estado;
          if (state === 'PUBLICADO' && delivered.current.get(item.id) !== state) changed = true;
          delivered.current.set(item.id, state);
        }
        if (changed) {
          clearTimeout(refresh);
          refresh = setTimeout(() => callback.current?.(), 2000);
        }
      } catch (cause) {
        if (cause.name !== 'AbortError') setError('No se pudo actualizar el seguimiento de solicitudes.');
      } finally {
        if (!stopped) timer = setTimeout(poll, 3000);
      }
    }
    poll();
    return () => { stopped = true; controller.abort(); clearTimeout(timer); clearTimeout(refresh); };
  }, []);
  const visible = operations.filter((item) => !tipos || tipos.includes(item.tipo)).slice(0, 10);
  if (!visible.length && !error) return null;
  return <section className="avi-recent-orders-panel" aria-label="Seguimiento de solicitudes" aria-live="polite">
    <h2 className="avi-panel-title">Seguimiento de solicitudes</h2>
    <p>El envío todavía no confirma que el cambio haya sido aceptado. Consulta los datos actualizados del galpón.</p>
    {error && <p role="alert">{error}</p>}
    {visible.map((item) => <p key={item.id}>
      <strong>{labels[item.tipo] || 'Solicitud'}</strong> · {states[item.integracion?.estado] || 'Estado no disponible'} · Referencia {item.id.slice(0, 8)}
    </p>)}
  </section>;
}
