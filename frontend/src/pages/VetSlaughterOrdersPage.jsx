import { useCallback, useEffect, useMemo, useState } from 'react';
import Sidebar from '../components/Sidebar';
import Topbar from '../components/Topbar';
import StatCard from '../components/StatCard';
import BaseModal from '../components/BaseModal';
import ApiMessage from '../components/ApiMessage';
import { listDiagnoses } from '../services/diagnosesApi';
import { getIsolatedBarns } from '../services/isolationApi';
import { getBarn } from '../services/barnsApi';
import { listSanitarySlaughterOrders, issueSanitarySlaughterOrder, executeSanitarySlaughterOrder } from '../services/sanitarySlaughterApi';

function VetSlaughterOrdersPage() {
  const [orders, setOrders] = useState([]);
  const [diagnoses, setDiagnoses] = useState([]);
  const [isolatedBarns, setIsolatedBarns] = useState([]);
  const [barnNames, setBarnNames] = useState({});
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('TODOS');
  const [diagnosisId, setDiagnosisId] = useState('');
  const [modal, setModal] = useState(null);
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState(null);

  const load = useCallback(async (signal) => {
    setLoading(true);
    try {
      const [nextOrders, nextDiagnoses, nextBarns] = await Promise.all([
        listSanitarySlaughterOrders({ signal }), listDiagnoses(signal), getIsolatedBarns({ signal })
      ]);
      setOrders(nextOrders);
      setDiagnoses(nextDiagnoses);
      setIsolatedBarns(nextBarns);
      const ids = [...new Set(nextOrders.map((order) => order.galponId))];
      const details = await Promise.allSettled(ids.map((id) => getBarn(id, { signal })));
      setBarnNames(Object.fromEntries(details.flatMap((result, index) =>
        result.status === 'fulfilled' ? [[ids[index], result.value.nombre]] : [])));
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

  const eligible = useMemo(() => diagnoses.filter((diagnosis) =>
    diagnosis.estado === 'ACTIVO' && diagnosis.requiereSacrificioSanitario &&
    isolatedBarns.some((barn) => barn.id === diagnosis.galponId) &&
    !orders.some((order) => order.diagnosticoId === diagnosis.id)
  ), [diagnoses, isolatedBarns, orders]);
  const selectedDiagnosis = eligible.find((diagnosis) => diagnosis.id === diagnosisId);
  const filtered = orders.filter((order) => {
    const diagnosis = diagnoses.find((item) => item.id === order.diagnosticoId);
    const terms = [order.id, order.galponId, order.loteId, barnNames[order.galponId], diagnosis?.enfermedadNombre].join(' ').toLowerCase();
    return terms.includes(search.trim().toLowerCase()) && (status === 'TODOS' || status === order.estado);
  });

  async function issue(event) {
    event.preventDefault();
    if (!selectedDiagnosis) return;
    setBusy(true);
    try {
      await issueSanitarySlaughterOrder(selectedDiagnosis.galponId);
      setModal(null);
      setDiagnosisId('');
      await load();
      setMessage({ type: 'success', text: 'Orden sanitaria emitida y guardada.' });
    } catch (error) {
      setMessage({ type: 'error', text: error.message });
    } finally { setBusy(false); }
  }

  async function execute() {
    if (!modal?.order) return;
    setBusy(true);
    try {
      await executeSanitarySlaughterOrder(modal.order.id);
      setModal(null);
      await load();
      setMessage({ type: 'success', text: 'Sacrificio sanitario ejecutado. La población del lote quedó en cero.' });
    } catch (error) {
      setMessage({ type: 'error', text: error.message });
    } finally { setBusy(false); }
  }

  return <div className="avi-role-layout">
    <Sidebar role="veterinario" />
    <div className="avi-main-frame">
      <Topbar roleLabel="Veterinario" avatarSrc="/assets/vet-avatar.svg" />
      <ApiMessage message={message?.text} type={message?.type} onClose={() => setMessage(null)} />
      <div className="avi-dashboard-content">
        <div className="avi-page-heading">
          <p className="avi-page-subtitle">Órdenes de sacrificio sanitario registradas en el backend</p>
          <button type="button" className="avi-btn-orange-cta" onClick={() => setModal({ kind: 'issue' })}>Emitir orden sanitaria</button>
        </div>
        <div className="avi-metrics-row">
          <StatCard label="Órdenes emitidas" value={String(orders.length)} status="Registro del backend" statusColor="#2d743b" icon="/assets/vet-stethoscope.svg" tileBg="#e6f7e4" width="437px" />
          <StatCard label="Pendientes de ejecución" value={String(orders.filter((order) => order.estado === 'PENDIENTE').length)} status="Requieren confirmación veterinaria" statusColor="#9e4a0f" icon="/assets/vet-syringe.svg" tileBg="#f4eee3" width="437px" />
        </div>
        <div className="avi-receptions-filter-bar">
          <input className="avi-receptions-search-input" placeholder="Buscar orden, galpón, lote o enfermedad" value={search} onChange={(event) => setSearch(event.target.value)} />
          <select className="avi-receptions-select" value={status} onChange={(event) => setStatus(event.target.value)}>
            <option value="TODOS">Todos los estados</option><option value="PENDIENTE">Pendiente</option><option value="EJECUTADA">Ejecutada</option>
          </select>
          <button type="button" className="avi-receptions-filter-btn" onClick={() => load()}>Actualizar</button>
        </div>
        <div className="avi-recent-orders-panel">
          <div className="avi-panel-header avi-panel-header-full"><h2 className="avi-panel-title">Órdenes de sacrificio sanitario</h2></div>
          <div className="avi-orders-table">
            <div className="avi-orders-thead">
              <span style={{ flex: 1.4 }}>Orden</span><span style={{ flex: 1 }}>Galpón</span><span style={{ flex: 1.4 }}>Diagnóstico</span>
              <span style={{ flex: 0.8 }}>Estado</span><span style={{ flex: 0.9 }}>Aves sacrificadas</span><span style={{ flex: 0.9 }}>Acción</span>
            </div>
            {loading && <p>Cargando órdenes...</p>}
            {!loading && filtered.length === 0 && <p>No hay órdenes para mostrar.</p>}
            {filtered.map((order) => {
              const diagnosis = diagnoses.find((item) => item.id === order.diagnosticoId);
              return <div className="avi-orders-trow" key={order.id}>
                <span style={{ flex: 1.4, overflowWrap: 'anywhere' }}>{order.id}</span>
                <span style={{ flex: 1 }}>{barnNames[order.galponId] || order.galponId}</span>
                <span style={{ flex: 1.4 }}>{diagnosis?.enfermedadNombre || order.diagnosticoId}</span>
                <span style={{ flex: 0.8 }}>{order.estado}</span>
                <span style={{ flex: 0.9 }}>{order.poblacionSacrificada ?? '—'}</span>
                <span style={{ flex: 0.9 }}>{order.estado === 'PENDIENTE' && <button type="button" className="avi-admin-table-btn avi-admin-table-btn-orange" onClick={() => setModal({ kind: 'execute', order })}>Confirmar ejecución</button>}</span>
              </div>;
            })}
          </div>
        </div>
      </div>
    </div>
    <BaseModal isOpen={modal?.kind === 'issue'} onClose={() => setModal(null)} maxWidth="680px">
      <form className="avi-figma-modal-720" onSubmit={issue}>
        <div className="avi-figma-modal-header"><h2 className="avi-figma-modal-title">Emitir orden sanitaria</h2></div>
        <p>Selecciona un diagnóstico activo que requiera sacrificio sanitario en un galpón aislado.</p>
        <select className="avi-figma-input avi-figma-select" required value={diagnosisId} onChange={(event) => setDiagnosisId(event.target.value)}>
          <option value="">Seleccionar diagnóstico</option>
          {eligible.map((diagnosis) => <option key={diagnosis.id} value={diagnosis.id}>{isolatedBarns.find((barn) => barn.id === diagnosis.galponId)?.nombre || diagnosis.galponId} · {diagnosis.enfermedadNombre}</option>)}
        </select>
        {eligible.length === 0 && <p>No hay diagnósticos elegibles sin orden.</p>}
        <div className="avi-figma-modal-footer"><button type="button" className="avi-figma-btn-cancel" onClick={() => setModal(null)}>Cancelar</button><button type="submit" className="avi-figma-btn-green" disabled={busy || !selectedDiagnosis}>Emitir orden</button></div>
      </form>
    </BaseModal>
    <BaseModal isOpen={modal?.kind === 'execute'} onClose={() => setModal(null)} maxWidth="680px">
      {modal?.order && <div className="avi-figma-modal-720">
        <div className="avi-figma-modal-header"><h2 className="avi-figma-modal-title">Confirmar sacrificio sanitario</h2></div>
        <p>Se sacrificará la población vigente de {barnNames[modal.order.galponId] || 'este galpón'}. El lote quedará con población cero y el galpón pasará a vaciado sanitario.</p>
        <div className="avi-figma-modal-footer"><button type="button" className="avi-figma-btn-cancel" onClick={() => setModal(null)}>Cancelar</button><button type="button" className="avi-figma-btn-green" disabled={busy} onClick={execute}>Confirmar ejecución</button></div>
      </div>}
    </BaseModal>
  </div>;
}

export default VetSlaughterOrdersPage;
