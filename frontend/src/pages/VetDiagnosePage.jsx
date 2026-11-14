import { useEffect, useState } from 'react';
import Sidebar from '../components/Sidebar';
import Topbar from '../components/Topbar';
import StatCard from '../components/StatCard';
import BaseModal from '../components/BaseModal';
import ApiMessage from '../components/ApiMessage';
import { getIsolatedBarns } from '../services/isolationApi';
import { listDiseases } from '../services/diseasesApi';
import { listMedications } from '../services/medicationsApi';
import { createDiagnosis, getDiagnosis, listDiagnoses } from '../services/diagnosesApi';
import {
  createMedicineConsumption, getLotMedicineConsumptions, getMedicineConsumption
} from '../services/medicineConsumptionsApi';

const blank = { galponId: '', enfermedadId: '', medicacionId: '', observaciones: '' };
const todayInColombia = () => new Intl.DateTimeFormat('sv-SE', {
  timeZone: 'America/Bogota', year: 'numeric', month: '2-digit', day: '2-digit'
}).format(new Date());

function VetDiagnosePage() {
  const [diagnoses, setDiagnoses] = useState([]);
  const [barns, setBarns] = useState([]);
  const [diseases, setDiseases] = useState([]);
  const [medications, setMedications] = useState([]);
  const [form, setForm] = useState(blank);
  const [open, setOpen] = useState(false);
  const [detail, setDetail] = useState(null);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [message, setMessage] = useState('');
  const [messageType, setMessageType] = useState('info');
  const [saving, setSaving] = useState(false);
  const [consumptions, setConsumptions] = useState([]);
  const [consumptionDetail, setConsumptionDetail] = useState(null);
  const [consumptionQuantity, setConsumptionQuantity] = useState('');
  const [consumptionUnit, setConsumptionUnit] = useState('MILILITRO');
  const [consumptionDate, setConsumptionDate] = useState(todayInColombia);
  const [consumptionKey, setConsumptionKey] = useState(() => crypto.randomUUID());
  const [consumptionSaving, setConsumptionSaving] = useState(false);
  const [consumptionUncertain, setConsumptionUncertain] = useState(false);

  async function refresh(signal) {
    try {
      const [diagnosesResult, barnsResult, diseasesResult, medicationsResult] = await Promise.all([
        listDiagnoses(signal), getIsolatedBarns({ signal }),
        listDiseases(signal), listMedications(signal)
      ]);
      setDiagnoses(diagnosesResult);
      setBarns(barnsResult);
      setDiseases(diseasesResult);
      setMedications(medicationsResult);
    } catch (error) {
      if (error.name !== 'AbortError') {
        setMessageType('error');
        setMessage(error.message);
      }
    }
  }
  useEffect(() => {
    const controller = new AbortController();
    refresh(controller.signal);
    return () => controller.abort();
  }, []);

  const selectedDisease = diseases.find((item) => item.id === form.enfermedadId);
  const compatibleMedications = medications.filter((item) => item.enfermedadId === form.enfermedadId);
  const barnName = (id) => barns.find((item) => item.id === id)?.nombre || id;
  const filtered = diagnoses.filter((item) => {
    const text = search.trim().toLowerCase();
    return (!text || item.enfermedadNombre.toLowerCase().includes(text) ||
      (item.medicamentoNombre || '').toLowerCase().includes(text) ||
      barnName(item.galponId).toLowerCase().includes(text) ||
      item.id.toLowerCase().includes(text)) &&
      (!statusFilter || item.estado === statusFilter);
  });

  async function submit(event) {
    event.preventDefault();
    setSaving(true);
    try {
      const saved = await createDiagnosis(form.galponId, {
        enfermedadId: form.enfermedadId,
        medicacionId: selectedDisease?.requiereSacrificioSanitario ? null : form.medicacionId,
        observaciones: form.observaciones.trim()
      });
      setDiagnoses((previous) => [saved, ...previous]);
      setOpen(false);
      setMessageType('success');
      setMessage('Diagnóstico registrado para ' + barnName(saved.galponId));
    } catch (error) {
      setMessageType('error');
      setMessage(error.message);
    } finally {
      setSaving(false);
    }
  }

  async function showDetail(id) {
    try {
      const diagnosis = await getDiagnosis(id);
      const history = await getLotMedicineConsumptions(diagnosis.loteId);
      setDetail(diagnosis);
      setConsumptions(history);
      setConsumptionDetail(null);
      setConsumptionQuantity('');
      setConsumptionUnit('MILILITRO');
      setConsumptionDate(todayInColombia());
      setConsumptionKey(crypto.randomUUID());
      setConsumptionUncertain(false);
    } catch (error) {
      setMessageType('error');
      setMessage(error.message);
    }
  }

  async function saveConsumption(event) {
    event.preventDefault();
    if (!detail || !(Number(consumptionQuantity) > 0)) return;
    setConsumptionSaving(true);
    try {
      const saved = await createMedicineConsumption(detail.id, {
        cantidad: Number(consumptionQuantity),
        unidad: consumptionUnit,
        fechaAplicacion: consumptionDate
      }, consumptionKey);
      setConsumptions((previous) => [saved, ...previous.filter((item) => item.id !== saved.id)]);
      setConsumptionDetail(saved);
      setConsumptionQuantity('');
      setConsumptionKey(crypto.randomUUID());
      setConsumptionUncertain(false);
      setMessageType('success');
      setMessage('Consumo de medicamento registrado.');
    } catch (error) {
      setConsumptionUncertain(!error.status || error.status >= 500);
      setMessageType('error');
      setMessage(error.message);
    } finally {
      setConsumptionSaving(false);
    }
  }

  async function showConsumption(id) {
    try {
      setConsumptionDetail(await getMedicineConsumption(id));
    } catch (error) {
      setMessageType('error');
      setMessage(error.message);
    }
  }

  return <div className="avi-role-layout">
    <Sidebar role="veterinario" />
    <div className="avi-main-frame">
      <Topbar roleLabel="Veterinario" avatarSrc="/assets/vet-avatar.svg" />
      <ApiMessage message={message} type={messageType} onClose={() => setMessage('')} />
      <div className="avi-dashboard-content">
        <div className="avi-page-heading">
          <p className="avi-page-subtitle">
            Diagnósticos de galpones aislados y tratamientos registrados
          </p>
          <button type="button" className="avi-btn-green-cta"
            onClick={() => { setForm(blank); setOpen(true); }}>
            ＋ Nuevo diagnóstico
          </button>
        </div>
        <div className="avi-metrics-row">
          <StatCard label="Galpones en aislamiento" value={String(barns.length)}
            status="Disponibles para evaluación" statusColor="#9e4a0f"
            icon="/assets/vet-bell.svg" tileBg="#f4eee3" width="437px" />
          <StatCard label="Diagnósticos registrados" value={String(diagnoses.length)}
            status="Historial del backend" statusColor="#2d743b"
            icon="/assets/vet-stethoscope.svg" tileBg="#e6f7e4" width="437px" />
        </div>
        <div className="avi-receptions-filter-bar">
          <input type="search" className="avi-receptions-search-input"
            placeholder="Buscar galpón, enfermedad o medicamento"
            value={search} onChange={(event) => setSearch(event.target.value)} />
          <select className="avi-receptions-select" value={statusFilter}
            onChange={(event) => setStatusFilter(event.target.value)}>
            <option value="">Todos los estados</option>
            <option value="ACTIVO">Activo</option>
            <option value="REINTEGRADO">Reintegrado</option>
            <option value="REINTEGRO_OMITIDO">Reintegro omitido</option>
          </select>
          <button type="button" className="avi-receptions-filter-btn"
            onClick={() => { setSearch(''); setStatusFilter(''); }}>Limpiar</button>
        </div>
        <div className="avi-recent-orders-panel">
          <div className="avi-panel-header avi-panel-header-full">
            <h2 className="avi-panel-title">Historial de diagnósticos</h2>
            <button type="button" className="avi-admin-table-btn avi-admin-table-btn-green"
              onClick={() => refresh()}>Actualizar</button>
          </div>
          <div className="avi-orders-table">
            <div className="avi-orders-thead">
              <span style={{ flex: 1.4 }}>Galpón</span>
              <span style={{ flex: 1.8 }}>Enfermedad</span>
              <span style={{ flex: 1.5 }}>Medicamento</span>
              <span style={{ flex: 0.9 }}>Fecha</span>
              <span style={{ flex: 0.8 }}>Estado</span>
              <span style={{ flex: 0.6 }}>Acción</span>
            </div>
            {filtered.map((item) => <div key={item.id} className="avi-orders-trow">
              <span className="avi-col-dark" style={{ flex: 1.4, minWidth: 0, overflowWrap: 'anywhere' }}>
                {barnName(item.galponId)}
              </span>
              <span style={{ flex: 1.8, minWidth: 0, overflowWrap: 'anywhere' }}>
                {item.enfermedadNombre}
              </span>
              <span style={{ flex: 1.5, minWidth: 0, overflowWrap: 'anywhere' }}>
                {item.medicamentoNombre || 'Sin medicación'}
              </span>
              <span style={{ flex: 0.9 }}>{item.fechaDiagnostico}</span>
              <span style={{ flex: 0.8 }}>{item.estado}</span>
              <span style={{ flex: 0.6 }}>
                <button type="button" className="avi-admin-table-btn avi-admin-table-btn-green"
                  onClick={() => showDetail(item.id)}>Detalle</button>
              </span>
            </div>)}
            {filtered.length === 0 && <p>No hay diagnósticos para mostrar.</p>}
          </div>
        </div>
      </div>
    </div>
    <BaseModal isOpen={open} onClose={() => setOpen(false)} maxWidth="720px">
      <form className="avi-figma-modal-720" onSubmit={submit}>
        <div className="avi-figma-modal-header">
          <h2 className="avi-figma-modal-title">Nuevo diagnóstico</h2>
          <button type="button" className="avi-figma-modal-close"
            onClick={() => setOpen(false)}>×</button>
        </div>
        <div className="avi-figma-modal-grid">
          <label className="avi-figma-field">
            <span className="avi-figma-label">Galpón aislado</span>
            <select className="avi-figma-input" value={form.galponId} required
              onChange={(event) => setForm({ ...form, galponId: event.target.value })}>
              <option value="">Seleccionar galpón</option>
              {barns.map((item) => <option key={item.id} value={item.id}>
                {item.nombre} · Lote {item.loteActual?.id || 'sin lote'}
              </option>)}
            </select>
          </label>
          <label className="avi-figma-field">
            <span className="avi-figma-label">Enfermedad</span>
            <select className="avi-figma-input" value={form.enfermedadId} required
              onChange={(event) => setForm({
                ...form, enfermedadId: event.target.value, medicacionId: ''
              })}>
              <option value="">Seleccionar enfermedad</option>
              {diseases.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}
            </select>
          </label>
          <label className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
            <span className="avi-figma-label">Medicación</span>
            {selectedDisease?.requiereSacrificioSanitario
              ? <div className="avi-figma-readonly-box">
                La enfermedad requiere sacrificio sanitario; no admite medicación.
              </div>
              : <select className="avi-figma-input" value={form.medicacionId} required
                disabled={!form.enfermedadId}
                onChange={(event) => setForm({ ...form, medicacionId: event.target.value })}>
                <option value="">Seleccionar medicación compatible</option>
                {compatibleMedications.map((item) => <option key={item.id} value={item.id}>
                  {item.nombreMedicamento} · {item.dosis}
                </option>)}
              </select>}
          </label>
          <label className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
            <span className="avi-figma-label">Observaciones</span>
            <textarea className="avi-figma-input" value={form.observaciones} required
              onChange={(event) => setForm({ ...form, observaciones: event.target.value })} />
          </label>
        </div>
        <div className="avi-figma-modal-footer">
          <button type="button" className="avi-figma-btn-cancel"
            onClick={() => setOpen(false)}>Cancelar</button>
          <button type="submit" className="avi-figma-btn-green" disabled={saving}>
            {saving ? 'Guardando…' : 'Registrar diagnóstico'}
          </button>
        </div>
      </form>
    </BaseModal>
    <BaseModal isOpen={Boolean(detail)} onClose={() => setDetail(null)} maxWidth="720px">
      {detail && <div className="avi-figma-modal-720">
        <div className="avi-figma-modal-header">
          <h2 className="avi-figma-modal-title">Diagnóstico de {barnName(detail.galponId)}</h2>
          <button type="button" className="avi-figma-modal-close"
            onClick={() => setDetail(null)}>×</button>
        </div>
        <div className="avi-figma-modal-grid">
          <p><strong>ID:</strong> {detail.id}</p>
          <p><strong>Estado:</strong> {detail.estado}</p>
          <p><strong>Enfermedad:</strong> {detail.enfermedadNombre}</p>
          <p><strong>Fecha:</strong> {detail.fechaDiagnostico}</p>
          <p><strong>Medicamento:</strong> {detail.medicamentoNombre || 'Sin medicación'}</p>
          {detail.dosis && <p><strong>Dosis:</strong> {detail.dosis}</p>}
          <p><strong>Duración:</strong> {detail.duracionDias == null
            ? 'No aplica' : `${detail.duracionDias} días`}</p>
          {detail.principioActivo && <p><strong>Principio activo:</strong> {detail.principioActivo}</p>}
          {detail.diasAplicacion && <p><strong>Días de aplicación:</strong> {detail.diasAplicacion.join(', ')}</p>}
          <p><strong>Reintegro previsto:</strong> {detail.fechaReintegro || 'No aplica'}</p>
          <p><strong>Sacrificio sanitario:</strong> {detail.requiereSacrificioSanitario ? 'Sí' : 'No'}</p>
          <p style={{ gridColumn: '1 / -1' }}><strong>Observaciones:</strong> {detail.observaciones}</p>
        </div>
        {detail.estado === 'ACTIVO' && detail.medicamentoId &&
          <form onSubmit={saveConsumption} className="avi-figma-modal-grid">
            <h3 style={{ gridColumn: '1 / -1' }}>Registrar consumo de medicamento</h3>
            <label className="avi-figma-field">
              <span className="avi-figma-label">Cantidad aplicada</span>
              <input type="number" min="0.0001" step="0.0001" required
                className="avi-figma-input" value={consumptionQuantity}
                disabled={consumptionSaving || consumptionUncertain}
                onChange={(event) => setConsumptionQuantity(event.target.value)} />
            </label>
            <label className="avi-figma-field">
              <span className="avi-figma-label">Unidad</span>
              <select className="avi-figma-input" value={consumptionUnit}
                disabled={consumptionSaving || consumptionUncertain}
                onChange={(event) => setConsumptionUnit(event.target.value)}>
                {['MILILITRO', 'GRAMO', 'MILIGRAMO', 'UNIDAD'].map((unit) =>
                  <option key={unit} value={unit}>{unit}</option>)}
              </select>
            </label>
            <label className="avi-figma-field">
              <span className="avi-figma-label">Fecha de aplicación</span>
              <input type="date" className="avi-figma-input" value={consumptionDate}
                min={detail.fechaDiagnostico} max={todayInColombia()} required
                disabled={consumptionSaving || consumptionUncertain}
                onChange={(event) => setConsumptionDate(event.target.value)} />
            </label>
            <div className="avi-figma-field" style={{ alignSelf: 'end' }}>
              <button type="submit" className="avi-figma-btn-green"
                disabled={consumptionSaving || !consumptionQuantity}>
                {consumptionSaving ? 'Guardando…'
                  : consumptionUncertain ? 'Reintentar mismo envío' : 'Registrar consumo'}
              </button>
            </div>
            {consumptionUncertain && <p style={{ gridColumn: '1 / -1' }}>
              No se confirmó la respuesta. Reintenta sin cambiar los datos: se conserva la misma clave.
            </p>}
          </form>}
        <div className="avi-figma-modal-grid">
          <h3 style={{ gridColumn: '1 / -1' }}>Consumos del lote</h3>
          {consumptions.length === 0 && <p>No hay consumos registrados para este lote.</p>}
          {consumptions.map((item) => <div key={item.id}
            style={{ gridColumn: '1 / -1', display: 'flex', justifyContent: 'space-between', gap: 12 }}>
            <span>{item.fechaAplicacion} · {item.cantidadIngresada} {item.unidadIngresada}</span>
            <span>{item.actor}</span>
            <button type="button" className="avi-admin-table-btn avi-admin-table-btn-green"
              onClick={() => showConsumption(item.id)}>Ver costo</button>
          </div>)}
          {consumptionDetail && <div style={{ gridColumn: '1 / -1' }}>
            <h4>Detalle del consumo · {consumptionDetail.id}</h4>
            {consumptionDetail.detallesInventario.map((item) => <p key={item.recepcionId}>
              Recepción {item.recepcionId}: {item.cantidadUnidadBase} {consumptionDetail.unidadBase}
              {' · '}{item.precioNetoHistoricoUnidadBase} {item.moneda} por unidad
            </p>)}
          </div>}
        </div>
      </div>}
    </BaseModal>
  </div>;
}

export default VetDiagnosePage;
