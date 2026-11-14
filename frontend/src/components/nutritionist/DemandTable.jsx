import { useState } from 'react';
import {
  formatBirds,
  formatKg,
  formatRation,
  formatSacks
} from '../../utils/nutritionUtils';

function DemandTable({
  computedRows = [],
  sackWeightKg = 40,
  onChangeSackWeight,
  onRegisterConsumption,
  onOpenAssign,
  onOpenAdjust
}) {
  const [editingBarn, setEditingBarn] = useState(null);
  const [tempConsumption, setTempConsumption] = useState('');

  const totalBirds = computedRows.reduce(
    (acc, r) => acc + (Number(r.liveBirds) || 0),
    0
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

  const handleStartEditConsumption = (row) => {
    setEditingBarn(row.galpon);
    setTempConsumption(String(Number(row.registeredConsumptionKg).toFixed(1)));
  };

  const handleSaveConsumption = (galpon) => {
    const val = parseFloat(tempConsumption);
    if (!Number.isNaN(val) && val >= 0 && onRegisterConsumption) {
      onRegisterConsumption(galpon, val);
    }
    setEditingBarn(null);
  };

  return (
    <div className="avi-recent-orders-panel">
      <div className="avi-panel-header avi-panel-header-full">
        <div className="avi-panel-icon-28">
          <img alt="" src="/assets/admin-inv-panel.svg" />
        </div>
        <h2 className="avi-panel-title">
          Demanda diaria estimada vs. consumo registrado por galpón y lote
        </h2>
        <div className="avi-panel-spacer" />

        {onChangeSackWeight && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '13px', fontWeight: 600, color: '#1b562a' }}>
              Peso por bulto (compartido):
            </span>
            {[40, 50].map((w) => (
              <button
                key={w}
                type="button"
                onClick={() => onChangeSackWeight(w)}
                className={
                  Number(sackWeightKg) === w
                    ? 'avi-admin-table-btn avi-admin-table-btn-green'
                    : 'avi-admin-table-btn avi-admin-table-btn-outline'
                }
                style={{ minWidth: '64px', height: '30px' }}
              >
                {w} kg
              </button>
            ))}
          </div>
        )}
      </div>

      <div className="avi-orders-table">
        <div className="avi-orders-thead">
          <span style={{ flex: '1.15' }}>Galpón / Lote</span>
          <span style={{ flex: '1.1' }}>Aves vivas · Edad</span>
          <span style={{ flex: '1.4' }}>Plan y Etapa vigente</span>
          <span style={{ flex: '1.05' }}>Ración / ave</span>
          <span style={{ flex: '1.35' }}>Demanda estimada (Plan)</span>
          <span style={{ flex: '1.45' }}>Consumo registrado hoy</span>
          <span style={{ flex: '1.2', textAlign: 'right' }}>Acciones</span>
        </div>

        {computedRows.map((row) => {
          const diff = Number(row.diffKg) || 0;
          const isExact = Math.abs(diff) < 0.2;
          return (
            <div key={row.galpon} className="avi-orders-trow">
              {/* Galpón y Lote */}
              <div
                style={{
                  flex: '1.15',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '2px'
                }}
              >
                <span className="avi-col-dark" style={{ fontWeight: 700 }}>
                  {row.galpon}
                </span>
                <span className="avi-col-muted" style={{ fontSize: '12px' }}>
                  Lote {row.loteId} · Inicio {row.startDate}
                </span>
              </div>

              {/* Aves vivas y edad */}
              <div
                style={{
                  flex: '1.1',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '2px'
                }}
              >
                <span className="avi-col-dark" style={{ fontWeight: 600 }}>
                  {formatBirds(row.liveBirds)}
                </span>
                <span className="avi-col-muted" style={{ fontSize: '12px' }}>
                  Día {row.ageDays} {row.ageWeeksText}
                </span>
              </div>

              {/* Plan y Etapa */}
              <div
                style={{
                  flex: '1.4',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '4px',
                  alignItems: 'flex-start'
                }}
              >
                <span className="avi-col-dark" style={{ fontWeight: 600 }}>
                  {row.planName}
                </span>
                <span
                  className="avi-barn-status-pill"
                  style={{
                    backgroundColor: row.hasProrroga ? '#ffe8ba' : '#d6f0d1',
                    color: row.hasProrroga ? '#bf5400' : '#1f5929'
                  }}
                >
                  <span
                    className="avi-barn-status-dot"
                    style={{
                      backgroundColor: row.hasProrroga ? '#f59e0b' : '#2d9f42'
                    }}
                  />
                  {row.currentStageName}
                </span>
              </div>

              {/* Ración configurada */}
              <div
                style={{
                  flex: '1.05',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '2px'
                }}
              >
                <span className="avi-col-dark" style={{ fontWeight: 600 }}>
                  {formatRation(row.rationKg)}
                </span>
                <span className="avi-col-muted" style={{ fontSize: '11px' }}>
                  {row.feedType}
                </span>
              </div>

              {/* Demanda estimada (calculada del plan) */}
              <div
                style={{
                  flex: '1.35',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '2px'
                }}
              >
                <strong style={{ color: '#090c18', fontSize: '14px' }}>
                  {formatKg(row.estimatedDemandKg, 1)} / día
                </strong>
                <span style={{ fontSize: '12px', color: '#1b562a', fontWeight: 600 }}>
                  ({formatSacks(row.estimatedSacks, 2)} de {sackWeightKg} kg)
                </span>
              </div>

              {/* Consumo registrado en galpón */}
              <div
                style={{
                  flex: '1.45',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '2px'
                }}
              >
                {editingBarn === row.galpon ? (
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                    <input
                      type="number"
                      step="0.1"
                      min="0"
                      className="avi-figma-input"
                      style={{ width: '90px', height: '30px', padding: '0 8px' }}
                      value={tempConsumption}
                      onChange={(e) => setTempConsumption(e.target.value)}
                    />
                    <button
                      type="button"
                      className="avi-admin-table-btn avi-admin-table-btn-green"
                      style={{ height: '30px', padding: '0 10px' }}
                      onClick={() => handleSaveConsumption(row.galpon)}
                    >
                      OK
                    </button>
                  </div>
                ) : (
                  <>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <strong style={{ color: '#090c18', fontSize: '14px' }}>
                        {formatKg(row.registeredConsumptionKg, 1)}
                      </strong>
                      {onRegisterConsumption && (
                        <button
                          type="button"
                          onClick={() => handleStartEditConsumption(row)}
                          style={{
                            border: '1px solid #dfd8ca',
                            background: '#faf6ee',
                            borderRadius: '6px',
                            fontSize: '11px',
                            padding: '2px 6px',
                            cursor: 'pointer',
                            color: '#5d4037'
                          }}
                        >
                          Registrar
                        </button>
                      )}
                    </div>
                    <span
                      style={{
                        fontSize: '12px',
                        color: isExact ? '#2d743b' : '#b45309',
                        fontWeight: 500
                      }}
                    >
                      {formatSacks(row.registeredSacks, 2)} ·{' '}
                      {isExact
                        ? 'Cumple estimado'
                        : `${diff > 0 ? '+' : ''}${formatKg(diff, 1)} vs plan`}
                    </span>
                  </>
                )}
              </div>

              {/* Acciones */}
              <div
                style={{
                  flex: '1.2',
                  display: 'flex',
                  justifyContent: 'flex-end',
                  gap: '6px'
                }}
              >
                {onOpenAssign && (
                  <button
                    type="button"
                    className="avi-admin-table-btn avi-admin-table-btn-green"
                    onClick={() => onOpenAssign(row.galpon, row.planId)}
                  >
                    Cambiar plan
                  </button>
                )}
                {onOpenAdjust && (
                  <button
                    type="button"
                    className="avi-admin-table-btn avi-admin-table-btn-orange"
                    onClick={() => onOpenAdjust(row.galpon)}
                  >
                    Prórroga
                  </button>
                )}
              </div>
            </div>
          );
        })}

        {/* Fila de Totales Diarios Calculados */}
        <div
          className="avi-orders-trow"
          style={{
            backgroundColor: '#faf6ee',
            borderTop: '2px solid #dfd8ca',
            fontWeight: 700
          }}
        >
          <span className="avi-col-dark" style={{ flex: '1.15' }}>
            TOTAL DIARIO ({computedRows.length} lotes)
          </span>
          <span className="avi-col-dark" style={{ flex: '1.1' }}>
            {formatBirds(totalBirds)}
          </span>
          <span className="avi-col-muted" style={{ flex: '1.4' }}>
            Cálculo: Σ (aves vivas × ración)
          </span>
          <span className="avi-col-muted" style={{ flex: '1.05' }}>
            1 bulto = {sackWeightKg} kg
          </span>
          <span style={{ flex: '1.35', color: '#1b562a' }}>
            {formatKg(totalEstimatedKg, 1)} ({formatSacks(totalEstimatedSacks, 2)})
          </span>
          <span style={{ flex: '1.45', color: '#9e4a0f' }}>
            {formatKg(totalRegisteredKg, 1)} ({formatSacks(totalRegisteredSacks, 2)})
          </span>
          <span style={{ flex: '1.2' }} />
        </div>
      </div>
    </div>
  );
}

export default DemandTable;
