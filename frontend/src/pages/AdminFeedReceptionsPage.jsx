import { useEffect, useState } from 'react';
import Sidebar from '../components/Sidebar';
import Topbar from '../components/Topbar';
import BaseModal from '../components/BaseModal';
import ApiMessage from '../components/ApiMessage';
import {
  createFeedReception,
  getFeedReception,
  getFeedReceptionEditForm,
  listAvailableFoods,
  listFeedReceptions,
  updateFeedReception
} from '../services/feedReceptionsApi';

const emptyForm = {
  alimentoId: '',
  tipoAlimento: '',
  codigoLote: '',
  fechaIngreso: '',
  fechaVencimiento: '',
  cantidadBultos: '',
  pesoPorBultoKg: '',
  precioNetoPorBulto: '',
  impuestoPorcentaje: ''
};

const currency = (value) => new Intl.NumberFormat('es-CO', {
  style: 'currency', currency: 'COP', maximumFractionDigits: 2
}).format(Number(value || 0));

const number = (value) => new Intl.NumberFormat('es-CO', {
  maximumFractionDigits: 2
}).format(Number(value || 0));

function dateRange(period) {
  const days = Number(period.slice(0, -1));
  const today = new Date();
  const start = new Date(today);
  start.setDate(today.getDate() - days + 1);
  const format = (date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  return { from: format(start), to: format(today) };
}

function AdminFeedReceptionsPage() {
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('Todos');
  const [dateFilter, setDateFilter] = useState('30d');
  const [filters, setFilters] = useState({ page: 0, ...dateRange('30d') });
  const [history, setHistory] = useState({ contenido: [], total: 0, recepcionesMes: 0, pesoMesKg: 0 });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [reloadKey, setReloadKey] = useState(0);
  const [isRegisterOpen, setIsRegisterOpen] = useState(false);
  const [editingReception, setEditingReception] = useState(null);
  const [detailReception, setDetailReception] = useState(null);
  const [toastMsg, setToastMsg] = useState('');
  const [regForm, setRegForm] = useState(emptyForm);
  const [editForm, setEditForm] = useState(emptyForm);
  const [availableFoods, setAvailableFoods] = useState([]);
  const [foodsLoading, setFoodsLoading] = useState(false);
  const selectedFoodType = editingReception?.tipoAlimento || regForm.tipoAlimento;

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError('');
    listFeedReceptions(filters, { signal: controller.signal })
      .then(setHistory)
      .catch((cause) => {
        if (cause.name !== 'AbortError') setError(cause.message);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [filters, reloadKey]);

  useEffect(() => {
    if (!selectedFoodType) {
      setAvailableFoods([]);
      return;
    }
    const controller = new AbortController();
    setFoodsLoading(true);
    setAvailableFoods([]);
    listAvailableFoods(selectedFoodType, { signal: controller.signal })
      .then(setAvailableFoods)
      .catch((cause) => {
        if (cause.name !== 'AbortError') setError(cause.message);
      })
      .finally(() => {
        if (!controller.signal.aborted) setFoodsLoading(false);
      });
    return () => controller.abort();
  }, [selectedFoodType]);

  const refresh = () => setReloadKey((value) => value + 1);

  const handleOpenEdit = async (row) => {
    setError('');
    try {
      const form = await getFeedReceptionEditForm(row.id);
      setEditForm(form);
      setEditingReception(row);
    } catch (cause) {
      setError(cause.message);
    }
  };

  const handleOpenDetail = async (row) => {
    setError('');
    try {
      setDetailReception(await getFeedReception(row.id));
    } catch (cause) {
      setError(cause.message);
    }
  };

  const handleRegisterSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    try {
      const created = await createFeedReception({
        ...regForm,
        cantidadBultos: Number(regForm.cantidadBultos),
        pesoPorBultoKg: Number(regForm.pesoPorBultoKg),
        precioNetoPorBulto: Number(regForm.precioNetoPorBulto),
        impuestoPorcentaje: Number(regForm.impuestoPorcentaje)
      });
      setIsRegisterOpen(false);
      setRegForm(emptyForm);
      setToastMsg(`Recepción ${created.codigoLote} registrada correctamente.`);
      setFilters({ page: 0, ...dateRange('30d') });
      refresh();
    } catch (cause) {
      setError(cause.message);
    } finally {
      setSaving(false);
    }
  };

  const handleEditSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    try {
      await updateFeedReception(editingReception.id, {
        ...editForm,
        cantidadBultos: Number(editForm.cantidadBultos),
        pesoPorBultoKg: Number(editForm.pesoPorBultoKg),
        precioNetoPorBulto: Number(editForm.precioNetoPorBulto),
        impuestoPorcentaje: Number(editForm.impuestoPorcentaje)
      });
      setEditingReception(null);
      setToastMsg(`Recepción ${editingReception.codigoLote} actualizada correctamente.`);
      refresh();
    } catch (cause) {
      setError(cause.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="avi-role-layout">
      <Sidebar role="administrador" />

      <div className="avi-main-frame" data-node-id="216:2923" data-name="Administrador Recepciones Alimento">
        <Topbar roleLabel="Administrador" avatarSrc="/assets/admin-avatar.svg" />
        <ApiMessage message={toastMsg} type="success" onClose={() => setToastMsg('')} />
        <ApiMessage message={error} type="error" onClose={() => setError('')} />

        <div className="avi-dashboard-content" data-node-id="216:2931">
          {/* Page heading with "+ Registrar recepción" button */}
          <div className="avi-page-heading" data-node-id="216:2932">
            <div className="avi-heading-copy" data-node-id="216:2933">
              <h1 className="avi-page-title avi-title-lg" data-node-id="216:2934">
                Recepciones de alimentos
              </h1>
              <p className="avi-page-subtitle" data-node-id="216:2935">
                Registro, consulta y trazabilidad de entradas a bodega
              </p>
            </div>

            <button
              type="button"
              className="avi-btn-orange-cta"
              data-node-id="216:2936"
              onClick={() => setIsRegisterOpen(true)}
            >
              <span>＋  Registrar recepción</span>
            </button>
          </div>

          {/* 2 Metric cards (352x96) */}
          <div className="avi-receptions-metrics-row" data-node-id="216:2938">
            <div className="avi-reception-kpi-card" data-node-id="216:2939">
              <span className="avi-reception-kpi-label">Recepciones del mes</span>
              <strong className="avi-reception-kpi-value" style={{ color: '#2d743b' }}>
                {history.recepcionesMes}
              </strong>
            </div>

            <div className="avi-reception-kpi-card" data-node-id="216:2942">
              <span className="avi-reception-kpi-label">Kilogramos ingresados</span>
              <strong className="avi-reception-kpi-value" style={{ color: '#407db8' }}>
                {number(history.pesoMesKg)} kg
              </strong>
            </div>
          </div>

          {/* Search & Filter Bar (Node 216:2945) */}
          <div className="avi-receptions-filter-bar" data-node-id="216:2945">
            <input
              type="text"
              className="avi-receptions-search-input"
              placeholder="Buscar por recepción o lote"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              data-node-id="216:2946"
            />

            <select
              className="avi-receptions-select"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              data-node-id="216:2948"
            >
              <option value="Todos">Todos los estados  ▾</option>
              <option value="CONFIRMADA">Confirmadas</option>
            </select>

            <select
              className="avi-receptions-select"
              value={dateFilter}
              onChange={(e) => setDateFilter(e.target.value)}
              data-node-id="216:2950"
            >
              <option value="30d">Últimos 30 días  ▾</option>
              <option value="7d">Últimos 7 días</option>
              <option value="90d">Últimos 90 días</option>
            </select>

            <button
              type="button"
              className="avi-receptions-filter-btn"
              data-node-id="216:2952"
              onClick={() => setFilters({
                page: 0,
                search: searchTerm,
                status: statusFilter === 'Todos' ? '' : statusFilter,
                ...dateRange(dateFilter)
              })}
            >
              Filtrar
            </button>
          </div>

          {/* Table: Recepciones de Alimentos Registradas (Node 216:2954) */}
          <div className="avi-recent-orders-panel" data-node-id="216:2954">
            <div className="avi-panel-header avi-panel-header-full" data-node-id="216:2955">
              <div className="avi-panel-icon-28" data-node-id="216:2956">
                <img alt="" src="/assets/admin-orders-icon.svg" />
              </div>
              <h2 className="avi-panel-title" data-node-id="216:2959">
                Recepciones de Alimentos Registradas
              </h2>
            </div>

            <div className="avi-orders-table" data-node-id="216:2963">
              <div className="avi-orders-thead" data-node-id="216:2964">
                <span style={{ flex: '1.1' }}>Lote</span>
                <span style={{ flex: '1.3' }}>Alimento</span>
                <span style={{ flex: '1.3', textAlign: 'center' }}>Cantidad de bultos</span>
                <span style={{ flex: '1.3', textAlign: 'center' }}>Precio neto x Bulto</span>
                <span style={{ flex: '1.1', textAlign: 'center' }}>Peso x bulto</span>
                <span style={{ flex: '0.8', textAlign: 'center' }}>Detalles</span>
                <span style={{ flex: '0.7', textAlign: 'center' }}>Editar</span>
              </div>

              {!error && history.contenido.map((row) => (
                <div key={row.id} className="avi-orders-trow">
                  <span className="avi-col-dark" style={{ flex: '1.1' }}>
                    {row.codigoLote}
                  </span>
                  <span className="avi-col-dark" style={{ flex: '1.3' }}>
                    {row.nombreAlimento}
                  </span>
                  <span className="avi-col-dark" style={{ flex: '1.3', textAlign: 'center' }}>
                    {row.cantidadBultos}
                  </span>
                  <span className="avi-col-muted" style={{ flex: '1.3', textAlign: 'center' }}>
                    {currency(row.precioNetoPorBulto)}
                  </span>
                  <span className="avi-col-muted" style={{ flex: '1.1', textAlign: 'center' }}>
                    {number(row.pesoPorBultoKg)} kg
                  </span>
                  <div style={{ flex: '0.8', display: 'flex', justifyContent: 'center' }}>
                    <button
                      type="button"
                      className="avi-table-icon-btn"
                      title="Ver detalles de recepción"
                      onClick={() => handleOpenDetail(row)}
                    >
                      <img alt="Detalles" src="/assets/admin-file-doc.svg" />
                    </button>
                  </div>
                  <div style={{ flex: '0.7', display: 'flex', justifyContent: 'center' }}>
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

      {/* Modal 1: Registrar recepción de alimento (Node 232:4255) */}
      <BaseModal
        isOpen={isRegisterOpen}
        onClose={() => setIsRegisterOpen(false)}
        maxWidth="720px"
      >
        <form className="avi-figma-modal-720" onSubmit={handleRegisterSubmit} data-node-id="232:4255">
          <div className="avi-figma-modal-header">
            <div className="avi-figma-modal-titles">
              <h2 className="avi-figma-modal-title">Registrar recepción de alimento</h2>
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
              <label className="avi-figma-label">Tipo de alimento</label>
              <select
                className="avi-figma-input"
                value={regForm.tipoAlimento}
                onChange={(e) => setRegForm({ ...regForm, tipoAlimento: e.target.value, alimentoId: '' })}
                required
              >
                <option value="">Seleccionar</option>
                <option value="PRE_INICIO">Pre-inicio</option>
                <option value="INICIO">Inicio</option>
                <option value="ENGORDE">Engorde</option>
              </select>
            </div>

            <div className="avi-figma-field">
              <label className="avi-figma-label">Alimento</label>
              <select
                className="avi-figma-input"
                value={regForm.alimentoId}
                onChange={(e) => {
                  const food = availableFoods.find((item) => item.id === e.target.value);
                  setRegForm({
                    ...regForm,
                    alimentoId: e.target.value,
                    pesoPorBultoKg: food ? String(food.pesoNominalPorBultoKg) : ''
                  });
                }}
                disabled={!regForm.tipoAlimento || foodsLoading}
                required
              >
                <option value="">{foodsLoading ? 'Cargando alimentos...' : 'Seleccionar alimento'}</option>
                {availableFoods.map((food) => (
                  <option key={food.id} value={food.id}>
                    {food.nombre} ({number(food.pesoNominalPorBultoKg)} kg/bulto)
                  </option>
                ))}
              </select>
              {regForm.tipoAlimento && !foodsLoading && availableFoods.length === 0 && (
                <p className="avi-page-subtitle">No hay alimentos de este tipo. Registra primero el producto.</p>
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

            <div className="avi-figma-field">
              <label className="avi-figma-label">Cantidad de bultos</label>
              <input
                type="number"
                className="avi-figma-input"
                min="1"
                step="1"
                value={regForm.cantidadBultos}
                onChange={(e) => setRegForm({ ...regForm, cantidadBultos: e.target.value })}
                required
              />
            </div>

            <div className="avi-figma-field">
              <label className="avi-figma-label">Peso por bulto (kg)</label>
              <input
                type="number"
                className="avi-figma-input"
                min="0.0001"
                step="0.0001"
                value={regForm.pesoPorBultoKg}
                onChange={(e) => setRegForm({ ...regForm, pesoPorBultoKg: e.target.value })}
                required
              />
            </div>

            <div className="avi-figma-field">
              <label className="avi-figma-label">Precio neto x bulto</label>
              <input
                type="number"
                className="avi-figma-input"
                min="0.01"
                step="0.01"
                value={regForm.precioNetoPorBulto}
                onChange={(e) => setRegForm({ ...regForm, precioNetoPorBulto: e.target.value })}
                required
              />
            </div>

            <div className="avi-figma-field">
              <label className="avi-figma-label">Impuesto (%)</label>
              <input
                type="number"
                className="avi-figma-input"
                min="0"
                step="0.01"
                value={regForm.impuestoPorcentaje}
                onChange={(e) => setRegForm({ ...regForm, impuestoPorcentaje: e.target.value })}
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
            <button type="submit" className="avi-figma-btn-orange" disabled={saving}>
              {saving ? 'Guardando...' : 'Registrar recepción'}
            </button>
          </div>
        </form>
      </BaseModal>

      {/* Modal 2: Editar recepción de alimento (Node 379:66) */}
      <BaseModal
        isOpen={Boolean(editingReception)}
        onClose={() => setEditingReception(null)}
        maxWidth="720px"
      >
        {editingReception && (
          <form className="avi-figma-modal-720" onSubmit={handleEditSubmit} data-node-id="379:66">
            <div className="avi-figma-modal-header">
              <div className="avi-figma-modal-titles">
                <h2 className="avi-figma-modal-title">Editar recepción de alimento</h2>
                <p className="avi-figma-modal-subtitle">edición de entrada a inventario</p>
              </div>
              <button
                type="button"
                className="avi-figma-modal-close"
                onClick={() => setEditingReception(null)}
              >
                ×
              </button>
            </div>

            <div className="avi-figma-modal-grid">
              <div className="avi-figma-field">
                <label className="avi-figma-label">Alimento</label>
                <select
                  className="avi-figma-input"
                  value={editForm.alimentoId}
                  onChange={(e) => setEditForm({ ...editForm, alimentoId: e.target.value })}
                  disabled={foodsLoading}
                  required
                >
                  <option value="">Seleccionar alimento</option>
                  {editForm.alimentoId && !availableFoods.some((food) => food.id === editForm.alimentoId) && (
                    <option value={editForm.alimentoId}>{editingReception.nombreAlimento}</option>
                  )}
                  {availableFoods.map((food) => (
                    <option key={food.id} value={food.id}>
                      {food.nombre} ({number(food.pesoNominalPorBultoKg)} kg/bulto)
                    </option>
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
                <label className="avi-figma-label">Cantidad de bultos</label>
                <input
                  type="number"
                  className="avi-figma-input"
                  min="1"
                  step="1"
                  value={editForm.cantidadBultos}
                  onChange={(e) => setEditForm({ ...editForm, cantidadBultos: e.target.value })}
                  required
                />
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Peso por bulto (kg)</label>
                <input
                  type="number"
                  className="avi-figma-input"
                  min="0.0001"
                  step="0.0001"
                  value={editForm.pesoPorBultoKg}
                  onChange={(e) => setEditForm({ ...editForm, pesoPorBultoKg: e.target.value })}
                  required
                />
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Precio neto x bulto</label>
                <input
                  type="number"
                  className="avi-figma-input"
                  min="0.01"
                  step="0.01"
                  value={editForm.precioNetoPorBulto}
                  onChange={(e) => setEditForm({ ...editForm, precioNetoPorBulto: e.target.value })}
                  required
                />
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Impuesto (%)</label>
                <input
                  type="number"
                  className="avi-figma-input"
                  min="0"
                  step="0.01"
                  value={editForm.impuestoPorcentaje}
                  onChange={(e) => setEditForm({ ...editForm, impuestoPorcentaje: e.target.value })}
                  required
                />
              </div>
            </div>

            <div className="avi-figma-modal-footer avi-figma-modal-footer-spaced">
              <button
                type="button"
                className="avi-figma-btn-cancel"
                onClick={() => setEditingReception(null)}
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

      {/* Modal 3: Detalles recepción de alimento (Node 379:147) */}
      <BaseModal
        isOpen={Boolean(detailReception)}
        onClose={() => setDetailReception(null)}
        maxWidth="720px"
      >
        {detailReception && (
          <div className="avi-figma-modal-720" data-node-id="379:147">
            <div className="avi-figma-modal-header">
              <div className="avi-figma-modal-titles">
                <h2 className="avi-figma-modal-title">Detalles recepción de alimento</h2>
                <p className="avi-figma-modal-subtitle">detalles de entrada a inventario</p>
              </div>
              <button
                type="button"
                className="avi-figma-modal-close"
                onClick={() => setDetailReception(null)}
              >
                ×
              </button>
            </div>

            <div className="avi-figma-modal-grid">
              <div className="avi-figma-field">
                <label className="avi-figma-label">Alimento</label>
                <div className="avi-figma-readonly-box">{detailReception.nombreAlimento}</div>
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Codigo del lote</label>
                <div className="avi-figma-readonly-box">
                  {detailReception.codigoLote}
                </div>
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Fecha de ingreso</label>
                <div className="avi-figma-readonly-box">{detailReception.fechaIngreso}</div>
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Fecha de vencimiento</label>
                <div className="avi-figma-readonly-box">
                  {detailReception.fechaVencimiento}
                </div>
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Cantidad de bultos</label>
                <div className="avi-figma-readonly-box">{detailReception.cantidadBultos}</div>
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Peso por bulto</label>
                <div className="avi-figma-readonly-box">{number(detailReception.pesoPorBultoKg)} kg</div>
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Precio neto x bulto</label>
                <div className="avi-figma-readonly-box">{currency(detailReception.precioNetoPorBulto)}</div>
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Impuesto</label>
                <div className="avi-figma-readonly-box">{number(detailReception.impuestoPorcentaje)} %</div>
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Peso total</label>
                <div className="avi-figma-readonly-box">
                  {number(detailReception.cantidadTotalKg)} kg
                </div>
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Tipo de alimento</label>
                <div className="avi-figma-readonly-box">
                  {detailReception.tipoAlimento}
                </div>
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Valor del impuesto</label>
                <div className="avi-figma-readonly-box">
                  {currency(detailReception.valorImpuesto)}
                </div>
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Total de la compra</label>
                <div className="avi-figma-readonly-box">
                  {currency(detailReception.precioTotal)}
                </div>
              </div>
            </div>

            <div className="avi-figma-modal-footer">
              <button
                type="button"
                className="avi-figma-btn-cancel"
                onClick={() => setDetailReception(null)}
              >
                Cancelar
              </button>
            </div>
          </div>
        )}
      </BaseModal>
    </div>
  );
}

export default AdminFeedReceptionsPage;
