import { useEffect, useState } from 'react';
import Sidebar from '../components/Sidebar';
import Topbar from '../components/Topbar';
import ApiMessage from '../components/ApiMessage';
import {
  adjustBarnNutritionStage, getBarnNutritionPlan, listTemplates, localBarnId,
  substituteBarnNutritionPlan
} from '../services/nutritionCatalogApi';

const labels = { PRE_INICIO: 'Pre-inicio', INICIO: 'Inicio', ENGORDE: 'Engorde' };
const number = (value) => new Intl.NumberFormat('es-CO', { maximumFractionDigits: 2 }).format(Number(value || 0));

function NutritionistAssignmentsLivePage() {
  const [plan, setPlan] = useState(null);
  const [templates, setTemplates] = useState([]);
  const [selectedTemplateId, setSelectedTemplateId] = useState('');
  const [justification, setJustification] = useState('');
  const [newEndDay, setNewEndDay] = useState('');
  const [adjustmentReason, setAdjustmentReason] = useState('BAJO_PESO');
  const [adjustmentJustification, setAdjustmentJustification] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [toast, setToast] = useState('');
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError('');
    Promise.all([
      getBarnNutritionPlan(localBarnId, { signal: controller.signal }),
      listTemplates({ signal: controller.signal })
    ]).then(([loadedPlan, loadedTemplates]) => {
      setPlan(loadedPlan);
      setTemplates(loadedTemplates.filter((item) => item.activo));
    }).catch((cause) => {
      if (cause.name !== 'AbortError') setError(cause.message);
    }).finally(() => {
      if (!controller.signal.aborted) setLoading(false);
    });
    return () => controller.abort();
  }, [reloadKey]);

  const substitute = async (event) => {
    event.preventDefault();
    if (!selectedTemplateId || !justification.trim()) return;
    setSaving(true);
    setError('');
    try {
      await substituteBarnNutritionPlan(localBarnId, {
        plantillaId: selectedTemplateId,
        justificacion: justification.trim()
      });
      setSelectedTemplateId('');
      setJustification('');
      setToast('El plan del galpón se actualizó correctamente.');
      setReloadKey((value) => value + 1);
    } catch (cause) { setError(cause.message); }
    finally { setSaving(false); }
  };

  const active = plan?.etapas?.find((stage) => stage.estadoEtapa === 'ACTIVA');
  const future = plan?.etapas?.filter((stage) => stage.estadoEtapa === 'FUTURA') || [];

  const adjustStage = async (event) => {
    event.preventDefault();
    const parsedEndDay = Number(newEndDay);
    if (!active || !Number.isInteger(parsedEndDay) || parsedEndDay <= active.diaFin
      || !adjustmentJustification.trim()) return;
    setSaving(true);
    setError('');
    try {
      await adjustBarnNutritionStage(localBarnId, active.id, {
        nuevoDiaFin: parsedEndDay,
        motivo: adjustmentReason,
        justificacion: adjustmentJustification.trim()
      });
      setNewEndDay('');
      setAdjustmentJustification('');
      setToast('La duración se ajustó y la proyección se recalculó.');
      setReloadKey((value) => value + 1);
    } catch (cause) { setError(cause.message); }
    finally { setSaving(false); }
  };

  return (
    <div className="avi-role-layout">
      <Sidebar role="nutricionista" />
      <div className="avi-main-frame">
        <Topbar roleLabel="Nutricionista" avatarSrc="/assets/admin-avatar.svg" />
        <ApiMessage message={toast} type="success" onClose={() => setToast('')} />
        <ApiMessage message={error} type="error" onClose={() => setError('')} />
        <div className="avi-dashboard-content">
          <div className="avi-page-heading">
            <div className="avi-heading-copy">
              <h1 className="avi-page-title avi-title-lg">Plan nutricional del galpón</h1>
              <p className="avi-page-subtitle">Consulta la etapa vigente y sustituye las etapas futuras del lote.</p>
            </div>
          </div>
          {loading && <p className="avi-page-subtitle">Cargando plan nutricional...</p>}
          {plan && (
            <>
              <div className="avi-recent-orders-panel">
                <div className="avi-panel-header avi-panel-header-full">
                  <h2 className="avi-panel-title">Lote {plan.loteId} · {plan.estado}</h2>
                </div>
                <p className="avi-page-subtitle">Salida proyectada: {plan.fechaSalidaProyectada}</p>
                <p className="avi-page-subtitle">Etapa activa: {active ? `${labels[active.etapaCrianza]} · ${number(active.demandaDiariaKg)} kg/día` : 'Ninguna'}</p>
                <div className="avi-orders-table">
                  <div className="avi-orders-thead">
                    <span style={{ flex: 1 }}>Etapa</span><span style={{ flex: 1 }}>Estado</span>
                    <span style={{ flex: 1 }}>Días</span><span style={{ flex: 1 }}>Ración</span>
                    <span style={{ flex: 1 }}>Demanda diaria</span><span style={{ flex: 1 }}>Autonomía</span>
                  </div>
                  {plan.etapas.map((stage) => (
                    <div className="avi-orders-trow" key={stage.id}>
                      <span style={{ flex: 1 }}>{labels[stage.etapaCrianza]}</span>
                      <span style={{ flex: 1 }}>{stage.estadoEtapa}</span>
                      <span style={{ flex: 1 }}>{stage.diaInicio}–{stage.diaFin}</span>
                      <span style={{ flex: 1 }}>{stage.racionKgAveDia} kg/ave/día</span>
                      <span style={{ flex: 1 }}>{number(stage.demandaDiariaKg)} kg</span>
                      <span style={{ flex: 1 }}>{number(stage.diasAutonomia)} días</span>
                    </div>
                  ))}
                </div>
              </div>
              {active && (
                <div className="avi-recent-orders-panel">
                  <div className="avi-panel-header avi-panel-header-full">
                    <h2 className="avi-panel-title">Extender etapa activa · {labels[active.etapaCrianza]}</h2>
                  </div>
                  <p className="avi-page-subtitle">
                    Termina actualmente el día {active.diaFin}. Las etapas futuras y la salida proyectada se desplazarán según el ajuste.
                  </p>
                  <form className="avi-figma-modal-grid" onSubmit={adjustStage}>
                    <div className="avi-figma-field">
                      <label className="avi-figma-label" htmlFor="nutrition-new-end-day">Nuevo día final</label>
                      <input id="nutrition-new-end-day" className="avi-figma-input" type="number"
                        min={active.diaFin + 1} step="1" value={newEndDay}
                        onChange={(event) => setNewEndDay(event.target.value)}
                        placeholder={String(active.diaFin + 1)} required />
                    </div>
                    <div className="avi-figma-field">
                      <label className="avi-figma-label" htmlFor="nutrition-adjustment-reason">Motivo</label>
                      <select id="nutrition-adjustment-reason" className="avi-figma-input"
                        value={adjustmentReason} onChange={(event) => setAdjustmentReason(event.target.value)}>
                        <option value="BAJO_PESO">Bajo peso</option>
                        <option value="CUARENTENA_SANITARIA">Cuarentena sanitaria</option>
                      </select>
                    </div>
                    <div className="avi-figma-field">
                      <label className="avi-figma-label" htmlFor="nutrition-adjustment-justification">Justificación técnica</label>
                      <input id="nutrition-adjustment-justification" className="avi-figma-input"
                        value={adjustmentJustification}
                        onChange={(event) => setAdjustmentJustification(event.target.value)} required />
                    </div>
                    <button className="avi-btn-orange-cta" type="submit" disabled={saving}>
                      {saving ? 'Guardando...' : 'Aplicar ajuste'}
                    </button>
                  </form>
                </div>
              )}
              <div className="avi-recent-orders-panel">
                <div className="avi-panel-header avi-panel-header-full"><h2 className="avi-panel-title">Sustituir etapas futuras</h2></div>
                <p className="avi-page-subtitle">{future.length} etapa(s) futura(s). Al sustituir la plantilla, la etapa activa conserva sus valores.</p>
                <form className="avi-figma-modal-grid" onSubmit={substitute}>
                  <div className="avi-figma-field">
                    <label className="avi-figma-label">Nueva plantilla</label>
                    <select className="avi-figma-input" value={selectedTemplateId}
                      onChange={(event) => setSelectedTemplateId(event.target.value)} required>
                      <option value="">Seleccionar plantilla</option>
                      {templates.map((item) => <option key={item.id} value={item.id}>{item.nombre} ({item.tipoAve})</option>)}
                    </select>
                  </div>
                  <div className="avi-figma-field">
                    <label className="avi-figma-label">Justificación</label>
                    <input className="avi-figma-input" value={justification}
                      onChange={(event) => setJustification(event.target.value)} required />
                  </div>
                  <button className="avi-btn-orange-cta" type="submit" disabled={saving || future.length === 0}>
                    {saving ? 'Guardando...' : 'Sustituir plan'}
                  </button>
                </form>
              </div>
              <div className="avi-recent-orders-panel">
                <div className="avi-panel-header avi-panel-header-full"><h2 className="avi-panel-title">Historial de cambios</h2></div>
                {(plan.historialAjustes || []).map((entry, index) => (
                  <p className="avi-page-subtitle" key={`${entry.fechaRegistro}-${index}`}>
                    {entry.accion} · {entry.actor} · {entry.justificacion || 'Sin justificación'} · {new Date(entry.fechaRegistro).toLocaleString('es-CO')}
                  </p>
                ))}
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

export default NutritionistAssignmentsLivePage;
