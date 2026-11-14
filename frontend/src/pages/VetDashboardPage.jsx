import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Sidebar from '../components/Sidebar';
import Topbar from '../components/Topbar';
import StatCard from '../components/StatCard';
import BaseModal from '../components/BaseModal';
import ApiMessage from '../components/ApiMessage';
import { getPendingHealthAlerts, getHealthAlert } from '../services/healthAlertsApi';
import { getPendingIsolationRequests, getIsolationRequest, validateIsolationRequest, getIsolatedBarns } from '../services/isolationApi';
import { listDiagnoses } from '../services/diagnosesApi';

function VetDashboardPage() {
  const navigate = useNavigate();
  const [healthAlerts, setHealthAlerts] = useState(null);
  const [selectedAlert, setSelectedAlert] = useState(null);
  const [alertsReload, setAlertsReload] = useState(0);
  const [alertsError, setAlertsError] = useState('');
  const [pendingRequests, setPendingRequests] = useState([]);
  const [isolatedBarns, setIsolatedBarns] = useState([]);
  const [diagnoses, setDiagnoses] = useState([]);
  const [requestsLoading, setRequestsLoading] = useState(true);
  const [requestsReload, setRequestsReload] = useState(0);
  const [selectedRequest, setSelectedRequest] = useState(null);
  const [decision, setDecision] = useState('ACEPTAR');
  const [observations, setObservations] = useState('');
  const [decisionSaving, setDecisionSaving] = useState(false);
  const [decisionMessage, setDecisionMessage] = useState('');

  useEffect(() => {
    const controller = new AbortController();
    setRequestsLoading(true);
    Promise.all([
      getPendingIsolationRequests({ signal: controller.signal }),
      getIsolatedBarns({ signal: controller.signal }),
      listDiagnoses(controller.signal)
    ])
      .then(([requests, barns, diagnosisList]) => {
        setPendingRequests(requests);
        setIsolatedBarns(barns);
        setDiagnoses(diagnosisList);
      })
      .catch((cause) => { if (cause.name !== 'AbortError') setAlertsError(cause.message); })
      .finally(() => { if (!controller.signal.aborted) setRequestsLoading(false); });
    return () => controller.abort();
  }, [requestsReload]);

  const openRequest = async (requestId) => {
    setAlertsError('');
    try {
      const detail = await getIsolationRequest(requestId);
      setSelectedRequest(detail);
      setDecision('ACEPTAR');
      setObservations('');
    } catch (cause) {
      setAlertsError(cause.message);
    }
  };

  const saveDecision = async (event) => {
    event.preventDefault();
    if (!selectedRequest || !observations.trim()) return;
    setDecisionSaving(true);
    setAlertsError('');
    try {
      const result = await validateIsolationRequest(selectedRequest.id, {
        decision,
        observaciones: observations.trim()
      });
      setSelectedRequest(null);
      setDecisionMessage(`Solicitud ${result.estado.toLowerCase()}. Estado del galpón: ${result.estadoGalponResultante}.`);
      setRequestsReload((value) => value + 1);
    } catch (cause) {
      setAlertsError(cause.message);
    } finally {
      setDecisionSaving(false);
    }
  };

  useEffect(() => {
    const controller = new AbortController();
    getPendingHealthAlerts({ signal: controller.signal })
      .then(setHealthAlerts)
      .catch((cause) => { if (cause.name !== 'AbortError') setAlertsError(cause.message); });
    return () => controller.abort();
  }, [alertsReload]);

  const openAlert = async (id) => {
    setAlertsError('');
    try {
      setSelectedAlert(await getHealthAlert(id));
    } catch (cause) {
      setAlertsError(cause.message);
    }
  };

  return (
    <div className="avi-role-layout">
      <Sidebar role="veterinario" />

      <div
        className="avi-main-frame"
        data-node-id="106:60"
        data-name="Veterinario Inicio"
      >
        <Topbar roleLabel="Veterinario" avatarSrc="/assets/vet-avatar.svg" />
        <ApiMessage message={alertsError} type="error" onClose={() => setAlertsError('')} />
        <ApiMessage message={decisionMessage} type="success" onClose={() => setDecisionMessage('')} />

        <div className="avi-dashboard-content" data-node-id="106:68">
          {/* Subtítulo superior discreto */}
          <div className="avi-page-heading" data-node-id="106:69">
            <div className="avi-heading-copy" data-node-id="106:70">
              <p className="avi-page-subtitle" data-node-id="106:72">
                Resumen sanitario de galpones, solicitudes de revisión y diagnósticos clínicos
              </p>
            </div>
          </div>

          {/* 2 Tarjetas KPI principales */}
          <div className="avi-metrics-row" data-node-id="106:73">
            <StatCard
              label="Alertas sanitarias"
              value={healthAlerts ? `${healthAlerts.totalElementos} alertas` : '—'}
              status="Pendientes de atención veterinaria"
              statusColor="#9e4a0f"
              icon="/assets/vet-bell.svg"
              tileBg="#f4eee3"
              width="437px"
            />
            <StatCard
              label="Diagnósticos realizados"
              value={`${diagnoses.length} Evaluaciones`}
              status="Control sanitario activo"
              statusColor="#2d743b"
              icon="/assets/vet-stethoscope.svg"
              tileBg="#e6f7e4"
              width="437px"
            />
          </div>

          <div className="avi-recent-orders-panel">
            <div className="avi-panel-header avi-panel-header-full">
              <div className="avi-panel-icon-28"><img alt="" src="/assets/vet-bell.svg" /></div>
              <h2 className="avi-panel-title">Alertas de mortalidad pendientes</h2>
              <div className="avi-panel-spacer" />
              <button type="button" className="avi-admin-table-btn avi-admin-table-btn-green"
                onClick={() => setAlertsReload((value) => value + 1)}>Actualizar</button>
            </div>
            {!healthAlerts && !alertsError && <p className="avi-page-subtitle">Cargando alertas...</p>}
            {healthAlerts?.elementos?.length === 0 && <p className="avi-page-subtitle">No hay alertas pendientes.</p>}
            {healthAlerts?.elementos?.length > 0 && <div className="avi-orders-table">
              <div className="avi-orders-thead">
                <span style={{ flex: 1 }}>Galpón</span>
                <span style={{ flex: 1 }}>Muertes</span>
                <span style={{ flex: 1 }}>Porcentaje</span>
                <span style={{ flex: 1 }}>Causa</span>
                <span style={{ flex: 1 }}>Prioridad</span>
                <span style={{ flex: 1 }}>Acción</span>
              </div>
              {healthAlerts.elementos.map((alert) => <div className="avi-orders-trow" key={alert.id}>
                <span style={{ flex: 1 }}>{alert.nombreGalpon}</span>
                <span style={{ flex: 1 }}>{alert.cantidadMuertes}</span>
                <span style={{ flex: 1 }}>{alert.porcentajeMortalidad}%</span>
                <span style={{ flex: 1 }}>{alert.causaProbable}</span>
                <span style={{ flex: 1 }}>{alert.prioridad}</span>
                <span style={{ flex: 1 }}><button type="button"
                  className="avi-admin-table-btn avi-admin-table-btn-green"
                  onClick={() => openAlert(alert.id)}>Ver detalle</button></span>
              </div>)}
            </div>}
          </div>

          {/* Operational panels */}
          <div className="avi-operational-panels" data-node-id="106:105">
            {/* Galpones aislados del backend */}
            <div className="avi-panel-barn-status" data-node-id="106:106">
              <div className="avi-panel-header" data-node-id="106:107">
                <div className="avi-panel-icon-28">
                  <img alt="" src="/assets/admin-chart.svg" />
                </div>
                <h2 className="avi-panel-title">Galpones en aislamiento</h2>
              </div>
              {!requestsLoading && isolatedBarns.length === 0 &&
                <p className="avi-page-subtitle">No hay galpones aislados.</p>}
              {isolatedBarns.map((barn) => <div className="avi-crit-item-row" key={barn.id}>
                <span className="avi-crit-item-name">{barn.nombre}</span>
                <span className="avi-barn-status-pill" style={{ backgroundColor: '#ffe8ba', color: '#bf5400' }}>
                  Aislamiento · {Number(barn.loteActual?.poblacionActual || 0).toLocaleString('es-CO')} aves
                </span>
              </div>)}
            </div>

            {/* Galpones diagnosticados */}
            <div className="avi-panel-critical-inv" data-node-id="106:130">
              <div className="avi-panel-header" data-node-id="106:131">
                <div className="avi-panel-icon-24">
                  <img alt="" src="/assets/vet-service.svg" />
                </div>
                <h2 className="avi-panel-title">Galpones diagnosticados</h2>
              </div>

              {diagnoses.length === 0 &&
                <p className="avi-page-subtitle">No hay diagnósticos registrados.</p>}
              {diagnoses.slice(0, 3).map((item) => (
                <div
                  key={item.id}
                  className="avi-crit-item-row"
                  style={{ cursor: 'pointer' }}
                  onClick={() => navigate('/veterinario/diagnosticar')}
                  title="Ir a Diagnosticar"
                >
                  <div className="avi-crit-item-left">
                    <span className="avi-crit-item-name">
                      {isolatedBarns.find((barn) => barn.id === item.galponId)?.nombre || item.galponId}
                    </span>
                  </div>
                  <div
                    className="avi-crit-badge"
                    style={{ backgroundColor: item.estado === 'ACTIVO' ? '#ffe8ba' : '#d6f0d1' }}
                  >
                    <div className="avi-crit-dot">
                      <img alt="" src={item.estado === 'ACTIVO' ? '/assets/dot-orange.svg' : '/assets/vet-dot-aceptada.svg'} />
                    </div>
                    <span style={{ color: item.estado === 'ACTIVO' ? '#bf5400' : '#1f5929' }}>
                      {item.estado}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Solicitudes de aislamiento pendientes */}
          <div className="avi-recent-orders-panel" data-node-id="106:165">
            <div
              className="avi-panel-header avi-panel-header-full"
              data-node-id="106:166"
            >
              <div className="avi-panel-icon-28">
                <img alt="" src="/assets/admin-orders-icon.svg" />
              </div>
              <h2 className="avi-panel-title">Solicitudes de aislamiento pendientes</h2>
              <div className="avi-panel-spacer" />
              <button type="button" className="avi-admin-table-btn avi-admin-table-btn-green"
                onClick={() => setRequestsReload((value) => value + 1)}>Actualizar</button>
            </div>

            {requestsLoading && <p className="avi-page-subtitle">Cargando solicitudes...</p>}
            {!requestsLoading && pendingRequests.length === 0 &&
              <p className="avi-page-subtitle">No hay solicitudes pendientes de validación.</p>}

            {pendingRequests.length > 0 && <div className="avi-orders-table" data-node-id="106:174">
              <div className="avi-orders-thead" data-node-id="106:175">
                <span style={{ flex: 1 }}>Solicitud</span>
                <span style={{ flex: 1 }}>Galpón</span>
                <span style={{ flex: 2 }}>Motivo reportado</span>
                <span style={{ flex: 1 }}>Población reportada</span>
                <span style={{ flex: 1 }}>Acción</span>
              </div>

              {pendingRequests.map((req) => (
                <div key={req.id} className="avi-orders-trow">
                  <span className="avi-col-dark" style={{ flex: 1 }}>{req.id.slice(0, 8)}</span>
                  <span className="avi-col-dark" style={{ flex: 1 }}>{req.galponId.slice(0, 8)}</span>
                  <span className="avi-col-muted" style={{ flex: 2 }}>{req.motivo}</span>
                  <span className="avi-col-dark" style={{ flex: 1 }}>{Number(req.poblacionVivaReportada).toLocaleString('es-CO')}</span>
                  <span style={{ flex: 1 }}><button type="button" className="avi-admin-table-btn avi-admin-table-btn-green"
                    onClick={() => openRequest(req.id)}>Evaluar</button></span>
                </div>
              ))}
            </div>}
          </div>
        </div>
      </div>
      <BaseModal isOpen={Boolean(selectedAlert)}
        onClose={() => setSelectedAlert(null)} maxWidth="720px">
        {selectedAlert && <div className="avi-figma-modal-720">
          <div className="avi-figma-modal-header">
            <div className="avi-figma-modal-titles">
              <h2 className="avi-figma-modal-title">Alerta sanitaria · {selectedAlert.nombreGalpon}</h2>
              <p className="avi-figma-modal-subtitle">{selectedAlert.id}</p>
            </div>
            <button type="button" className="avi-figma-modal-close"
              onClick={() => setSelectedAlert(null)}>×</button>
          </div>
          <div className="avi-figma-modal-grid">
            <p><strong>Prioridad:</strong> {selectedAlert.prioridad}</p>
            <p><strong>Estado:</strong> {selectedAlert.estado}</p>
            <p><strong>Galpón:</strong> {selectedAlert.nombreGalpon}</p>
            <p><strong>Estado del galpón:</strong> {selectedAlert.estadoGalpon}</p>
            <p><strong>Muertes:</strong> {selectedAlert.cantidadMuertes}</p>
            <p><strong>Mortalidad:</strong> {selectedAlert.porcentajeMortalidad}%</p>
            <p><strong>Población actual:</strong> {Number(selectedAlert.poblacionActual).toLocaleString('es-CO')}</p>
            <p><strong>Edad:</strong> {selectedAlert.edadDias} días</p>
            <p><strong>Causa probable:</strong> {selectedAlert.causaProbable}</p>
            <p><strong>Responsable:</strong> {selectedAlert.responsable}</p>
            <p style={{ gridColumn: '1 / -1' }}>
              <strong>Observaciones:</strong> {selectedAlert.observaciones || 'Sin observaciones'}
            </p>
          </div>
        </div>}
      </BaseModal>
      <BaseModal isOpen={Boolean(selectedRequest)}
        onClose={() => { if (!decisionSaving) setSelectedRequest(null); }} maxWidth="720px">
        {selectedRequest && <form className="avi-figma-modal-720" onSubmit={saveDecision}>
          <div className="avi-figma-modal-header">
            <div className="avi-figma-modal-titles">
              <h2 className="avi-figma-modal-title">Validar aislamiento</h2>
              <p className="avi-figma-modal-subtitle">Solicitud {selectedRequest.id}</p>
            </div>
            <button type="button" className="avi-figma-modal-close" disabled={decisionSaving}
              onClick={() => setSelectedRequest(null)}>×</button>
          </div>
          <div className="avi-figma-modal-grid">
            <div className="avi-figma-field">
              <label className="avi-figma-label">Galpón</label>
              <div className="avi-figma-readonly-box">{selectedRequest.galponId}</div>
            </div>
            <div className="avi-figma-field">
              <label className="avi-figma-label">Población reportada</label>
              <div className="avi-figma-readonly-box">{Number(selectedRequest.poblacionVivaReportada).toLocaleString('es-CO')} aves</div>
            </div>
            <div className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
              <label className="avi-figma-label">Motivo</label>
              <div className="avi-figma-readonly-box">{selectedRequest.motivo}</div>
            </div>
            <div className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
              <label className="avi-figma-label">Signos clínicos</label>
              <div className="avi-figma-readonly-box">{selectedRequest.signosClinicos}</div>
            </div>
            <div className="avi-figma-field">
              <label className="avi-figma-label" htmlFor="isolation-decision">Decisión</label>
              <select id="isolation-decision" className="avi-figma-input avi-figma-select"
                value={decision} disabled={decisionSaving} onChange={(event) => setDecision(event.target.value)}>
                <option value="ACEPTAR">Aceptar aislamiento</option>
                <option value="RECHAZAR">Rechazar solicitud</option>
              </select>
            </div>
            <div className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
              <label className="avi-figma-label" htmlFor="isolation-observations">Observaciones de la decisión</label>
              <input id="isolation-observations" className="avi-figma-input" required
                value={observations} disabled={decisionSaving}
                onChange={(event) => setObservations(event.target.value)} />
            </div>
          </div>
          <div className="avi-figma-modal-footer">
            <button type="button" className="avi-figma-btn-cancel" disabled={decisionSaving}
              onClick={() => setSelectedRequest(null)}>Cancelar</button>
            <button type="submit" className="avi-figma-btn-green" disabled={decisionSaving}>
              {decisionSaving ? 'Guardando...' : 'Guardar decisión'}
            </button>
          </div>
        </form>}
      </BaseModal>
    </div>
  );
}

export default VetDashboardPage;
