import { useEffect, useState } from 'react';
import BaseModal from '../BaseModal';
import { formatBirds, formatStageDays } from '../../utils/nutritionUtils';

function AdjustStageModal({
  isOpen,
  onClose,
  nutriPlans = [],
  assignedBarnPlans = [],
  initialGalpon = 'Galpón 2',
  onAdjust
}) {
  const [form, setForm] = useState({
    galpon: initialGalpon,
    stageIndex: 1,
    etapaLabel: 'Inicio Extendido',
    nuevoDiaFin: '26',
    justificacion:
      'Extensión de alimento iniciador por bajo peso registrado en pesaje de semana 2.'
  });

  useEffect(() => {
    if (!isOpen) return;
    const barn =
      assignedBarnPlans.find((b) => b.galpon === initialGalpon) ||
      assignedBarnPlans[0];
    const plan =
      nutriPlans.find((p) => p.id === barn?.planId) || nutriPlans[0];
    const targetStage = plan?.stages?.[1] || plan?.stages?.[0];

    setForm({
      galpon: barn?.galpon || 'Galpón 2',
      stageIndex: plan?.stages?.length > 1 ? 1 : 0,
      etapaLabel: targetStage?.name
        ? `${targetStage.name} Extendido`
        : 'Inicio Extendido',
      nuevoDiaFin: String((Number(targetStage?.endDay) || 21) + 5),
      justificacion:
        barn?.notes ||
        'Extensión de alimento iniciador por bajo peso registrado en pesaje de semana 2.'
    });
  }, [isOpen, initialGalpon, assignedBarnPlans, nutriPlans]);

  const currentBarn =
    assignedBarnPlans.find((b) => b.galpon === form.galpon) ||
    assignedBarnPlans[0];
  const currentPlan =
    nutriPlans.find((p) => p.id === currentBarn?.planId) || nutriPlans[0];
  const stages = currentPlan?.stages || [];
  const chosenStage = stages[form.stageIndex] || stages[0];
  const minEndDay = Number(chosenStage?.startDay) || 1;
  const parsedEndDay = Math.max(minEndDay, parseInt(form.nuevoDiaFin, 10) || 26);

  const handleSubmit = (e) => {
    e.preventDefault();
    onAdjust({
      galpon: form.galpon,
      stageIndex: Number(form.stageIndex) || 0,
      etapaLabel: form.etapaLabel,
      nuevoDiaFin: parsedEndDay,
      justificacion: form.justificacion
    });
    onClose();
  };

  return (
    <BaseModal isOpen={isOpen} onClose={onClose} maxWidth="720px">
      <form
        className="avi-figma-modal-720"
        data-node-id="216:2070"
        onSubmit={handleSubmit}
      >
        <div className="avi-figma-modal-header">
          <div className="avi-figma-modal-titles">
            <h2 className="avi-figma-modal-title">Ajustar duración de etapa</h2>
            <p className="avi-figma-modal-subtitle">
              Contingencia nutricional por peso o cuarentena sanitaria con desplazamiento en cascada
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
          <div className="avi-figma-field">
            <label className="avi-figma-label">Galpón y lote seleccionado</label>
            <div className="avi-figma-select-wrap">
              <select
                className="avi-figma-input avi-figma-select"
                value={form.galpon}
                onChange={(e) => setForm({ ...form, galpon: e.target.value })}
              >
                {assignedBarnPlans.map((b) => (
                  <option key={b.galpon} value={b.galpon}>
                    {b.galpon} — Lote {b.loteId} ({formatBirds(b.liveBirds)})
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
            <label className="avi-figma-label">Etapa del plan a extender</label>
            <div className="avi-figma-select-wrap">
              <select
                className="avi-figma-input avi-figma-select"
                value={form.stageIndex}
                onChange={(e) => {
                  const idx = Number(e.target.value);
                  const st = stages[idx];
                  setForm({
                    ...form,
                    stageIndex: idx,
                    etapaLabel: st ? `${st.name} Extendido` : 'Etapa Extendida',
                    nuevoDiaFin: String((Number(st?.endDay) || 21) + 4)
                  });
                }}
              >
                {stages.map((st, idx) => (
                  <option key={st.id || idx} value={idx}>
                    {st.name} ({formatStageDays(st)})
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
              Nuevo día final de la etapa (Mín. día {minEndDay})
            </label>
            <input
              type="number"
              min={minEndDay}
              max="90"
              className="avi-figma-input"
              value={form.nuevoDiaFin}
              onChange={(e) =>
                setForm({ ...form, nuevoDiaFin: e.target.value })
              }
              required
            />
          </div>

          <div className="avi-figma-field">
            <label className="avi-figma-label">Inicio automático siguiente etapa</label>
            <div className="avi-figma-valid-box">
              Día {parsedEndDay + 1} (Sin solapamiento ni días vacíos)
            </div>
          </div>

          <div className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
            <label className="avi-figma-label">Justificación del ajuste (obligatorio)</label>
            <input
              type="text"
              className="avi-figma-input"
              value={form.justificacion}
              onChange={(e) =>
                setForm({ ...form, justificacion: e.target.value })
              }
              required
            />
          </div>

          <div className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
            <label className="avi-figma-label">Desplazamiento en cascada</label>
            <div className="avi-figma-demand-box">
              La etapa siguiente se desplazará automáticamente para iniciar el{' '}
              <strong>Día {parsedEndDay + 1}</strong> sin superponer ni dejar huecos en el ciclo del lote.
            </div>
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
          <button type="submit" className="avi-figma-btn-orange">
            Aplicar ajuste
          </button>
        </div>
      </form>
    </BaseModal>
  );
}

export default AdjustStageModal;
