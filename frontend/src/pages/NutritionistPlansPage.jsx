import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import Sidebar from '../components/Sidebar';
import Topbar from '../components/Topbar';
import StatCard from '../components/StatCard';
import ApiMessage from '../components/ApiMessage';
import BaseModal from '../components/BaseModal';
import NutritionTemplateFormModal from '../components/nutritionist/NutritionTemplateFormModal';
import CommercialFoodFormModal from '../components/nutritionist/CommercialFoodFormModal';
import {
  createCommercialFood, createTemplate, getTemplate, listCompatibleFoods,
  listTemplates, updateTemplate
} from '../services/nutritionCatalogApi';
import {
  computePlanSummary,
  formatKg,
  formatRation,
  formatStageDays
} from '../utils/nutritionUtils';

const TYPES = ['PRE_INICIO', 'INICIO', 'ENGORDE'];
const LABELS = { PRE_INICIO: 'Pre-inicio', INICIO: 'Inicio', ENGORDE: 'Engorde' };
const emptyFoods = { PRE_INICIO: [], INICIO: [], ENGORDE: [] };

function NutritionistPlansPage() {

  const location = useLocation();
  const navigate = useNavigate();

  const [toast, setToast] = useState('');
  const [error, setError] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [typeFilter, setTypeFilter] = useState('Todos');
  const [statusFilter, setStatusFilter] = useState('Todos');
  const [templates, setTemplates] = useState([]);
  const [foods, setFoods] = useState(emptyFoods);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);
  const [isPlanModalOpen, setIsPlanModalOpen] = useState(false);
  const [editingPlan, setEditingPlan] = useState(null);
  const [detailPlan, setDetailPlan] = useState(null);
  const [isFoodModalOpen, setIsFoodModalOpen] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError('');
    Promise.all([
      listTemplates({ signal: controller.signal }),
      ...TYPES.map((type) => listCompatibleFoods(type, { signal: controller.signal }))
    ]).then(([loadedTemplates, ...foodsByType]) => {
      setTemplates(loadedTemplates);
      setFoods(Object.fromEntries(TYPES.map((type, index) => [type, foodsByType[index]])));
    }).catch((cause) => {
      if (cause.name !== 'AbortError') setError(cause.message);
    }).finally(() => {
      if (!controller.signal.aborted) setLoading(false);
    });
    return () => controller.abort();
  }, [reloadKey]);

  useEffect(() => {
    const navState = location.state;
    if (navState?.action === 'new') {
      setEditingPlan(null);
      setIsPlanModalOpen(true);
    } else if (navState?.action === 'edit') {
      const target = templates.find((plan) => plan.id === navState.planId);
      if (target) {
        setEditingPlan(target);
        setIsPlanModalOpen(true);
      }
    }
    if (navState?.action) navigate(location.pathname, { replace: true, state: null });
  }, [location.state, location.pathname, navigate, templates]);

  const handleOpenNewPlan = () => {
    setEditingPlan(null);
    setIsPlanModalOpen(true);
  };

  const handleOpenEditPlan = (plan) => {
    setEditingPlan(plan);
    setIsPlanModalOpen(true);
  };

  const handleOpenDetail = async (plan) => {
    setError('');
    try { setDetailPlan(await getTemplate(plan.id)); }
    catch (cause) { setError(cause.message); }
  };

  const handleSavePlan = async (data) => {
    setSaving(true);
    setError('');
    try {
      const saved = editingPlan ? await updateTemplate(editingPlan.id, data) : await createTemplate(data);
      setIsPlanModalOpen(false);
      setEditingPlan(null);
      setToast(`Plantilla "${saved.nombre}" guardada correctamente.`);
      setReloadKey((value) => value + 1);
    } catch (cause) { setError(cause.message); }
    finally { setSaving(false); }
  };

  const handleSaveFood = async (data) => {
    setSaving(true);
    setError('');
    try {
      const saved = await createCommercialFood(data);
      setIsFoodModalOpen(false);
      setToast(`Alimento "${saved.nombre}" registrado correctamente.`);
      setReloadKey((value) => value + 1);
      return true;
    } catch (cause) { setError(cause.message); return false; }
    finally { setSaving(false); }
  };

  const filteredPlans = templates.filter((plan) => {
    const q = searchQuery.toLowerCase();
    const matchesSearch =
      plan.nombre.toLowerCase().includes(q) ||
      plan.tipoAve.toLowerCase().includes(q) ||
      plan.etapas.some((stage) => stage.tipoAlimento.toLowerCase().includes(q));
    const matchesType = typeFilter === 'Todos' || plan.tipoAve === typeFilter;
    const matchesStatus = statusFilter === 'Todos' || (statusFilter === 'activo' ? plan.activo : !plan.activo);
    return matchesSearch && matchesType && matchesStatus;
  });

  const activeCount = templates.filter((plan) => plan.activo).length;
  const inactiveCount = templates.length - activeCount;

  const avgCycleDays =
    activeCount > 0
      ? Math.round(
          templates
            .filter((plan) => plan.activo)
            .reduce(
              (acc, plan) => acc + Math.max(...plan.etapas.map((stage) => stage.diaFin)),
              0
            ) / activeCount
        )
      : 0;

  return (
    <div className="avi-role-layout">
      <Sidebar role="nutricionista" />

      <div
        className="avi-main-frame"
        data-node-id="216:1723"
        data-name="Nutricionista Planes"
      >
        <Topbar roleLabel="Nutricionista" avatarSrc="/assets/admin-avatar.svg" />
        <ApiMessage message={toast} type="success" onClose={() => setToast('')} />
        <ApiMessage message={error} type="error" onClose={() => setError('')} />

        <div className="avi-dashboard-content" data-node-id="216:1729">
          {/* Subtítulo superior y botón de acción */}
          <div className="avi-page-heading" data-node-id="216:1742">
            <div className="avi-heading-copy">
              <p className="avi-page-subtitle" data-node-id="216:1747">
                Crear y editar plantillas de tres etapas con alimentos del catálogo
              </p>
            </div>

            <div className="avi-worker-header-actions" data-node-id="216:1748">
              <button type="button" className="avi-admin-table-btn avi-admin-table-btn-outline"
                onClick={() => setIsFoodModalOpen(true)}>
                ＋ Registrar alimento
              </button>
              <button
                type="button"
                className="avi-btn-orange-cta"
                onClick={handleOpenNewPlan}
                data-node-id="216:1749"
              >
                <span>＋ Nueva plantilla</span>
              </button>
            </div>
          </div>

          {/* 2 Tarjetas KPI Exclusivas de Planes Nutricionales */}
          <div className="avi-metrics-row" data-node-id="216:1760">
            <StatCard
              label="Plantillas activas"
              value={`${activeCount} Plantillas`}
              status={`${inactiveCount} inactivas`}
              statusColor="#2d743b"
              icon="/assets/admin-orden.svg"
              tileBg="#f4eee3"
              width="437px"
            />
            <StatCard
              label="Duración media de ciclo"
              value={`${avgCycleDays} Días`}
              status="Secuencia validada sin solapamientos"
              statusColor="#9e4a0f"
              icon="/assets/admin-inventario.svg"
              tileBg="#f4eee3"
              width="444px"
            />
          </div>

          {/* Barra de Filtrado y Búsqueda con el mismo diseño de Recepciones */}
          <div className="avi-receptions-filter-bar" data-node-id="216:2945">
            <input
              type="text"
              className="avi-receptions-search-input"
              placeholder="Buscar por plantilla, ave o etapa"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />

            <select
              className="avi-receptions-select"
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value)}
            >
              <option value="Todos">Todos los tipos de ave</option>
              {[...new Set(templates.map((plan) => plan.tipoAve))].map((type) => (
                <option key={type} value={type}>{type}</option>
              ))}
            </select>

            <select
              className="avi-receptions-select"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="Todos">Todos los estados  ▾</option>
              <option value="activo">Activos</option>
              <option value="inactivo">Inactivas</option>
            </select>
          </div>

          {/* CATÁLOGO DE PLANES NUTRICIONALES */}
          <div className="avi-recent-orders-panel" data-node-id="216:1785">
            <div className="avi-panel-header avi-panel-header-full" data-node-id="216:1786">
              <div className="avi-panel-icon-28">
                <img alt="" src="/assets/admin-orders-icon.svg" />
              </div>
              <h2 className="avi-panel-title">Plantillas nutricionales</h2>
              <div className="avi-panel-spacer" />
              <span className="avi-worker-operator-tag">
                Mostrando {filteredPlans.length} de {templates.length} plantillas
              </span>
            </div>

            <div className="avi-worker-barns-grid" data-node-id="216:1790">
              {filteredPlans.map((plan) => {
                const stages = plan.etapas.map((stage) => ({
                  id: stage.etapaCrianza,
                  name: LABELS[stage.etapaCrianza],
                  startDay: stage.diaInicio,
                  endDay: stage.diaFin,
                  rationKg: stage.racionKgAveDia,
                  feedType: stage.alimentoId
                    ? foods[stage.tipoAlimento]?.find((item) => item.id === stage.alimentoId)?.nombre || stage.tipoAlimento
                    : `${LABELS[stage.tipoAlimento]} · pendiente de alimento`
                }));
                const summary = computePlanSummary(stages);

                return (
                  <div
                    key={plan.id}
                    className="avi-worker-barn-card"
                    style={{ opacity: plan.activo ? 1 : 0.78 }}
                  >
                    <div className="avi-worker-barn-top">
                      <div className="avi-crit-item-left">
                        <div
                          className="avi-crit-icon-tile"
                          style={{ backgroundColor: '#e6f7e4' }}
                        >
                          <div className="avi-crit-icon-26">
                            <img alt="" src="/assets/admin-feed.svg" />
                          </div>
                        </div>
                        <div className="avi-worker-barn-titles">
                          <h3>
                            {plan.nombre}
                          </h3>
                          <span>
                            Ciclo de {summary.totalCycleDays} días · {stages.length} etapas
                          </span>
                        </div>
                      </div>

                      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '4px' }}>
                        <span
                          className="avi-barn-status-pill"
                          style={{
                            backgroundColor: '#d6f0d1',
                            color: '#1f5929'
                          }}
                        >
                          <span
                            className="avi-barn-status-dot"
                            style={{ backgroundColor: '#2d9f42' }}
                          />
                          {plan.tipoAve}
                        </span>
                        <span
                          style={{
                            fontSize: '11px',
                            fontWeight: 600,
                            padding: '2px 8px',
                            borderRadius: '999px',
                            backgroundColor: plan.activo ? '#e6f7e4' : '#e5e7eb',
                            color: plan.activo ? '#1b562a' : '#4b5563'
                          }}
                        >
                          {plan.activo ? 'Activo' : 'Inactivo'}
                        </span>
                      </div>
                    </div>

                    <p className="avi-nutri-plan-desc">
                      {plan.esPredeterminado ? 'Plantilla predeterminada' : 'Plantilla personalizada'}
                    </p>

                    <div className="avi-nutri-stages-stack">
                      {stages.map((st, idx) => (
                        <div key={st.id || idx} className="avi-nutri-stage-box">
                          <div className="avi-nutri-stage-meta">
                            <strong>{st.name}</strong>
                            <span>
                              {formatStageDays(st)} · {st.feedType || 'Alimento Balanceado'}
                            </span>
                          </div>
                          <span className="avi-nutri-stage-ration">
                            {formatRation(st.rationKg)}
                          </span>
                        </div>
                      ))}
                    </div>

                    {/* Resumen rápido del plan en tarjeta */}
                    <div
                      style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        fontSize: '12px',
                        color: '#5d4037',
                        backgroundColor: '#faf6ee',
                        padding: '8px 12px',
                        borderRadius: '8px',
                        border: '1px solid #ece5d5'
                      }}
                    >
                      <span>
                        Acumulado ciclo: <strong>{formatKg(summary.totalFeedPerBirdKg, 2)}/ave</strong>
                      </span>
                      <span>
                        Media: <strong>{formatRation(summary.avgRationKg)}</strong>
                      </span>
                    </div>

                    <div className="avi-worker-barn-actions" style={{ flexWrap: 'wrap' }}>
                      <button
                        type="button"
                        className="avi-admin-table-btn avi-admin-table-btn-outline"
                        onClick={() => handleOpenDetail(plan)}
                      >
                        Detalle
                      </button>
                      <button
                        type="button"
                        className="avi-admin-table-btn avi-admin-table-btn-orange"
                        style={{ flex: 1 }}
                        onClick={() => handleOpenEditPlan(plan)}
                      >
                        Editar plan
                      </button>
                    </div>
                  </div>
                );
              })}
              {loading && <p className="avi-page-subtitle">Cargando plantillas...</p>}
              {!loading && filteredPlans.length === 0 && (
                <p className="avi-page-subtitle">No hay plantillas para los filtros seleccionados.</p>
              )}
            </div>
          </div>
          <div className="avi-recent-orders-panel">
            <div className="avi-panel-header avi-panel-header-full">
              <h2 className="avi-panel-title">Alimentos comerciales</h2>
            </div>
            <div className="avi-orders-table">
              <div className="avi-orders-thead">
                <span style={{ flex: 1 }}>Etapa</span>
                <span style={{ flex: 2 }}>Alimento</span>
                <span style={{ flex: 1 }}>Bulto</span>
                <span style={{ flex: 1 }}>Proteína</span>
                <span style={{ flex: 1 }}>Stock</span>
              </div>
              {TYPES.flatMap((type) => foods[type].map((food) => (
                <div className="avi-orders-trow" key={food.id}>
                  <span style={{ flex: 1 }}>{LABELS[type]}</span>
                  <span style={{ flex: 2 }}>{food.nombre}</span>
                  <span style={{ flex: 1 }}>{food.pesoNominalPorBultoKg} kg</span>
                  <span style={{ flex: 1 }}>{food.proteinaPorcentaje} %</span>
                  <span style={{ flex: 1 }}>{food.stockDisponibleKg} kg</span>
                </div>
              )))}
              {!loading && TYPES.every((type) => foods[type].length === 0) && (
                <p className="avi-page-subtitle">No hay alimentos comerciales registrados.</p>
              )}
            </div>
          </div>
        </div>
      </div>

      <NutritionTemplateFormModal
        isOpen={isPlanModalOpen}
        onClose={() => setIsPlanModalOpen(false)}
        template={editingPlan}
        foods={foods}
        onSave={handleSavePlan}
        saving={saving}
        error={error}
      />
      <CommercialFoodFormModal
        isOpen={isFoodModalOpen}
        onClose={() => setIsFoodModalOpen(false)}
        onSave={handleSaveFood}
        saving={saving}
        error={error}
      />
      <BaseModal isOpen={Boolean(detailPlan)} onClose={() => setDetailPlan(null)} maxWidth="720px">
        {detailPlan && (
          <div className="avi-figma-modal-720">
            <div className="avi-figma-modal-header">
              <div className="avi-figma-modal-titles">
                <h2 className="avi-figma-modal-title">{detailPlan.nombre}</h2>
                <p className="avi-figma-modal-subtitle">{detailPlan.tipoAve} · {detailPlan.activo ? 'Activa' : 'Inactiva'}</p>
              </div>
              <button type="button" className="avi-figma-modal-close" onClick={() => setDetailPlan(null)}>×</button>
            </div>
            <div className="avi-figma-modal-grid">
              {detailPlan.etapas.map((stage) => (
                <div key={stage.etapaCrianza} className="avi-figma-demand-box" style={{ gridColumn: '1 / -1' }}>
                  <strong>{LABELS[stage.etapaCrianza]}</strong>
                  <p>Días {stage.diaInicio} a {stage.diaFin} · {formatRation(stage.racionKgAveDia)}</p>
                  <p>Alimento: {stage.alimentoId
                    ? foods[stage.tipoAlimento]?.find((food) => food.id === stage.alimentoId)?.nombre || stage.alimentoId
                    : 'Pendiente de selección'} · Bulto de {stage.pesoNominalPorBultoKg} kg</p>
                </div>
              ))}
            </div>
            <div className="avi-figma-modal-footer">
              <button type="button" className="avi-figma-btn-cancel" onClick={() => setDetailPlan(null)}>Cerrar</button>
            </div>
          </div>
        )}
      </BaseModal>
    </div>
  );
}

export default NutritionistPlansPage;
