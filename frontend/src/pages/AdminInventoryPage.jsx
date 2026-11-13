import { useEffect, useState } from 'react';
import Sidebar from '../components/Sidebar';
import Topbar from '../components/Topbar';
import BaseModal from '../components/BaseModal';
import ApiMessage from '../components/ApiMessage';
import { getFoodRequirement, getInventory, getInventorySummary } from '../services/inventoryApi';

const emptyInventory = {
  alimentos: [],
  medicamentos: [],
  tarjetasAlimentoKg: {},
  tarjetasMedicamento: {}
};

const formatNumber = (value) => new Intl.NumberFormat('es-CO', {
  maximumFractionDigits: 4
}).format(Number(value || 0));

const stageLabels = { PRE_INICIO: 'Pre-inicio', INICIO: 'Inicio', ENGORDE: 'Engorde' };
const coverageLabels = {
  CUMPLE: 'Cobertura completa',
  COBERTURA_PARCIAL: 'Cobertura parcial',
  SIN_COBERTURA: 'Sin cobertura',
  SIN_REQUERIMIENTO: 'Sin requerimiento'
};

const coverageColors = {
  CUMPLE: { backgroundColor: '#e6f7e4', color: '#2d743b' },
  COBERTURA_PARCIAL: { backgroundColor: '#fff1d6', color: '#a85100' },
  SIN_COBERTURA: { backgroundColor: '#ffe0e3', color: '#bf0000' },
  SIN_REQUERIMIENTO: { backgroundColor: '#e8eef4', color: '#475569' }
};

