import { useContext, useState } from 'react';
import { FarmContext } from '../context/FarmContext';
import Sidebar from '../components/Sidebar';
import Topbar from '../components/Topbar';
import StatCard from '../components/StatCard';
import ApiMessage from '../components/ApiMessage';
import AssignPlanModal from '../components/nutritionist/AssignPlanModal';
import AdjustStageModal from '../components/nutritionist/AdjustStageModal';
import {
  computeAssignmentMetrics,
  findStageForAge,
  formatAgeWeeks,
  formatBirds,
  formatKg,
  formatRation,
  formatSacks,
  formatStageDays
} from '../utils/nutritionUtils';

function NutritionistAssignmentsPage() {
  const {
    nutriPlans,
    assignedBarnPlans,
    sackWeightKg,
    assignNutriPlan,
    adjustNutriStage
  } = useContext(FarmContext);

  const [toast, setToast] = useState('');

  // Estado del formulario rápido en página para asignar plan a lote
  const activePlans = nutriPlans.filter((p) => p.status !== 'archivado');
  const [quickAssign, setQuickAssign] = useState({
    galpon: assignedBarnPlans[0]?.galpon || 'Galpón 1',
    loteId: assignedBarnPlans[0]?.loteId || '#LDP-001',
    liveBirds: assignedBarnPlans[0]?.liveBirds || 1200,
    ageDays: assignedBarnPlans[0]?.ageDays || 25,
    planId: assignedBarnPlans[0]?.planId || 'plan-1',
    startDate: assignedBarnPlans[0]?.startDate || '2026-10-12'
  });

  // Estados de modales
  const [isAssignModalOpen, setIsAssignModalOpen] = useState(false);
  const [assignModalDefaults, setAssignModalDefaults] = useState({
    galpon: 'Galpón 1',
    planId: 'plan-1'
  });

  const [isAdjustModalOpen, setIsAdjustModalOpen] = useState(false);
  const [adjustModalGalpon, setAdjustModalGalpon] = useState('Galpón 2');

  const computedBarns = assignedBarnPlans.map((b) =>
    computeAssignmentMetrics(b, nutriPlans, sackWeightKg)
  );

  const totalLiveBirds = computedBarns.reduce(
    (acc, r) => acc + (Number(r.liveBirds) || 0),
    0
  );
  const prorrogaCount = computedBarns.filter((r) => r.hasProrroga).length;

  const handleSelectBarnInForm = (nextGalpon) => {
    const found = assignedBarnPlans.find((b) => b.galpon === nextGalpon);
    if (!found) return;
    setQuickAssign({
      galpon: found.galpon,
      loteId: found.loteId,
      liveBirds: Number(found.liveBirds) || 1000,
      ageDays: Number(found.ageDays) || 1,
      planId: found.planId,
      startDate: found.startDate || '2026-10-12'
    });
  };

  const quickSelectedPlan =
    nutriPlans.find((p) => p.id === quickAssign.planId) || nutriPlans[0];
  const quickMatchingStage = findStageForAge(
    quickSelectedPlan,
    quickAssign.ageDays
  );
  const quickRationKg = Number(quickMatchingStage?.rationKg) || 0.045;
  const quickDemandKg = (Number(quickAssign.liveBirds) || 0) * quickRationKg;
  const quickDemandSacks = quickDemandKg / (Number(sackWeightKg) || 40);

  const handleQuickAssignSubmit = (e) => {
    e.preventDefault();
    assignNutriPlan(quickAssign);
    setToast(
      `Plan "${quickSelectedPlan?.name || ''}" asignado al lote ${quickAssign.loteId} en ${quickAssign.galpon}.`
    );
  };

  return (
    <div className="avi-role-layout">
      <Sidebar role="nutricionista" />

      <div className="avi-main-frame">
        <Topbar roleLabel="Nutricionista" avatarSrc="/assets/admin-avatar.svg" />
        <ApiMessage message={toast} type="success" onClose={() => setToast('')} />

        <div className="avi-dashboard-content">
          {/* Subtítulo superior y acciones */}
          <div className="avi-page-heading">
            <div className="avi-heading-copy">
              <p className="avi-page-subtitle">
                Asignar un plan nutricional al lote activo de cada galpón y revisar su etapa vigente
              </p>
            </div>

            <div className="avi-worker-header-actions">
              <button
                type="button"
                className="avi-btn-green-cta"
                onClick={() => {
                  setAssignModalDefaults({
                    galpon: 'Galpón 1',
                    planId: activePlans[0]?.id || 'plan-1'
                  });
                  setIsAssignModalOpen(true);
                }}
              >
                <span>＋  Asignar plan a galpón</span>
              </button>
            </div>
          </div>

          {/* 2 Tarjetas KPI de Asignaciones */}
          <div className="avi-metrics-row">
            <StatCard
              label="Lotes activos con plan"
              value={`${computedBarns.length} Lotes`}
              status="100% de galpones asignados"
              statusColor="#2d743b"
              icon="/assets/admin-galpon.svg"
              tileBg="#e6f7e4"
              width="437px"
            />
            <StatCard
              label="Lotes en prórroga"
              value={`${prorrogaCount} Lote(s)`}
              status="Etapa extendida sin solapamiento"
              statusColor="#9e4a0f"
              icon="/assets/admin-inventario.svg"
              tileBg="#f4eee3"
              width="444px"
            />
          </div>

          {/* Formulario directo de Asignación vinculado al Lote */}
          <div className="avi-recent-orders-panel">
            <div className="avi-panel-header avi-panel-header-full">
              <div className="avi-panel-icon-28">
                <img alt="" src="/assets/admin-home-icon.svg" />
              </div>
              <h2 className="avi-panel-title">
                Asignar o actualizar plan del lote activo en galpón
              </h2>
            </div>

            <form
              onSubmit={handleQuickAssignSubmit}
              className="avi-figma-modal-grid"
              style={{ paddingTop: '8px' }}
            >
              <div className="avi-figma-field">
                <label className="avi-figma-label">1. Galpón y lote activo</label>
                <div className="avi-figma-select-wrap">
                  <select
                    className="avi-figma-input avi-figma-select"
                    value={quickAssign.galpon}
                    onChange={(e) => handleSelectBarnInForm(e.target.value)}
                  >
                    {assignedBarnPlans.map((b) => (
                      <option key={b.galpon} value={b.galpon}>
                        {b.galpon} — Lote {b.loteId}
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
                <label className="avi-figma-label">
                  2. Edad actual del lote (días) · {formatAgeWeeks(quickAssign.ageDays)}
                </label>
                <input
                  type="number"
                  min="1"
                  max="90"
                  className="avi-figma-input"
                  value={quickAssign.ageDays}
                  onChange={(e) =>
                    setQuickAssign({
                      ...quickAssign,
                      ageDays: parseInt(e.target.value, 10) || 1
                    })
                  }
                  title="Edad del lote en días"
                  required
                />
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">3. Plan seleccionado</label>
                <div className="avi-figma-select-wrap">
                  <select
                    className="avi-figma-input avi-figma-select"
                    value={quickAssign.planId}
                    onChange={(e) =>
                      setQuickAssign({ ...quickAssign, planId: e.target.value })
                    }
                  >
                    {activePlans.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.name} {p.version > 1 ? `v${p.version}` : ''} ({p.badge})
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
                <label className="avi-figma-label">4. Fecha de inicio del plan</label>
                <input
                  type="date"
                  className="avi-figma-input"
                  value={quickAssign.startDate}
                  onChange={(e) =>
                    setQuickAssign({ ...quickAssign, startDate: e.target.value })
                  }
                  required
                />
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">
                  Etapa que le corresponde (Día {quickAssign.ageDays})
                </label>
                <div className="avi-figma-valid-box">
                  {quickMatchingStage.name} ({formatStageDays(quickMatchingStage)}) ·{' '}
                  {formatRation(quickRationKg)}
                </div>
              </div>

              <div className="avi-figma-field">
                <label className="avi-figma-label">
                  5. Alimento y ración de la etapa
                </label>
                <div
                  className="avi-figma-readonly-box"
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center'
                  }}
                >
                  <strong>
                    {quickMatchingStage.feedType || 'Alimento Balanceado'} · {formatRation(quickRationKg)}
                  </strong>
                  <button type="submit" className="avi-admin-table-btn avi-admin-table-btn-green">
                    Guardar asignación
                  </button>
                </div>
              </div>
            </form>
          </div>

          {/* Tabla de Lotes y Galpones Asignados */}
          <div className="avi-recent-orders-panel">
            <div className="avi-panel-header avi-panel-header-full">
              <div className="avi-panel-icon-28">
                <img alt="" src="/assets/admin-orders-icon.svg" />
              </div>
              <h2 className="avi-panel-title">
                Revisión de etapa y plan por lote en cada galpón
              </h2>
            </div>

            <div className="avi-orders-table">
              <div className="avi-orders-thead">
                <span style={{ flex: '1.2' }}>Galpón y Lote</span>
                <span style={{ flex: '1.2' }}>Edad del lote</span>
                <span style={{ flex: '1.5' }}>Plan asignado</span>
                <span style={{ flex: '1.4' }}>Etapa vigente</span>
                <span style={{ flex: '1.1' }}>Fecha inicio</span>
                <span style={{ flex: '1.4' }}>Ración configurada</span>
                <span style={{ flex: '1.3', textAlign: 'right' }}>Acciones</span>
              </div>

              {computedBarns.map((row) => (
                <div key={row.galpon} className="avi-orders-trow">
                  <div
                    style={{
                      flex: '1.2',
                      display: 'flex',
                      flexDirection: 'column',
                      gap: '2px'
                    }}
                  >
                    <span className="avi-col-dark" style={{ fontWeight: 700 }}>
                      {row.galpon}
                    </span>
                    <span className="avi-col-muted" style={{ fontSize: '12px' }}>
                      Lote {row.loteId}
                    </span>
                  </div>

                  <span className="avi-col-dark" style={{ flex: '1.2' }}>
                    Día {row.ageDays}{' '}
                    <span className="avi-col-muted">{row.ageWeeksText}</span>
                  </span>

                  <span className="avi-col-dark" style={{ flex: '1.45', fontWeight: 600 }}>
                    {row.planName}
                  </span>

                  <div style={{ flex: '1.4', display: 'flex', alignItems: 'center' }}>
                    <span
                      className="avi-barn-status-pill"
                      style={{
                        backgroundColor: row.hasProrroga ? '#ffe8ba' : '#d6f0d1',
                        color: row.hasProrroga ? '#bf5400' : '#1f5929'
                      }}
                    >
                      <span
                        className="avi-barn-status-dot"
                        style={{
                          backgroundColor: row.hasProrroga ? '#f59e0b' : '#2d9f42'
                        }}
                      />
                      {row.currentStageName}
                    </span>
                  </div>

                  <span className="avi-col-dark" style={{ flex: '1.1' }}>
                    {row.startDate}
                  </span>

                  <span className="avi-col-dark" style={{ flex: '1.4', fontWeight: 600 }}>
                    {formatRation(row.rationKg)}
                  </span>

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
                      onClick={() => {
                        setAssignModalDefaults({
                          galpon: row.galpon,
                          planId: row.planId
                        });
                        setIsAssignModalOpen(true);
                      }}
                    >
                      Cambiar plan
                    </button>
                    <button
                      type="button"
                      className="avi-admin-table-btn avi-admin-table-btn-orange"
                      onClick={() => {
                        setAdjustModalGalpon(row.galpon);
                        setIsAdjustModalOpen(true);
                      }}
                    >
                      Prórroga
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      <AssignPlanModal
        isOpen={isAssignModalOpen}
        onClose={() => setIsAssignModalOpen(false)}
        nutriPlans={nutriPlans}
        assignedBarnPlans={assignedBarnPlans}
        sackWeightKg={sackWeightKg}
        initialGalpon={assignModalDefaults.galpon}
        initialPlanId={assignModalDefaults.planId}
        onAssign={(data) => {
          assignNutriPlan(data);
          setToast(`Asignación actualizada en ${data.galpon} (Lote ${data.loteId}).`);
        }}
      />

      <AdjustStageModal
        isOpen={isAdjustModalOpen}
        onClose={() => setIsAdjustModalOpen(false)}
        nutriPlans={nutriPlans}
        assignedBarnPlans={assignedBarnPlans}
        initialGalpon={adjustModalGalpon}
        onAdjust={(data) => {
          adjustNutriStage(data);
          setToast(
            `Prórroga aplicada en ${data.galpon} hasta el día ${data.nuevoDiaFin}.`
          );
        }}
      />
    </div>
  );
}

export default NutritionistAssignmentsPage;
