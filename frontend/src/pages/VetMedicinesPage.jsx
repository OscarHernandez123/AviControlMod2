import { useEffect, useState } from 'react';
import Sidebar from '../components/Sidebar';
import Topbar from '../components/Topbar';
import StatCard from '../components/StatCard';
import BaseModal from '../components/BaseModal';
import ApiMessage from '../components/ApiMessage';
import { listDiseases } from '../services/diseasesApi';
import { getInventory } from '../services/inventoryApi';
import { createMedication, listMedications, updateMedication } from '../services/medicationsApi';

const blank = {
  enfermedadId: '', medicamentoId: '', dosis: '', duracionDias: 1,
  descripcion: '', nombreMedicamento: '', principioActivo: '', diasAplicacion: ''
};

function VetMedicinesPage() {
  const [items, setItems] = useState([]);
  const [diseases, setDiseases] = useState([]);
  const [products, setProducts] = useState([]);
  const [form, setForm] = useState(blank);
  const [editingId, setEditingId] = useState(null);
  const [detail, setDetail] = useState(null);
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState('');
  const [message, setMessage] = useState('');
  const [messageType, setMessageType] = useState('info');
  const [saving, setSaving] = useState(false);

  async function refresh(signal) {
    try {
      const [medications, diseasesResult, inventory] = await Promise.all([
        listMedications(signal), listDiseases(signal), getInventory({ signal })
      ]);
      setItems(medications);
      setDiseases(diseasesResult);
      setProducts(inventory.medicamentos || []);
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

  function edit(item) {
    setDetail(null);
    setEditingId(item.id);
    setForm({
      enfermedadId: item.enfermedadId,
      medicamentoId: item.medicamentoId,
      dosis: item.dosis,
      duracionDias: item.duracionDias,
      descripcion: item.descripcion,
      nombreMedicamento: item.nombreMedicamento,
      principioActivo: item.principioActivo,
      diasAplicacion: item.diasAplicacion?.join(', ') || ''
    });
    setOpen(true);
  }

  function parseDays(value, duration) {
    if (!value.trim()) return null;
    const days = value.split(',').map((part) => Number(part.trim()));
    if (days.some((day) => !Number.isInteger(day) || day < 1 || day > duration) ||
      new Set(days).size !== days.length) {
      throw new Error('Los días de aplicación deben ser números únicos entre 1 y la duración.');
    }
    return days;
  }

  async function submit(event) {
    event.preventDefault();
    setSaving(true);
    try {
      const duration = Number(form.duracionDias);
      const body = {
        enfermedadId: form.enfermedadId,
        medicamentoId: form.medicamentoId,
        dosis: form.dosis.trim(),
        duracionDias: duration,
        descripcion: form.descripcion.trim(),
        nombreMedicamento: form.nombreMedicamento.trim(),
        principioActivo: form.principioActivo.trim(),
        diasAplicacion: parseDays(form.diasAplicacion, duration)
      };
      const saved = editingId
        ? await updateMedication(editingId, body)
        : await createMedication(body);
      setItems((previous) => [saved, ...previous.filter((item) => item.id !== saved.id)]);
      setOpen(false);
      setMessageType('success');
      setMessage('Tratamiento guardado para ' + saved.nombreMedicamento);
    } catch (error) {
      setMessageType('error');
      setMessage(error.message);
    } finally {
      setSaving(false);
    }
  }

  const filtered = items.filter((item) => {
    const disease = diseases.find((row) => row.id === item.enfermedadId);
    const text = search.trim().toLowerCase();
    return !text || (item.nombreMedicamento || '').toLowerCase().includes(text) ||
      (item.principioActivo || '').toLowerCase().includes(text) ||
      (disease?.nombre || '').toLowerCase().includes(text);
  });
  const diseaseName = (id) => diseases.find((item) => item.id === id)?.nombre || id;

  return <div className="avi-role-layout">
    <Sidebar role="veterinario" />
    <div className="avi-main-frame">
      <Topbar roleLabel="Veterinario" avatarSrc="/assets/vet-avatar.svg" />
      <ApiMessage message={message} type={messageType} onClose={() => setMessage('')} />
      <div className="avi-dashboard-content">
        <div className="avi-page-heading">
          <p className="avi-page-subtitle">Tratamientos registrados para diagnósticos veterinarios</p>
          <button type="button" className="avi-btn-green-cta" onClick={() => {
            setEditingId(null); setForm(blank); setOpen(true);
          }}>＋ Registrar medicación</button>
        </div>
        <div className="avi-metrics-row">
          <StatCard label="Tratamientos registrados" value={String(items.length)}
            status="Registros del backend" statusColor="#2d743b"
            icon="/assets/vet-syringe.svg" tileBg="#e6f7e4" width="437px" />
          <StatCard label="Medicamentos disponibles" value={String(products.filter((item) => item.stockActual > 0).length)}
            status="Según inventario" statusColor="#2d743b"
            icon="/assets/vet-stethoscope.svg" tileBg="#e6f7e4" width="437px" />
        </div>
        <div className="avi-receptions-filter-bar">
          <input type="search" className="avi-receptions-search-input"
            placeholder="Buscar por medicamento, principio activo o enfermedad"
            value={search} onChange={(event) => setSearch(event.target.value)} />
          <button type="button" className="avi-receptions-filter-btn" onClick={() => setSearch('')}>Limpiar</button>
        </div>
        <div className="avi-recent-orders-panel">
          <div className="avi-panel-header avi-panel-header-full">
            <h2 className="avi-panel-title">Medicaciones registradas</h2>
            <button type="button" className="avi-admin-table-btn avi-admin-table-btn-green"
              onClick={() => refresh()}>Actualizar</button>
          </div>
          <div className="avi-orders-table">
            <div className="avi-orders-thead">
              <span style={{ flex: 1.5 }}>Medicamento</span>
              <span style={{ flex: 1.5 }}>Enfermedad</span>
              <span style={{ flex: 1.5 }}>Dosis</span>
              <span style={{ flex: 0.7 }}>Duración</span>
              <span style={{ flex: 1 }}>Acciones</span>
            </div>
            {filtered.map((item) => <div key={item.id} className="avi-orders-trow">
              <span className="avi-col-dark" style={{ flex: 1.5, minWidth: 0, overflowWrap: 'anywhere' }}>
                {item.nombreMedicamento}
              </span>
              <span style={{ flex: 1.5, minWidth: 0, overflowWrap: 'anywhere' }}>
                {diseaseName(item.enfermedadId)}
              </span>
              <span style={{ flex: 1.5, minWidth: 0, overflowWrap: 'anywhere' }}>{item.dosis}</span>
              <span style={{ flex: 0.7 }}>{item.duracionDias} días</span>
              <span style={{ flex: 1, display: 'flex', gap: 6 }}>
                <button type="button" className="avi-admin-table-btn avi-admin-table-btn-green"
                  onClick={() => setDetail(item)}>Ver ficha</button>
                <button type="button" className="avi-admin-table-btn avi-admin-table-btn-orange"
                  onClick={() => edit(item)}>Editar</button>
              </span>
            </div>)}
            {filtered.length === 0 && <p>No hay medicaciones para mostrar.</p>}
          </div>
        </div>
      </div>
    </div>
    <BaseModal isOpen={open} onClose={() => setOpen(false)} maxWidth="720px">
      <form className="avi-figma-modal-720" onSubmit={submit}>
        <div className="avi-figma-modal-header">
          <h2 className="avi-figma-modal-title">
            {editingId ? 'Editar medicación' : 'Registrar medicación'}
          </h2>
          <button type="button" className="avi-figma-modal-close"
            onClick={() => setOpen(false)}>×</button>
        </div>
        <div className="avi-figma-modal-grid">
          <label className="avi-figma-field">
            <span className="avi-figma-label">Enfermedad</span>
            <select className="avi-figma-input" value={form.enfermedadId} required
              onChange={(event) => setForm({ ...form, enfermedadId: event.target.value })}>
              <option value="">Seleccionar enfermedad</option>
              {diseases.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}
            </select>
          </label>
          <label className="avi-figma-field">
            <span className="avi-figma-label">Medicamento de inventario</span>
            <select className="avi-figma-input" value={form.medicamentoId} required
              onChange={(event) => {
                const product = products.find((item) => item.medicamentoId === event.target.value);
                setForm({
                  ...form, medicamentoId: event.target.value,
                  nombreMedicamento: product?.medicamento || ''
                });
              }}>
              <option value="">Seleccionar medicamento</option>
              {products.filter((item) => item.stockActual > 0 || item.medicamentoId === form.medicamentoId)
                .map((item) => <option key={item.medicamentoId} value={item.medicamentoId}>
                  {item.medicamento} · {item.stockActual} {item.unidad}
                </option>)}
            </select>
          </label>
          <label className="avi-figma-field">
            <span className="avi-figma-label">Nombre del medicamento</span>
            <input className="avi-figma-input" value={form.nombreMedicamento} readOnly />
          </label>
          <label className="avi-figma-field">
            <span className="avi-figma-label">Principio activo</span>
            <input className="avi-figma-input" value={form.principioActivo} required
              onChange={(event) => setForm({ ...form, principioActivo: event.target.value })} />
          </label>
          <label className="avi-figma-field">
            <span className="avi-figma-label">Dosis</span>
            <input className="avi-figma-input" value={form.dosis} required
              onChange={(event) => setForm({ ...form, dosis: event.target.value })} />
          </label>
          <label className="avi-figma-field">
            <span className="avi-figma-label">Duración (días)</span>
            <input type="number" min="1" className="avi-figma-input" value={form.duracionDias} required
              onChange={(event) => setForm({ ...form, duracionDias: event.target.value })} />
          </label>
          <label className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
            <span className="avi-figma-label">Días de aplicación (opcional, separados por comas)</span>
            <input className="avi-figma-input" placeholder="Ej. 1, 2, 3"
              value={form.diasAplicacion}
              onChange={(event) => setForm({ ...form, diasAplicacion: event.target.value })} />
          </label>
          <label className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
            <span className="avi-figma-label">Descripción del tratamiento</span>
            <textarea className="avi-figma-input" value={form.descripcion} required
              onChange={(event) => setForm({ ...form, descripcion: event.target.value })} />
          </label>
        </div>
        <div className="avi-figma-modal-footer">
          <button type="button" className="avi-figma-btn-cancel"
            onClick={() => setOpen(false)}>Cancelar</button>
          <button type="submit" className="avi-figma-btn-green" disabled={saving}>
            {saving ? 'Guardando…' : 'Guardar medicación'}
          </button>
        </div>
      </form>
    </BaseModal>
    <BaseModal isOpen={Boolean(detail)} onClose={() => setDetail(null)} maxWidth="680px">
      {detail && <div className="avi-figma-modal-720">
        <div className="avi-figma-modal-header">
          <h2 className="avi-figma-modal-title">{detail.nombreMedicamento}</h2>
          <button type="button" className="avi-figma-modal-close"
            onClick={() => setDetail(null)}>×</button>
        </div>
        <div className="avi-figma-modal-grid">
          <p><strong>Enfermedad:</strong> {diseaseName(detail.enfermedadId)}</p>
          <p><strong>Principio activo:</strong> {detail.principioActivo}</p>
          <p><strong>Dosis:</strong> {detail.dosis}</p>
          <p><strong>Duración:</strong> {detail.duracionDias} días</p>
          <p><strong>Días de aplicación:</strong> {detail.diasAplicacion?.join(', ') || 'Todos'}</p>
          <p style={{ gridColumn: '1 / -1' }}><strong>Descripción:</strong> {detail.descripcion}</p>
        </div>
        <div className="avi-figma-modal-footer">
          <button type="button" className="avi-figma-btn-orange"
            onClick={() => edit(detail)}>Editar</button>
        </div>
      </div>}
    </BaseModal>
  </div>;
}

export default VetMedicinesPage;
