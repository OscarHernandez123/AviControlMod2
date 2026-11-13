import { useState } from 'react';
import BaseModal from '../BaseModal';

const emptyForm = {
  nombre: '', tipoAlimento: 'PRE_INICIO', pesoNominalPorBultoKg: '',
  proteinaPorcentaje: '', energiaKcalKg: '', duracionRecomendadaEtapaDias: ''
};

function CommercialFoodFormModal({ isOpen, onClose, onSave, saving, error }) {
  const [form, setForm] = useState(emptyForm);
  const submit = async (event) => {
    event.preventDefault();
    const saved = await onSave({
      id: crypto.randomUUID(),
      nombre: form.nombre.trim(),
      tipoAlimento: form.tipoAlimento,
      pesoNominalPorBultoKg: Number(form.pesoNominalPorBultoKg),
      proteinaPorcentaje: Number(form.proteinaPorcentaje),
      energiaKcalKg: Number(form.energiaKcalKg),
      duracionRecomendadaEtapaDias: form.duracionRecomendadaEtapaDias
        ? Number(form.duracionRecomendadaEtapaDias) : null
    });
    if (saved) setForm(emptyForm);
  };

  return (
    <BaseModal isOpen={isOpen} onClose={onClose} maxWidth="720px">
      <form className="avi-figma-modal-720" onSubmit={submit}>
        <div className="avi-figma-modal-header">
          <div className="avi-figma-modal-titles">
            <h2 className="avi-figma-modal-title">Registrar alimento comercial</h2>
            <p className="avi-figma-modal-subtitle">El identificador se genera automáticamente.</p>
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
            <label className="avi-figma-label">Etapa</label>
            <select className="avi-figma-input" value={form.tipoAlimento}
              onChange={(e) => setForm({ ...form, tipoAlimento: e.target.value })}>
              <option value="PRE_INICIO">Pre-inicio</option>
              <option value="INICIO">Inicio</option>
              <option value="ENGORDE">Engorde</option>
            </select>
          </div>
          {[
            ['pesoNominalPorBultoKg', 'Peso nominal por bulto (kg)', '0.0001', '0.0001'],
            ['proteinaPorcentaje', 'Proteína (%)', '0', '0.01'],
            ['energiaKcalKg', 'Energía (kcal/kg)', '0.01', '0.01'],
            ['duracionRecomendadaEtapaDias', 'Duración recomendada (días, opcional)', '1', '1']
          ].map(([key, label, min, step]) => (
            <div className="avi-figma-field" key={key}>
              <label className="avi-figma-label">{label}</label>
              <input className="avi-figma-input" type="number" min={min} step={step}
                max={key === 'proteinaPorcentaje' ? 100 : undefined}
                value={form[key]} onChange={(e) => setForm({ ...form, [key]: e.target.value })}
                required={key !== 'duracionRecomendadaEtapaDias'} />
            </div>
          ))}
        </div>
        <div className="avi-figma-modal-footer">
          {error && <p style={{ color: '#b91c1c', marginRight: 'auto' }}>{error}</p>}
          <button type="button" className="avi-figma-btn-cancel" onClick={onClose}>Cancelar</button>
          <button type="submit" className="avi-figma-btn-orange" disabled={saving}>
            {saving ? 'Guardando...' : 'Registrar alimento'}
          </button>
        </div>
      </form>
    </BaseModal>
  );
}

export default CommercialFoodFormModal;
