import { useEffect, useState } from 'react';
import Sidebar from '../components/Sidebar';
import Topbar from '../components/Topbar';
import StatCard from '../components/StatCard';
import BaseModal from '../components/BaseModal';
import ApiMessage from '../components/ApiMessage';
import { createDisease, listDiseases, updateDisease } from '../services/diseasesApi';

const blank = {
  nombre: '', nivelRiesgo: 'MODERADO', tipo: 'OTRA',
  descripcionClinica: '', requiereSacrificioSanitario: false
};
const types = ['VIRAL', 'BACTERIANA', 'PARASITARIA', 'FUNGICA', 'OTRA'];
const risks = ['BAJO', 'MODERADO', 'ALTO', 'CRITICO'];

function VetDiseasesPage() {
  const [items, setItems] = useState([]);
  const [form, setForm] = useState(blank);
  const [editingId, setEditingId] = useState(null);
  const [detail, setDetail] = useState(null);
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [riskFilter, setRiskFilter] = useState('');
  const [message, setMessage] = useState('');
  const [messageType, setMessageType] = useState('info');
  const [saving, setSaving] = useState(false);

  async function refresh(signal) {
    try {
      setItems(await listDiseases(signal));
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
  const filtered = items.filter((item) => {
    const text = search.trim().toLowerCase();
    return (!text || item.nombre.toLowerCase().includes(text) ||
      item.descripcionClinica.toLowerCase().includes(text) ||
      item.id.toLowerCase().includes(text)) &&
      (!typeFilter || item.tipo === typeFilter) &&
      (!riskFilter || item.nivelRiesgo === riskFilter);
  });
  function edit(item) {
    setDetail(null);
    setEditingId(item.id);
    setForm({
      nombre: item.nombre, nivelRiesgo: item.nivelRiesgo, tipo: item.tipo,
      descripcionClinica: item.descripcionClinica,
      requiereSacrificioSanitario: item.requiereSacrificioSanitario
    });
    setOpen(true);
  }
  async function submit(event) {
    event.preventDefault();
    setSaving(true);
    try {
      const saved = editingId
        ? await updateDisease(editingId, form)
        : await createDisease(form);
      setItems((previous) => [...previous.filter((item) => item.id !== saved.id), saved]
        .sort((a, b) => a.nombre.localeCompare(b.nombre)));
      setOpen(false);
      setMessageType('success');
      setMessage('Enfermedad guardada: ' + saved.nombre);
    } catch (error) {
      setMessageType('error');
      setMessage(error.message);
    } finally {
      setSaving(false);
    }
  }
  return <div className="avi-role-layout">
    <Sidebar role="veterinario" />
    <div className="avi-main-frame">
      <Topbar roleLabel="Veterinario" avatarSrc="/assets/vet-avatar.svg" />
      <ApiMessage message={message} type={messageType} onClose={() => setMessage('')} />
      <div className="avi-dashboard-content">
        <div className="avi-page-heading">
          <p className="avi-page-subtitle">Catálogo de enfermedades para diagnósticos clínicos</p>
          <button type="button" className="avi-btn-green-cta" onClick={() => {
            setEditingId(null); setForm(blank); setOpen(true);
          }}>＋ Registrar enfermedad</button>
        </div>
        <div className="avi-metrics-row">
          <StatCard label="Enfermedades registradas" value={String(items.length)}
            status="Registros del backend" statusColor="#2d743b"
            icon="/assets/vet-stethoscope.svg" tileBg="#e6f7e4" width="437px" />
          <StatCard label="Requieren sacrificio sanitario"
            value={String(items.filter((item) => item.requiereSacrificioSanitario).length)}
            status="Según el catálogo" statusColor="#9e4a0f"
            icon="/assets/vet-bell.svg" tileBg="#f4eee3" width="437px" />
        </div>
        <div className="avi-receptions-filter-bar">
          <input className="avi-receptions-search-input" type="search"
            placeholder="Buscar enfermedad o descripción" value={search}
            onChange={(event) => setSearch(event.target.value)} />
          <select className="avi-receptions-select" value={typeFilter}
            onChange={(event) => setTypeFilter(event.target.value)}>
            <option value="">Todos los tipos</option>
            {types.map((value) => <option key={value}>{value}</option>)}
          </select>
          <select className="avi-receptions-select" value={riskFilter}
            onChange={(event) => setRiskFilter(event.target.value)}>
            <option value="">Todos los riesgos</option>
            {risks.map((value) => <option key={value}>{value}</option>)}
          </select>
          <button type="button" className="avi-receptions-filter-btn"
            onClick={() => { setSearch(''); setTypeFilter(''); setRiskFilter(''); }}>Limpiar</button>
        </div>
        <div className="avi-recent-orders-panel">
          <div className="avi-panel-header avi-panel-header-full">
            <h2 className="avi-panel-title">Enfermedades registradas</h2>
            <button type="button" className="avi-admin-table-btn avi-admin-table-btn-green"
              onClick={() => refresh()}>Actualizar</button>
          </div>
          <div className="avi-orders-table">
            <div className="avi-orders-thead">
              <span style={{ flex: 2 }}>Nombre</span><span style={{ flex: 1 }}>Tipo</span>
              <span style={{ flex: 1 }}>Riesgo</span>
              <span style={{ flex: 1 }}>Sacrificio sanitario</span>
              <span style={{ flex: 1 }}>Acciones</span>
            </div>
            {filtered.map((item) => <div key={item.id} className="avi-orders-trow">
              <span className="avi-col-dark" style={{ flex: 2 }}>{item.nombre}</span>
              <span style={{ flex: 1 }}>{item.tipo}</span>
              <span style={{ flex: 1 }}>{item.nivelRiesgo}</span>
              <span style={{ flex: 1 }}>{item.requiereSacrificioSanitario ? 'Sí' : 'No'}</span>
              <span style={{ flex: 1, display: 'flex', gap: 6 }}>
                <button type="button" className="avi-admin-table-btn avi-admin-table-btn-green"
                  onClick={() => setDetail(item)}>Ver ficha</button>
                <button type="button" className="avi-admin-table-btn avi-admin-table-btn-orange"
                  onClick={() => edit(item)}>Editar</button>
              </span>
            </div>)}
            {filtered.length === 0 && <p>No hay enfermedades para mostrar.</p>}
          </div>
        </div>
      </div>
    </div>
    <BaseModal isOpen={open} onClose={() => setOpen(false)} maxWidth="720px">
      <form className="avi-figma-modal-720" onSubmit={submit}>
        <div className="avi-figma-modal-header">
          <h2 className="avi-figma-modal-title">
            {editingId ? 'Editar enfermedad' : 'Registrar enfermedad'}
          </h2>
          <button type="button" className="avi-figma-modal-close"
            onClick={() => setOpen(false)}>×</button>
        </div>
        <div className="avi-figma-modal-grid">
          <label className="avi-figma-field"><span className="avi-figma-label">Nombre</span>
            <input className="avi-figma-input" value={form.nombre} required
              onChange={(event) => setForm({ ...form, nombre: event.target.value })} />
          </label>
          <label className="avi-figma-field"><span className="avi-figma-label">Tipo</span>
            <select className="avi-figma-input" value={form.tipo}
              onChange={(event) => setForm({ ...form, tipo: event.target.value })}>
              {types.map((value) => <option key={value}>{value}</option>)}
            </select>
          </label>
          <label className="avi-figma-field"><span className="avi-figma-label">Nivel de riesgo</span>
            <select className="avi-figma-input" value={form.nivelRiesgo}
              onChange={(event) => setForm({ ...form, nivelRiesgo: event.target.value })}>
              {risks.map((value) => <option key={value}>{value}</option>)}
            </select>
          </label>
          <label className="avi-figma-field">
            <span className="avi-figma-label">Requiere sacrificio sanitario</span>
            <select className="avi-figma-input" value={String(form.requiereSacrificioSanitario)}
              onChange={(event) => setForm({
                ...form, requiereSacrificioSanitario: event.target.value === 'true'
              })}>
              <option value="false">No</option><option value="true">Sí</option>
            </select>
          </label>
          <label className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
            <span className="avi-figma-label">Descripción clínica</span>
            <textarea className="avi-figma-input" value={form.descripcionClinica} required
              onChange={(event) => setForm({ ...form, descripcionClinica: event.target.value })} />
          </label>
        </div>
        <div className="avi-figma-modal-footer">
          <button type="button" className="avi-figma-btn-cancel"
            onClick={() => setOpen(false)}>Cancelar</button>
          <button type="submit" className="avi-figma-btn-green" disabled={saving}>
            {saving ? 'Guardando…' : 'Guardar enfermedad'}
          </button>
        </div>
      </form>
    </BaseModal>
    <BaseModal isOpen={Boolean(detail)} onClose={() => setDetail(null)} maxWidth="680px">
      {detail && <div className="avi-figma-modal-720">
        <div className="avi-figma-modal-header">
          <h2 className="avi-figma-modal-title">{detail.nombre}</h2>
          <button type="button" className="avi-figma-modal-close"
            onClick={() => setDetail(null)}>×</button>
        </div>
        <div className="avi-figma-modal-grid">
          <p><strong>ID:</strong> {detail.id}</p>
          <p><strong>Tipo:</strong> {detail.tipo}</p>
          <p><strong>Riesgo:</strong> {detail.nivelRiesgo}</p>
          <p><strong>Sacrificio sanitario:</strong> {detail.requiereSacrificioSanitario ? 'Sí' : 'No'}</p>
          <p style={{ gridColumn: '1 / -1' }}><strong>Descripción clínica:</strong> {detail.descripcionClinica}</p>
        </div>
        <div className="avi-figma-modal-footer">
          <button type="button" className="avi-figma-btn-orange"
            onClick={() => edit(detail)}>Editar</button>
        </div>
      </div>}
    </BaseModal>
  </div>;
}

export default VetDiseasesPage;
