import { useContext, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { FarmContext } from '../context/FarmContext';
import Sidebar from '../components/Sidebar';
import Topbar from '../components/Topbar';
import StatCard from '../components/StatCard';
import ApiMessage from '../components/ApiMessage';
import { getPendingHealthAlerts } from '../services/healthAlertsApi';

function VetDashboardPage() {
  const { vetDiagnosedBarns, vetReviewRequests, vetDiagnoses } =
    useContext(FarmContext);
  const navigate = useNavigate();
  const [healthAlerts, setHealthAlerts] = useState(null);
  const [alertsError, setAlertsError] = useState('');

  useEffect(() => {
    const controller = new AbortController();
    getPendingHealthAlerts({ signal: controller.signal })
      .then(setHealthAlerts)
      .catch((cause) => { if (cause.name !== 'AbortError') setAlertsError(cause.message); });
    return () => controller.abort();
  }, []);

  const vetBarnStatus = [
    { id: 'disp', label: 'Disponible', count: 10, widthPx: 185, color: '#59bf63' },
    { id: 'cos', label: 'En cosecha', count: 2, widthPx: 54, color: '#ffa80d' },
    { id: 'ais', label: 'Aislado', count: 2, widthPx: 54, color: '#ff0d11' }
  ];

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
              value={`${vetDiagnoses?.length || 4} Evaluaciones`}
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
              </div>
              {healthAlerts.elementos.map((alert) => <div className="avi-orders-trow" key={alert.id}>
                <span style={{ flex: 1 }}>{alert.nombreGalpon}</span>
                <span style={{ flex: 1 }}>{alert.cantidadMuertes}</span>
                <span style={{ flex: 1 }}>{alert.porcentajeMortalidad}%</span>
                <span style={{ flex: 1 }}>{alert.causaProbable}</span>
                <span style={{ flex: 1 }}>{alert.prioridad}</span>
              </div>)}
            </div>}
          </div>

          {/* Operational panels */}
          <div className="avi-operational-panels" data-node-id="106:105">
            {/* Estado de galpones */}
            <div className="avi-panel-barn-status" data-node-id="106:106">
              <div className="avi-panel-header" data-node-id="106:107">
                <div className="avi-panel-icon-28">
                  <img alt="" src="/assets/admin-chart.svg" />
                </div>
                <h2 className="avi-panel-title">Estado de galpones</h2>
              </div>

              {vetBarnStatus.map((row) => (
                <div key={row.id} className="avi-status-progress-row">
                  <span className="avi-status-label">{row.label}</span>
                  <div className="avi-progress-track">
                    <div
                      className="avi-progress-bar"
                      style={{
                        width: `${row.widthPx}px`,
                        backgroundColor: row.color
                      }}
                    />
                  </div>
                  <span className="avi-status-count">{row.count}</span>
                </div>
              ))}
            </div>

            {/* Galpones diagnosticados */}
            <div className="avi-panel-critical-inv" data-node-id="106:130">
              <div className="avi-panel-header" data-node-id="106:131">
                <div className="avi-panel-icon-24">
                  <img alt="" src="/assets/vet-service.svg" />
                </div>
                <h2 className="avi-panel-title">Galpones diagnosticados</h2>
              </div>

              {vetDiagnosedBarns.slice(0, 3).map((item) => (
                <div
                  key={item.id}
                  className="avi-crit-item-row"
                  style={{ cursor: 'pointer' }}
                  onClick={() => navigate('/veterinario/diagnosticar')}
                  title="Ir a Diagnosticar"
                >
                  <div className="avi-crit-item-left">
                    <span className="avi-crit-item-name">{item.name}</span>
                  </div>
                  <div
                    className="avi-crit-badge"
                    style={{ backgroundColor: item.badgeBg }}
                  >
                    <div className="avi-crit-dot">
                      <img alt="" src={item.dotIcon} />
                    </div>
                    <span style={{ color: item.badgeColor }}>{item.status}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Solicitudes de revisión recientes */}
          <div className="avi-recent-orders-panel" data-node-id="106:165">
            <div
              className="avi-panel-header avi-panel-header-full"
              data-node-id="106:166"
            >
              <div className="avi-panel-icon-28">
                <img alt="" src="/assets/admin-orders-icon.svg" />
              </div>
              <h2 className="avi-panel-title">Solicitudes de revisión</h2>
              <div className="avi-panel-spacer" />
              <div
                className="avi-panel-chevron"
                style={{ cursor: 'pointer' }}
                title="Ir a Diagnosticar"
                onClick={() => navigate('/veterinario/diagnosticar')}
              >
                <img alt="" src="/assets/admin-chevron.svg" />
              </div>
            </div>

            <div className="avi-orders-table" data-node-id="106:174">
              <div className="avi-orders-thead" data-node-id="106:175">
                <span style={{ flex: '0.8' }}>Solicitud</span>
                <span style={{ flex: '1.1' }}>Galpón</span>
                <span style={{ flex: '1.8' }}>Motivo reportado</span>
                <span style={{ flex: '1' }}>Fecha</span>
                <span style={{ flex: '1.1' }}>Estado</span>
              </div>

              {vetReviewRequests.slice(0, 3).map((req) => (
                <div key={req.id} className="avi-orders-trow">
                  <span
                    className="avi-col-dark"
                    style={{ flex: '0.8', fontWeight: 700 }}
                  >
                    {req.id}
                  </span>
                  <span className="avi-col-dark" style={{ flex: '1.1' }}>
                    {req.galpon}
                  </span>
                  <span className="avi-col-muted" style={{ flex: '1.8' }}>
                    {req.motivo || 'Revisión sanitaria de galpón'}
                  </span>
                  <span className="avi-col-dark" style={{ flex: '1' }}>
                    {req.fecha}
                  </span>
                  <div
                    style={{
                      flex: '1.1',
                      display: 'flex',
                      alignItems: 'center'
                    }}
                  >
                    <div
                      className="avi-vet-status-pill"
                      style={{
                        backgroundColor: req.badgeBg,
                        width: `${req.widthPx}px`
                      }}
                    >
                      <div className="avi-vet-dot-12">
                        <img alt="" src={req.dotIcon} />
                      </div>
                      <span style={{ color: req.badgeColor }}>{req.estado}</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default VetDashboardPage;
