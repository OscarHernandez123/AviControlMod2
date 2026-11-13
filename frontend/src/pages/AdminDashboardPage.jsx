import { useContext, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { FarmContext } from '../context/FarmContext';
import Sidebar from '../components/Sidebar';
import Topbar from '../components/Topbar';
import StatCard from '../components/StatCard';
import { getBackendHealth } from '../services/backendApi';

function AdminDashboardPage() {
  const navigate = useNavigate();
  const [backendStatus, setBackendStatus] = useState('Comprobando conexión con el backend...');
  const {
    adminMetrics,
    barnStatusSummary,
    criticalInventory,
    recentFeedReceptions
  } = useContext(FarmContext);

  useEffect(() => {
    const controller = new AbortController();
    getBackendHealth({ signal: controller.signal })
      .then(() => setBackendStatus('Backend conectado'))
      .catch((error) => {
        if (error.name !== 'AbortError') {
          setBackendStatus(`Backend no disponible: ${error.message}`);
        }
      });
    return () => controller.abort();
  }, []);

  return (
    <div className="avi-role-layout">
      <Sidebar role="administrador" />

      <div className="avi-main-frame" data-node-id="104:23" data-name="Administrador Inicio">
        <Topbar roleLabel="Administrador" avatarSrc="/assets/admin-avatar.svg" />

        <div className="avi-dashboard-content" data-node-id="104:43">
          {/* Page heading */}
          <div className="avi-page-heading" data-node-id="104:44">
            <div className="avi-heading-copy" data-node-id="104:45">
              <p className="avi-page-subtitle" data-node-id="104:47">
                Resumen general de la operación
              </p>
              <p className="avi-page-subtitle" role="status">{backendStatus}</p>
            </div>
          </div>

          {/* Metrics */}
          <div className="avi-metrics-row" data-node-id="104:52">
            <StatCard
              label="Galpones"
              value={adminMetrics.galponesTotal}
              status={`${adminMetrics.galponesEnProduccion} en producción`}
              statusColor="#2d743b"
              icon="/assets/admin-galpon.svg"
              tileBg="#f4eee3"
              width="437px"
            />
            <StatCard
              label="Inventario"
              value={`${adminMetrics.inventarioCobertura} %`}
              status="Cobertura disponible"
              statusColor="#2d743b"
              icon="/assets/admin-inventario.svg"
              tileBg="#e6f7e4"
              width="437px"
            />
            <StatCard
              label="Órdenes de sacrificio"
              value={adminMetrics.ordenesSacrificio}
              status={`${adminMetrics.ordenesPendientes} pendientes`}
              statusColor="#9e4a0f"
              icon="/assets/admin-orden.svg"
              tileBg="#f4eee3"
              width="444px"
            />
          </div>

          {/* Operational panels */}
          <div className="avi-operational-panels" data-node-id="104:84">
            {/* Estado de galpones */}
            <div className="avi-panel-barn-status" data-node-id="104:85">
              <div className="avi-panel-header" data-node-id="104:86">
                <div className="avi-panel-icon-28" data-node-id="104:87">
                  <img alt="" src="/assets/admin-chart.svg" />
                </div>
                <h2 className="avi-panel-title" data-node-id="104:91">
                  Estado de galpones
                </h2>
              </div>

              {barnStatusSummary.map((row) => (
                <div key={row.id} className="avi-status-progress-row">
                  <span className="avi-status-label">{row.label}</span>
                  <div className="avi-progress-track">
                    {row.widthPx > 0 && (
                      <div
                        className="avi-progress-bar"
                        style={{
                          width: `${row.widthPx}px`,
                          backgroundColor: row.color
                        }}
                      />
                    )}
                  </div>
                  <span className="avi-status-count">{row.count}</span>
                </div>
              ))}
            </div>

            {/* Inventario crítico */}
            <div className="avi-panel-critical-inv" data-node-id="104:110">
              <div className="avi-panel-header" data-node-id="104:111">
                <div className="avi-panel-icon-28" data-node-id="104:112">
                  <img alt="" src="/assets/admin-inv-panel.svg" />
                </div>
                <h2 className="avi-panel-title" data-node-id="104:114">
                  Inventario crítico
                </h2>
              </div>

              {criticalInventory.map((item) => (
                <div key={item.id} className="avi-crit-item-row">
                  <div className="avi-crit-item-left">
                    <div
                      className="avi-crit-icon-tile"
                      style={{ backgroundColor: item.tileBg }}
                    >
                      <div className="avi-crit-icon-26">
                        <img alt="" src={item.icon} />
                      </div>
                    </div>
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

          {/* Recepciones de Alimentos Registradas Recientemente */}
          <div className="avi-recent-orders-panel" data-node-id="218:284">
            <div className="avi-panel-header avi-panel-header-full" data-node-id="218:285">
              <div className="avi-panel-icon-28" data-node-id="218:286">
                <img alt="" src="/assets/admin-orders-icon.svg" />
              </div>
              <h2 className="avi-panel-title" data-node-id="218:289">
                Recepciones de Alimentos Registradas Recientemente
              </h2>
              <div className="avi-panel-spacer" />
              <div
                className="avi-panel-chevron"
                data-node-id="218:291"
                style={{ cursor: 'pointer' }}
                title="Ir a Recepciones de Alimentos"
                onClick={() => navigate('/administrador/recepciones-alimento')}
              >
                <img alt="" src="/assets/admin-chevron.svg" />
              </div>
            </div>

            <div className="avi-orders-table" data-node-id="218:293">
              <div className="avi-orders-thead" data-node-id="218:294">
                <span style={{ flex: '1.1' }}>Lote</span>
                <span style={{ flex: '1.4' }}>Alimento</span>
                <span style={{ flex: '1.1' }}>Cantidad de bultos</span>
                <span style={{ flex: '1.1' }}>Precio x Bulto</span>
                <span style={{ flex: '1' }}>Peso x bulto</span>
                <span style={{ flex: '1.2' }}>Fecha</span>
              </div>

              {recentFeedReceptions.map((row) => (
                <div key={row.id} className="avi-orders-trow">
                  <span className="avi-col-dark" style={{ flex: '1.1' }}>
                    {row.id}
                  </span>
                  <span className="avi-col-dark" style={{ flex: '1.4' }}>
                    {row.alimento}
                  </span>
                  <span className="avi-col-dark" style={{ flex: '1.1' }}>
                    {row.bultos}
                  </span>
                  <span className="avi-col-muted" style={{ flex: '1.1' }}>
                    {row.precio}
                  </span>
                  <span className="avi-col-muted" style={{ flex: '1' }}>
                    {row.peso}
                  </span>
                  <span className="avi-col-muted" style={{ flex: '1.2' }}>
                    {row.fecha}
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default AdminDashboardPage;
