import { useEffect, useState } from 'react';
import Sidebar from '../components/Sidebar';
import Topbar from '../components/Topbar';
import BaseModal from '../components/BaseModal';
import ApiMessage from '../components/ApiMessage';
import {
  createMedicineReception,
  getMedicineReception,
  getMedicineReceptionEditForm,
  listMedicineReceptions,
  listMedicinesForReception,
  updateMedicineReception
} from '../services/medicineReceptionsApi';

const emptyForm = {
  medicamentoId: '', codigoLote: '', presentacion: 'Frasco', fechaIngreso: '',
  fechaVencimiento: '', cantidadPresentaciones: '', contenidoPorPresentacion: '',
  unidadContenido: 'MILILITRO', precioNetoPorPresentacion: '', impuestoPorcentaje: ''
};

const currency = (value) => new Intl.NumberFormat('es-CO', {
  style: 'currency', currency: 'COP', maximumFractionDigits: 2
}).format(Number(value || 0));
const number = (value) => new Intl.NumberFormat('es-CO', {
  maximumFractionDigits: 2
}).format(Number(value || 0));
const unit = { MILILITRO: 'ml', MILIGRAMO: 'mg', GRAMO: 'g', UNIDAD: 'unidad' };
const unitLabel = { MILILITRO: 'Mililitros (ml)', MILIGRAMO: 'Miligramos (mg)', GRAMO: 'Gramos (g)', UNIDAD: 'Unidades' };
function compatibleUnits(medicineId, medicines, currentUnit) {
  const base = medicines.find((item) => item.id === medicineId)?.unidadBase;
  if (base === 'GRAMO') return ['GRAMO', 'MILIGRAMO'];
  return base ? [base] : [currentUnit].filter(Boolean);
}

