import { useEffect, useState } from 'react';
import BaseModal from '../BaseModal';

const TYPES = ['PRE_INICIO', 'INICIO', 'ENGORDE'];
const LABELS = { PRE_INICIO: 'Pre-inicio', INICIO: 'Inicio', ENGORDE: 'Engorde' };
const defaultStages = () => TYPES.map((type, index) => ({
  etapaCrianza: type,
  diaInicio: [1, 8, 22][index],
  diaFin: [7, 21, 45][index],
  racionKgAveDia: '',
  tipoAlimento: type,
  pesoNominalPorBultoKg: 40,
  alimentoId: null
}));

function NutritionTemplateFormModal({ isOpen, onClose, template, foods, onSave, saving, error }) {
  const [form, setForm] = useState({ nombre: '', tipoAve: 'BROILER', esPredeterminado: false, etapas: defaultStages() });

  useEffect(() => {
    if (!isOpen) return;
    setForm(template ? {
      nombre: template.nombre,
      tipoAve: template.tipoAve,
      esPredeterminado: template.esPredeterminado,
      etapas: template.etapas.map((stage) => ({ ...stage }))
    } : { nombre: '', tipoAve: 'BROILER', esPredeterminado: false, etapas: defaultStages() });
  }, [isOpen, template]);

  const updateStage = (index, field, value) => {
    setForm((current) => ({
      ...current,
      etapas: current.etapas.map((stage, stageIndex) => {
        if (stageIndex !== index) return stage;
        if (field === 'diaFin') {
          return { ...stage, diaFin: value };
        }
        if (field === 'alimentoId') {
          const selected = foods[stage.etapaCrianza]?.find((food) => food.id === value);
          return { ...stage, alimentoId: value || null,
            pesoNominalPorBultoKg: selected?.pesoNominalPorBultoKg || stage.pesoNominalPorBultoKg };
        }
        return { ...stage, [field]: value };
      })
    }));
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    let nextStart = 1;
    const etapas = form.etapas.map((stage) => {
      const normalized = {
        ...stage,
        diaInicio: nextStart,
        diaFin: Number(stage.diaFin),
        racionKgAveDia: Number(stage.racionKgAveDia),
        pesoNominalPorBultoKg: Number(stage.pesoNominalPorBultoKg),
        alimentoId: stage.alimentoId || null
      };
      nextStart = normalized.diaFin + 1;
      return normalized;
    });
    if (etapas.some((stage) => stage.diaFin < stage.diaInicio || stage.racionKgAveDia <= 0)) return;
    onSave({ ...form, nombre: form.nombre.trim(), tipoAve: form.tipoAve.trim(), etapas });
  };

  if (!isOpen) return null;
  let nextStart = 1;
  return (
    <BaseModal isOpen={isOpen} onClose={onClose} maxWidth="740px">
      <form className="avi-figma-modal-720" onSubmit={handleSubmit}>
        <div className="avi-figma-modal-header">
          <div className="avi-figma-modal-titles">
            <h2 className="avi-figma-modal-title">{template ? 'Editar plantilla nutricional' : 'Nueva plantilla nutricional'}</h2>
            <p className="avi-figma-modal-subtitle">Configura las tres etapas consecutivas del ciclo.</p>
          </div>
          <button type="button" className="avi-figma-modal-close" onClick={onClose}>×</button>
        </div>

        <div className="avi-figma-modal-grid">
          <div className="avi-figma-field">
            <label className="avi-figma-label">Nombre</label>
            <input className="avi-figma-input" value={form.nombre}
              onChange={(e) => setForm({ ...form, nombre: e.target.value })} required />
          </div>
          <div className="avi-figma-field">
            <label className="avi-figma-label">Tipo de ave</label>
            <input className="avi-figma-input" value={form.tipoAve}
              onChange={(e) => setForm({ ...form, tipoAve: e.target.value })}
              disabled={Boolean(template)} required />
          </div>
          <label className="avi-figma-field" style={{ gridColumn: '1 / -1', flexDirection: 'row', alignItems: 'center' }}>
            <input type="checkbox" checked={form.esPredeterminado}
              onChange={(e) => setForm({ ...form, esPredeterminado: e.target.checked })} />
            Plantilla predeterminada
          </label>
          {form.etapas.map((stage, index) => {
            const start = nextStart;
            nextStart = Number(stage.diaFin) + 1;
            return (
              <div key={stage.etapaCrianza} className="avi-figma-demand-box"
                style={{ gridColumn: '1 / -1', display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '10px' }}>
                <strong style={{ gridColumn: '1 / -1' }}>{LABELS[stage.etapaCrianza]} · desde el día {start}</strong>
                <div className="avi-figma-field">
                  <label className="avi-figma-label">Día final</label>
                  <input className="avi-figma-input" type="number" min={start} step="1"
                    value={stage.diaFin} onChange={(e) => updateStage(index, 'diaFin', e.target.value)} required />
                </div>
                <div className="avi-figma-field">
                  <label className="avi-figma-label">Ración (kg/ave/día)</label>
                  <input className="avi-figma-input" type="number" min="0.0001" step="0.0001"
                    value={stage.racionKgAveDia}
                    onChange={(e) => updateStage(index, 'racionKgAveDia', e.target.value)} required />
                </div>
                <div className="avi-figma-field">
                  <label className="avi-figma-label">Alimento comercial (opcional)</label>
                  <select className="avi-figma-input" value={stage.alimentoId || ''}
                    onChange={(e) => updateStage(index, 'alimentoId', e.target.value)}>
                    <option value="">Pendiente de selección</option>
                    {(foods[stage.etapaCrianza] || []).map((food) => (
                      <option key={food.id} value={food.id}>{food.nombre} · {food.pesoNominalPorBultoKg} kg/bulto</option>
                    ))}
                  </select>
                </div>
                <div className="avi-figma-field">
                  <label className="avi-figma-label">Peso nominal por bulto (kg)</label>
                  <input className="avi-figma-input" type="number" min="0.0001" step="0.0001"
                    value={stage.pesoNominalPorBultoKg}
                    onChange={(e) => updateStage(index, 'pesoNominalPorBultoKg', e.target.value)}
                    disabled={Boolean(stage.alimentoId)} required />
                </div>
              </div>
            );
          })}
        </div>
        <div className="avi-figma-modal-footer">
          {error && <p style={{ color: '#b91c1c', marginRight: 'auto' }}>{error}</p>}
          <button type="button" className="avi-figma-btn-cancel" onClick={onClose}>Cancelar</button>
          <button type="submit" className="avi-figma-btn-orange" disabled={saving}>
            {saving ? 'Guardando...' : template ? 'Guardar cambios' : 'Crear plantilla'}
          </button>
        </div>
      </form>
    </BaseModal>
  );
}

export default NutritionTemplateFormModal;