function AdminInventoryPage() {
  const [inventory, setInventory] = useState(emptyInventory);
  const [summary, setSummary] = useState({ recepcionesAlimentoRecientes: [] });
  const [selectedReq, setSelectedReq] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);
  const [loadKey, setLoadKey] = useState(0);
  const [detailLoading, setDetailLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setLoadFailed(false);
    Promise.all([
      getInventory({ signal: controller.signal }),
      getInventorySummary({ signal: controller.signal })
    ])
      .then(([stock, warehouse]) => {
        setInventory(stock);
        setSummary(warehouse);
      })
      .catch((cause) => {
        if (cause.name !== 'AbortError') {
          setError(cause.message);
          setLoadFailed(true);
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [loadKey]);

  const openRequirement = async (id) => {
    setError('');
    setDetailLoading(true);
    try {
      setSelectedReq(await getFoodRequirement(id));
    } catch (cause) {
      setError(cause.message);
    } finally {
      setDetailLoading(false);
    }
  };

  return (
    <div className="avi-role-layout">
      <Sidebar role="administrador" />

      <div className="avi-main-frame" data-node-id="216:3318" data-name="Inventario">
        <Topbar roleLabel="Administrador" avatarSrc="/assets/admin-avatar.svg" />
        <ApiMessage message={error} type="error" onClose={() => setError('')} />

        <div className="avi-inv-content" data-node-id="216:3324">
          {/* Page Intro */}
          <div className="avi-inv-header" data-node-id="216:3325">
            <h1 className="avi-inv-title" data-node-id="216:3327">
              Inventario
            </h1>
            <p className="avi-inv-subtitle" data-node-id="216:3328">
              Consulta del stock disponible de alimentos por etapa y medicamentos
            </p>
          </div>

          {loading && <p className="avi-inv-subtitle" role="status">Cargando inventario...</p>}
          {loadFailed && (
            <div role="alert">
              <p className="avi-inv-subtitle">No se pudo cargar el inventario.</p>
              <button type="button" onClick={() => setLoadKey((value) => value + 1)}>Reintentar</button>
            </div>
          )}

          {!loading && !loadFailed && <>

          {/* Top 4 KPI Cards (Feed Stock) */}
          <div className="avi-inv-kpi-grid" data-node-id="216:3329">
            <div className="avi-inv-kpi-card" data-node-id="216:3330">
              <span className="avi-inv-kpi-label">Pre-inicio</span>
              <strong className="avi-inv-kpi-value">{formatNumber(inventory.tarjetasAlimentoKg.PRE_INICIO)} kg</strong>
            </div>
            <div className="avi-inv-kpi-card" data-node-id="216:3333">
              <span className="avi-inv-kpi-label">Inicio</span>
              <strong className="avi-inv-kpi-value">{formatNumber(inventory.tarjetasAlimentoKg.INICIO)} kg</strong>
            </div>
            <div className="avi-inv-kpi-card" data-node-id="216:3336">
              <span className="avi-inv-kpi-label">Engorde</span>
              <strong className="avi-inv-kpi-value">{formatNumber(inventory.tarjetasAlimentoKg.ENGORDE)} kg</strong>
            </div>
            <div className="avi-inv-kpi-card avi-inv-kpi-highlight" data-node-id="216:3339">
              <span className="avi-inv-kpi-label">Total alimento disponible</span>
              <strong className="avi-inv-kpi-value">{formatNumber(inventory.tarjetasAlimentoKg.TOTAL)} kg</strong>
            </div>
          </div>

          {/* Panel 1: Stock de alimentos */}
          <div className="avi-inv-panel" data-node-id="216:3342">
            <div className="avi-panel-header" data-node-id="216:3343">
              <div className="avi-panel-icon-28" data-node-id="216:3344">
                <img alt="" src="/assets/admin-inv-panel.svg" />
              </div>
              <h2 className="avi-panel-title" data-node-id="216:3345">
                Stock de alimentos
              </h2>
            </div>

            <div className="avi-inv-table" data-node-id="216:3348">
              <div className="avi-inv-thead" data-node-id="216:3349">
                <span style={{ flex: '1.1' }}>Etapa</span>
                <span style={{ flex: '1.2' }}>Alimento</span>
                <span style={{ flex: '1.1' }}>Recepciones activas</span>
                <span style={{ flex: '1.1' }}>Próximo vencimiento</span>
                <span style={{ flex: '1.1' }}>Stock actual</span>
                <span style={{ flex: '1.1', textAlign: 'center' }}>Cobertura</span>
              </div>

              {inventory.alimentos.map((item) => (
                <div
                  key={`${item.alimentoId}-${item.etapaCrianza}`}
                  className="avi-inv-trow"
                  onClick={() => openRequirement(item.alimentoId)}
                  style={{ cursor: 'pointer' }}
                  title="Haz clic para ver detalles del requerimiento de alimento"
                >
                  <span style={{ flex: '1.1' }}>{stageLabels[item.etapaCrianza] || item.etapaCrianza}</span>
                  <span style={{ flex: '1.2' }}>{item.alimento}</span>
                  <span style={{ flex: '1.1' }}>{item.recepcionesActivas}</span>
                  <span style={{ flex: '1.1' }}>{item.proximoVencimiento || '—'}</span>
                  <span style={{ flex: '1.1' }}>{formatNumber(item.stockActualKg)} kg</span>
                  <div style={{ flex: '1.1', display: 'flex', justifyContent: 'center' }}>
                    <button
                      type="button"
                      className="avi-inv-demand-badge"
                      style={coverageColors[item.estadoCobertura] || coverageColors.SIN_REQUERIMIENTO}
                      disabled={detailLoading}
                      onClick={(e) => {
                        e.stopPropagation();
                        openRequirement(item.alimentoId);
                      }}
                    >
                      <span
                        className="avi-inv-demand-dot"
                        style={{ backgroundColor: coverageColors[item.estadoCobertura]?.color || '#475569' }}
                      />
                      {coverageLabels[item.estadoCobertura] || item.estadoCobertura}
                    </button>
                  </div>
                </div>
              ))}
              {!loading && !error && inventory.alimentos.length === 0 && (
                <p className="avi-inv-subtitle">No hay alimentos disponibles en inventario.</p>
              )}
            </div>
          </div>

          {/* Middle 4 KPI Cards (Medicine Presentations) */}
          <div className="avi-inv-kpi-grid" data-node-id="388:66">
            <div className="avi-inv-kpi-card" data-node-id="388:67">
              <span className="avi-inv-kpi-label">Cantidad de Frascos</span>
              <strong className="avi-inv-kpi-value">{formatNumber(inventory.tarjetasMedicamento.FRASCOS)}</strong>
            </div>
            <div className="avi-inv-kpi-card" data-node-id="388:70">
              <span className="avi-inv-kpi-label">Cantidad de Bolsas</span>
              <strong className="avi-inv-kpi-value">{formatNumber(inventory.tarjetasMedicamento.BOLSAS)}</strong>
            </div>
            <div className="avi-inv-kpi-card" data-node-id="388:73">
              <span className="avi-inv-kpi-label">Cantidad de Cajas</span>
              <strong className="avi-inv-kpi-value">{formatNumber(inventory.tarjetasMedicamento.CAJAS)}</strong>
            </div>
            <div className="avi-inv-kpi-card avi-inv-kpi-highlight" data-node-id="388:76">
              <span className="avi-inv-kpi-label">Total de Presentaciones</span>
              <strong className="avi-inv-kpi-value">{formatNumber(inventory.tarjetasMedicamento.TOTAL)}</strong>
            </div>
          </div>

          {/* Panel 2: Stock de medicamentos */}
          <div className="avi-inv-panel" data-node-id="388:80">
            <div className="avi-panel-header" data-node-id="388:81">
              <div className="avi-panel-icon-28" data-node-id="388:82">
                <img alt="" src="/assets/admin-inv-panel.svg" />
              </div>
              <h2 className="avi-panel-title" data-node-id="388:85">
                Stock de medicamentos
              </h2>
            </div>

            <div className="avi-inv-table" data-node-id="388:88">
              <div className="avi-inv-thead" data-node-id="388:89">
                <span style={{ flex: '1.2' }}>Medicamento</span>
                <span style={{ flex: '1.1' }}>Presentación</span>
                <span style={{ flex: '1' }}>Cantidad</span>
                <span style={{ flex: '1.3' }}>Contenido por presentación</span>
                <span style={{ flex: '1' }}>Stock actual</span>
              </div>

              {inventory.medicamentos.map((med) => (
                <div key={`${med.medicamentoId}-${med.presentacion}-${med.contenidoPorPresentacion}-${med.unidad}`} className="avi-inv-trow">
                  <span style={{ flex: '1.2' }}>{med.medicamento}</span>
                  <span style={{ flex: '1.1' }}>{med.presentacion}</span>
                  <span style={{ flex: '1' }}>{formatNumber(med.cantidad)}</span>
                  <span style={{ flex: '1.3' }}>{formatNumber(med.contenidoPorPresentacion)} {med.unidad}</span>
                  <span style={{ flex: '1' }}>{formatNumber(med.stockActual)} {med.unidad}</span>
                </div>
              ))}
              {!loading && !error && inventory.medicamentos.length === 0 && (
                <p className="avi-inv-subtitle">No hay medicamentos disponibles en inventario.</p>
              )}
            </div>
          </div>

          <div className="avi-inv-kpi-grid">
            <div className="avi-inv-kpi-card">
              <span className="avi-inv-kpi-label">Capacidad de bodega</span>
              <strong className="avi-inv-kpi-value">
                {Number(summary.capacidadMaximaKg) > 0 ? `${formatNumber(summary.capacidadMaximaKg)} kg` : 'No configurada'}
              </strong>
            </div>
            <div className="avi-inv-kpi-card">
              <span className="avi-inv-kpi-label">Ocupación de bodega</span>
              <strong className="avi-inv-kpi-value">
                {summary.ocupacionActualKg == null ? 'Sin dato' : `${formatNumber(summary.ocupacionActualKg)} kg`}
              </strong>
            </div>
            <div className="avi-inv-kpi-card">
              <span className="avi-inv-kpi-label">Porcentaje de ocupación</span>
              <strong className="avi-inv-kpi-value">
                {summary.porcentajeOcupacion == null ? 'Sin dato' : `${formatNumber(summary.porcentajeOcupacion)} %`}
              </strong>
            </div>
          </div>

          <div className="avi-inv-panel">
            <div className="avi-panel-header">
              <h2 className="avi-panel-title">Recepciones recientes de alimento</h2>
            </div>
            <div className="avi-inv-table">
              <div className="avi-inv-thead">
                <span style={{ flex: 1 }}>Lote</span>
                <span style={{ flex: 1 }}>Alimento</span>
                <span style={{ flex: 1 }}>Bultos</span>
                <span style={{ flex: 1 }}>Fecha de ingreso</span>
              </div>
              {summary.recepcionesAlimentoRecientes.map((reception) => (
                <div className="avi-inv-trow" key={`${reception.codigoLote}-${reception.fechaIngreso}`}>
                  <span style={{ flex: 1 }}>{reception.codigoLote}</span>
                  <span style={{ flex: 1 }}>{reception.alimento}</span>
                  <span style={{ flex: 1 }}>{reception.cantidadBultos}</span>
                  <span style={{ flex: 1 }}>{reception.fechaIngreso}</span>
                </div>
              ))}
              {!loading && !error && summary.recepcionesAlimentoRecientes.length === 0 && (
                <p className="avi-inv-subtitle">No hay recepciones recientes.</p>
              )}
            </div>
          </div>
          </>}
        </div>
      </div>

      {/* Modal · Detalles Requerimiento (Node 386:302) */}
      <BaseModal
        isOpen={Boolean(selectedReq)}
        onClose={() => setSelectedReq(null)}
        maxWidth="720px"
      >
        {selectedReq && (
          <div className="avi-figma-modal-720" data-node-id="386:302">
            <div className="avi-figma-modal-header" data-node-id="386:303">
              <div className="avi-figma-modal-titles">
                <h2 className="avi-figma-modal-title">Detalles requerimiento de alimento</h2>
                <p className="avi-figma-modal-subtitle">
                  detalles del alimento requerido de lotes activos
                </p>
              </div>
              <button
                type="button"
                className="avi-figma-modal-close"
                onClick={() => setSelectedReq(null)}
              >
                ×
              </button>
            </div>

            <div className="avi-figma-modal-grid">
              <div className="avi-figma-field">
                <label className="avi-figma-label">Alimento</label>
                <div className="avi-figma-readonly-box">{selectedReq.alimento}</div>
              </div>
              <div className="avi-figma-field">
                <label className="avi-figma-label">Etapa</label>
                <div className="avi-figma-readonly-box">{stageLabels[selectedReq.etapaCrianza] || selectedReq.etapaCrianza}</div>
              </div>
              <div className="avi-figma-field">
                <label className="avi-figma-label">Stock Actual</label>
                <div className="avi-figma-readonly-box">{formatNumber(selectedReq.stockActualKg)} kg</div>
              </div>
              <div className="avi-figma-field">
                <label className="avi-figma-label">Demanda</label>
                <div className="avi-figma-demand-box">{formatNumber(selectedReq.demandaKg)} kg</div>
              </div>
              <div className="avi-figma-field">
                <label className="avi-figma-label">Estado de cobertura</label>
                <div className="avi-figma-readonly-box">{coverageLabels[selectedReq.estado] || selectedReq.estado}</div>
              </div>
            </div>

            <div className="avi-figma-modal-footer">
              <button
                type="button"
                className="avi-figma-btn-cancel"
                onClick={() => setSelectedReq(null)}
              >
                Cancelar
              </button>
            </div>
          </div>
        )}
      </BaseModal>
    </div>
  );
}

export default AdminInventoryPage;