function dateRange(period) {
  const days = Number(period.slice(0, -1));
  const today = new Date();
  const start = new Date(today);
  start.setDate(today.getDate() - days + 1);
  const format = (date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  return { from: format(start), to: format(today) };
}

function AdminMedReceptionsPage() {
  const [searchTerm, setSearchTerm] = useState('');
  const [presentationFilter, setPresentationFilter] = useState('Todas');
  const [dateFilter, setDateFilter] = useState('30d');
  const [filters, setFilters] = useState({ page: 0, ...dateRange('30d') });
  const [history, setHistory] = useState({ contenido: [], total: 0, recepcionesMes: 0 });
  const [medicines, setMedicines] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [reloadKey, setReloadKey] = useState(0);
  const [isRegisterOpen, setIsRegisterOpen] = useState(false);
  const [editingMed, setEditingMed] = useState(null);
  const [detailMed, setDetailMed] = useState(null);
  const [toastMsg, setToastMsg] = useState('');
  const [regForm, setRegForm] = useState(emptyForm);
  const [editForm, setEditForm] = useState(emptyForm);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError('');
    listMedicineReceptions(filters, { signal: controller.signal })
      .then(setHistory)
      .catch((cause) => { if (cause.name !== 'AbortError') setError(cause.message); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [filters, reloadKey]);

  useEffect(() => {
    const controller = new AbortController();
    listMedicinesForReception({ signal: controller.signal })
      .then(setMedicines)
      .catch((cause) => { if (cause.name !== 'AbortError') setError(cause.message); });
    return () => controller.abort();
  }, []);

  const refresh = () => setReloadKey((value) => value + 1);

  const handleOpenEdit = async (row) => {
    setError('');
    try {
      setEditForm(await getMedicineReceptionEditForm(row.id));
      setEditingMed(row);
    } catch (cause) { setError(cause.message); }
  };

  const handleOpenDetail = async (row) => {
    setError('');
    try { setDetailMed(await getMedicineReception(row.id)); }
    catch (cause) { setError(cause.message); }
  };

  const handleRegisterSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    try {
      const created = await createMedicineReception({
        ...regForm,
        cantidadPresentaciones: Number(regForm.cantidadPresentaciones),
        contenidoPorPresentacion: Number(regForm.contenidoPorPresentacion),
        precioNetoPorPresentacion: Number(regForm.precioNetoPorPresentacion),
        impuestoPorcentaje: Number(regForm.impuestoPorcentaje)
      });
      setIsRegisterOpen(false);
      setRegForm(emptyForm);
      setToastMsg(`Recepción ${created.codigoLote} registrada correctamente.`);
      setFilters({ page: 0, ...dateRange('30d') });
      refresh();
    } catch (cause) { setError(cause.message); }
    finally { setSaving(false); }
  };

  const handleEditSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    try {
      await updateMedicineReception(editingMed.id, {
        ...editForm,
        cantidadPresentaciones: Number(editForm.cantidadPresentaciones),
        contenidoPorPresentacion: Number(editForm.contenidoPorPresentacion),
        precioNetoPorPresentacion: Number(editForm.precioNetoPorPresentacion),
        impuestoPorcentaje: Number(editForm.impuestoPorcentaje)
      });
      setEditingMed(null);
      setToastMsg(`Recepción ${editingMed.codigoLote} actualizada correctamente.`);
      refresh();
    } catch (cause) { setError(cause.message); }
    finally { setSaving(false); }
  };

  return (
    <div className="avi-role-layout">
      <Sidebar role="administrador" />

      <div className="avi-main-frame" data-node-id="216:3119" data-name="Administrador Recepciones Medicamento">
        <Topbar roleLabel="Administrador" avatarSrc="/assets/admin-avatar.svg" />
        <ApiMessage message={toastMsg} type="success" onClose={() => setToastMsg('')} />
        <ApiMessage message={error} type="error" onClose={() => setError('')} />

        <div className="avi-dashboard-content" data-node-id="216:3127">
          {/* Page heading */}
          <div className="avi-page-heading" data-node-id="216:3128">
            <div className="avi-heading-copy" data-node-id="216:3129">
              <h1 className="avi-page-title avi-title-lg" data-node-id="216:3130">
                Recepciones de medicamentos
              </h1>
              <p className="avi-page-subtitle" data-node-id="216:3131">
                Registro, consulta y trazabilidad de entradas de medicamentos a bodega
              </p>
            </div>

            <button
              type="button"
              className="avi-btn-orange-cta"
              data-node-id="216:3132"
              onClick={() => setIsRegisterOpen(true)}
            >
              <span>＋  Registrar recepción</span>
            </button>
          </div>

          {/* 1 Metric card (352x96) */}
          <div className="avi-receptions-metrics-row" data-node-id="216:3134">
            <div className="avi-reception-kpi-card" data-node-id="216:3135">
              <span className="avi-reception-kpi-label">Recepciones del mes</span>
              <strong className="avi-reception-kpi-value" style={{ color: '#2d743b' }}>
                {history.recepcionesMes}
              </strong>
            </div>
          </div>

          {/* Search & Filter Bar (Node 216:3138) */}
          <div className="avi-receptions-filter-bar" data-node-id="216:3138">
            <input
              type="text"
              className="avi-receptions-search-input"
              placeholder="Buscar por lote o medicamento"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              data-node-id="216:3139"
            />

            <select
              className="avi-receptions-select"
              value={presentationFilter}
              onChange={(e) => setPresentationFilter(e.target.value)}
              data-node-id="216:3141"
            >
              <option value="Todas">Todas las presentaciones  ▾</option>
              <option value="Frasco">Frasco</option>
              <option value="Bolsa">Bolsa</option>
              <option value="Caja">Caja</option>
            </select>

            <select
              className="avi-receptions-select"
              value={dateFilter}
              onChange={(e) => setDateFilter(e.target.value)}
              data-node-id="216:3143"
            >
              <option value="30d">Últimos 30 días  ▾</option>
              <option value="7d">Últimos 7 días</option>
              <option value="90d">Últimos 90 días</option>
            </select>

            <button
              type="button"
              className="avi-receptions-filter-btn"
              data-node-id="216:3145"
              onClick={() => setFilters({
                page: 0,
                ...dateRange(dateFilter),
                search: searchTerm,
                presentation: presentationFilter === 'Todas' ? '' : presentationFilter
              })}
            >
              Filtrar
            </button>
          </div>

          {/* Table: Recepciones de Medicamentos Registradas (Node 216:3147) */}
          <div className="avi-recent-orders-panel" data-node-id="216:3147">
            <div className="avi-panel-header avi-panel-header-full" data-node-id="216:3148">
              <div className="avi-panel-icon-28" data-node-id="216:3149">
                <img alt="" src="/assets/admin-orders-icon.svg" />
              </div>
              <h2 className="avi-panel-title" data-node-id="216:3152">
                Recepciones de Medicamentos Registradas
              </h2>
            </div>

            <div className="avi-orders-table" data-node-id="216:3156">
              <div className="avi-orders-thead" data-node-id="216:3157">
                <span style={{ flex: '1.1' }}>Lote</span>
                <span style={{ flex: '1.2' }}>Medicamento</span>
                <span style={{ flex: '1' }}>Presentación</span>
                <span style={{ flex: '0.9', textAlign: 'center' }}>Cantidad</span>
                <span style={{ flex: '1.3', textAlign: 'center' }}>Precio x Presentación</span>
                <span style={{ flex: '1.4', textAlign: 'center' }}>Contenido x Presentación</span>
                <span style={{ flex: '0.7', textAlign: 'center' }}>Detalles</span>
                <span style={{ flex: '0.6', textAlign: 'center' }}>Editar</span>
              </div>

              {history.contenido.map((row) => (
                <div key={row.id} className="avi-orders-trow">
                  <span className="avi-col-dark" style={{ flex: '1.1' }}>
                    {row.codigoLote}
                  </span>
                  <span className="avi-col-dark" style={{ flex: '1.2' }}>
                    {row.nombreMedicamento}
                  </span>
                  <span className="avi-col-dark" style={{ flex: '1' }}>
                    {row.presentacion}
                  </span>
                  <span className="avi-col-dark" style={{ flex: '0.9', textAlign: 'center' }}>
                    {row.cantidadPresentaciones}
                  </span>
                  <span className="avi-col-muted" style={{ flex: '1.3', textAlign: 'center' }}>
                    {currency(row.precioNetoPorPresentacion)}
                  </span>
                  <span className="avi-col-muted" style={{ flex: '1.4', textAlign: 'center' }}>
                    {number(row.contenidoPorPresentacion)} {unit[row.unidadContenido] || row.unidadContenido || ''}
                  </span>
                  <div style={{ flex: '0.7', display: 'flex', justifyContent: 'center' }}>
                    <button
                      type="button"
                      className="avi-table-icon-btn"
                      title="Ver detalles de recepción"
                      onClick={() => handleOpenDetail(row)}
                    >
                      <img alt="Detalles" src="/assets/admin-file-doc.svg" />
                    </button>
                  </div>
                  <div style={{ flex: '0.6', display: 'flex', justifyContent: 'center' }}>
                    <button
                      type="button"
                      className="avi-table-icon-btn"
                      title="Editar recepción"
                      onClick={() => handleOpenEdit(row)}
                    >
                      <img alt="Editar" src="/assets/admin-file-add.svg" />
                    </button>
                  </div>
                </div>
              ))}
              {!loading && !error && history.contenido.length === 0 && (
                <p className="avi-page-subtitle">No hay recepciones para los filtros seleccionados.</p>
              )}
              {loading && <p className="avi-page-subtitle">Cargando recepciones...</p>}
            </div>
            <div className="avi-figma-modal-footer">
              <button type="button" disabled={loading || filters.page === 0}
                onClick={() => setFilters((current) => ({ ...current, page: current.page - 1 }))}>
                Anterior
              </button>
              <span>Página {filters.page + 1} de {Math.max(1, Math.ceil(history.total / 20))}</span>
              <button type="button" disabled={loading || (filters.page + 1) * 20 >= history.total}
                onClick={() => setFilters((current) => ({ ...current, page: current.page + 1 }))}>
                Siguiente
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Modal 1: Registrar recepción de medicamento (Node 232:3539) */}
      <BaseModal
        isOpen={isRegisterOpen}
        onClose={() => setIsRegisterOpen(false)}
        maxWidth="720px"
      >
        <form className="avi-figma-modal-720" onSubmit={handleRegisterSubmit} data-node-id="232:3539">
          <div className="avi-figma-modal-header">
            <div className="avi-figma-modal-titles">
              <h2 className="avi-figma-modal-title">Registrar recepción de medicamento</h2>
              <p className="avi-figma-modal-subtitle">Nueva entrada a inventario</p>
            </div>
            <button
              type="button"
              className="avi-figma-modal-close"
              onClick={() => setIsRegisterOpen(false)}
            >
              ×
            </button>
          </div>

          <div className="avi-figma-modal-grid">
            <div className="avi-figma-field">
              <label className="avi-figma-label">Medicamento</label>
              <select
                className="avi-figma-input"
                value={regForm.medicamentoId}
                onChange={(e) => {
                  const selected = medicines.find((item) => item.id === e.target.value);
                  setRegForm({ ...regForm, medicamentoId: e.target.value,
                    unidadContenido: selected?.unidadBase || 'MILILITRO' });
                }}
                required
              >
                <option value="">Seleccionar medicamento</option>
                {medicines.map((medicine) => (
                  <option key={medicine.id} value={medicine.id}>{medicine.nombre}</option>
                ))}
              </select>
              {medicines.length === 0 && (
                <p className="avi-page-subtitle">No hay medicamentos disponibles en el catálogo.</p>
              )}
            </div>

            <div className="avi-figma-field">
              <label className="avi-figma-label">Codigo del lote</label>
              <input
                type="text"
                className="avi-figma-input"
                value={regForm.codigoLote}
                onChange={(e) => setRegForm({ ...regForm, codigoLote: e.target.value })}
                required
              />
            </div>

            <div className="avi-figma-field">
              <label className="avi-figma-label">Presentación</label>
              <select
                className="avi-figma-input"
                value={regForm.presentacion}
                onChange={(e) => setRegForm({ ...regForm, presentacion: e.target.value })}
              >
                <option value="Frasco">Frasco</option>
                <option value="Bolsa">Bolsa</option>
                <option value="Caja">Caja</option>
              </select>
            </div>

            <div className="avi-figma-field">
              <label className="avi-figma-label">Cantidad</label>
              <input
                className="avi-figma-input"
                type="number"
                min="1"
                step="1"
                value={regForm.cantidadPresentaciones}
                onChange={(e) => setRegForm({ ...regForm, cantidadPresentaciones: e.target.value })}
                required
              />
            </div>

            <div className="avi-figma-field">
              <label className="avi-figma-label">Contenido por presentación</label>
              <input
                type="number"
                min="0.0001"
                step="0.0001"
                className="avi-figma-input"
                value={regForm.contenidoPorPresentacion}
                onChange={(e) => setRegForm({ ...regForm, contenidoPorPresentacion: e.target.value })}
                required
              />
            </div>

            <div className="avi-figma-field">
              <label className="avi-figma-label">Unidad de medida</label>
              <select
                className="avi-figma-input"
                value={regForm.unidadContenido}
                onChange={(e) => setRegForm({ ...regForm, unidadContenido: e.target.value })}
              >
                {compatibleUnits(regForm.medicamentoId, medicines, regForm.unidadContenido).map((value) => (
                  <option key={value} value={value}>{unitLabel[value]}</option>
                ))}
              </select>
            </div>

            <div className="avi-figma-field">
              <label className="avi-figma-label">Precio neto por presentación</label>
              <input
                type="number"
                min="0.01"
                step="0.01"
                className="avi-figma-input"
                value={regForm.precioNetoPorPresentacion}
                onChange={(e) => setRegForm({ ...regForm, precioNetoPorPresentacion: e.target.value })}
                required
              />
            </div>

            <div className="avi-figma-field">
              <label className="avi-figma-label">Impuesto</label>
              <input
                type="number"
                min="0"
                step="0.01"
                className="avi-figma-input"
                value={regForm.impuestoPorcentaje}
                onChange={(e) => setRegForm({ ...regForm, impuestoPorcentaje: e.target.value })}
                required
              />
            </div>

            <div className="avi-figma-field">
              <label className="avi-figma-label">Fecha de ingreso</label>
              <input
                type="date"
                className="avi-figma-input"
                value={regForm.fechaIngreso}
                onChange={(e) => setRegForm({ ...regForm, fechaIngreso: e.target.value })}
                required
              />
            </div>

            <div className="avi-figma-field">
              <label className="avi-figma-label">Fecha de vencimiento</label>
              <input
                type="date"
                className="avi-figma-input"
                value={regForm.fechaVencimiento}
                onChange={(e) => setRegForm({ ...regForm, fechaVencimiento: e.target.value })}
                required
              />
            </div>
          </div>

          <div className="avi-figma-modal-footer">
            <button
              type="button"
              className="avi-figma-btn-cancel"
              onClick={() => setIsRegisterOpen(false)}
            >
              Cancelar
            </button>
            <button type="submit" className="avi-figma-btn-orange" disabled={saving || medicines.length === 0}>
              {saving ? 'Guardando...' : 'Registrar recepción'}
            </button>
          </div>
        </form>
      </BaseModal>

      {/* Modal 2: Editar recepción de medicamento (Node 383:60) */}
      <BaseModal
        isOpen={Boolean(editingMed)}
        onClose={() => setEditingMed(null)}
        maxWidth="720px"
      >
        {editingMed && (
          <form className="avi-figma-modal-720" onSubmit={handleEditSubmit} data-node-id="383:60">
            <div className="avi-figma-modal-header">
              <div className="avi-figma-modal-titles">
                <h2 className="avi-figma-modal-title">Editar recepción de medicamento</h2>
                <p className="avi-figma-modal-subtitle">Editar entrada a inventario</p>
              </div>
              <button
                type="button"
                className="avi-figma-modal-close"
                onClick={() => setEditingMed(null)}
              >
                ×
              </button>
            </div>

            <div className="avi-figma-modal-grid">
              <div className="avi-figma-field">
                <label className="avi-figma-label">Medicamento</label>
                <select
                  className="avi-figma-input"
                  value={editForm.medicamentoId}
                  onChange={(e) => {
                    const selected = medicines.find((item) => item.id === e.target.value);
                    setEditForm({ ...editForm, medicamentoId: e.target.value,
                      unidadContenido: selected?.unidadBase || editForm.unidadContenido });
                  }}
                  required
                >
                  {editForm.medicamentoId && !medicines.some((item) => item.id === editForm.medicamentoId) && (
                    <option value={editForm.medicamentoId}>{editingMed.nombreMedicamento}</option>
                  )}
                  {medicines.map((medicine) => (
                    <option key={medicine.id} value={medicine.id}>{medicine.nombre}</option>
                  ))}
                </select>
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Fecha de vencimiento</label>
                <input
                  type="date"
                  className="avi-figma-input"
                  value={editForm.fechaVencimiento}
                  onChange={(e) => setEditForm({ ...editForm, fechaVencimiento: e.target.value })}
                  required
                />
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Cantidad</label>
                <input
                  type="number"
                  min="1"
                  step="1"
                  className="avi-figma-input"
                  value={editForm.cantidadPresentaciones}
                  onChange={(e) => setEditForm({ ...editForm, cantidadPresentaciones: e.target.value })}
                  required
                />
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Contenido por presentación</label>
                <input
                  type="number"
                  min="0.0001"
                  step="0.0001"
                  className="avi-figma-input"
                  value={editForm.contenidoPorPresentacion}
                  onChange={(e) => setEditForm({ ...editForm, contenidoPorPresentacion: e.target.value })}
                  required
                />
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Unidad de medida</label>
                <select
                  className="avi-figma-input"
                  value={editForm.unidadContenido}
                  onChange={(e) => setEditForm({ ...editForm, unidadContenido: e.target.value })}
                  required
                >
                  {compatibleUnits(editForm.medicamentoId, medicines, editForm.unidadContenido).map((value) => (
                    <option key={value} value={value}>{unitLabel[value]}</option>
                  ))}
                </select>
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Impuesto</label>
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  className="avi-figma-input"
                  value={editForm.impuestoPorcentaje}
                  onChange={(e) => setEditForm({ ...editForm, impuestoPorcentaje: e.target.value })}
                  required
                />
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Precio neto por presentación</label>
                <input
                  type="number"
                  min="0.01"
                  step="0.01"
                  className="avi-figma-input"
                  value={editForm.precioNetoPorPresentacion}
                  onChange={(e) => setEditForm({ ...editForm, precioNetoPorPresentacion: e.target.value })}
                  required
                />
              </div>
            </div>

            <div className="avi-figma-modal-footer avi-figma-modal-footer-spaced">
              <button
                type="button"
                className="avi-figma-btn-cancel"
                onClick={() => setEditingMed(null)}
              >
                Cancelar
              </button>
              <button type="submit" className="avi-figma-btn-green" disabled={saving}>
                {saving ? 'Guardando...' : 'Editar recepción'}
              </button>
            </div>
          </form>
        )}
      </BaseModal>

      {/* Modal 3: Detalles recepción de medicamento (Node 383:199) */}
      <BaseModal
        isOpen={Boolean(detailMed)}
        onClose={() => setDetailMed(null)}
        maxWidth="720px"
      >
        {detailMed && (
          <div className="avi-figma-modal-720" data-node-id="383:199">
            <div className="avi-figma-modal-header">
              <div className="avi-figma-modal-titles">
                <h2 className="avi-figma-modal-title">Detalles recepción de medicamento</h2>
                <p className="avi-figma-modal-subtitle">Detalle entrada a inventario</p>
              </div>
              <button
                type="button"
                className="avi-figma-modal-close"
                onClick={() => setDetailMed(null)}
              >
                ×
              </button>
            </div>

            <div className="avi-figma-modal-grid">
              <div className="avi-figma-field">
                <label className="avi-figma-label">Medicamento</label>
                <div className="avi-figma-readonly-box">{detailMed.nombreMedicamento}</div>
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Codigo del lote</label>
                <div className="avi-figma-readonly-box">
                  {detailMed.codigoLote}
                </div>
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Presentación</label>
                <div className="avi-figma-readonly-box">{detailMed.presentacion}</div>
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Cantidad</label>
                <div className="avi-figma-readonly-box">{detailMed.cantidadPresentaciones}</div>
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Contenido por presentación</label>
                <div className="avi-figma-readonly-box">{number(detailMed.contenidoPorPresentacion)}</div>
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Unidad de medida</label>
                <div className="avi-figma-readonly-box">{unit[detailMed.unidadContenido] || detailMed.unidadContenido}</div>
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Precio neto por presentación</label>
                <div className="avi-figma-readonly-box">{currency(detailMed.precioNetoPorPresentacion)}</div>
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Impuesto</label>
                <div className="avi-figma-readonly-box">{number(detailMed.impuestoPorcentaje)} %</div>
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Fecha de ingreso</label>
                <div className="avi-figma-readonly-box">{detailMed.fechaIngreso}</div>
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Fecha de vencimiento</label>
                <div className="avi-figma-readonly-box">
                  {detailMed.fechaVencimiento}
                </div>
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Subtotal Neto</label>
                <div className="avi-figma-readonly-box">
                  {currency(detailMed.precioNetoTotal)}
                </div>
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Precio total</label>
                <div className="avi-figma-readonly-box">
                  {currency(detailMed.precioTotal)}
                </div>
              </div>
            </div>

            <div className="avi-figma-modal-footer">
              <button
                type="button"
                className="avi-figma-btn-cancel"
                onClick={() => setDetailMed(null)}
              >
                Cerrar
              </button>
            </div>
          </div>
        )}
      </BaseModal>
    </div>
  );
}

export default AdminMedReceptionsPage;
