import { useEffect, useState } from 'react';
import BaseModal from '../BaseModal';
import {
  findStageForAge,
  formatAgeWeeks,
  formatBirds,
  formatKg,
  formatRation,
  formatSacks,
  formatStageDays
} from '../../utils/nutritionUtils';

function AssignPlanModal({
  isOpen,
  onClose,
  nutriPlans = [],
  assignedBarnPlans = [],
  sackWeightKg = 40,
  initialGalpon = 'Galpón 1',
  initialPlanId = 'plan-1',
  onAssign
}) {
  const activePlans = nutriPlans.filter((p) => p.status !== 'archivado');
  const [form, setForm] = useState({
    galpon: initialGalpon,
    loteId: '#LDP-001',
    liveBirds: 1200,
    ageDays: 25,
    planId: initialPlanId,
    startDate: '2026-11-06',
    notes: ''
  });

  useEffect(() => {
    if (!isOpen) return;
    const targetBarn =
      assignedBarnPlans.find((b) => b.galpon === initialGalpon) ||
      assignedBarnPlans[0];
    const defaultPlan =
      activePlans.find((p) => p.id === (initialPlanId || targetBarn?.planId)) ||
      activePlans[0] ||
      nutriPlans[0];

    setForm({
      galpon: targetBarn?.galpon || 'Galpón 1',
      loteId: targetBarn?.loteId || '#LDP-001',
      liveBirds: Number(targetBarn?.liveBirds) || 1200,
      ageDays: Number(targetBarn?.ageDays) || 25,
      planId: defaultPlan?.id || 'plan-1',
      startDate: targetBarn?.startDate || '2026-11-06',
      notes: targetBarn?.notes || ''
    });
  }, [isOpen, initialGalpon, initialPlanId, assignedBarnPlans, nutriPlans]);

  const handleBarnChange = (nextGalpon) => {
    const found = assignedBarnPlans.find((b) => b.galpon === nextGalpon);
    if (!found) {
      setForm((prev) => ({ ...prev, galpon: nextGalpon }));
      return;
    }
    setForm((prev) => ({
      ...prev,
      galpon: found.galpon,
      loteId: found.loteId,
      liveBirds: Number(found.liveBirds) || 1000,
      ageDays: Number(found.ageDays) || 1,
      planId: prev.planId || found.planId,
      startDate: found.startDate || prev.startDate,
      notes: found.notes || ''
    }));
  };

  const selectedPlan =
    nutriPlans.find((p) => p.id === form.planId) || nutriPlans[0];
  const matchingStage = findStageForAge(selectedPlan, form.ageDays);
  const rationKg = Number(matchingStage?.rationKg) || 0.045;
  const dailyDemandKg = (Number(form.liveBirds) || 0) * rationKg;
  const dailySacks = dailyDemandKg / (Number(sackWeightKg) || 40);

  const handleSubmit = (e) => {
    e.preventDefault();
    onAssign({
      galpon: form.galpon,
      loteId: form.loteId,
      liveBirds: Math.max(1, Number(form.liveBirds) || 1),
      ageDays: Math.max(1, Number(form.ageDays) || 1),
      planId: form.planId,
      startDate: form.startDate,
      notes: form.notes
    });
    onClose();
  };

  return (
    <BaseModal isOpen={isOpen} onClose={onClose} maxWidth="720px">
      <form
        className="avi-figma-modal-720"
        data-node-id="216:2036"
        onSubmit={handleSubmit}
      >
        <div className="avi-figma-modal-header">
          <div className="avi-figma-modal-titles">
            <h2 className="avi-figma-modal-title">
              Asignar plan nutricional a lote en galpón
            </h2>
            <p className="avi-figma-modal-subtitle">
              Vincula el régimen alimenticio al lote activo del galpón y define su etapa vigente
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

        <div className="avi-figma-modal-grid">
          {/* 1. Galpón y lote activo */}
          <div className="avi-figma-field">
            <label className="avi-figma-label">1. Galpón y lote activo</label>
            <div className="avi-figma-select-wrap">
              <select
                className="avi-figma-input avi-figma-select"
                value={form.galpon}
                onChange={(e) => handleBarnChange(e.target.value)}
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

          {/* 2. Edad del lote */}
          <div className="avi-figma-field">
            <label className="avi-figma-label">
              2. Edad actual del lote (días) · {formatAgeWeeks(form.ageDays)}
            </label>
            <input
              type="number"
              min="1"
              max="90"
              className="avi-figma-input"
              value={form.ageDays}
              onChange={(e) =>
                setForm({ ...form, ageDays: parseInt(e.target.value, 10) || 1 })
              }
              required
            />
          </div>

          {/* 3. Plan seleccionado y etapa que le corresponde */}
          <div className="avi-figma-field">
            <label className="avi-figma-label">3. Plan nutricional a aplicar</label>
            <div className="avi-figma-select-wrap">
              <select
                className="avi-figma-input avi-figma-select"
                value={form.planId}
                onChange={(e) => setForm({ ...form, planId: e.target.value })}
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

          {/* 4. Fecha de inicio */}
          <div className="avi-figma-field">
            <label className="avi-figma-label">4. Fecha de inicio de asignación</label>
            <input
              type="date"
              className="avi-figma-input"
              value={form.startDate}
              onChange={(e) => setForm({ ...form, startDate: e.target.value })}
              required
            />
          </div>

          {/* Etapa detectada según edad del lote */}
          <div className="avi-figma-field">
            <label className="avi-figma-label">
              Etapa correspondiente al Día {form.ageDays}
            </label>
            <div className="avi-figma-valid-box">
              {matchingStage.name} ({formatStageDays(matchingStage)})
            </div>
          </div>

          <div className="avi-figma-field">
            <label className="avi-figma-label">Alimento y ración de la etapa</label>
            <div className="avi-figma-readonly-box">
              {matchingStage.feedType || 'Alimento Balanceado'} · {formatRation(rationKg)}
            </div>
          </div>

          <div className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
            <label className="avi-figma-label">Observaciones de la asignación</label>
            <input
              type="text"
              className="avi-figma-input"
              placeholder="Ej. Cambio programado según curva de crecimiento del lote..."
              value={form.notes}
              onChange={(e) => setForm({ ...form, notes: e.target.value })}
            />
          </div>
        </div>

        <div className="avi-figma-modal-footer">
          <button
            type="button"
            className="avi-figma-btn-cancel"
            onClick={onClose}
          >
            Cancelar
          </button>
          <button type="submit" className="avi-figma-btn-green">
            Confirmar asignación al lote {form.loteId}
          </button>
        </div>
      </form>
    </BaseModal>
  );
}

export default AssignPlanModal;
