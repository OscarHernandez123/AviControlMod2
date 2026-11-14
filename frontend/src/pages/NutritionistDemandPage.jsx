import { useContext, useState } from 'react';
import { FarmContext } from '../context/FarmContext';
import Sidebar from '../components/Sidebar';
import Topbar from '../components/Topbar';
import StatCard from '../components/StatCard';
import ApiMessage from '../components/ApiMessage';
import DemandTable from '../components/nutritionist/DemandTable';
import AssignPlanModal from '../components/nutritionist/AssignPlanModal';
import AdjustStageModal from '../components/nutritionist/AdjustStageModal';
import {
  computeAssignmentMetrics,
  formatKg,
  formatSacks
} from '../utils/nutritionUtils';

function NutritionistDemandPage() {
  const {
    nutriPlans,
    assignedBarnPlans,
    sackWeightKg,
    setSackWeightKg,
    registerBarnFeedConsumption,
    assignNutriPlan,
    adjustNutriStage
  } = useContext(FarmContext);

  const [toast, setToast] = useState('');

  const [isAssignModalOpen, setIsAssignModalOpen] = useState(false);
  const [assignDefaults, setAssignDefaults] = useState({
    galpon: 'Galpón 1',
    planId: 'plan-1'
  });

  const [isAdjustModalOpen, setIsAdjustModalOpen] = useState(false);
  const [adjustGalpon, setAdjustGalpon] = useState('Galpón 2');

  const computedRows = assignedBarnPlans.map((b) =>
    computeAssignmentMetrics(b, nutriPlans, sackWeightKg)
  );

  const totalEstimatedKg = computedRows.reduce(
    (acc, r) => acc + (Number(r.estimatedDemandKg) || 0),
    0
  );
  const totalEstimatedSacks = totalEstimatedKg / (Number(sackWeightKg) || 40);

  const totalRegisteredKg = computedRows.reduce(
    (acc, r) => acc + (Number(r.registeredConsumptionKg) || 0),
    0
  );
  const totalRegisteredSacks = totalRegisteredKg / (Number(sackWeightKg) || 40);

  // Agrupación de demanda diaria por tipo de alimento
  const demandByFeedType = computedRows.reduce((acc, row) => {
    const key = row.feedType || 'Alimento Balanceado';
    if (!acc[key]) {
      acc[key] = { feedType: key, kg: 0, barnsCount: 0 };
    }
    acc[key].kg += row.estimatedDemandKg;
    acc[key].barnsCount += 1;
    return acc;
  }, {});

  const handleSackWeightChange = (nextWeight) => {
    setSackWeightKg(nextWeight);
    setToast(
      `Peso por bulto actualizado a ${nextWeight} kg en Nutricionista y Trabajador.`
    );
  };

  const handleRegisterConsumption = (galpon, valueKg) => {
    registerBarnFeedConsumption(galpon, valueKg);
    setToast(
      `Consumo suministrado en ${galpon} registrado en ${formatKg(valueKg, 1)}.`
    );
  };

  return (
    <div className="avi-role-layout">
      <Sidebar role="nutricionista" />

      <div className="avi-main-frame">
        <Topbar roleLabel="Nutricionista" avatarSrc="/assets/admin-avatar.svg" />
        <ApiMessage message={toast} type="success" onClose={() => setToast('')} />

        <div className="avi-dashboard-content">
          {/* Encabezado */}
          <div className="avi-page-heading">
            <div className="avi-heading-copy">
              <h1 className="avi-page-title avi-title-lg">
                Demanda de alimento
              </h1>
              <p className="avi-page-subtitle">
                Cálculo dinámico de demanda esperada (aves vivas × ración) frente al consumo registrado en campo
              </p>
            </div>
          </div>

          {/* 3 Tarjetas KPI Calculadas */}
          <div className="avi-metrics-row">
            <StatCard
              label="Demanda estimada (Plan)"
              value={formatKg(totalEstimatedKg, 1)}
              status={`${formatSacks(totalEstimatedSacks, 2)} esperados hoy`}
              statusColor="#2d743b"
              icon="/assets/admin-inventario.svg"
              tileBg="#f4eee3"
              width="437px"
            />
            <StatCard
              label="Consumo registrado hoy"
              value={formatKg(totalRegisteredKg, 1)}
              status={`${formatSacks(totalRegisteredSacks, 2)} suministrados en galpón`}
              statusColor="#9e4a0f"
              icon="/assets/admin-orden.svg"
              tileBg="#f4eee3"
              width="437px"
            />
            <StatCard
              label="Peso de bulto compartido"
              value={`${sackWeightKg} kg`}
              status="Sincronizado con panel Trabajador"
              statusColor="#2d743b"
              icon="/assets/admin-galpon.svg"
              tileBg="#e6f7e4"
              width="444px"
            />
          </div>

          {/* Tabla modular de Demanda Estimada vs Consumo Registrado */}
          <DemandTable
            computedRows={computedRows}
            sackWeightKg={sackWeightKg}
            onChangeSackWeight={handleSackWeightChange}
            onRegisterConsumption={handleRegisterConsumption}
            onOpenAssign={(galpon, planId) => {
              setAssignDefaults({ galpon, planId });
              setIsAssignModalOpen(true);
            }}
            onOpenAdjust={(galpon) => {
              setAdjustGalpon(galpon);
              setIsAdjustModalOpen(true);
            }}
          />

          {/* Resumen de Requerimiento Diario por Tipo de Alimento */}
          <div className="avi-recent-orders-panel">
            <div className="avi-panel-header avi-panel-header-full">
              <div className="avi-panel-icon-28">
                <img alt="" src="/assets/admin-orders-icon.svg" />
              </div>
              <h2 className="avi-panel-title">
                Consolidado diario requerido por tipo de alimento
              </h2>
            </div>

            <div className="avi-worker-barns-grid">
              {Object.values(demandByFeedType).map((item) => {
                const sacks = item.kg / (Number(sackWeightKg) || 40);
                return (
                  <div key={item.feedType} className="avi-worker-barn-card">
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
                          <h3>{item.feedType}</h3>
                          <span>
                            Requerido en {item.barnsCount}{' '}
                            {item.barnsCount === 1 ? 'galpón' : 'galpones'}
                          </span>
                        </div>
                      </div>
                    </div>

                    <div className="avi-nutri-stage-box">
                      <div className="avi-nutri-stage-meta">
                        <strong>Total diario estimado</strong>
                        <span>Equivalencia en bultos de {sackWeightKg} kg</span>
                      </div>
                      <span className="avi-nutri-stage-ration">
                        {formatKg(item.kg, 1)} ({formatSacks(sacks, 2)})
                      </span>
                    </div>
                  </div>
                );
              })}
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
        initialGalpon={assignDefaults.galpon}
        initialPlanId={assignDefaults.planId}
        onAssign={(data) => {
          assignNutriPlan(data);
          setToast(
            `Plan actualizado en ${data.galpon} con recálculo inmediato de demanda.`
          );
        }}
      />

      <AdjustStageModal
        isOpen={isAdjustModalOpen}
        onClose={() => setIsAdjustModalOpen(false)}
        nutriPlans={nutriPlans}
        assignedBarnPlans={assignedBarnPlans}
        initialGalpon={adjustGalpon}
        onAdjust={(data) => {
          adjustNutriStage(data);
          setToast(`Prórroga aplicada en ${data.galpon}.`);
        }}
      />
    </div>
  );
}

export default NutritionistDemandPage;
