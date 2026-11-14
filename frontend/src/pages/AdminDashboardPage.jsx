import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Sidebar from '../components/Sidebar';
import Topbar from '../components/Topbar';
import StatCard from '../components/StatCard';
import ApiMessage from '../components/ApiMessage';
import { getBackendHealth } from '../services/backendApi';
import { getBarnSummary } from '../services/barnsApi';
import { getInventory, getInventorySummary } from '../services/inventoryApi';
import { getProductiveSlaughterSummary } from '../services/productiveSlaughterApi';
import { listFeedReceptions } from '../services/feedReceptionsApi';

const BARN_STATES = [
  ['PRODUCTIVO', 'Productivos', '#2d9f42'],
  ['EN_COSECHA', 'En cosecha', '#f59e0b'],
  ['AISLAMIENTO', 'Aislados', '#dc2626'],
  ['VACIADO_SANITARIO', 'Vaciado sanitario', '#8b5e3c'],
  ['DISPONIBLE', 'Disponibles', '#407db8'],
  ['MANTENIMIENTO', 'Mantenimiento', '#737373']
];

const money = (value) => new Intl.NumberFormat('es-CO', {
  style: 'currency', currency: 'COP', maximumFractionDigits: 0
}).format(value);

function AdminDashboardPage() {
  const navigate = useNavigate();
  const [backendStatus, setBackendStatus] = useState('Comprobando conexión con el backend...');
  const [barnSummary, setBarnSummary] = useState(null);
  const [inventory, setInventory] = useState(null);
  const [inventorySummary, setInventorySummary] = useState(null);
  const [ordersSummary, setOrdersSummary] = useState(null);
  const [receptions, setReceptions] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const controller = new AbortController();
    getBackendHealth({ signal: controller.signal })
      .then(() => setBackendStatus('Backend conectado'))
      .catch((error) => {
        if (error.name !== 'AbortError') {
          setBackendStatus(`Backend no disponible: ${error.message}`);
        }
      });
    Promise.all([
      getBarnSummary({ signal: controller.signal }),
      getInventory({ signal: controller.signal }),
      getInventorySummary({ signal: controller.signal }),
      getProductiveSlaughterSummary(controller.signal),
      listFeedReceptions({}, { signal: controller.signal })
    ]).then(([barns, stock, stockSummary, orders, feed]) => {
      setBarnSummary(barns);
      setInventory(stock);
      setInventorySummary(stockSummary);
      setOrdersSummary(orders);
      setReceptions(feed.contenido || []);
    }).catch((cause) => {
      if (cause.name !== 'AbortError') setError(cause.message);
    }).finally(() => {
      if (!controller.signal.aborted) setLoading(false);
    });
    return () => controller.abort();
  }, []);

  const counts = barnSummary?.porEstado || {};
  const maxCount = Math.max(1, ...Object.values(counts));
  const barnStatusSummary = BARN_STATES.map(([id, label, color]) => ({
    id, label, color, count: counts[id] || 0,
    widthPx: Math.round(((counts[id] || 0) / maxCount) * 190)
  }));
  const criticalInventory = [
    ...(inventory?.alimentos || [])
      .filter((item) => ['COBERTURA_PARCIAL', 'SIN_COBERTURA'].includes(item.estadoCobertura))
      .map((item) => ({
        id: `alimento-${item.alimentoId}`, name: `${item.alimento} · ${item.stockActualKg} kg`,
        status: item.estadoCobertura === 'SIN_COBERTURA' ? 'Sin cobertura' : 'Cobertura parcial',
        tileBg: '#f4eee3', icon: '/assets/admin-inv-panel.svg', badgeBg: '#ffe8ba',
        dotIcon: '/assets/dot-orange.svg', badgeColor: '#9e4a0f'
      })),
    ...(inventory?.medicamentos || [])
      .filter((item) => Number(item.stockActual) <= 0)
      .map((item) => ({
        id: `medicamento-${item.medicamentoId}`, name: item.medicamento,
        status: 'Sin stock', tileBg: '#f4eee3', icon: '/assets/admin-inv-panel.svg',
        badgeBg: '#ffe0e3', dotIcon: '/assets/dot-red.svg', badgeColor: '#dc2626'
      }))
  ];
  const recentFeedReceptions = receptions.slice(0, 5).map((row) => ({
    id: row.id, codigoLote: row.codigoLote, alimento: row.nombreAlimento,
    bultos: row.cantidadBultos, precio: money(row.precioNetoPorBulto),
    peso: `${row.pesoPorBultoKg} kg`, fecha: row.fechaIngreso
  }));

  return (
    <div className="avi-role-layout">
      <Sidebar role="administrador" />

      <div className="avi-main-frame" data-node-id="104:23" data-name="Administrador Inicio">
        <Topbar roleLabel="Administrador" avatarSrc="/assets/admin-avatar.svg" />
        <ApiMessage message={error} type="error" onClose={() => setError('')} />

        <div className="avi-dashboard-content" data-node-id="104:43">
          {/* Page heading */}
          <div className="avi-page-heading" data-node-id="104:44">
            <div className="avi-heading-copy" data-node-id="104:45">
              <p className="avi-page-subtitle" data-node-id="104:47">
                Resumen general de la operación
              </p>
              <p className="avi-page-subtitle" role="status">{backendStatus}</p>
              {loading && <p className="avi-page-subtitle">Cargando indicadores...</p>}
            </div>
          </div>

          {/* Metrics */}
          <div className="avi-metrics-row" data-node-id="104:52">
            <StatCard
              label="Galpones"
              value={barnSummary?.totalRegistrado ?? '—'}
              status={`${counts.PRODUCTIVO || 0} productivos · ${counts.EN_COSECHA || 0} en cosecha`}
              statusColor="#2d743b"
              icon="/assets/admin-galpon.svg"
              tileBg="#f4eee3"
              width="437px"
            />
            <StatCard
              label="Inventario"
              value={inventory ? `${inventory.tarjetasAlimentoKg?.TOTAL || 0} kg` : '—'}
              status={inventorySummary?.porcentajeOcupacion == null
                ? 'Alimento disponible'
                : `${inventorySummary.porcentajeOcupacion} % de ocupación`}
              statusColor="#2d743b"
              icon="/assets/admin-inventario.svg"
              tileBg="#e6f7e4"
              width="437px"
            />
            <StatCard
              label="Órdenes de sacrificio"
              value={ordersSummary?.totalOrdenes ?? '—'}
              status={`${ordersSummary?.ordenesPendientes ?? 0} pendientes`}
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
              {!loading && criticalInventory.length === 0 && <p className="avi-page-subtitle">No hay productos con cobertura crítica.</p>}
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
                    {row.codigoLote}
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
              {!loading && recentFeedReceptions.length === 0 && <p className="avi-page-subtitle">No hay recepciones recientes.</p>}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default AdminDashboardPage;
