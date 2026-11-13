import { useContext, useState } from 'react';
import { FarmContext } from '../context/FarmContext';
import Sidebar from '../components/Sidebar';
import Topbar from '../components/Topbar';
import BaseModal from '../components/BaseModal';
import ApiMessage from '../components/ApiMessage';

function AdminSlaughterOrdersPage() {
  const {
    slaughterOrders,
    evaluableBarnsForSlaughter,
    validateBarnForSlaughter,
    scheduleSlaughterOrder,
    cancelSlaughterOrder,
    saveSlaughterResults
  } = useContext(FarmContext);

  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('Todos');
  const [dateFilter, setDateFilter] = useState('30d');
  const [toastMsg, setToastMsg] = useState('');

  // Modal 1: Validar Aptitud para Sacrificio
  const [isValidateOpen, setIsValidateOpen] = useState(false);
  const [selectedEvalId, setSelectedEvalId] = useState(
    evaluableBarnsForSlaughter[0]?.id || 'eval-3'
  );

  // Modal 2: Programar / Reprogramar Orden de Sacrificio
  const [schedulingOrder, setSchedulingOrder] = useState(null);
  const [scheduleForm, setScheduleForm] = useState({
    fechaIso: '2026-09-22T06:00',
    plantaDestino: 'Planta Central Avícola - Turno Mañana',
    observaciones: 'Transporte programado con jaulas limpias y desinfectadas a las 05:30 AM.'
  });

  // Modal 3: Resultados Finales de Sacrificio
  const [weighingOrder, setWeighingOrder] = useState(null);
  const [resultsForm, setResultsForm] = useState({
    cantidadFinal: '4.180',
    pesoTotalKg: '10.450,00'
  });

  const selectedEvalBarn =
    evaluableBarnsForSlaughter.find((b) => b.id === selectedEvalId) ||
    evaluableBarnsForSlaughter[0];

  const filteredOrders = slaughterOrders.filter((row) => {
    const matchesSearch =
      !searchTerm.trim() ||
      row.id.toLowerCase().includes(searchTerm.toLowerCase()) ||
      row.galpon.toLowerCase().includes(searchTerm.toLowerCase()) ||
      row.lote.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesStatus = statusFilter === 'Todos' || row.estado === statusFilter;
    return matchesSearch && matchesStatus;
  });

  const enCosechaCount = slaughterOrders.filter((o) => o.estado === 'En Cosecha').length || 2;
  const ordenesActivasCount =
    slaughterOrders.filter((o) => o.estado === 'Programada' || o.estado === 'Ejecutada').length || 3;

  const handleOpenSchedule = (order) => {
    setScheduleForm({
      fechaIso: order.fechaIso || '2026-09-22T06:00',
      plantaDestino: order.plantaDestino || 'Planta Central Avícola - Turno Mañana',
      observaciones:
        order.observaciones ||
        'Transporte programado con jaulas limpias y desinfectadas a las 05:30 AM.'
    });
    setSchedulingOrder(order);
  };

  const handleOpenWeigh = (order) => {
    setResultsForm({
      cantidadFinal: order.cantidadFinal || '4.180',
      pesoTotalKg: order.pesoTotalKg || '10.450,00'
    });
    setWeighingOrder(order);
  };

  const handleConfirmValidation = (e) => {
    e.preventDefault();
    if (!selectedEvalBarn) return;
    validateBarnForSlaughter(selectedEvalBarn);
    setIsValidateOpen(false);
    setToastMsg(`${selectedEvalBarn.galpon} validado y pasado a estado En Cosecha.`);
  };

  const handleSaveSchedule = (e) => {
    e.preventDefault();
    if (!schedulingOrder) return;
    const raw = scheduleForm.fechaIso || '2026-09-22T06:00';
    const [datePart, timePart = '06:00'] = raw.split('T');
    const [, month, day] = datePart.split('-');
    const months = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];
    const monthLabel = months[(parseInt(month, 10) || 9) - 1] || 'Sep';
    const fechaFormateada = `${day || '22'} ${monthLabel} 2026, ${timePart}`;

    scheduleSlaughterOrder({
      galpon: schedulingOrder.galpon,
      fechaIso: scheduleForm.fechaIso,
      fechaFormateada,
      plantaDestino: scheduleForm.plantaDestino,
      observaciones: scheduleForm.observaciones
    });
    setSchedulingOrder(null);
    setToastMsg(`Orden de sacrificio para ${schedulingOrder.galpon} programada (${fechaFormateada}).`);
  };

  const handleSaveResults = (e) => {
    e.preventDefault();
    if (!weighingOrder) return;
    saveSlaughterResults({
      galpon: weighingOrder.galpon,
      cantidadFinal: resultsForm.cantidadFinal,
      pesoTotalKg: resultsForm.pesoTotalKg
    });
    setWeighingOrder(null);
    setToastMsg(`Resultados finales de ${weighingOrder.galpon} registrados correctamente.`);
  };

  const computeAvgWeight = () => {
    const birds =
      parseFloat(String(resultsForm.cantidadFinal).replace(/\./g, '').replace(',', '.')) || 0;
    const weight =
      parseFloat(String(resultsForm.pesoTotalKg).replace(/\./g, '').replace(',', '.')) || 0;
    if (birds <= 0) return '0,00 kg';
    return `${(weight / birds).toFixed(2).replace('.', ',')} kg`;
  };

  const getStatusBadgeStyle = (estado) => {
    if (estado === 'En Cosecha') {
      return { bg: '#d6f0d1', color: '#1f5929', dot: '#2d9f42' };
    }
    if (estado === 'Programada') {
      return { bg: '#d1ebf0', color: '#1f3659', dot: '#2c5282' };
    }
    if (estado === 'Ejecutada') {
      return { bg: '#ffe8ba', color: '#bf5400', dot: '#f59e0b' };
    }
    return { bg: '#dbdbdb', color: '#5a5a5a', dot: '#737373' };
  };

  return (
    <div className="avi-role-layout">
      <Sidebar role="administrador" />

      <div className="avi-main-frame" data-node-id="232:3892" data-name="Administrador Ordenes Sacrificio">
        <Topbar roleLabel="Administrador" avatarSrc="/assets/admin-avatar.svg" />
        <ApiMessage message={toastMsg} type="success" onClose={() => setToastMsg('')} />

        <div className="avi-dashboard-content">
          {/* Page heading matching Recepciones de Alimento / Medicamento */}
          <div className="avi-page-heading">
            <div className="avi-heading-copy">
              <h1 className="avi-page-title avi-title-lg">
                Órdenes de sacrificio
              </h1>
              <p className="avi-page-subtitle">
                Gestión, validación de aptitud y programación de sacrificio comercial
              </p>
            </div>

            <button
              type="button"
              className="avi-btn-orange-cta"
              onClick={() => setIsValidateOpen(true)}
            >
              <span>＋  Validar aptitud de galpón</span>
            </button>
          </div>

          {/* 3 Metric cards matching Recepciones / Inventario */}
          <div className="avi-receptions-metrics-row">
            <div className="avi-reception-kpi-card">
              <span className="avi-reception-kpi-label">Galpones en cosecha</span>
              <strong className="avi-reception-kpi-value" style={{ color: '#2d743b' }}>
                {enCosechaCount}
              </strong>
            </div>

            <div className="avi-reception-kpi-card">
              <span className="avi-reception-kpi-label">Órdenes activas</span>
              <strong className="avi-reception-kpi-value" style={{ color: '#407db8' }}>
                {ordenesActivasCount}
              </strong>
            </div>

            <div className="avi-reception-kpi-card">
              <span className="avi-reception-kpi-label">Total aves este mes</span>
              <strong className="avi-reception-kpi-value" style={{ color: '#2d743b' }}>
                18,500 aves
              </strong>
            </div>
          </div>

          {/* Search & Filter Bar matching Recepciones */}
          <div className="avi-receptions-filter-bar">
            <input
              type="text"
              className="avi-receptions-search-input"
              placeholder="Buscar por orden, galpón o lote"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />

            <select
              className="avi-receptions-select"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="Todos">Todos los estados  ▾</option>
              <option value="Programada">Programada</option>
              <option value="Ejecutada">Ejecutada</option>
              <option value="En Cosecha">En Cosecha</option>
              <option value="Cancelada">Cancelada</option>
            </select>

            <select
              className="avi-receptions-select"
              value={dateFilter}
              onChange={(e) => setDateFilter(e.target.value)}
            >
              <option value="30d">Últimos 30 días  ▾</option>
              <option value="7d">Últimos 7 días</option>
              <option value="90d">Últimos 90 días</option>
            </select>

            <button
              type="button"
              className="avi-receptions-filter-btn"
              onClick={() => setToastMsg('Filtros aplicados al listado de órdenes de sacrificio.')}
            >
              Filtrar
            </button>
          </div>

          {/* Table Panel matching Galpones & Recepciones */}
          <div className="avi-recent-orders-panel">
            <div className="avi-panel-header avi-panel-header-full">
              <div className="avi-panel-icon-28">
                <img alt="" src="/assets/admin-orders-icon.svg" />
              </div>
              <h2 className="avi-panel-title">
                Órdenes de Sacrificio Registradas
              </h2>
            </div>

            <div className="avi-orders-table">
              <div className="avi-orders-thead">
                <span style={{ flex: '0.9' }}>Orden</span>
                <span style={{ flex: '1' }}>Galpón</span>
                <span style={{ flex: '0.9' }}>Lote</span>
                <span style={{ flex: '0.9', textAlign: 'center' }}>Edad lote</span>
                <span style={{ flex: '1', textAlign: 'center' }}>Población</span>
                <span style={{ flex: '1.3', textAlign: 'center' }}>Fecha sacrificio</span>
                <span style={{ flex: '1.1', textAlign: 'center' }}>Estado</span>
                <span style={{ flex: '1.4', textAlign: 'center' }}>Acciones</span>
              </div>

              {filteredOrders.map((row) => {
                const badge = getStatusBadgeStyle(row.estado);
                return (
                  <div key={row.galpon} className="avi-orders-trow">
                    <span className="avi-col-dark" style={{ flex: '0.9' }}>
                      {row.id}
                    </span>
                    <span className="avi-col-dark" style={{ flex: '1' }}>
                      {row.galpon}
                    </span>
                    <span className="avi-col-dark" style={{ flex: '0.9' }}>
                      {row.lote}
                    </span>
                    <span className="avi-col-muted" style={{ flex: '0.9', textAlign: 'center' }}>
                      {row.edadDias} días
                    </span>
                    <span className="avi-col-muted" style={{ flex: '1', textAlign: 'center' }}>
                      {row.poblacion}
                    </span>
                    <span className="avi-col-muted" style={{ flex: '1.3', textAlign: 'center' }}>
                      {row.fechaSacrificio}
                    </span>
                    <div style={{ flex: '1.1', display: 'flex', justifyContent: 'center' }}>
                      <span
                        className="avi-barn-status-pill"
                        style={{
                          backgroundColor: badge.bg,
                          color: badge.color
                        }}
                      >
                        <span
                          className="avi-barn-status-dot"
                          style={{ backgroundColor: badge.dot }}
                        />
                        {row.estado}
                      </span>
                    </div>
                    <div
                      style={{
                        flex: '1.4',
                        display: 'flex',
                        justifyContent: 'center',
                        alignItems: 'center',
                        gap: '8px'
                      }}
                    >
                      {row.estado === 'Programada' && (
                        <>
                          <button
                            type="button"
                            className="avi-admin-table-btn avi-admin-table-btn-green"
                            onClick={() => handleOpenSchedule(row)}
                          >
                            Reprogramar
                          </button>
                          <button
                            type="button"
                            className="avi-admin-table-btn avi-admin-table-btn-danger"
                            onClick={() => {
                              cancelSlaughterOrder(row.galpon);
                              setToastMsg(`Orden de ${row.galpon} cancelada.`);
                            }}
                          >
                            Cancelar
                          </button>
                        </>
                      )}

                      {row.estado === 'Ejecutada' && (
                        <button
                          type="button"
                          className="avi-admin-table-btn avi-admin-table-btn-green"
                          onClick={() => handleOpenWeigh(row)}
                        >
                          Registrar peso
                        </button>
                      )}

                      {row.estado === 'En Cosecha' && (
                        <button
                          type="button"
                          className="avi-admin-table-btn avi-admin-table-btn-orange"
                          onClick={() => handleOpenSchedule(row)}
                        >
                          Programar
                        </button>
                      )}

                      {row.estado === 'Cancelada' && (
                        <span className="avi-col-muted">Sin acciones</span>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      </div>

      {/* Modal 1: Validar aptitud para sacrificio (720px Admin Modal style) */}
      <BaseModal
        isOpen={isValidateOpen}
        onClose={() => setIsValidateOpen(false)}
        maxWidth="720px"
      >
        <form className="avi-figma-modal-720" onSubmit={handleConfirmValidation}>
          <div className="avi-figma-modal-header">
            <div className="avi-figma-modal-titles">
              <h2 className="avi-figma-modal-title">Validar aptitud para sacrificio</h2>
              <p className="avi-figma-modal-subtitle">
                Evaluación de edad y estado operativo del galpón
              </p>
            </div>
            <button
              type="button"
              className="avi-figma-modal-close"
              onClick={() => setIsValidateOpen(false)}
            >
              ×
            </button>
          </div>

          <div className="avi-figma-modal-grid">
            <div className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
              <label className="avi-figma-label">Galpón a evaluar</label>
              <select
                className="avi-figma-input"
                value={selectedEvalId}
                onChange={(e) => setSelectedEvalId(e.target.value)}
              >
                {evaluableBarnsForSlaughter.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.galpon} — Lote {b.lote} ({b.edadDias} días · {b.estadoOperativo})
                  </option>
                ))}
              </select>
            </div>

            {selectedEvalBarn && (
              <>
                <div className="avi-figma-field">
                  <label className="avi-figma-label">Lote alojado</label>
                  <div className="avi-figma-readonly-box">{selectedEvalBarn.lote}</div>
                </div>

                <div className="avi-figma-field">
                  <label className="avi-figma-label">Población actual</label>
                  <div className="avi-figma-readonly-box">{selectedEvalBarn.poblacion}</div>
                </div>

                <div className="avi-figma-field">
                  <label className="avi-figma-label">Edad del lote (mínimo 45 días)</label>
                  <div className="avi-figma-readonly-box">{selectedEvalBarn.edadDias} días</div>
                </div>

                <div className="avi-figma-field">
                  <label className="avi-figma-label">Resultado de validación</label>
                  <div
                    className={
                      selectedEvalBarn.apto
                        ? 'avi-figma-valid-box'
                        : 'avi-figma-demand-box'
                    }
                  >
                    {selectedEvalBarn.apto
                      ? 'Cumple (Apto para pasar a En Cosecha)'
                      : 'No cumple edad mínima requerida'}
                  </div>
                </div>
              </>
            )}
          </div>

          <div className="avi-figma-modal-footer">
            <button
              type="button"
              className="avi-figma-btn-cancel"
              onClick={() => setIsValidateOpen(false)}
            >
              Cancelar
            </button>
            <button type="submit" className="avi-figma-btn-orange">
              Confirmar cosecha
            </button>
          </div>
        </form>
      </BaseModal>

      {/* Modal 2: Programar orden de sacrificio (720px Admin Modal style) */}
      <BaseModal
        isOpen={Boolean(schedulingOrder)}
        onClose={() => setSchedulingOrder(null)}
        maxWidth="720px"
      >
        {schedulingOrder && (
          <form className="avi-figma-modal-720" onSubmit={handleSaveSchedule}>
            <div className="avi-figma-modal-header">
              <div className="avi-figma-modal-titles">
                <h2 className="avi-figma-modal-title">Programar orden de sacrificio</h2>
                <p className="avi-figma-modal-subtitle">
                  Asignación de fecha y logística comercial de salida
                </p>
              </div>
              <button
                type="button"
                className="avi-figma-modal-close"
                onClick={() => setSchedulingOrder(null)}
              >
                ×
              </button>
            </div>

            <div className="avi-figma-modal-grid">
              <div className="avi-figma-field">
                <label className="avi-figma-label">Galpón</label>
                <div className="avi-figma-readonly-box">{schedulingOrder.galpon}</div>
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Código del lote</label>
                <div className="avi-figma-readonly-box">{schedulingOrder.lote}</div>
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Población a sacrificar</label>
                <div className="avi-figma-readonly-box">{schedulingOrder.poblacion}</div>
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Fecha y hora programada</label>
                <input
                  type="datetime-local"
                  className="avi-figma-input"
                  value={scheduleForm.fechaIso}
                  onChange={(e) =>
                    setScheduleForm({ ...scheduleForm, fechaIso: e.target.value })
                  }
                  required
                />
              </div>

              <div className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
                <label className="avi-figma-label">Planta de faenado / Destino</label>
                <input
                  type="text"
                  className="avi-figma-input"
                  value={scheduleForm.plantaDestino}
                  onChange={(e) =>
                    setScheduleForm({ ...scheduleForm, plantaDestino: e.target.value })
                  }
                  required
                />
              </div>

              <div className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
                <label className="avi-figma-label">Observaciones de cosecha</label>
                <input
                  type="text"
                  className="avi-figma-input"
                  value={scheduleForm.observaciones}
                  onChange={(e) =>
                    setScheduleForm({ ...scheduleForm, observaciones: e.target.value })
                  }
                />
              </div>
            </div>

            <div className="avi-figma-modal-footer">
              <button
                type="button"
                className="avi-figma-btn-cancel"
                onClick={() => setSchedulingOrder(null)}
              >
                Cancelar
              </button>
              <button type="submit" className="avi-figma-btn-green">
                Guardar programación
              </button>
            </div>
          </form>
        )}
      </BaseModal>

      {/* Modal 3: Resultados finales de sacrificio (720px Admin Modal style) */}
      <BaseModal
        isOpen={Boolean(weighingOrder)}
        onClose={() => setWeighingOrder(null)}
        maxWidth="720px"
      >
        {weighingOrder && (
          <form className="avi-figma-modal-720" onSubmit={handleSaveResults}>
            <div className="avi-figma-modal-header">
              <div className="avi-figma-modal-titles">
                <h2 className="avi-figma-modal-title">Resultados finales de sacrificio</h2>
                <p className="avi-figma-modal-subtitle">
                  Captura de peso real y conteo final de aves en planta
                </p>
              </div>
              <button
                type="button"
                className="avi-figma-modal-close"
                onClick={() => setWeighingOrder(null)}
              >
                ×
              </button>
            </div>

            <div className="avi-figma-modal-grid">
              <div className="avi-figma-field">
                <label className="avi-figma-label">Galpón</label>
                <div className="avi-figma-readonly-box">{weighingOrder.galpon}</div>
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Código del lote</label>
                <div className="avi-figma-readonly-box">{weighingOrder.lote}</div>
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Población inicial</label>
                <div className="avi-figma-readonly-box">{weighingOrder.poblacion}</div>
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Fecha de sacrificio</label>
                <div className="avi-figma-readonly-box">{weighingOrder.fechaSacrificio}</div>
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Cantidad final de aves</label>
                <input
                  type="text"
                  className="avi-figma-input"
                  value={resultsForm.cantidadFinal}
                  onChange={(e) =>
                    setResultsForm({ ...resultsForm, cantidadFinal: e.target.value })
                  }
                  required
                />
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Peso total en planta (kg)</label>
                <input
                  type="text"
                  className="avi-figma-input"
                  value={resultsForm.pesoTotalKg}
                  onChange={(e) =>
                    setResultsForm({ ...resultsForm, pesoTotalKg: e.target.value })
                  }
                  required
                />
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Peso promedio por ave</label>
                <div className="avi-figma-valid-box">{computeAvgWeight()}</div>
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">Estado de liquidación</label>
                <div className="avi-figma-demand-box">Editable (Pendiente de liquidación)</div>
              </div>
            </div>

            <div className="avi-figma-modal-footer">
              <button
                type="button"
                className="avi-figma-btn-cancel"
                onClick={() => setWeighingOrder(null)}
              >
                Cancelar
              </button>
              <button type="submit" className="avi-figma-btn-green">
                Guardar resultados
              </button>
            </div>
          </form>
        )}
      </BaseModal>
    </div>
  );
}

export default AdminSlaughterOrdersPage;
