import { useEffect, useState } from 'react';
import Sidebar from '../components/Sidebar';
import Topbar from '../components/Topbar';
import StatCard from '../components/StatCard';
import ApiMessage from '../components/ApiMessage';
import { getLotFoodRequirement, getSupplyBalance } from '../services/nutritionCatalogApi';

const format = (value) => new Intl.NumberFormat('es-CO', { maximumFractionDigits: 2 }).format(Number(value || 0));
const money = (value) => value == null ? 'Pendiente' : new Intl.NumberFormat('es-CO', {
  style: 'currency', currency: 'COP', maximumFractionDigits: 2
}).format(Number(value));

function NutritionistProjectionsPage() {
  const [requirement, setRequirement] = useState(null);
  const [balance, setBalance] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError('');
    Promise.all([
      getLotFoodRequirement(undefined, { signal: controller.signal }),
      getSupplyBalance({ signal: controller.signal })
    ]).then(([lot, supply]) => {
      setRequirement(lot);
      setBalance(supply);
    }).catch((cause) => {
      if (cause.name !== 'AbortError') setError(cause.message);
    }).finally(() => {
      if (!controller.signal.aborted) setLoading(false);
    });
    return () => controller.abort();
  }, [reloadKey]);

  const projectedKg = requirement?.etapas?.reduce((sum, item) => sum + Number(item.kilogramosProyectados || 0), 0) || 0;
  const missingKg = balance?.alimentos?.reduce((sum, item) => sum + Math.max(0, -Number(item.balanceKg || 0)), 0) || 0;
  const suggestedSacks = balance?.alimentos?.reduce((sum, item) => sum + Number(item.bultosSugeridos || 0), 0) || 0;

  return (
    <div className="avi-role-layout">
      <Sidebar role="nutricionista" />
      <div className="avi-main-frame">
        <Topbar roleLabel="Nutricionista" avatarSrc="/assets/admin-avatar.svg" />
        <ApiMessage message={error} type="error" onClose={() => setError('')} />
        <div className="avi-dashboard-content">
          <div className="avi-page-heading">
            <div className="avi-heading-copy">
              <h1 className="avi-page-title avi-title-lg">Proyecciones y abastecimiento</h1>
              <p className="avi-page-subtitle">Demanda activa del lote y comparación con las existencias.</p>
            </div>
            <button className="avi-btn-orange-cta" type="button" onClick={() => setReloadKey((value) => value + 1)}>
              Actualizar
            </button>
          </div>
          {loading && <p className="avi-page-subtitle">Cargando proyecciones...</p>}
          {requirement && balance && (
            <>
              <div className="avi-metrics-row">
                <StatCard label="Proyección activa" value={`${format(projectedKg)} kg`}
                  status={`Lote ${requirement.codigoLote}`} statusColor="#2d743b"
                  icon="/assets/admin-feed.svg" tileBg="#e6f7e4" width="437px" />
                <StatCard label="Faltante" value={`${format(missingKg)} kg`}
                  status="Demanda menos existencia disponible" statusColor="#9e4a0f"
                  icon="/assets/admin-inventario.svg" tileBg="#f4eee3" width="437px" />
                <StatCard label="Bultos sugeridos" value={format(suggestedSacks)}
                  status="Redondeados al bulto completo" statusColor="#9e4a0f"
                  icon="/assets/admin-orden.svg" tileBg="#f4eee3" width="437px" />
              </div>
              <div className="avi-recent-orders-panel">
                <div className="avi-panel-header avi-panel-header-full">
                  <h2 className="avi-panel-title">Proyección por etapa · {requirement.codigoLote}</h2>
                </div>
                <div className="avi-orders-table">
                  <div className="avi-orders-thead">
                    <span style={{ flex: 1 }}>Etapa</span><span style={{ flex: 1 }}>Estado</span>
                    <span style={{ flex: 1 }}>Días</span><span style={{ flex: 1 }}>Ración</span>
                    <span style={{ flex: 1 }}>Proyección</span><span style={{ flex: 1 }}>Costo/kg</span>
                  </div>
                  {requirement.etapas.map((stage) => (
                    <div className="avi-orders-trow" key={stage.proyeccionId}>
                      <span style={{ flex: 1 }}>{stage.etapaCrianza}</span>
                      <span style={{ flex: 1 }}>{stage.estadoEtapa}</span>
                      <span style={{ flex: 1 }}>{stage.diasEfectivos}</span>
                      <span style={{ flex: 1 }}>{stage.cuotaKgAveDia} kg/ave/día</span>
                      <span style={{ flex: 1 }}>{format(stage.kilogramosProyectados)} kg</span>
                      <span style={{ flex: 1 }}>{money(stage.costoUnitarioKg)}</span>
                    </div>
                  ))}
                </div>
              </div>
              <div className="avi-recent-orders-panel">
                <div className="avi-panel-header avi-panel-header-full"><h2 className="avi-panel-title">Balance de abastecimiento</h2></div>
                <div className="avi-orders-table">
                  <div className="avi-orders-thead">
                    <span style={{ flex: 1 }}>Presentación</span><span style={{ flex: 1 }}>Demanda</span>
                    <span style={{ flex: 1 }}>Existencia</span><span style={{ flex: 1 }}>Balance</span>
                    <span style={{ flex: 1 }}>Estado</span><span style={{ flex: 1 }}>Bultos sugeridos</span>
                  </div>
                  {balance.alimentos.map((item) => (
                    <div className="avi-orders-trow" key={`${item.alimentoId}-${item.pesoNominalPorBultoKg}`}>
                      <span style={{ flex: 1 }}>{item.pesoNominalPorBultoKg} kg/bulto</span>
                      <span style={{ flex: 1 }}>{format(item.demandaConsolidadaKg)} kg</span>
                      <span style={{ flex: 1 }}>{format(item.existenciaDisponibleKg)} kg</span>
                      <span style={{ flex: 1 }}>{format(item.balanceKg)} kg</span>
                      <span style={{ flex: 1 }}>{item.estado === 'REABASTECIMIENTO_NECESARIO' ? 'Reabastecer' : 'Cubierto'}</span>
                      <span style={{ flex: 1 }}>{format(item.bultosSugeridos)}</span>
                    </div>
                  ))}
                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

export default NutritionistProjectionsPage;
