import { useEffect, useState } from 'react';
import BaseModal from '../BaseModal';
import StagesEditor from './StagesEditor';
import {
  formatKg,
  formatRation,
  validatePlanData
} from '../../utils/nutritionUtils';

const DEFAULT_STAGES = [
  {
    id: 'st-init-1',
    name: 'Pre-Inicio',
    startDay: 1,
    endDay: 7,
    feedType: 'Alimento A (Pre-iniciador)',
    rationKg: 0.015
  },
  {
    id: 'st-init-2',
    name: 'Inicio',
    startDay: 8,
    endDay: 21,
    feedType: 'Alimento B (Iniciador)',
    rationKg: 0.045
  },
  {
    id: 'st-init-3',
    name: 'Broiler / Engorde',
    startDay: 22,
    endDay: 45,
    feedType: 'Alimento C (Broiler)',
    rationKg: 0.090
  }
];

function PlanFormModal({
  isOpen,
  onClose,
  editingPlan = null,
  assignedBarnsForPlan = [],
  onSave
}) {
  const [form, setForm] = useState({
    name: 'Plan Engorde Acelerado 2026',
    badge: 'Recomendado',
    status: 'activo',
    description: 'Optimizado para galpones comerciales estándar (Ciclo 45 días).',
    stages: DEFAULT_STAGES
  });

  const [createVersion, setCreateVersion] = useState(true);
  const [selectedBarnsForNewVersion, setSelectedBarnsForNewVersion] = useState([]);
  const [attemptedSubmit, setAttemptedSubmit] = useState(false);

  useEffect(() => {
    if (!isOpen) return;
    setAttemptedSubmit(false);

    if (editingPlan) {
      setForm({
        name: editingPlan.name || '',
        badge: editingPlan.badge || 'Recomendado',
        status: editingPlan.status || 'activo',
        description: editingPlan.description || '',
        stages:
          Array.isArray(editingPlan.stages) && editingPlan.stages.length > 0
            ? editingPlan.stages.map((st, idx) => ({
                id: st.id || `st-${idx}`,
                name: st.name || `Etapa ${idx + 1}`,
                startDay: Number(st.startDay) || 1,
                endDay: Number(st.endDay) || 7,
                feedType: st.feedType || 'Alimento A (Pre-iniciador)',
                rationKg: Number(st.rationKg) || 0.015
              }))
            : DEFAULT_STAGES
      });
      setCreateVersion(assignedBarnsForPlan.length > 0);
      setSelectedBarnsForNewVersion(assignedBarnsForPlan.map((b) => b.galpon));
    } else {
      setForm({
        name: 'Plan Engorde Acelerado 2026',
        badge: 'Recomendado',
        status: 'activo',
        description: 'Optimizado para galpones comerciales estándar (Ciclo 45 días).',
        stages: DEFAULT_STAGES
      });
      setCreateVersion(false);
      setSelectedBarnsForNewVersion([]);
    }
  }, [isOpen, editingPlan, assignedBarnsForPlan]);

  const validation = validatePlanData(form);
  const { summary } = validation;
  const isAssignedToBarns = Boolean(editingPlan && assignedBarnsForPlan.length > 0);

  const toggleBarnSelection = (galponName) => {
    setSelectedBarnsForNewVersion((prev) =>
      prev.includes(galponName)
        ? prev.filter((g) => g !== galponName)
        : [...prev, galponName]
    );
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    setAttemptedSubmit(true);
    if (!validation.valid) return;

    const normalizedPlan = {
      id: editingPlan?.id || `plan-${Date.now()}`,
      name: form.name.trim(),
      version: Number(editingPlan?.version) || 1,
      badge: form.badge,
      status: form.status,
      description: form.description.trim(),
      type: form.badge === 'Contingencia' ? 'contingency' : 'standard',
      stages: form.stages.map((st, idx) => ({
        id: st.id || `st-${Date.now()}-${idx}`,
        name: st.name.trim(),
        startDay: Number(st.startDay),
        endDay: Number(st.endDay),
        feedType: st.feedType,
        rationKg: Number(parseFloat(st.rationKg).toFixed(3))
      }))
    };

    onSave({
      plan: normalizedPlan,
      createVersion: isAssignedToBarns ? createVersion : false,
      applyToBarns: isAssignedToBarns && createVersion ? selectedBarnsForNewVersion : []
    });
  };

  return (
    <BaseModal isOpen={isOpen} onClose={onClose} maxWidth="740px">
      <form
        className="avi-figma-modal-720"
        data-node-id="216:1972"
        onSubmit={handleSubmit}
      >
        <div className="avi-figma-modal-header">
          <div className="avi-figma-modal-titles">
            <h2 className="avi-figma-modal-title" style={{ fontSize: '24px', lineHeight: '30px' }}>
              {editingPlan
                ? `Editar plan nutricional${editingPlan.version > 1 ? ` (v${editingPlan.version})` : ''}`
                : 'Nuevo plan nutricional'}
            </h2>
            <p className="avi-figma-modal-subtitle">
              Configuración de etapas consecutivas, tipos de alimento y ración diaria (kg/ave/día)
            </p>
          </div>
          <button
            type="button"
            className="avi-figma-modal-close"
            onClick={onClose}
          >
            ×
          </button>
        </div>

        <div className="avi-figma-modal-grid" style={{ gap: '12px' }}>
          <div
            style={{
              gridColumn: '1 / -1',
              display: 'grid',
              gridTemplateColumns: '1.4fr 1fr 0.9fr',
              gap: '12px'
            }}
          >
            <div className="avi-figma-field">
              <label className="avi-figma-label">Nombre del plan</label>
              <input
                type="text"
                className="avi-figma-input"
                style={{ height: '38px' }}
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                placeholder="Ej. Plan Estándar Broiler"
                required
              />
            </div>

            <div className="avi-figma-field">
              <label className="avi-figma-label">Clasificación</label>
              <div className="avi-figma-select-wrap">
                <select
                  className="avi-figma-input avi-figma-select"
                  style={{ height: '38px' }}
                  value={form.badge}
                  onChange={(e) => setForm({ ...form, badge: e.target.value })}
                >
                  <option value="Recomendado">Recomendado</option>
                  <option value="Alto Rendimiento">Alto Rendimiento</option>
                  <option value="Contingencia">Contingencia</option>
                </select>
                <img
                  alt=""
                  src="/assets/admin-select-chevron.svg"
                  className="avi-figma-select-chevron"
                />
              </div>
            </div>

            <div className="avi-figma-field">
              <label className="avi-figma-label">Estado</label>
              <div className="avi-figma-select-wrap">
                <select
                  className="avi-figma-input avi-figma-select"
                  style={{ height: '38px' }}
                  value={form.status}
                  onChange={(e) => setForm({ ...form, status: e.target.value })}
                >
                  <option value="activo">Activo</option>
                  <option value="borrador">Borrador</option>
                  <option value="archivado">Archivado</option>
                </select>
                <img
                  alt=""
                  src="/assets/admin-select-chevron.svg"
                  className="avi-figma-select-chevron"
                />
              </div>
            </div>
          </div>

          <div className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
            <label className="avi-figma-label">Descripción del régimen</label>
            <input
              type="text"
              className="avi-figma-input"
              style={{ height: '38px' }}
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
              placeholder="Describe el objetivo productivo y duración del régimen..."
              required
            />
          </div>

          {/* Editor compacto de etapas */}
          <StagesEditor
            stages={form.stages}
            onChange={(nextStages) => setForm({ ...form, stages: nextStages })}
          />

          {/* Errores de validación si existen */}
          {(!validation.valid || attemptedSubmit) && validation.errors.length > 0 && (
            <div
              style={{
                gridColumn: '1 / -1',
                backgroundColor: '#ffe0e3',
                border: '1px solid #f5a3ab',
                borderRadius: '10px',
                padding: '10px 14px',
                color: '#88131b',
                fontSize: '12px'
              }}
            >
              <strong style={{ display: 'block', marginBottom: '2px' }}>
                Corrige las siguientes validaciones:
              </strong>
              <ul style={{ margin: 0, paddingLeft: '16px' }}>
                {validation.errors.map((err, i) => (
                  <li key={i}>{err}</li>
                ))}
              </ul>
            </div>
          )}

          {/* Resumen compacto previo a guardar */}
          <div className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
            <div
              className="avi-figma-demand-box"
              style={{
                padding: '10px 14px',
                display: 'grid',
                gridTemplateColumns: 'repeat(4, 1fr)',
                gap: '10px'
              }}
            >
              <div>
                <span style={{ fontSize: '11px', color: '#606367', display: 'block' }}>
                  Etapas
                </span>
                <strong style={{ fontSize: '14px', color: '#090c18' }}>
                  {summary.stagesCount} etapas
                </strong>
              </div>
              <div>
                <span style={{ fontSize: '11px', color: '#606367', display: 'block' }}>
                  Duración del ciclo
                </span>
                <strong style={{ fontSize: '14px', color: '#090c18' }}>
                  Días {summary.firstDay} a {summary.totalCycleDays} ({summary.activeDurationDays}d)
                </strong>
              </div>
              <div>
                <span style={{ fontSize: '11px', color: '#606367', display: 'block' }}>
                  Ración media
                </span>
                <strong style={{ fontSize: '14px', color: '#1b562a' }}>
                  {formatRation(summary.avgRationKg)}
                </strong>
              </div>
              <div>
                <span style={{ fontSize: '11px', color: '#606367', display: 'block' }}>
                  Acumulado / ave
                </span>
                <strong style={{ fontSize: '14px', color: '#9e4a0f' }}>
                  {formatKg(summary.totalFeedPerBirdKg, 2)} / ciclo
                </strong>
              </div>
            </div>
          </div>

          {/* Protección de planes ya asignados a galpones: Versionado */}
          {isAssignedToBarns && (
            <div
              style={{
                gridColumn: '1 / -1',
                backgroundColor: '#fff8eb',
                border: '1px solid #f59e0b',
                borderRadius: '10px',
                padding: '10px 14px',
                display: 'flex',
                flexDirection: 'column',
                gap: '6px'
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '8px' }}>
                <strong style={{ color: '#92400e', fontSize: '13px' }}>
                  Plan asignado a {assignedBarnsForPlan.length}{' '}
                  {assignedBarnsForPlan.length === 1 ? 'galpón' : 'galpones'}
                </strong>
                <label
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                    fontSize: '12px',
                    fontWeight: 600,
                    color: '#1b562a',
                    cursor: 'pointer'
                  }}
                >
                  <input
                    type="checkbox"
                    checked={createVersion}
                    onChange={(e) => setCreateVersion(e.target.checked)}
                  />
                  Crear versión v{(Number(editingPlan?.version) || 1) + 1} y elegir dónde aplicarla
                </label>
              </div>

              {createVersion && (
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                  {assignedBarnsForPlan.map((b) => (
                    <label
                      key={b.galpon}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: '6px',
                        backgroundColor: '#ffffff',
                        border: '1px solid #dfd8ca',
                        borderRadius: '6px',
                        padding: '4px 10px',
                        fontSize: '12px',
                        cursor: 'pointer'
                      }}
                    >
                      <input
                        type="checkbox"
                        checked={selectedBarnsForNewVersion.includes(b.galpon)}
                        onChange={() => toggleBarnSelection(b.galpon)}
                      />
                      <strong>{b.galpon}</strong>
                      <span style={{ color: '#606367' }}>({b.loteId})</span>
                    </label>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>

        <div className="avi-figma-modal-footer">
          <button
            type="button"
            className="avi-figma-btn-cancel"
            onClick={onClose}
          >
            Cancelar
          </button>
          <button
            type="submit"
            className="avi-figma-btn-orange"
            disabled={!validation.valid}
            style={{ opacity: validation.valid ? 1 : 0.6 }}
          >
            {editingPlan
              ? isAssignedToBarns && createVersion
                ? `Guardar como v${(Number(editingPlan.version) || 1) + 1}`
                : 'Guardar cambios'
              : 'Guardar plan'}
          </button>
        </div>
      </form>
    </BaseModal>
  );
}

export default PlanFormModal;
