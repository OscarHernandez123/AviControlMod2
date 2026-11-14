import { useCallback, useEffect, useState } from 'react';
import Sidebar from '../components/Sidebar';
import Topbar from '../components/Topbar';
import BaseModal from '../components/BaseModal';
import ApiMessage from '../components/ApiMessage';
import { getBarn, listBarns } from '../services/barnsApi';
import {
  listProductiveSlaughterOrders, getProductiveSlaughterSummary, validateSlaughterFitness,
  scheduleSlaughter, rescheduleSlaughter, cancelSlaughter, getSlaughterResult, saveSlaughterResult
} from '../services/productiveSlaughterApi';

function localDateTime(date) {
  const value = new Date(date);
  const pad = (number) => String(number).padStart(2, '0');
  return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}T${pad(value.getHours())}:${pad(value.getMinutes())}`;
}

async function allBarns(signal) {
  const items = [];
  let page = 0;
  let pages = 1;
  while (page < pages) {
    const result = await listBarns(page, { signal });
    items.push(...(result.elementos || []));
    pages = result.totalPaginas || 0;
    page += 1;
  }
  return items;
}

function AdminSlaughterOrdersPage() {
  const [orders, setOrders] = useState([]);
  const [summary, setSummary] = useState({ totalOrdenes: 0, ordenesPendientes: 0 });
  const [barns, setBarns] = useState([]);
  const [activeBarns, setActiveBarns] = useState([]);
  const [results, setResults] = useState({});
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState(null);
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('TODOS');
  const [modal, setModal] = useState(null);
  const [barnId, setBarnId] = useState('');
  const [scheduledAt, setScheduledAt] = useState(localDateTime(Date.now() + 3600000));
  const [birds, setBirds] = useState('');
  const [weight, setWeight] = useState('');

  const load = useCallback(async (signal) => {
    setLoading(true);
    try {
      const [nextOrders, nextSummary, nextBarns] = await Promise.all([
        listProductiveSlaughterOrders(signal), getProductiveSlaughterSummary(signal), allBarns(signal)
      ]);
      setOrders(nextOrders);
      setSummary(nextSummary);
      setBarns(nextBarns);
      const candidates = nextBarns.filter((barn) => ['PRODUCTIVO', 'EN_COSECHA'].includes(barn.estado));
      const details = await Promise.allSettled(candidates.map((barn) => getBarn(barn.id, { signal })));
      setActiveBarns(details.flatMap((result) => result.status === 'fulfilled' ? [result.value] : []));
      const executed = nextOrders.filter((order) => order.estado === 'EJECUTADA');
      const fetched = await Promise.allSettled(executed.map((order) => getSlaughterResult(order.id, signal)));
      setResults(Object.fromEntries(fetched.flatMap((result, index) =>
        result.status === 'fulfilled' ? [[executed[index].id, result.value]] : [])));
    } catch (error) {
      if (error.name !== 'AbortError') setMessage({ type: 'error', text: error.message });
    } finally {
      if (!signal?.aborted) setLoading(false);
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    load(controller.signal);
    return () => controller.abort();
  }, [load]);

  const readyToValidate = activeBarns.filter((barn) => barn.estado === 'PRODUCTIVO' && barn.loteActual);
  const readyToSchedule = activeBarns.filter((barn) => barn.estado === 'EN_COSECHA' && barn.loteActual &&
    !orders.some((order) => order.galponId === barn.id && order.loteId === barn.loteActual.id));
  const eligible = modal?.kind === 'validate' ? readyToValidate : readyToSchedule;
  const filtered = orders.filter((order) => {
    const barnName = barns.find((barn) => barn.id === order.galponId)?.nombre;
    const terms = [order.id, order.galponId, order.loteId, barnName].join(' ').toLowerCase();
    return terms.includes(search.trim().toLowerCase()) && (status === 'TODOS' || order.estado === status);
  });

  function open(kind, order = null) {
    setMessage(null);
    setBarnId('');
    setScheduledAt(localDateTime(Math.max(Date.now() + 3600000, order ? Date.parse(order.fechaHoraProgramada) : 0)));
    const result = order && results[order.id];
    setBirds(result ? String(result.cantidadFinalPollos) : '');
    setWeight(result ? String(result.pesoTotalKg) : '');
    setModal({ kind, order });
  }

  async function submit(event) {
    event.preventDefault();
    if (!modal) return;
    setBusy(true);
    try {
      let result;
      if (modal.kind === 'validate') result = await validateSlaughterFitness(barnId);
      if (modal.kind === 'schedule') result = await scheduleSlaughter(barnId, new Date(scheduledAt).toISOString());
      if (modal.kind === 'reschedule') result = await rescheduleSlaughter(modal.order.id, new Date(scheduledAt).toISOString());
      if (modal.kind === 'cancel') result = await cancelSlaughter(modal.order.id);
      if (modal.kind === 'result') result = await saveSlaughterResult(modal.order.id, {
        cantidadFinalPollos: Number(birds), pesoTotalKg: Number(weight)
      }, Boolean(results[modal.order.id]));
      setModal(null);
      await load();
      const feedback = modal.kind === 'validate'
        ? result.apto ? 'Galpón apto; pasó a En cosecha.' : result.motivosNoAptitud.join(' · ')
        : modal.kind === 'result' ? 'Resultado de sacrificio guardado.' : `Orden ${result.estado.toLowerCase()} correctamente.`;
      setMessage({ type: modal.kind === 'validate' && !result.apto ? 'error' : 'success', text: feedback });
    } catch (error) {
      setMessage({ type: 'error', text: error.message });
    } finally {
      setBusy(false);
    }
  }

  const title = {
    validate: 'Validar aptitud para sacrificio', schedule: 'Programar orden de sacrificio',
    reschedule: 'Reprogramar orden', cancel: 'Cancelar orden', result: 'Resultado final de sacrificio'
  };

  return <div className="avi-role-layout">
    <Sidebar role="administrador" />
    <div className="avi-main-frame">
      <Topbar roleLabel="Administrador" avatarSrc="/assets/admin-avatar.svg" />
      <ApiMessage message={message?.text} type={message?.type} onClose={() => setMessage(null)} />
      <div className="avi-dashboard-content">
        <div className="avi-page-heading">
          <div><h1 className="avi-page-title avi-title-lg">Órdenes de sacrificio</h1>
            <p className="avi-page-subtitle">Aptitud, programación y resultados del sacrificio productivo</p></div>
          <button type="button" className="avi-btn-orange-cta" onClick={() => open('validate')}>Validar aptitud de galpón</button>
        </div>
        <div className="avi-receptions-metrics-row">
          <div className="avi-reception-kpi-card"><span className="avi-reception-kpi-label">Galpones en cosecha</span><strong className="avi-reception-kpi-value">{activeBarns.filter((barn) => barn.estado === 'EN_COSECHA').length}</strong></div>
          <div className="avi-reception-kpi-card"><span className="avi-reception-kpi-label">Órdenes totales</span><strong className="avi-reception-kpi-value">{summary.totalOrdenes}</strong></div>
          <div className="avi-reception-kpi-card"><span className="avi-reception-kpi-label">Órdenes pendientes</span><strong className="avi-reception-kpi-value">{summary.ordenesPendientes}</strong></div>
        </div>
        <div className="avi-receptions-filter-bar">
          <input className="avi-receptions-search-input" placeholder="Buscar por orden, galpón o lote" value={search} onChange={(event) => setSearch(event.target.value)} />
          <select className="avi-receptions-select" value={status} onChange={(event) => setStatus(event.target.value)}>
            <option value="TODOS">Todos los estados</option><option value="PROGRAMADA">Programada</option><option value="REPROGRAMADA">Reprogramada</option><option value="EJECUTADA">Ejecutada</option><option value="CANCELADA">Cancelada</option>
          </select>
          <button type="button" className="avi-receptions-filter-btn" onClick={() => load()}>Actualizar</button>
          <button type="button" className="avi-btn-orange-cta" onClick={() => open('schedule')}>Programar orden</button>
        </div>
        <div className="avi-recent-orders-panel">
          <div className="avi-panel-header avi-panel-header-full"><h2 className="avi-panel-title">Órdenes registradas</h2></div>
          <div className="avi-orders-table avi-productive-orders-table">
            <div className="avi-orders-thead"><span style={{ flex: 1.3 }}>Orden</span><span style={{ flex: 1 }}>Galpón</span><span style={{ flex: 0.9 }}>Fecha</span><span style={{ flex: 0.8 }}>Estado</span><span style={{ flex: 1 }}>Resultado</span><span style={{ flex: 1.6 }}>Acciones</span></div>
            {loading && <p>Cargando órdenes...</p>}
            {!loading && filtered.length === 0 && <p>No hay órdenes para mostrar.</p>}
            {filtered.map((order) => {
              const result = results[order.id];
              const beforeDate = Date.parse(order.fechaHoraProgramada) > Date.now();
              const pending = ['PROGRAMADA', 'REPROGRAMADA'].includes(order.estado) && beforeDate;
              return <div key={order.id} className="avi-orders-trow">
                <span style={{ flex: 1.3, overflowWrap: 'anywhere' }}>{order.id}</span>
                <span style={{ flex: 1 }}>{barns.find((barn) => barn.id === order.galponId)?.nombre || order.galponId}</span>
                <span style={{ flex: 0.9 }}>{new Date(order.fechaHoraProgramada).toLocaleString('es-CO')}</span>
                <span style={{ flex: 0.8 }}>{order.estado}</span>
                <span style={{ flex: 1 }}>{result ? `${result.cantidadFinalPollos} aves · ${result.pesoTotalKg} kg` : '—'}</span>
                <span style={{ flex: 1.6, display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                  {pending && <button type="button" className="avi-admin-table-btn avi-admin-table-btn-orange" onClick={() => open('reschedule', order)}>Reprogramar</button>}
                  {pending && <button type="button" className="avi-admin-table-btn avi-admin-table-btn-green" onClick={() => open('cancel', order)}>Cancelar</button>}
                  {order.estado === 'EJECUTADA' && <button type="button" className="avi-admin-table-btn avi-admin-table-btn-green" onClick={() => open('result', order)}>{result ? 'Ver / corregir' : 'Registrar resultado'}</button>}
                </span>
              </div>;
            })}
          </div>
          <p className="avi-page-subtitle">Las órdenes se ejecutan automáticamente al llegar la fecha programada. Pulsa Actualizar para ver el nuevo estado.</p>
        </div>
      </div>
    </div>
    <BaseModal isOpen={Boolean(modal)} onClose={() => setModal(null)} maxWidth="680px">
      {modal && <form className="avi-figma-modal-720" onSubmit={submit}>
        <div className="avi-figma-modal-header"><h2 className="avi-figma-modal-title">{title[modal.kind]}</h2></div>
        {['validate', 'schedule'].includes(modal.kind) && <div className="avi-figma-field">
          <label className="avi-figma-label">Galpón</label>
          <select className="avi-figma-input avi-figma-select" required value={barnId} onChange={(event) => setBarnId(event.target.value)}>
            <option value="">Seleccionar galpón</option>
            {eligible.map((barn) => <option key={barn.id} value={barn.id}>{barn.nombre} · {barn.loteActual?.edad?.diasTotales} días · {barn.loteActual?.poblacionActual} aves</option>)}
          </select>
          {eligible.length === 0 && <p>No hay galpones elegibles para esta operación.</p>}
        </div>}
        {['schedule', 'reschedule'].includes(modal.kind) && <div className="avi-figma-field">
          <label className="avi-figma-label">Fecha y hora futura</label>
          <input type="datetime-local" className="avi-figma-input" required min={localDateTime(Date.now() + 60000)} value={scheduledAt} onChange={(event) => setScheduledAt(event.target.value)} />
        </div>}
        {modal.kind === 'cancel' && <p>¿Cancelar la orden {modal.order.id}? No podrá programarse otra para el mismo lote.</p>}
        {modal.kind === 'result' && <div className="avi-figma-modal-grid">
          <div className="avi-figma-field"><label className="avi-figma-label">Cantidad final de pollos</label><input type="number" min="1" step="1" className="avi-figma-input" required value={birds} onChange={(event) => setBirds(event.target.value)} /></div>
          <div className="avi-figma-field"><label className="avi-figma-label">Peso total (kg)</label><input type="number" min="0.01" step="0.01" className="avi-figma-input" required value={weight} onChange={(event) => setWeight(event.target.value)} /></div>
          {results[modal.order.id]?.estado !== 'DISPONIBLE' && results[modal.order.id] && <p>Este resultado ya fue utilizado y no admite correcciones.</p>}
        </div>}
        <div className="avi-figma-modal-footer"><button type="button" className="avi-figma-btn-cancel" onClick={() => setModal(null)}>Cerrar</button>
          <button type="submit" className="avi-figma-btn-green" disabled={busy || (['validate', 'schedule'].includes(modal.kind) && !barnId) || (modal.kind === 'result' && results[modal.order.id]?.estado !== 'DISPONIBLE' && Boolean(results[modal.order.id]))}>Confirmar</button>
        </div>
      </form>}
    </BaseModal>
  </div>;
}

export default AdminSlaughterOrdersPage;
