import { useContext, useState } from 'react';
import { FarmContext } from '../context/FarmContext';
import Sidebar from '../components/Sidebar';
import Topbar from '../components/Topbar';
import StatCard from '../components/StatCard';
import BaseModal from '../components/BaseModal';
import ApiMessage from '../components/ApiMessage';

function VetSlaughterOrdersPage() {
  const {
    vetSlaughterOrders,
    vetDiseases,
    assignedBarnPlans,
    saveVetSlaughterOrder,
    updateVetSlaughterStatus
  } = useContext(FarmContext);

  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('Todos');
  const [priorityFilter, setPriorityFilter] = useState('Todas');
  const [toastMsg, setToastMsg] = useState('');

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [detailOrder, setDetailOrder] = useState(null);

  const [form, setForm] = useState({
    id: '',
    galpon: 'Galpón 5',
    lote: '#LDP-005',
    enfermedad: vetDiseases[0]?.nombre || 'Enfermedad de Newcastle',
    motivo: 'Contención sanitaria preventiva tras evaluación clínica',
    prioridad: 'Urgente',
    fechaEmision: '2026-09-20',
    protocoloBioseguridad:
      'Despoblado en circuito cerrado, desinfección terminal con amonio cuaternario y vacío sanitario de 21 días',
    estado: 'Pendiente ejecución'
  });

  const filteredOrders = vetSlaughterOrders.filter((row) => {
    const q = searchTerm.trim().toLowerCase();
    const matchesSearch =
      !q ||
      row.id.toLowerCase().includes(q) ||
      row.galpon.toLowerCase().includes(q) ||
      row.lote.toLowerCase().includes(q) ||
      row.enfermedad.toLowerCase().includes(q) ||
      row.motivo.toLowerCase().includes(q);
    const matchesStatus = statusFilter === 'Todos' || row.estado === statusFilter;
    const matchesPriority =
      priorityFilter === 'Todas' || row.prioridad === priorityFilter;
    return matchesSearch && matchesStatus && matchesPriority;
  });

  const pendingCount = vetSlaughterOrders.filter(
    (o) => o.estado === 'Pendiente ejecución'
  ).length;

  const handleOpenNew = () => {
    const nextNum = String(vetSlaughterOrders.length + 1).padStart(2, '0');
    setForm({
      id: `#SAC-VET-${nextNum}`,
      galpon: 'Galpón 5',
      lote: '#LDP-005',
      enfermedad: vetDiseases[0]?.nombre || 'Enfermedad de Newcastle',
      motivo: 'Contención sanitaria preventiva tras evaluación clínica',
      prioridad: 'Urgente',
      fechaEmision: '2026-09-20',
      protocoloBioseguridad:
        'Despoblado en circuito cerrado, desinfección terminal con amonio cuaternario y vacío sanitario de 21 días',
      estado: 'Pendiente ejecución'
    });
    setIsModalOpen(true);
  };

  const handleOpenEdit = (order) => {
    setForm({ ...order });
    setIsModalOpen(true);
  };

  const handleBarnChange = (galponName) => {
    const match = assignedBarnPlans.find((b) => b.galpon === galponName);
    setForm((prev) => ({
      ...prev,
      galpon: galponName,
      lote: match?.loteId || `#LDP-00${galponName.replace(/\D/g, '') || '1'}`
    }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    saveVetSlaughterOrder(form);
    setIsModalOpen(false);
    setToastMsg(`Orden sanitaria ${form.id} registrada para ${form.galpon}.`);
  };

  const getBadgeStyle = (estado) => {
    if (estado === 'Pendiente ejecución') {
      return { bg: '#ffe0e3', color: '#ed2b33', dot: '/assets/dot-red.svg' };
    }
    if (estado === 'Aprobada') {
      return { bg: '#d6f0d1', color: '#1f5929', dot: '/assets/vet-dot-aceptada.svg' };
    }
    return { bg: '#ffe8ba', color: '#bf5400', dot: '/assets/dot-orange.svg' };
  };

  return (
    <div className="avi-role-layout">
      <Sidebar role="veterinario" />

      <div className="avi-main-frame">
        <Topbar roleLabel="Veterinario" avatarSrc="/assets/vet-avatar.svg" />
        <ApiMessage
          message={toastMsg}
          type="success"
          onClose={() => setToastMsg('')}
        />

        <div className="avi-dashboard-content">
          {/* Subtítulo superior y botón de acción */}
          <div className="avi-page-heading">
            <div className="avi-heading-copy">
              <p className="avi-page-subtitle">
                Emisión de dictámenes de sacrificio sanitario, liberación de retiro farmacológico y protocolos de bioseguridad
              </p>
            </div>

            <button
              type="button"
              className="avi-btn-orange-cta"
              onClick={handleOpenNew}
            >
              <span>＋  Emitir orden sanitaria</span>
            </button>
          </div>

          {/* 2 Tarjetas KPI */}
          <div className="avi-metrics-row">
            <StatCard
              label="Órdenes sanitarias emitidas"
              value={`${vetSlaughterOrders.length} Órdenes`}
              status="Control sanitario y retiro"
              statusColor="#2d743b"
              icon="/assets/vet-stethoscope.svg"
              tileBg="#e6f7e4"
              width="437px"
            />
            <StatCard
              label="Pendientes de ejecución"
              value={`${pendingCount} Galpón(es)`}
              status="Prioridad sanitaria activa"
              statusColor="#9e4a0f"
              icon="/assets/vet-syringe.svg"
              tileBg="#f4eee3"
              width="437px"
            />
          </div>

          {/* Barra de búsqueda y filtro estilo Administrador */}
          <form
            className="avi-receptions-filter-bar"
            onSubmit={(e) => e.preventDefault()}
          >
            <input
              type="text"
              className="avi-receptions-search-input"
              placeholder="Buscar por orden, galpón, lote o patología..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />

            <select
              className="avi-receptions-select"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="Todos">Todos los estados</option>
              <option value="Pendiente ejecución">Pendiente ejecución</option>
              <option value="Aprobada">Aprobada</option>
              <option value="Ejecutada">Ejecutada</option>
            </select>

            <select
              className="avi-receptions-select"
              value={priorityFilter}
              onChange={(e) => setPriorityFilter(e.target.value)}
            >
              <option value="Todas">Todas las prioridades</option>
              <option value="Urgente">Prioridad Urgente</option>
              <option value="Alta">Prioridad Alta</option>
              <option value="Programada">Programada</option>
            </select>

            <button
              type="button"
              className="avi-receptions-filter-btn"
              onClick={() => {
                setSearchTerm('');
                setStatusFilter('Todos');
                setPriorityFilter('Todas');
              }}
            >
              Filtrar
            </button>
          </form>

          {/* Tabla de Órdenes Sanitarias */}
          <div className="avi-recent-orders-panel">
            <div className="avi-panel-header avi-panel-header-full">
              <div className="avi-panel-icon-28">
                <img alt="" src="/assets/admin-orders-icon.svg" />
              </div>
              <h2 className="avi-panel-title">
                Registro de órdenes y dictámenes de sacrificio
              </h2>
            </div>

            <div className="avi-orders-table">
              <div className="avi-orders-thead">
                <span style={{ flex: '1' }}>Orden</span>
                <span style={{ flex: '1.1' }}>Galpón / Lote</span>
                <span style={{ flex: '1.5' }}>Diagnóstico / Criterio</span>
                <span style={{ flex: '0.9' }}>Prioridad</span>
                <span style={{ flex: '1' }}>Emisión</span>
                <span style={{ flex: '1.2' }}>Estado</span>
                <span style={{ flex: '1.3', textAlign: 'right' }}>Acciones</span>
              </div>

              {filteredOrders.map((row) => {
                const st = getBadgeStyle(row.estado);
                return (
                  <div key={row.id} className="avi-orders-trow">
                    <span
                      className="avi-col-dark"
                      style={{ flex: '1', fontWeight: 700 }}
                    >
                      {row.id}
                    </span>
                    <span className="avi-col-dark" style={{ flex: '1.1' }}>
                      {row.galpon} ({row.lote})
                    </span>
                    <span className="avi-col-dark" style={{ flex: '1.5' }}>
                      {row.enfermedad}
                    </span>
                    <span
                      className="avi-col-dark"
                      style={{
                        flex: '0.9',
                        fontWeight: 600,
                        color: row.prioridad === 'Urgente' ? '#dc2626' : '#1f3659'
                      }}
                    >
                      {row.prioridad}
                    </span>
                    <span className="avi-col-muted" style={{ flex: '1' }}>
                      {row.fechaEmision}
                    </span>
                    <div style={{ flex: '1.2', display: 'flex', alignItems: 'center' }}>
                      <div
                        className="avi-crit-badge"
                        style={{ backgroundColor: st.bg }}
                      >
                        <div className="avi-crit-dot">
                          <img alt="" src={st.dot} />
                        </div>
                        <span style={{ color: st.color }}>{row.estado}</span>
                      </div>
                    </div>
                    <div
                      style={{
                        flex: '1.3',
                        display: 'flex',
                        justifyContent: 'flex-end',
                        gap: '6px'
                      }}
                    >
                      <button
                        type="button"
                        className="avi-admin-table-btn avi-admin-table-btn-green"
                        onClick={() => setDetailOrder(row)}
                      >
                        Dictamen
                      </button>
                      <button
                        type="button"
                        className="avi-admin-table-btn avi-admin-table-btn-orange"
                        onClick={() => handleOpenEdit(row)}
                      >
                        Editar
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      </div>

      {/* Modal: Emitir / Editar Orden de Sacrificio Sanitario */}
      <BaseModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        maxWidth="720px"
      >
        <form className="avi-figma-modal-720" onSubmit={handleSubmit}>
          <div className="avi-figma-modal-header">
            <div className="avi-figma-modal-titles">
              <h2 className="avi-figma-modal-title">
                Emitir orden de sacrificio / dictamen sanitario ({form.id})
              </h2>
              <p className="avi-figma-modal-subtitle">
                Autorización veterinaria por contención epidemiológica o cumplimiento de retiro
              </p>
            </div>
            <button
              type="button"
              className="avi-figma-modal-close"
              onClick={() => setIsModalOpen(false)}
            >
              ×
            </button>
          </div>

          <div className="avi-figma-modal-grid">
            <div className="avi-figma-field">
              <label className="avi-figma-label">Galpón evaluado</label>
              <div className="avi-figma-select-wrap">
                <select
                  className="avi-figma-input avi-figma-select"
                  value={form.galpon}
                  onChange={(e) => handleBarnChange(e.target.value)}
                >
                  {[
                    'Galpón 1',
                    'Galpón 2',
                    'Galpón 3',
                    'Galpón 4',
                    'Galpón 5',
                    'Galpón 6',
                    'Galpón 7',
                    'Galpón 9'
                  ].map((g) => (
                    <option key={g} value={g}>
                      {g}
                    </option>
                  ))}
                </select>
                <img
                  alt=""
                  src="/assets/admin-select-chevron.svg"
                  className="avi-figma-select-chevron"
                />
              </div>
            </div>

            <div className="avi-figma-field">
              <label className="avi-figma-label">Código del lote activo</label>
              <input
                type="text"
                className="avi-figma-input"
                value={form.lote}
                onChange={(e) => setForm({ ...form, lote: e.target.value })}
                required
              />
            </div>

            <div className="avi-figma-field">
              <label className="avi-figma-label">
                Patología o criterio sanitario
              </label>
              <div className="avi-figma-select-wrap">
                <select
                  className="avi-figma-input avi-figma-select"
                  value={form.enfermedad}
                  onChange={(e) =>
                    setForm({ ...form, enfermedad: e.target.value })
                  }
                >
                  <option value="Apto comercial (Retiro cumplido)">
                    Apto comercial (Retiro cumplido)
                  </option>
                  {vetDiseases.map((d) => (
                    <option key={d.id} value={d.nombre}>
                      {d.nombre} ({d.tipo})
                    </option>
                  ))}
                </select>
                <img
                  alt=""
                  src="/assets/admin-select-chevron.svg"
                  className="avi-figma-select-chevron"
                />
              </div>
            </div>

            <div className="avi-figma-field">
              <label className="avi-figma-label">Nivel de prioridad</label>
              <div className="avi-figma-select-wrap">
                <select
                  className="avi-figma-input avi-figma-select"
                  value={form.prioridad}
                  onChange={(e) =>
                    setForm({ ...form, prioridad: e.target.value })
                  }
                >
                  <option value="Urgente">Urgente (Contención inmediata)</option>
                  <option value="Alta">Alta (Dentro de 24h)</option>
                  <option value="Programada">Programada (Cosecha avalada)</option>
                </select>
                <img
                  alt=""
                  src="/assets/admin-select-chevron.svg"
                  className="avi-figma-select-chevron"
                />
              </div>
            </div>

            <div className="avi-figma-field">
              <label className="avi-figma-label">Fecha de emisión</label>
              <input
                type="date"
                className="avi-figma-input"
                value={form.fechaEmision}
                onChange={(e) =>
                  setForm({ ...form, fechaEmision: e.target.value })
                }
                required
              />
            </div>

            <div className="avi-figma-field">
              <label className="avi-figma-label">Estado de la orden</label>
              <div className="avi-figma-select-wrap">
                <select
                  className="avi-figma-input avi-figma-select"
                  value={form.estado}
                  onChange={(e) => setForm({ ...form, estado: e.target.value })}
                >
                  <option value="Pendiente ejecución">Pendiente ejecución</option>
                  <option value="Aprobada">Aprobada</option>
                  <option value="Ejecutada">Ejecutada</option>
                </select>
                <img
                  alt=""
                  src="/assets/admin-select-chevron.svg"
                  className="avi-figma-select-chevron"
                />
              </div>
            </div>

            <div className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
              <label className="avi-figma-label">Motivo clínico / Justificación</label>
              <input
                type="text"
                className="avi-figma-input"
                value={form.motivo}
                onChange={(e) => setForm({ ...form, motivo: e.target.value })}
                required
              />
            </div>

            <div className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
              <label className="avi-figma-label">
                Protocolo de bioseguridad y disposición final
              </label>
              <input
                type="text"
                className="avi-figma-input"
                value={form.protocoloBioseguridad}
                onChange={(e) =>
                  setForm({ ...form, protocoloBioseguridad: e.target.value })
                }
                required
              />
            </div>
          </div>

          <div className="avi-figma-modal-footer">
            <button
              type="button"
              className="avi-figma-btn-cancel"
              onClick={() => setIsModalOpen(false)}
            >
              Cancelar
            </button>
            <button type="submit" className="avi-figma-btn-green">
              Guardar orden sanitaria
            </button>
          </div>
        </form>
      </BaseModal>

      {/* Modal: Ver Dictamen Detallado */}
      <BaseModal
        isOpen={Boolean(detailOrder)}
        onClose={() => setDetailOrder(null)}
        maxWidth="680px"
      >
        {detailOrder && (
          <div className="avi-figma-modal-720">
            <div className="avi-figma-modal-header">
              <div className="avi-figma-modal-titles">
                <h2 className="avi-figma-modal-title">
                  Dictamen sanitario {detailOrder.id} · {detailOrder.galpon}
                </h2>
                <p className="avi-figma-modal-subtitle">
                  Lote {detailOrder.lote} · Emitido el {detailOrder.fechaEmision}
                </p>
              </div>
              <button
                type="button"
                className="avi-figma-modal-close"
                onClick={() => setDetailOrder(null)}
              >
                ×
              </button>
            </div>

            <div className="avi-figma-modal-grid">
              <div className="avi-figma-field">
                <label className="avi-figma-label">Criterio / Patología</label>
                <div className="avi-figma-readonly-box">
                  {detailOrder.enfermedad}
                </div>
              </div>
              <div className="avi-figma-field">
                <label className="avi-figma-label">Prioridad y Estado</label>
                <div className="avi-figma-valid-box">
                  {detailOrder.prioridad} · {detailOrder.estado}
                </div>
              </div>
              <div className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
                <label className="avi-figma-label">Justificación clínica</label>
                <div className="avi-figma-readonly-box">{detailOrder.motivo}</div>
              </div>
              <div className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
                <label className="avi-figma-label">Protocolo de bioseguridad</label>
                <div className="avi-figma-demand-box">
                  {detailOrder.protocoloBioseguridad}
                </div>
              </div>
            </div>

            <div className="avi-figma-modal-footer">
              <button
                type="button"
                className="avi-figma-btn-cancel"
                onClick={() => setDetailOrder(null)}
              >
                Cerrar
              </button>
              {detailOrder.estado !== 'Ejecutada' && (
                <button
                  type="button"
                  className="avi-figma-btn-green"
                  onClick={() => {
                    updateVetSlaughterStatus(detailOrder.id, 'Ejecutada');
                    setToastMsg(
                      `Orden ${detailOrder.id} marcada como Ejecutada.`
                    );
                    setDetailOrder(null);
                  }}
                >
                  Marcar como ejecutada
                </button>
              )}
            </div>
          </div>
        )}
      </BaseModal>
    </div>
  );
}

export default VetSlaughterOrdersPage;
