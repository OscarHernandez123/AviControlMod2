import { useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { FarmContext } from '../context/FarmContext';
import Sidebar from '../components/Sidebar';
import Topbar from '../components/Topbar';
import StatCard from '../components/StatCard';
import {
  computeAssignmentMetrics,
  findStageForAge,
  formatRation
} from '../utils/nutritionUtils';

function NutritionistDashboardPage() {
  const { nutriPlans, assignedBarnPlans, sackWeightKg } = useContext(FarmContext);
  const navigate = useNavigate();

  const activePlans = nutriPlans.filter((p) => p.status !== 'archivado');
  const computedBarns = assignedBarnPlans.map((b) =>
    computeAssignmentMetrics(b, nutriPlans, sackWeightKg)
  );

  const totalBarns = Math.max(1, computedBarns.length);

  // Conteo dinámico de lotes por etapa nutricional (3 barras igual que Administrador)
  const preInicioCount = computedBarns.filter((b) =>
    b.currentStageName.toLowerCase().includes('pre-inicio')
  ).length;
  const inicioCount = computedBarns.filter(
    (b) =>
      b.currentStageName.toLowerCase().includes('inicio') &&
      !b.currentStageName.toLowerCase().includes('pre-inicio')
  ).length;
  const engordeCount = computedBarns.filter(
    (b) =>
      b.currentStageName.toLowerCase().includes('broiler') ||
      b.currentStageName.toLowerCase().includes('engorde')
  ).length;

  const stageDistribution = [
    {
      id: 'st-dist-1',
      label: 'Pre-Inicio',
      count: preInicioCount,
      widthPx: Math.round((preInicioCount / totalBarns) * 190),
      color: '#407db8'
    },
    {
      id: 'st-dist-2',
      label: 'Inicio / Prórroga',
      count: inicioCount,
      widthPx: Math.round((inicioCount / totalBarns) * 190),
      color: '#f59e0b'
    },
    {
      id: 'st-dist-3',
      label: 'Engorde',
      count: engordeCount,
      widthPx: Math.round((engordeCount / totalBarns) * 190),
      color: '#2d9f42'
    }
  ];

  // Generación dinámica de alertas y próximos cambios de etapa
  const nutritionAlerts = computedBarns.slice(0, 3).map((row) => {
    const plan = nutriPlans.find((p) => p.id === row.planId) || nutriPlans[0];
    const stageObj = findStageForAge(plan, row.ageDays);
    const daysRemaining = Math.max(
      0,
      (Number(stageObj?.endDay) || 45) - (Number(row.ageDays) || 1)
    );

    if (row.hasProrroga) {
      return {
        id: `alert-${row.galpon}`,
        galpon: row.galpon,
        name: `${row.galpon} (${row.loteId}) · ${row.currentStageName}`,
        status: 'Prórroga activa',
        tileBg: '#fff1d6',
        icon: '/assets/admin-feed.svg',
        badgeBg: '#ffe8ba',
        badgeColor: '#bf5400',
        dotIcon: '/assets/dot-orange.svg'
      };
    }

    if (daysRemaining <= 7) {
      return {
        id: `alert-${row.galpon}`,
        galpon: row.galpon,
        name: `${row.galpon} (${row.loteId}) · Cambio de etapa en ${daysRemaining} días`,
        status: `Día ${row.ageDays}`,
        tileBg: '#f4eee3',
        icon: '/assets/admin-galpon.svg',
        badgeBg: '#fff1d6',
        badgeColor: '#eb6b00',
        dotIcon: '/assets/dot-orange.svg'
      };
    }

    return {
      id: `alert-${row.galpon}`,
      galpon: row.galpon,
      name: `${row.galpon} (${row.loteId}) · ${row.currentStageName}`,
      status: `${daysRemaining} días restantes`,
      tileBg: '#e6f7e4',
      icon: '/assets/admin-feed.svg',
      badgeBg: '#d6f0d1',
      badgeColor: '#1f5929',
      dotIcon: '/assets/vet-dot-aceptada.svg'
    };
  });

  return (
    <div className="avi-role-layout">
      <Sidebar role="nutricionista" />

      <div
        className="avi-main-frame"
        data-node-id="216:1723"
        data-name="Nutricionista Inicio"
      >
        <Topbar roleLabel="Nutricionista" avatarSrc="/assets/admin-avatar.svg" />

        <div className="avi-dashboard-content" data-node-id="216:1729">
          {/* Subtítulo superior discreto sin título redundante */}
          <div className="avi-page-heading" data-node-id="216:1742">
            <div className="avi-heading-copy">
              <p className="avi-page-subtitle" data-node-id="216:1747">
                Resumen general de etapas nutricionales, alertas de transición y movimientos recientes
              </p>
            </div>
          </div>

          {/* 2 Tarjetas KPI principales */}
          <div className="avi-metrics-row" data-node-id="216:1760">
            <StatCard
              label="Planes disponibles"
              value={`${activePlans.length} Planes`}
              status={`Catálogo activo (${nutriPlans.length} total)`}
              statusColor="#2d743b"
              icon="/assets/admin-orden.svg"
              tileBg="#f4eee3"
              width="437px"
            />
            <StatCard
              label="Galpones con plan"
              value={`${computedBarns.length} Galpones`}
              status="En seguimiento diario"
              statusColor="#2d743b"
              icon="/assets/admin-galpon.svg"
              tileBg="#e6f7e4"
              width="437px"
            />
          </div>

          {/* 2 Paneles Operativos lado a lado (mismo diseño que Inicio de Administrador) */}
          <div className="avi-operational-panels">
            {/* Panel Izquierdo: Distribución de lotes por etapa */}
            <div className="avi-panel-barn-status">
              <div className="avi-panel-header">
                <div className="avi-panel-icon-28">
                  <img alt="" src="/assets/admin-chart.svg" />
                </div>
                <h2 className="avi-panel-title">
                  Distribución de lotes por etapa
                </h2>
              </div>

              {stageDistribution.map((row) => (
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

            {/* Panel Derecho: Alertas y próximos cambios de etapa */}
            <div className="avi-panel-critical-inv">
              <div className="avi-panel-header">
                <div className="avi-panel-icon-28">
                  <img alt="" src="/assets/admin-inv-panel.svg" />
                </div>
                <h2 className="avi-panel-title">
                  Alertas y transiciones de etapa
                </h2>
              </div>

              {nutritionAlerts.map((item) => (
                <div
                  key={item.id}
                  className="avi-crit-item-row"
                  style={{ cursor: 'pointer' }}
                  onClick={() => navigate('/nutricionista/asignaciones')}
                  title="Ir a Asignaciones a galpones"
                >
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

          {/* Panel Inferior: Movimientos y asignaciones recientes (solo lectura) */}
          <div className="avi-recent-orders-panel">
            <div className="avi-panel-header avi-panel-header-full">
              <div className="avi-panel-icon-28">
                <img alt="" src="/assets/admin-orders-icon.svg" />
              </div>
              <h2 className="avi-panel-title">
                Asignaciones y ajustes nutricionales recientes
              </h2>
              <div className="avi-panel-spacer" />
              <div
                className="avi-panel-chevron"
                style={{ cursor: 'pointer' }}
                title="Ir a Asignaciones a galpones"
                onClick={() => navigate('/nutricionista/asignaciones')}
              >
                <img alt="" src="/assets/admin-chevron.svg" />
              </div>
            </div>

            <div className="avi-orders-table">
              <div className="avi-orders-thead">
                <span style={{ flex: '1.1' }}>Galpón / Lote</span>
                <span style={{ flex: '1.4' }}>Plan aplicado</span>
                <span style={{ flex: '1.3' }}>Etapa vigente</span>
                <span style={{ flex: '1.4' }}>Alimento asignado</span>
                <span style={{ flex: '1.1' }}>Ración x Ave</span>
                <span style={{ flex: '1.1' }}>Fecha inicio</span>
              </div>

              {computedBarns.slice(0, 3).map((row) => (
                <div key={row.galpon} className="avi-orders-trow">
                  <span className="avi-col-dark" style={{ flex: '1.1', fontWeight: 600 }}>
                    {row.galpon} ({row.loteId})
                  </span>
                  <span className="avi-col-dark" style={{ flex: '1.4' }}>
                    {row.planName}
                  </span>
                  <span className="avi-col-dark" style={{ flex: '1.3' }}>
                    {row.currentStageName}
                  </span>
                  <span className="avi-col-muted" style={{ flex: '1.4' }}>
                    {row.feedType}
                  </span>
                  <span className="avi-col-muted" style={{ flex: '1.1' }}>
                    {formatRation(row.rationKg)}
                  </span>
                  <span className="avi-col-muted" style={{ flex: '1.1' }}>
                    {row.startDate}
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

export default NutritionistDashboardPage;
