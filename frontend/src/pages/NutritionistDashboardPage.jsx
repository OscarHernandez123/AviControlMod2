import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Sidebar from '../components/Sidebar';
import Topbar from '../components/Topbar';
import StatCard from '../components/StatCard';
import ApiMessage from '../components/ApiMessage';
import { getBarn } from '../services/barnsApi';
import { getBarnNutritionPlan, listTemplates, localBarnId } from '../services/nutritionCatalogApi';

const STAGES = [
  ['PRE_INICIO', 'Pre-inicio', '#407db8'],
  ['INICIO', 'Inicio / prórroga', '#f59e0b'],
  ['ENGORDE', 'Engorde', '#2d9f42']
];

function NutritionistDashboardPage() {
  const navigate = useNavigate();
  const [templates, setTemplates] = useState([]);
  const [plan, setPlan] = useState(null);
  const [barn, setBarn] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const controller = new AbortController();
    Promise.all([
      listTemplates({ signal: controller.signal }),
      getBarnNutritionPlan(localBarnId, { signal: controller.signal }),
      getBarn(localBarnId, { signal: controller.signal })
    ]).then(([loadedTemplates, loadedPlan, loadedBarn]) => {
      setTemplates(loadedTemplates);
      setPlan(loadedPlan);
      setBarn(loadedBarn);
    }).catch((cause) => {
      if (cause.name !== 'AbortError') setError(cause.message);
    }).finally(() => {
      if (!controller.signal.aborted) setLoading(false);
    });
    return () => controller.abort();
  }, []);

  const activeTemplates = templates.filter((item) => item.activo);
  const stages = plan?.etapas || [];
  const currentStage = stages.find((item) => item.estadoEtapa === 'ACTIVA');
  const ageDays = barn?.loteActual?.edad?.diasTotales;
  const daysRemaining = currentStage && ageDays != null
    ? Math.max(0, currentStage.diaFin - ageDays) : null;
  const history = plan?.historialAjustes || [];
  const lastAssignment = [...history].reverse().find((item) =>
    item.accion === 'SUSTITUCION' || item.accion === 'ASIGNACION_MANUAL'
  );
  const currentTemplate = templates.find((item) => item.id === lastAssignment?.valorNuevo);
  const templateStage = currentTemplate?.etapas?.find((item) =>
    item.etapaCrianza === currentStage?.etapaCrianza
  );
  const extended = currentStage && templateStage && currentStage.diaFin > templateStage.diaFin;
  const stageCounts = STAGES.map(([id, label, color]) => ({
    id, label, color,
    count: plan?.estado === 'ACTIVO' && currentStage?.etapaCrianza === id ? 1 : 0
  }));

  return (
    <div className="avi-role-layout">
      <Sidebar role="nutricionista" />
      <div className="avi-main-frame">
        <Topbar roleLabel="Nutricionista" avatarSrc="/assets/admin-avatar.svg" />
        <ApiMessage message={error} type="error" onClose={() => setError('')} />
        <div className="avi-dashboard-content">
          <div className="avi-page-heading">
            <p className="avi-page-subtitle">
              Resumen general de etapas nutricionales, alertas de transición y movimientos recientes
            </p>
            {loading && <p className="avi-page-subtitle">Cargando indicadores...</p>}
          </div>

          <div className="avi-metrics-row">
            <StatCard label="Planes disponibles" value={loading ? '—' : `${activeTemplates.length} planes`}
              status={`Catálogo activo (${templates.length} total)`} icon="/assets/admin-orden.svg"
              tileBg="#f4eee3" width="437px" />
            <StatCard label="Galpones con plan" value={loading ? '—' : plan?.estado === 'ACTIVO' ? '1 galpón' : '0 galpones'}
              status={plan?.estado === 'ACTIVO' ? barn?.nombre || 'Plan activo' : 'Sin plan activo'}
              icon="/assets/admin-galpon.svg" tileBg="#e6f7e4" width="437px" />
          </div>

          <div className="avi-operational-panels">
            <div className="avi-panel-barn-status">
              <div className="avi-panel-header">
                <div className="avi-panel-icon-28"><img alt="" src="/assets/admin-chart.svg" /></div>
                <h2 className="avi-panel-title">Distribución de lotes por etapa</h2>
              </div>
              {stageCounts.map((row) => (
                <div key={row.id} className="avi-status-progress-row">
                  <span className="avi-status-label">{row.label}</span>
                  <div className="avi-progress-track">
                    {row.count > 0 && <div className="avi-progress-bar" style={{ width: '190px', backgroundColor: row.color }} />}
                  </div>
                  <span className="avi-status-count">{row.count}</span>
                </div>
              ))}
            </div>

            <div className="avi-panel-critical-inv">
              <div className="avi-panel-header">
                <div className="avi-panel-icon-28"><img alt="" src="/assets/admin-inv-panel.svg" /></div>
                <h2 className="avi-panel-title">Alertas y transiciones de etapa</h2>
              </div>
              {currentStage && (
                <div className="avi-crit-item-row" onClick={() => navigate('/nutricionista/asignaciones')}
                  style={{ cursor: 'pointer' }}>
                  <div className="avi-crit-item-left">
                    <div className="avi-crit-icon-tile" style={{ backgroundColor: '#fff1d6' }}>
                      <div className="avi-crit-icon-26"><img alt="" src="/assets/admin-feed.svg" /></div>
                    </div>
                    <span className="avi-crit-item-name">
                      {barn?.nombre || 'Galpón'} · {currentStage.etapaCrianza}
                      {extended ? ' · Prórroga activa' : daysRemaining != null ? ` · Cambio en ${daysRemaining} días` : ''}
                    </span>
                  </div>
                  <div className="avi-crit-badge" style={{ backgroundColor: '#ffe8ba' }}>
                    <span style={{ color: '#9e4a0f' }}>Autonomía {currentStage.diasAutonomia} días</span>
                  </div>
                </div>
              )}
              {!loading && !currentStage && <p className="avi-page-subtitle">No hay una etapa activa.</p>}
            </div>
          </div>

          <div className="avi-recent-orders-panel">
            <div className="avi-panel-header avi-panel-header-full">
              <div className="avi-panel-icon-28"><img alt="" src="/assets/admin-orders-icon.svg" /></div>
              <h2 className="avi-panel-title">Asignaciones y ajustes nutricionales recientes</h2>
              <div className="avi-panel-spacer" />
              <button type="button" className="avi-panel-chevron" title="Ir a asignaciones"
                onClick={() => navigate('/nutricionista/asignaciones')}>
                <img alt="" src="/assets/admin-chevron.svg" />
              </button>
            </div>
            <div className="avi-orders-table">
              <div className="avi-orders-thead">
                <span style={{ flex: 1.2 }}>Fecha</span><span style={{ flex: 1.2 }}>Acción</span>
                <span style={{ flex: 1.2 }}>Etapa</span><span style={{ flex: 2 }}>Detalle</span>
                <span style={{ flex: 1 }}>Responsable</span>
              </div>
              {[...history].reverse().slice(0, 5).map((item, index) => (
                <div key={`${item.fechaRegistro}-${index}`} className="avi-orders-trow">
                  <span style={{ flex: 1.2 }}>{item.fechaRegistro?.slice(0, 10)}</span>
                  <span style={{ flex: 1.2 }}>{item.accion.replaceAll('_', ' ')}</span>
                  <span style={{ flex: 1.2 }}>{stages.find((stage) => stage.id === item.etapaId)?.etapaCrianza || 'Plan'}</span>
                  <span style={{ flex: 2 }}>{item.justificacion
                    || templates.find((template) => template.id === item.valorNuevo)?.nombre
                    || (item.accion === 'SELECCION_ALIMENTO' ? 'Alimento seleccionado' : item.valorNuevo || '—')}</span>
                  <span style={{ flex: 1 }}>{item.actor}</span>
                </div>
              ))}
              {!loading && history.length === 0 && <p className="avi-page-subtitle">No hay ajustes registrados.</p>}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default NutritionistDashboardPage;
