import { useEffect, useState } from 'react';
import Sidebar from '../components/Sidebar';
import Topbar from '../components/Topbar';
import StatCard from '../components/StatCard';
import BaseModal from '../components/BaseModal';
import ApiMessage from '../components/ApiMessage';
import { getDailyFoodSummary, getBarnDailyFood, registerDailySupply } from '../services/dailyFoodApi';
import { getProductiveBarns, registerMortality } from '../services/workerApi';
import { createIsolationRequest, getIsolatedBarns } from '../services/isolationApi';
import { getDailyMedicationSummary, getBarnDailyMedication } from '../services/dailyMedicationApi';

function WorkerDashboardPage() {
  const [workerBarns, setWorkerBarns] = useState([]);
  const [barnsLoading, setBarnsLoading] = useState(true);
  const [mortalitySaving, setMortalitySaving] = useState(false);
  const [mortalityKey, setMortalityKey] = useState('');
  const [mortalityLocked, setMortalityLocked] = useState(false);
  const [toast, setToast] = useState('');
  const [foodError, setFoodError] = useState('');
  const [foodSummary, setFoodSummary] = useState(null);
  const [foodLoading, setFoodLoading] = useState(true);
  const [foodReload, setFoodReload] = useState(0);
  const [medicationSummary, setMedicationSummary] = useState(null);
  const [medicationLoading, setMedicationLoading] = useState(true);
  const [medicationError, setMedicationError] = useState('');
  const [medicationDetail, setMedicationDetail] = useState(null);
  const [supplyOpen, setSupplyOpen] = useState(false);
  const [supplyDetail, setSupplyDetail] = useState(null);
  const [supplyQuantity, setSupplyQuantity] = useState('');
  const [supplyKey, setSupplyKey] = useState('');
  const [supplySaving, setSupplySaving] = useState(false);
  const [isolationOpen, setIsolationOpen] = useState(false);
  const [isolationForm, setIsolationForm] = useState({ barnId: '', motivo: '', signosClinicos: '' });
  const [isolationSaving, setIsolationSaving] = useState(false);
  const [requestedIsolationBarns, setRequestedIsolationBarns] = useState([]);

  useEffect(() => {
    const controller = new AbortController();
    setFoodLoading(true);
    getDailyFoodSummary({ signal: controller.signal })
      .then(setFoodSummary)
      .catch((cause) => { if (cause.name !== 'AbortError') setFoodError(cause.message); })
      .finally(() => { if (!controller.signal.aborted) setFoodLoading(false); });
    return () => controller.abort();
  }, [foodReload]);

  useEffect(() => {
    const controller = new AbortController();
    setMedicationLoading(true);
    setMedicationError('');
    Promise.all([
      getProductiveBarns({ signal: controller.signal }),
      getIsolatedBarns({ signal: controller.signal })
    ])
      .then(([productive, isolated]) =>
        getDailyMedicationSummary(
          [...new Set([...productive, ...isolated].map((barn) => barn.id))],
          controller.signal
        ))
      .then(setMedicationSummary)
      .catch((cause) => { if (cause.name !== 'AbortError') setMedicationError(cause.message); })
      .finally(() => { if (!controller.signal.aborted) setMedicationLoading(false); });
    return () => controller.abort();
  }, [foodReload]);

  async function openMedicationDetail(barnId) {
    try {
      setMedicationError('');
      setMedicationDetail(await getBarnDailyMedication(barnId));
    } catch (cause) {
      setMedicationError(cause.message);
    }
  }

  useEffect(() => {
    const controller = new AbortController();
    setBarnsLoading(true);
    getProductiveBarns({ signal: controller.signal })
      .then((barns) => setWorkerBarns(barns.map((barn) => ({
        id: barn.id,
        name: barn.nombre,
        lote: `Lote ${foodSummary?.galpones?.find((row) => row.galponId === barn.id)?.loteCodigo || barn.loteActual.id}`,
        status: 'Productivo',
        poblacionNum: barn.loteActual.poblacionActual,
        poblacion: `${Number(barn.loteActual.poblacionActual).toLocaleString('es-CO')} aves`,
        edad: barn.loteActual.edad?.texto || 'Sin dato',
        dieta: foodSummary?.galpones?.find((row) => row.galponId === barn.id)?.nombreAlimento || 'Sin ración',
        racion: `${Number(foodSummary?.galpones?.find((row) => row.galponId === barn.id)?.cuotaKg || 0).toLocaleString('es-CO')} kg`
      }))))
      .catch((cause) => { if (cause.name !== 'AbortError') setFoodError(cause.message); })
      .finally(() => { if (!controller.signal.aborted) setBarnsLoading(false); });
    return () => controller.abort();
  }, [foodReload, foodSummary]);

  const openSupply = async (barnId) => {
    setFoodError('');
    try {
      const detail = await getBarnDailyFood(barnId);
      setSupplyDetail(detail);
      setSupplyQuantity('');
      setSupplyKey(crypto.randomUUID());
      setSupplyOpen(true);
    } catch (cause) {
      setFoodError(cause.message);
    }
  };

  const saveSupply = async (event) => {
    event.preventDefault();
    const quantity = Number(supplyQuantity);
    if (!supplyDetail || !Number.isFinite(quantity) || quantity <= 0) return;
    setSupplySaving(true);
    setFoodError('');
    try {
      await registerDailySupply(supplyDetail.galponId, {
        cantidadKg: quantity,
        fechaAplicacion: supplyDetail.fecha,
        idempotencyKey: supplyKey
      });
      setSupplyOpen(false);
      setSupplyDetail(null);
      setToast(`Suministro de ${quantity} kg registrado.`);
      setFoodReload((value) => value + 1);
    } catch (cause) {
      setFoodError(cause.message);
    } finally {
      setSupplySaving(false);
    }
  };

  const [isMortalityOpen, setIsMortalityOpen] = useState(false);
  const [mortalityForm, setMortalityForm] = useState({
    barnId: '',
    fecha: '',
    hora: '',
    cantidadBajas: '',
    causa: 'DESCONOCIDA',
    observaciones: ''
  });

  const openIsolation = (barnId) => {
    setIsolationForm({ barnId, motivo: '', signosClinicos: '' });
    setFoodError('');
    setIsolationOpen(true);
  };

  const saveIsolation = async (event) => {
    event.preventDefault();
    if (!isolationForm.barnId) return;
    setIsolationSaving(true);
    setFoodError('');
    try {
      const result = await createIsolationRequest(isolationForm.barnId, {
        motivo: isolationForm.motivo.trim(),
        signosClinicos: isolationForm.signosClinicos.trim()
      });
      setRequestedIsolationBarns((ids) => [...ids, result.galponId]);
      setIsolationOpen(false);
      setToast(`Solicitud ${result.id} enviada al veterinario. Estado: ${result.estado}.`);
    } catch (cause) {
      setFoodError(cause.message);
    } finally {
      setIsolationSaving(false);
    }
  };

  const totalLiveBirds = workerBarns.reduce(
    (sum, b) => sum + (b.poblacionNum || 0),
    0
  );

  const selectedMortalityBarn =
    workerBarns.find((b) => b.id === mortalityForm.barnId) || workerBarns[0];

  const bajasCount = Math.max(0, parseInt(mortalityForm.cantidadBajas, 10) || 0);
  const lotePercentage = selectedMortalityBarn?.poblacionNum
    ? ((bajasCount / selectedMortalityBarn.poblacionNum) * 100).toFixed(2)
    : '0.00';

  const handleOpenMortalityModal = (barnId) => {
    const now = new Date();
    setMortalityForm({
      barnId: barnId || workerBarns[0]?.id || '',
      fecha: `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`,
      hora: `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`,
      cantidadBajas: '',
      causa: 'DESCONOCIDA',
      observaciones: ''
    });
    setMortalityKey(crypto.randomUUID());
    setMortalityLocked(false);
    setFoodError('');
    setIsMortalityOpen(true);
  };

  const handleSaveMortality = async (e) => {
    e.preventDefault();
    if (!selectedMortalityBarn || bajasCount <= 0) return;
    setMortalitySaving(true);
    setMortalityLocked(true);
    setFoodError('');
    try {
      const result = await registerMortality(mortalityForm.barnId, {
        fechaHoraEvento: new Date(`${mortalityForm.fecha}T${mortalityForm.hora}:00`).toISOString(),
        cantidad: bajasCount,
        causa: mortalityForm.causa,
        observaciones: mortalityForm.observaciones
      }, mortalityKey);
      setIsMortalityOpen(false);
      setToast(`${bajasCount} bajas registradas en ${selectedMortalityBarn.name}. Población: ${Number(result.poblacionResultante).toLocaleString('es-CO')}. Clasificación: ${result.clasificacion}.`);
      setFoodReload((value) => value + 1);
    } catch (cause) {
      if (cause.status >= 400 && cause.status < 500) setMortalityLocked(false);
      setFoodError(`${cause.message}. Puedes reintentar con los mismos datos; se conserva la clave del registro.`);
    } finally {
      setMortalitySaving(false);
    }
  };

  return (
    <div className="avi-role-layout">
      <Sidebar role="trabajador" />

      <div
        className="avi-main-frame"
        data-node-id="238:5822"
        data-name="Trabajador Inicio"
      >
        <Topbar roleLabel="Trabajador" avatarSrc="/assets/admin-avatar.svg" />
        <ApiMessage message={toast} type="success" onClose={() => setToast('')} />
        <ApiMessage message={foodError} type="error" onClose={() => setFoodError('')} />
        <ApiMessage message={medicationError} type="error" onClose={() => setMedicationError('')} />

        <div className="avi-dashboard-content" data-node-id="238:5842">
          {/* Encabezado de Página */}
          <div className="avi-page-heading" data-node-id="238:5843">
            <div className="avi-heading-copy">
              <p className="avi-page-subtitle" data-node-id="238:5848">
                Control y registro diario de galpones productivos
              </p>
            </div>

            <div className="avi-worker-header-actions" data-node-id="238:5849">
              <button
                type="button"
                className="avi-btn-orange-cta"
                disabled={!workerBarns.length}
                onClick={() => handleOpenMortalityModal(workerBarns[0]?.id)}
                data-node-id="238:5850"
              >
                <span>＋  Registrar bajas</span>
              </button>
              <button type="button" className="avi-btn-green-cta" onClick={() => setFoodReload((value) => value + 1)}>
                Actualizar
              </button>
            </div>
          </div>

          {/* 3 Tarjetas KPI usando el componente StatCard y logos de Administrador */}
          <div className="avi-metrics-row" data-node-id="238:5861">
            <StatCard
              label="Galpones productivos"
              value={workerBarns.length}
              status={`${totalLiveBirds.toLocaleString('es-CO')} aves vivas`}
              statusColor="#2d743b"
              icon="/assets/admin-galpon.svg"
              tileBg="#f4eee3"
              width="437px"
            />
            <StatCard
              label="Alimento diario"
              value={foodSummary ? `${Number(foodSummary.requerimientoTotalKg || 0).toLocaleString('es-CO')} kg` : '—'}
              status={foodSummary ? `${Number(foodSummary.requerimientoTotalBultos || 0).toLocaleString('es-CO')} bultos requeridos hoy` : 'Cargando datos'}
              statusColor="#2d743b"
              icon="/assets/admin-inventario.svg"
              tileBg="#e6f7e4"
              width="437px"
            />
            <StatCard
              label="Estado de bodega"
              value={foodSummary?.estadoBodega === 'BODEGA_INSUFICIENTE' ? 'Insuficiente' : foodSummary ? 'Disponible' : '—'}
              status="Según el alimento diario"
              statusColor="#9e4a0f"
              icon="/assets/admin-orden.svg"
              tileBg="#f4eee3"
              width="444px"
            />
          </div>

          {/* Galpones productivos consultados del backend */}
          <div className="avi-recent-orders-panel" data-node-id="238:5892">
            <div className="avi-panel-header avi-panel-header-full" data-node-id="238:5893">
              <div className="avi-panel-icon-28">
                <img alt="" src="/assets/admin-home-icon.svg" />
              </div>
              <h2 className="avi-panel-title">Galpones productivos</h2>
              <div className="avi-panel-spacer" />
              <span className="avi-worker-operator-tag">Datos del backend</span>
            </div>

            {barnsLoading && <p className="avi-page-subtitle">Cargando galpones...</p>}
            {!barnsLoading && workerBarns.length === 0 &&
              <p className="avi-page-subtitle">No hay galpones productivos con lote activo.</p>}
            <div className="avi-worker-barns-grid" data-node-id="238:5898">
              {workerBarns.map((barn) => (
                <div
                  key={barn.id}
                  className="avi-worker-barn-card"
                >
                  <div className="avi-worker-barn-top">
                    <div className="avi-crit-item-left">
                      <div
                        className="avi-crit-icon-tile"
                        style={{
                          backgroundColor: '#e6f7e4'
                        }}
                      >
                        <div className="avi-crit-icon-26">
                          <img alt="" src="/assets/admin-galpon.svg" />
                        </div>
                      </div>
                      <div className="avi-worker-barn-titles">
                        <h3>{barn.name}</h3>
                        <span>{barn.lote}</span>
                      </div>
                    </div>

                    <span
                      className="avi-barn-status-pill"
                      style={{
                        backgroundColor: '#d6f0d1',
                        color: '#1f5929'
                      }}
                    >
                      <span
                        className="avi-barn-status-dot"
                        style={{
                          backgroundColor: '#2d9f42'
                        }}
                      />
                      {barn.status}
                    </span>
                  </div>

                  <div className="avi-worker-barn-metrics">
                    <div className="avi-worker-barn-metric-box">
                      <span>Población viva</span>
                      <strong>{barn.poblacion}</strong>
                    </div>
                    <div className="avi-worker-barn-metric-box">
                      <span>Edad del lote</span>
                      <strong>{barn.edad}</strong>
                    </div>
                  </div>

                  <div className="avi-worker-barn-details">
                    <div className="avi-worker-barn-detail-row">
                      <span>Dieta asignada hoy</span>
                      <strong>{barn.dieta}</strong>
                    </div>
                    <div className="avi-worker-barn-detail-row">
                      <span>Ración diaria</span>
                      <strong>{barn.racion}</strong>
                    </div>
                  </div>

                  <div className="avi-worker-barn-actions">
                    <button
                      type="button"
                      className="avi-admin-table-btn avi-admin-table-btn-orange"
                      style={{ flex: 1 }}
                      onClick={() => handleOpenMortalityModal(barn.id)}
                    >
                      Registrar bajas
                    </button>
                    <button type="button" className="avi-admin-table-btn avi-admin-table-btn-green"
                      style={{ flex: 1 }} disabled={requestedIsolationBarns.includes(barn.id)}
                      onClick={() => openIsolation(barn.id)}>
                      {requestedIsolationBarns.includes(barn.id) ? 'Solicitud pendiente' : 'Solicitar aislamiento'}
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Paneles Inferiores con el mismo estilo y logos de Administrador */}
          <div className="avi-worker-bottom-panels" style={{ display: 'block' }} data-node-id="238:5995">
            {/* Consulta y registro del alimento diario del backend */}
            <div className="avi-recent-orders-panel" data-node-id="238:5996">
              <div className="avi-panel-header avi-panel-header-full">
                <div className="avi-panel-icon-28"><img alt="" src="/assets/admin-inv-panel.svg" /></div>
                <h2 className="avi-panel-title">Consulta de alimento diario</h2>
                <div className="avi-panel-spacer" />
                <button type="button" className="avi-admin-table-btn avi-admin-table-btn-green"
                  onClick={() => setFoodReload((value) => value + 1)}>Actualizar</button>
              </div>
              {foodLoading && <p className="avi-page-subtitle">Cargando alimento diario...</p>}
              {!foodLoading && foodSummary?.galpones?.length === 0 &&
                <p className="avi-page-subtitle">No hay galpones con ración asignada hoy.</p>}
              {foodSummary?.galpones?.length > 0 && <div className="avi-orders-table">
                <div className="avi-orders-thead">
                  <span style={{ flex: 1 }}>Galpón</span>
                  <span style={{ flex: 1 }}>Alimento</span>
                  <span style={{ flex: 1 }}>Cuota</span>
                  <span style={{ flex: 1 }}>Bultos</span>
                  <span style={{ flex: 1 }}>Bodega</span>
                  <span style={{ flex: 1 }}>Acción</span>
                </div>
                {foodSummary.galpones.map((barn) => <div className="avi-orders-trow" key={barn.galponId}>
                  <span style={{ flex: 1 }}>{barn.nombreGalpon}</span>
                  <span style={{ flex: 1 }}>{barn.nombreAlimento || barn.tipoAlimento}</span>
                  <span style={{ flex: 1 }}>{Number(barn.cuotaKg || 0).toLocaleString('es-CO')} kg</span>
                  <span style={{ flex: 1 }}>{Number(barn.cuotaBultos || 0).toLocaleString('es-CO')}</span>
                  <span style={{ flex: 1 }}>{barn.estadoBodega === 'INSUFICIENTE' ? 'Insuficiente' : barn.estadoBodega}</span>
                  <span style={{ flex: 1 }}><button type="button" className="avi-admin-table-btn avi-admin-table-btn-green"
                    onClick={() => openSupply(barn.galponId)}>Suministrar</button></span>
                </div>)}
              </div>}
            </div>

            <div className="avi-recent-orders-panel">
              <div className="avi-panel-header avi-panel-header-full">
                <div className="avi-panel-icon-28"><img alt="" src="/assets/vet-syringe.svg" /></div>
                <h2 className="avi-panel-title">Medicación diaria</h2>
                <div className="avi-panel-spacer" />
                <button type="button" className="avi-admin-table-btn avi-admin-table-btn-green"
                  onClick={() => setFoodReload((value) => value + 1)}>Actualizar</button>
              </div>
              {medicationLoading && <p className="avi-page-subtitle">Cargando medicación diaria...</p>}
              {!medicationLoading && medicationSummary?.cantidadTratamientos === 0 &&
                <p className="avi-page-subtitle">No hay tratamientos activos hoy.</p>}
              {medicationSummary?.cantidadTratamientos > 0 && <div className="avi-orders-table">
                <div className="avi-orders-thead">
                  <span style={{ flex: 1 }}>Galpón</span>
                  <span style={{ flex: 1.2 }}>Medicamento</span>
                  <span style={{ flex: 1 }}>Progreso</span>
                  <span style={{ flex: 1.8 }}>Dosis de hoy</span>
                  <span style={{ flex: 0.8 }}>Aplica hoy</span>
                  <span style={{ flex: 0.8 }}>Acción</span>
                </div>
                {medicationSummary.galpones.flatMap((barn) =>
                  barn.tratamientos.map((item) => <div className="avi-orders-trow"
                    key={item.diagnosticoId}>
                    <span style={{ flex: 1 }}>{barn.nombreGalpon}</span>
                    <span style={{ flex: 1.2, minWidth: 0, overflowWrap: 'anywhere' }}>{item.medicamento}</span>
                    <span style={{ flex: 1 }}>{item.progreso}</span>
                    <span style={{ flex: 1.8, minWidth: 0, overflowWrap: 'anywhere' }}>{item.dosisHoy}</span>
                    <span style={{ flex: 0.8 }}>{item.aplicaHoy ? 'Sí' : 'No'}</span>
                    <span style={{ flex: 0.8 }}><button type="button"
                      className="avi-admin-table-btn avi-admin-table-btn-green"
                      onClick={() => openMedicationDetail(barn.galponId)}>Detalle</button></span>
                  </div>)
                )}
              </div>}
            </div>

          </div>
        </div>
      </div>

      <BaseModal isOpen={Boolean(medicationDetail)} onClose={() => setMedicationDetail(null)} maxWidth="720px">
        {medicationDetail && <div className="avi-figma-modal-720">
          <div className="avi-figma-modal-header">
            <h2 className="avi-figma-modal-title">
              Medicación diaria · {medicationDetail.nombreGalpon}
            </h2>
            <button type="button" className="avi-figma-modal-close"
              onClick={() => setMedicationDetail(null)}>×</button>
          </div>
          <div className="avi-figma-modal-grid">
            {medicationDetail.tratamientos.map((item) => <div key={item.diagnosticoId}
              style={{ gridColumn: '1 / -1' }}>
              <p><strong>Enfermedad:</strong> {item.enfermedad}</p>
              <p><strong>Medicamento:</strong> {item.medicamento}</p>
              <p><strong>Principio activo:</strong> {item.principioActivo}</p>
              <p><strong>Progreso:</strong> {item.progreso}</p>
              <p><strong>Dosis:</strong> {item.dosisHoy}</p>
              <p><strong>Vía o descripción:</strong> {item.viaAdministracion}</p>
              <p><strong>Aplica hoy:</strong> {item.aplicaHoy ? 'Sí' : 'No'}</p>
            </div>)}
          </div>
        </div>}
      </BaseModal>

      <BaseModal isOpen={isolationOpen} onClose={() => { if (!isolationSaving) setIsolationOpen(false); }} maxWidth="720px">
        <form className="avi-figma-modal-720" onSubmit={saveIsolation}>
          <div className="avi-figma-modal-header">
            <div className="avi-figma-modal-titles">
              <h2 className="avi-figma-modal-title">Solicitar aislamiento sanitario</h2>
              <p className="avi-figma-modal-subtitle">
                {workerBarns.find((barn) => barn.id === isolationForm.barnId)?.name}. El veterinario decidirá la solicitud.
              </p>
            </div>
            <button type="button" className="avi-figma-modal-close" disabled={isolationSaving}
              onClick={() => setIsolationOpen(false)}>×</button>
          </div>
          <div className="avi-figma-modal-grid">
            <div className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
              <label className="avi-figma-label" htmlFor="isolation-reason">Motivo</label>
              <input id="isolation-reason" className="avi-figma-input" required
                value={isolationForm.motivo} disabled={isolationSaving}
                onChange={(event) => setIsolationForm({ ...isolationForm, motivo: event.target.value })} />
            </div>
            <div className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
              <label className="avi-figma-label" htmlFor="isolation-signs">Signos clínicos reportados</label>
              <input id="isolation-signs" className="avi-figma-input" required
                value={isolationForm.signosClinicos} disabled={isolationSaving}
                onChange={(event) => setIsolationForm({ ...isolationForm, signosClinicos: event.target.value })} />
            </div>
          </div>
          <div className="avi-figma-modal-footer">
            <button type="button" className="avi-figma-btn-cancel" disabled={isolationSaving}
              onClick={() => setIsolationOpen(false)}>Cancelar</button>
            <button type="submit" className="avi-figma-btn-green" disabled={isolationSaving}>
              {isolationSaving ? 'Enviando...' : 'Enviar solicitud'}
            </button>
          </div>
        </form>
      </BaseModal>

      <BaseModal isOpen={supplyOpen} onClose={() => { if (!supplySaving) setSupplyOpen(false); }} maxWidth="720px">
        <form className="avi-figma-modal-720" onSubmit={saveSupply}>
          <div className="avi-figma-modal-header">
            <div className="avi-figma-modal-titles">
              <h2 className="avi-figma-modal-title">Registrar suministro de alimento</h2>
              <p className="avi-figma-modal-subtitle">{supplyDetail?.nombreGalpon} · {supplyDetail?.fecha}</p>
            </div>
            <button type="button" className="avi-figma-modal-close" disabled={supplySaving}
              onClick={() => setSupplyOpen(false)}>×</button>
          </div>
          <div className="avi-figma-modal-grid">
            <div className="avi-figma-field">
              <label className="avi-figma-label">Alimento</label>
              <div className="avi-figma-readonly-box">{supplyDetail?.nombreAlimento || supplyDetail?.tipoAlimento}</div>
            </div>
            <div className="avi-figma-field">
              <label className="avi-figma-label">Saldo pendiente</label>
              <div className="avi-figma-readonly-box">{supplyDetail?.saldoKg} kg</div>
            </div>
            <div className="avi-figma-field">
              <label className="avi-figma-label">Disponible en bodega</label>
              <div className="avi-figma-readonly-box">{supplyDetail?.disponibilidadKg} kg</div>
            </div>
            <div className="avi-figma-field">
              <label className="avi-figma-label" htmlFor="supply-quantity">Cantidad suministrada (kg)</label>
              <input id="supply-quantity" type="number" min="0.001" step="0.001"
                max={Math.min(Number(supplyDetail?.saldoKg || 0), Number(supplyDetail?.disponibilidadKg || 0))}
                className="avi-figma-input" value={supplyQuantity}
                onChange={(event) => setSupplyQuantity(event.target.value)} required />
            </div>
          </div>
          <div className="avi-figma-modal-footer">
            <button type="button" className="avi-figma-btn-cancel" disabled={supplySaving}
              onClick={() => setSupplyOpen(false)}>Cancelar</button>
            <button type="submit" className="avi-figma-btn-green"
              disabled={supplySaving || !supplyDetail || Number(supplyDetail.saldoKg) <= 0 || Number(supplyDetail.disponibilidadKg) <= 0}>
              {supplySaving ? 'Guardando...' : 'Registrar suministro'}
            </button>
          </div>
        </form>
      </BaseModal>

      {/* ── MODAL 1: REGISTRAR BAJAS / MORTALIDAD (Diseño unificado 720px de Administrador) ── */}
      <BaseModal
        isOpen={isMortalityOpen}
        onClose={() => { if (!mortalitySaving) setIsMortalityOpen(false); }}
        maxWidth="720px"
      >
        <form
          className="avi-figma-modal-720"
          data-node-id="238:6261"
          onSubmit={handleSaveMortality}
        >
          <div className="avi-figma-modal-header" data-node-id="238:6262">
            <div className="avi-figma-modal-titles">
              <h2 className="avi-figma-modal-title">Registrar bajas / mortalidad</h2>
              <p className="avi-figma-modal-subtitle">
                Descuenta la población viva y genera una alerta si el backend clasifica la mortalidad como anormal
              </p>
            </div>
            <button
              type="button"
              className="avi-figma-modal-close"
              disabled={mortalitySaving}
              onClick={() => setIsMortalityOpen(false)}
            >
              ×
            </button>
          </div>

          <div className="avi-figma-modal-grid" data-node-id="238:6273">
            <div className="avi-figma-field">
              <label className="avi-figma-label">Galpón productivo</label>
              <div className="avi-figma-select-wrap">
                <select
                  className="avi-figma-input avi-figma-select"
                  value={mortalityForm.barnId}
                  disabled={mortalityLocked}
                  onChange={(e) =>
                    setMortalityForm({ ...mortalityForm, barnId: e.target.value })
                  }
                >
                  {workerBarns.map((b) => (
                    <option key={b.id} value={b.id}>
                      {b.name} — {b.lote} ({b.poblacion})
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
              <label className="avi-figma-label">Población actual viva</label>
              <div className="avi-figma-valid-box">
                {selectedMortalityBarn?.poblacion} ({selectedMortalityBarn?.lote})
              </div>
            </div>

            <div className="avi-figma-field">
              <label className="avi-figma-label">Fecha del evento</label>
              <input
                type="date"
                className="avi-figma-input"
                value={mortalityForm.fecha}
                disabled={mortalityLocked}
                onChange={(e) =>
                  setMortalityForm({ ...mortalityForm, fecha: e.target.value })
                }
                required
              />
            </div>

            <div className="avi-figma-field">
              <label className="avi-figma-label">Hora del hallazgo</label>
              <input
                type="time"
                className="avi-figma-input"
                value={mortalityForm.hora}
                disabled={mortalityLocked}
                onChange={(e) =>
                  setMortalityForm({ ...mortalityForm, hora: e.target.value })
                }
                required
              />
            </div>

            <div className="avi-figma-field">
              <label className="avi-figma-label">Cantidad de bajas</label>
              <input
                type="number"
                min="1"
                max={selectedMortalityBarn?.poblacionNum || 0}
                className="avi-figma-input"
                value={mortalityForm.cantidadBajas}
                disabled={mortalityLocked}
                onChange={(e) =>
                  setMortalityForm({
                    ...mortalityForm,
                    cantidadBajas: e.target.value
                  })
                }
                required
              />
            </div>

            <div className="avi-figma-field">
              <label className="avi-figma-label">Causa probable / clasificación</label>
              <div className="avi-figma-select-wrap">
                <select
                  className="avi-figma-input avi-figma-select"
                  value={mortalityForm.causa}
                  disabled={mortalityLocked}
                  onChange={(e) =>
                    setMortalityForm({ ...mortalityForm, causa: e.target.value })
                  }
                >
                  <option value="DESCONOCIDA">Desconocida</option>
                  <option value="ENFERMEDAD">Enfermedad</option>
                  <option value="ACCIDENTE">Accidente</option>
                  <option value="ASFIXIA">Asfixia</option>
                  <option value="OTRA">Otra</option>
                </select>
                <img
                  alt=""
                  src="/assets/admin-select-chevron.svg"
                  className="avi-figma-select-chevron"
                />
              </div>
            </div>

            <div className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
              <label className="avi-figma-label">Bajas sobre población viva</label>
              <div className="avi-figma-valid-box">
                {bajasCount} aves · {lotePercentage}% del lote. El backend determinará la clasificación
                y generará una alerta veterinaria si corresponde.
              </div>
            </div>

            <div className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
              <label className="avi-figma-label">Observaciones adicionales</label>
              <input
                type="text"
                className="avi-figma-input"
                value={mortalityForm.observaciones}
                disabled={mortalityLocked}
                onChange={(e) =>
                  setMortalityForm({
                    ...mortalityForm,
                    observaciones: e.target.value
                  })
                }
              />
            </div>
          </div>

          <div className="avi-figma-modal-footer" data-node-id="238:6356">
            <button
              type="button"
              className="avi-figma-btn-cancel"
              disabled={mortalitySaving}
              onClick={() => setIsMortalityOpen(false)}
            >
              Cancelar
            </button>
            <button type="submit" className="avi-figma-btn-orange" disabled={mortalitySaving}>
              {mortalitySaving ? 'Guardando...' : mortalityLocked ? 'Reintentar registro' : 'Guardar mortalidad'}
            </button>
          </div>
        </form>
      </BaseModal>


    </div>
  );
}

export default WorkerDashboardPage;
