import BaseModal from '../BaseModal';
import {
  computePlanSummary,
  formatBirds,
  formatKg,
  formatRation,
  formatStageDays
} from '../../utils/nutritionUtils';

function PlanDetailModal({
  isOpen,
  onClose,
  plan,
  assignedBarns = [],
  onEdit,
  onAssign,
  onToggleArchive
}) {
  if (!plan) return null;

  const summary = computePlanSummary(plan.stages);
  const barnsUsingPlan = assignedBarns.filter((b) => b.planId === plan.id);

  const getStatusStyle = (status) => {
    if (status === 'archivado') {
      return { bg: '#e5e7eb', color: '#4b5563', label: 'Archivado' };
    }
    if (status === 'borrador') {
      return { bg: '#fff1d6', color: '#b45309', label: 'Borrador' };
    }
    return { bg: '#d6f0d1', color: '#1f5929', label: 'Activo' };
  };

  const stStyle = getStatusStyle(plan.status);

  return (
    <BaseModal isOpen={isOpen} onClose={onClose} maxWidth="720px">
      <div className="avi-figma-modal-720">
        <div className="avi-figma-modal-header">
          <div className="avi-figma-modal-titles">
            <h2 className="avi-figma-modal-title">
              Detalle del plan: {plan.name}{' '}
              {plan.version > 1 ? `(v${plan.version})` : ''}
            </h2>
            <p className="avi-figma-modal-subtitle">{plan.description}</p>
          </div>
          <button
            type="button"
            className="avi-figma-modal-close"
            onClick={onClose}
          >
            ×
          </button>
        </div>

        <div className="avi-figma-modal-grid">
          <div className="avi-figma-field">
            <label className="avi-figma-label">Clasificación y versión</label>
            <div className="avi-figma-readonly-box">
              {plan.badge} · Versión {plan.version || 1}
            </div>
          </div>

          <div className="avi-figma-field">
            <label className="avi-figma-label">Estado actual</label>
            <div
              className="avi-figma-valid-box"
              style={{
                backgroundColor: stStyle.bg,
                color: stStyle.color
              }}
            >
              {stStyle.label} ({barnsUsingPlan.length}{' '}
              {barnsUsingPlan.length === 1 ? 'lote asignado' : 'lotes asignados'})
            </div>
          </div>

          <div className="avi-figma-field">
            <label className="avi-figma-label">Duración total del ciclo</label>
            <div className="avi-figma-readonly-box">
              {summary.totalCycleDays} días ({summary.stagesCount} etapas)
            </div>
          </div>

          <div className="avi-figma-field">
            <label className="avi-figma-label">Consumo acumulado estimado / ave</label>
            <div className="avi-figma-readonly-box">
              {formatKg(summary.totalFeedPerBirdKg, 2)} (Media: {formatRation(summary.avgRationKg)})
            </div>
          </div>

          <div className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
            <label className="avi-figma-label">Desglose de etapas y raciones diarias</label>
            <div className="avi-orders-table" style={{ marginTop: '4px' }}>
              <div className="avi-orders-thead">
                <span style={{ flex: '1.1' }}>Etapa</span>
                <span style={{ flex: '1.1' }}>Periodo</span>
                <span style={{ flex: '1.6' }}>Tipo de alimento</span>
                <span style={{ flex: '1.2' }}>Ración diaria</span>
                <span style={{ flex: '1' }}>Total etapa / ave</span>
              </div>
              {plan.stages.map((st, idx) => {
                const dur = Math.max(
                  0,
                  (Number(st.endDay) || 0) - (Number(st.startDay) || 0) + 1
                );
                const stageTotal = dur * (Number(st.rationKg) || 0);
                return (
                  <div key={st.id || idx} className="avi-orders-trow">
                    <span className="avi-col-dark" style={{ flex: '1.1', fontWeight: 700 }}>
                      {st.name}
                    </span>
                    <span className="avi-col-dark" style={{ flex: '1.1' }}>
                      {formatStageDays(st)} ({dur}d)
                    </span>
                    <span className="avi-col-dark" style={{ flex: '1.6' }}>
                      {st.feedType || 'Alimento Balanceado'}
                    </span>
                    <span className="avi-col-dark" style={{ flex: '1.2', fontWeight: 600 }}>
                      {formatRation(st.rationKg)}
                    </span>
                    <span className="avi-col-dark" style={{ flex: '1' }}>
                      {formatKg(stageTotal, 2)}
                    </span>
                  </div>
                );
              })}
            </div>
          </div>

          <div className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
            <label className="avi-figma-label">
              Galpones y lotes que tienen asignado este plan ({barnsUsingPlan.length})
            </label>
            {barnsUsingPlan.length === 0 ? (
              <div className="avi-figma-demand-box">
                Ningún galpón tiene asignado este plan actualmente.
              </div>
            ) : (
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                {barnsUsingPlan.map((b) => (
                  <span
                    key={b.galpon}
                    className="avi-barn-status-pill"
                    style={{ backgroundColor: '#e6f7e4', color: '#1b562a' }}
                  >
                    <strong>{b.galpon}</strong> · Lote {b.loteId} (Día {b.ageDays})
                  </span>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="avi-figma-modal-footer">
          <button
            type="button"
            className="avi-figma-btn-cancel"
            onClick={() => {
              onToggleArchive(
                plan.id,
                plan.status === 'archivado' ? 'activo' : 'archivado'
              );
              onClose();
            }}
          >
            {plan.status === 'archivado' ? 'Reactivar plan' : 'Archivar plan'}
          </button>
          <button
            type="button"
            className="avi-figma-btn-orange"
            onClick={() => {
              onClose();
              onEdit(plan);
            }}
          >
            Editar plan
          </button>
          {plan.status === 'activo' && (
            <button
              type="button"
              className="avi-figma-btn-green"
              onClick={() => {
                onClose();
                onAssign(plan.id);
              }}
            >
              Asignar a galpón
            </button>
          )}
        </div>
      </div>
    </BaseModal>
  );
}

export default PlanDetailModal;
