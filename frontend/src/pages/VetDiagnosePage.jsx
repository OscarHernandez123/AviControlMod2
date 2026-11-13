import { useContext, useState } from 'react';
import { FarmContext } from '../context/FarmContext';
import Sidebar from '../components/Sidebar';
import Topbar from '../components/Topbar';
import StatCard from '../components/StatCard';
import BaseModal from '../components/BaseModal';
import ApiMessage from '../components/ApiMessage';

function VetDiagnosePage() {
  const {
    vetDiagnoses,
    vetReviewRequests,
    vetDiseases,
    vetMedicines,
    saveVetDiagnosis,
    updateVetRequestStatus
  } = useContext(FarmContext);

  const [searchTerm, setSearchTerm] = useState('');
  const [dictamenFilter, setDictamenFilter] = useState('Todos');
  const [severityFilter, setSeverityFilter] = useState('Todas');
  const [toastMsg, setToastMsg] = useState('');

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [detailDiag, setDetailDiag] = useState(null);

  const [form, setForm] = useState({
    id: '',
    solicitudId: '#0002',
    galpon: 'Galpón 2',
    lote: '#LDP-002',
    fecha: '2026-09-20',
    enfermedad: vetDiseases[0]?.nombre || 'Colibacilosis Aviar',
    severidad: 'Moderada',
    medicina: vetMedicines[0]?.nombre || 'Enrofloxacina 10% Solución Oral',
    dosis: '1 ml / 2 L de agua por 5 días',
    dictamen: 'En tratamiento',
    observaciones:
      'Aislamiento preventivo del galpón y seguimiento de consumo de agua cada 12 horas.'
  });

  const pendingRequests = vetReviewRequests.filter(
    (r) => r.estado === 'Pendiente'
  );

  const filteredDiagnoses = vetDiagnoses.filter((row) => {
    const q = searchTerm.trim().toLowerCase();
    const matchesSearch =
      !q ||
      row.id.toLowerCase().includes(q) ||
      row.galpon.toLowerCase().includes(q) ||
      row.lote.toLowerCase().includes(q) ||
      row.enfermedad.toLowerCase().includes(q) ||
      row.medicina.toLowerCase().includes(q);
    const matchesDictamen =
      dictamenFilter === 'Todos' || row.dictamen === dictamenFilter;
    const matchesSeverity =
      severityFilter === 'Todas' || row.severidad === severityFilter;
    return matchesSearch && matchesDictamen && matchesSeverity;
  });

  const handleOpenNew = (req = null) => {
    const nextNum = String(vetDiagnoses.length + 1).padStart(3, '0');
    const defaultMed = vetMedicines[0];
    setForm({
      id: `#DIAG-${nextNum}`,
      solicitudId: req ? req.id : 'Directo',
      galpon: req ? req.galpon : 'Galpón 2',
      lote: req?.lote || `#LDP-00${(req?.galpon || '2').replace(/\D/g, '') || '2'}`,
      fecha: '2026-09-20',
      enfermedad: vetDiseases[2]?.nombre || vetDiseases[0]?.nombre || 'Colibacilosis Aviar',
      severidad: 'Moderada',
      medicina: defaultMed?.nombre || 'Enrofloxacina 10% Solución Oral',
      dosis: defaultMed?.dosisRecomendada || '1 ml / 2 L de agua por 5 días',
      dictamen: 'En tratamiento',
      observaciones: req?.motivo
        ? `Atención a solicitud ${req.id} (${req.motivo}). Se instaura protocolo terapéutico.`
        : 'Evaluación clínica en galpón y prescripción veterinaria controlada.'
    });
    setIsModalOpen(true);
  };

  const handleOpenEdit = (diag) => {
    setForm({ ...diag });
    setIsModalOpen(true);
  };

  const handleMedicineChange = (medName) => {
    const found = vetMedicines.find((m) => m.nombre === medName);
    setForm((prev) => ({
      ...prev,
      medicina: medName,
      dosis: found ? found.dosisRecomendada : prev.dosis
    }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    saveVetDiagnosis(form);
    setIsModalOpen(false);
    setToastMsg(
      `Diagnóstico ${form.id} registrado para ${form.galpon} (${form.dictamen}).`
    );
  };

  const getDictamenBadge = (dictamen) => {
    if (dictamen === 'Sacrificio') {
      return { bg: '#ffe0e3', color: '#ed2b33', dot: '/assets/dot-red.svg' };
    }
    if (dictamen === 'En tratamiento') {
      return { bg: '#ffe8ba', color: '#bf5400', dot: '/assets/dot-orange.svg' };
    }
    return {
      bg: '#d6f0d1',
      color: '#1f5929',
      dot: '/assets/vet-dot-aceptada.svg'
    };
  };

  return (
    <div className="avi-role-layout">
      <Sidebar role="veterinario" />

      <div className="avi-main-frame">
        <Topbar roleLabel="Veterinario" avatarSrc="/assets/vet-avatar.svg" />
        <ApiMessage
          message={toastMsg}
          type="success"
          onClose={() => setToastMsg('')}
        />

        <div className="avi-dashboard-content">
          {/* Subtítulo superior y botón de acción */}
          <div className="avi-page-heading">
            <div className="avi-heading-copy">
              <p className="avi-page-subtitle">
                Evaluación clínica de galpones, atención a solicitudes de revisión y prescripción terapéutica
              </p>
            </div>

            <button
              type="button"
              className="avi-btn-green-cta"
              onClick={() => handleOpenNew(null)}
            >
              <span>＋  Nuevo diagnóstico clínico</span>
            </button>
          </div>

          {/* 2 Tarjetas KPI */}
          <div className="avi-metrics-row">
            <StatCard
              label="Solicitudes por diagnosticar"
              value={`${pendingRequests.length} Pendiente(s)`}
              status={`De ${vetReviewRequests.length} solicitudes recibidas`}
              statusColor="#9e4a0f"
              icon="/assets/vet-bell.svg"
              tileBg="#f4eee3"
              width="437px"
            />
            <StatCard
              label="Diagnósticos realizados"
              value={`${vetDiagnoses.length} Evaluaciones`}
              status="Historial clínico activo"
              statusColor="#2d743b"
              icon="/assets/vet-stethoscope.svg"
              tileBg="#e6f7e4"
              width="437px"
            />
          </div>

          {/* Solicitudes de revisión enviadas por trabajadores */}
          {pendingRequests.length > 0 && (
            <div className="avi-recent-orders-panel">
              <div className="avi-panel-header avi-panel-header-full">
                <div className="avi-panel-icon-28">
                  <img alt="" src="/assets/vet-bell.svg" />
                </div>
                <h2 className="avi-panel-title">
                  Solicitudes de revisión pendientes de dictamen
                </h2>
              </div>

              <div className="avi-orders-table">
                <div className="avi-orders-thead">
                  <span style={{ flex: '0.8' }}>Solicitud</span>
                  <span style={{ flex: '1.1' }}>Galpón / Lote</span>
                  <span style={{ flex: '2.2' }}>Motivo reportado en granja</span>
                  <span style={{ flex: '1' }}>Fecha</span>
                  <span style={{ flex: '1.3', textAlign: 'right' }}>Acción clínica</span>
                </div>

                {pendingRequests.map((req) => (
                  <div key={req.id} className="avi-orders-trow">
                    <span
                      className="avi-col-dark"
                      style={{ flex: '0.8', fontWeight: 700 }}
                    >
                      {req.id}
                    </span>
                    <span className="avi-col-dark" style={{ flex: '1.1', fontWeight: 600 }}>
                      {req.galpon} {req.lote ? `(${req.lote})` : ''}
                    </span>
                    <span className="avi-col-muted" style={{ flex: '2.2' }}>
                      {req.motivo || 'Revisión sanitaria solicitada por operario de galpón'}
                    </span>
                    <span className="avi-col-muted" style={{ flex: '1' }}>
                      {req.fecha}
                    </span>
                    <div
                      style={{
                        flex: '1.3',
                        display: 'flex',
                        justifyContent: 'flex-end',
                        gap: '6px'
                      }}
                    >
                      <button
                        type="button"
                        className="avi-admin-table-btn avi-admin-table-btn-green"
                        onClick={() => handleOpenNew(req)}
                      >
                        Diagnosticar
                      </button>
                      <button
                        type="button"
                        className="avi-admin-table-btn avi-admin-table-btn-orange"
                        onClick={() => {
                          updateVetRequestStatus(req.id, 'Rechazada');
                          setToastMsg(`Solicitud ${req.id} descartada.`);
                        }}
                      >
                        Descartar
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Barra de búsqueda y filtro */}
          <form
            className="avi-receptions-filter-bar"
            onSubmit={(e) => e.preventDefault()}
          >
            <input
              type="text"
              className="avi-receptions-search-input"
              placeholder="Buscar por diagnóstico, galpón, enfermedad o medicina recetada..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />

            <select
              className="avi-receptions-select"
              value={dictamenFilter}
              onChange={(e) => setDictamenFilter(e.target.value)}
            >
              <option value="Todos">Todos los dictámenes</option>
              <option value="En tratamiento">En tratamiento</option>
              <option value="Reintegro">Reintegro</option>
              <option value="Sacrificio">Sacrificio</option>
            </select>

            <select
              className="avi-receptions-select"
              value={severityFilter}
              onChange={(e) => setSeverityFilter(e.target.value)}
            >
              <option value="Todas">Todas las severidades</option>
              <option value="Leve">Severidad Leve</option>
              <option value="Moderada">Severidad Moderada</option>
              <option value="Crítica">Severidad Crítica</option>
            </select>

            <button
              type="button"
              className="avi-receptions-filter-btn"
              onClick={() => {
                setSearchTerm('');
                setDictamenFilter('Todos');
                setSeverityFilter('Todas');
              }}
            >
              Filtrar
            </button>
          </form>

          {/* Tabla de Diagnósticos Realizados */}
          <div className="avi-recent-orders-panel">
            <div className="avi-panel-header avi-panel-header-full">
              <div className="avi-panel-icon-28">
                <img alt="" src="/assets/vet-stethoscope.svg" />
              </div>
              <h2 className="avi-panel-title">
                Historial de diagnósticos y tratamientos por galpón
              </h2>
            </div>

            <div className="avi-orders-table">
              <div className="avi-orders-thead">
                <span style={{ flex: '0.9' }}>Código</span>
                <span style={{ flex: '1.1' }}>Galpón / Lote</span>
                <span style={{ flex: '1.4' }}>Enfermedad diagnosticada</span>
                <span style={{ flex: '1.5' }}>Medicina prescrita</span>
                <span style={{ flex: '1' }}>Dictamen</span>
                <span style={{ flex: '1.2', textAlign: 'right' }}>Acciones</span>
              </div>

              {filteredDiagnoses.map((row) => {
                const st = getDictamenBadge(row.dictamen);
                return (
                  <div key={row.id} className="avi-orders-trow">
                    <span
                      className="avi-col-dark"
                      style={{ flex: '0.9', fontWeight: 700 }}
                    >
                      {row.id}
                    </span>
                    <span className="avi-col-dark" style={{ flex: '1.1', fontWeight: 600 }}>
                      {row.galpon} ({row.lote})
                    </span>
                    <span className="avi-col-dark" style={{ flex: '1.4' }}>
                      {row.enfermedad}
                    </span>
                    <span className="avi-col-muted" style={{ flex: '1.5' }}>
                      {row.medicina}
                    </span>
                    <div style={{ flex: '1', display: 'flex', alignItems: 'center' }}>
                      <div
                        className="avi-crit-badge"
                        style={{ backgroundColor: st.bg }}
                      >
                        <div className="avi-crit-dot">
                          <img alt="" src={st.dot} />
                        </div>
                        <span style={{ color: st.color }}>{row.dictamen}</span>
                      </div>
                    </div>
                    <div
                      style={{
                        flex: '1.2',
                        display: 'flex',
                        justifyContent: 'flex-end',
                        gap: '6px'
                      }}
                    >
                      <button
                        type="button"
                        className="avi-admin-table-btn avi-admin-table-btn-green"
                        onClick={() => setDetailDiag(row)}
                      >
                        Detalle
                      </button>
                      <button
                        type="button"
                        className="avi-admin-table-btn avi-admin-table-btn-orange"
                        onClick={() => handleOpenEdit(row)}
                      >
                        Editar
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      </div>

      {/* Modal: Emitir / Editar Diagnóstico Clínico */}
      <BaseModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        maxWidth="720px"
      >
        <form className="avi-figma-modal-720" onSubmit={handleSubmit}>
          <div className="avi-figma-modal-header">
            <div className="avi-figma-modal-titles">
              <h2 className="avi-figma-modal-title">
                Diagnóstico clínico veterinario ({form.id})
              </h2>
              <p className="avi-figma-modal-subtitle">
                Vincula patología, tratamiento farmacológico y dictamen operativo del galpón
              </p>
            </div>
            <button
              type="button"
              className="avi-figma-modal-close"
              onClick={() => setIsModalOpen(false)}
            >
              ×
            </button>
          </div>

          <div className="avi-figma-modal-grid">
            <div className="avi-figma-field">
              <label className="avi-figma-label">Galpón evaluado</label>
              <div className="avi-figma-select-wrap">
                <select
                  className="avi-figma-input avi-figma-select"
                  value={form.galpon}
                  onChange={(e) =>
                    setForm({
                      ...form,
                      galpon: e.target.value,
                      lote: `#LDP-00${e.target.value.replace(/\D/g, '') || '1'}`
                    })
                  }
                >
                  {[
                    'Galpón 1',
                    'Galpón 2',
                    'Galpón 3',
                    'Galpón 4',
                    'Galpón 5',
                    'Galpón 6',
                    'Galpón 7',
                    'Galpón 9'
                  ].map((g) => (
                    <option key={g} value={g}>
                      {g}
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
              <label className="avi-figma-label">Lote y fecha de evaluación</label>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
                <input
                  type="text"
                  className="avi-figma-input"
                  value={form.lote}
                  onChange={(e) => setForm({ ...form, lote: e.target.value })}
                  required
                />
                <input
                  type="date"
                  className="avi-figma-input"
                  value={form.fecha}
                  onChange={(e) => setForm({ ...form, fecha: e.target.value })}
                  required
                />
              </div>
            </div>

            <div className="avi-figma-field">
              <label className="avi-figma-label">Enfermedad diagnosticada</label>
              <div className="avi-figma-select-wrap">
                <select
                  className="avi-figma-input avi-figma-select"
                  value={form.enfermedad}
                  onChange={(e) =>
                    setForm({ ...form, enfermedad: e.target.value })
                  }
                >
                  {vetDiseases.map((d) => (
                    <option key={d.id} value={d.nombre}>
                      {d.nombre} ({d.tipo})
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
              <label className="avi-figma-label">Severidad del cuadro clínico</label>
              <div className="avi-figma-select-wrap">
                <select
                  className="avi-figma-input avi-figma-select"
                  value={form.severidad}
                  onChange={(e) =>
                    setForm({ ...form, severidad: e.target.value })
                  }
                >
                  <option value="Leve">Leve</option>
                  <option value="Moderada">Moderada</option>
                  <option value="Crítica">Crítica</option>
                </select>
                <img
                  alt=""
                  src="/assets/admin-select-chevron.svg"
                  className="avi-figma-select-chevron"
                />
              </div>
            </div>

            <div className="avi-figma-field">
              <label className="avi-figma-label">Medicina prescrita</label>
              <div className="avi-figma-select-wrap">
                <select
                  className="avi-figma-input avi-figma-select"
                  value={form.medicina}
                  onChange={(e) => handleMedicineChange(e.target.value)}
                >
                  {vetMedicines.map((m) => (
                    <option key={m.id} value={m.nombre}>
                      {m.nombre} (Retiro: {m.tiempoRetiroDias}d)
                    </option>
                  ))}
                  <option value="Sin tratamiento curativo (Brote agudo)">
                    Sin tratamiento curativo (Brote agudo)
                  </option>
                </select>
                <img
                  alt=""
                  src="/assets/admin-select-chevron.svg"
                  className="avi-figma-select-chevron"
                />
              </div>
            </div>

            <div className="avi-figma-field">
              <label className="avi-figma-label">Dictamen sanitario del galpón</label>
              <div className="avi-figma-select-wrap">
                <select
                  className="avi-figma-input avi-figma-select"
                  value={form.dictamen}
                  onChange={(e) =>
                    setForm({ ...form, dictamen: e.target.value })
                  }
                >
                  <option value="En tratamiento">En tratamiento / Aislamiento</option>
                  <option value="Reintegro">Reintegro a producción</option>
                  <option value="Sacrificio">Sacrificio sanitario</option>
                </select>
                <img
                  alt=""
                  src="/assets/admin-select-chevron.svg"
                  className="avi-figma-select-chevron"
                />
              </div>
            </div>

            <div className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
              <label className="avi-figma-label">Posología y duración del tratamiento</label>
              <input
                type="text"
                className="avi-figma-input"
                value={form.dosis}
                onChange={(e) => setForm({ ...form, dosis: e.target.value })}
                required
              />
            </div>

            <div className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
              <label className="avi-figma-label">Observaciones clínicas y manejo</label>
              <input
                type="text"
                className="avi-figma-input"
                value={form.observaciones}
                onChange={(e) =>
                  setForm({ ...form, observaciones: e.target.value })
                }
                required
              />
            </div>
          </div>

          <div className="avi-figma-modal-footer">
            <button
              type="button"
              className="avi-figma-btn-cancel"
              onClick={() => setIsModalOpen(false)}
            >
              Cancelar
            </button>
            <button type="submit" className="avi-figma-btn-green">
              Guardar diagnóstico clínico
            </button>
          </div>
        </form>
      </BaseModal>

      {/* Modal: Detalle de Diagnóstico */}
      <BaseModal
        isOpen={Boolean(detailDiag)}
        onClose={() => setDetailDiag(null)}
        maxWidth="680px"
      >
        {detailDiag && (
          <div className="avi-figma-modal-720">
            <div className="avi-figma-modal-header">
              <div className="avi-figma-modal-titles">
                <h2 className="avi-figma-modal-title">
                  Ficha diagnóstica {detailDiag.id} · {detailDiag.galpon}
                </h2>
                <p className="avi-figma-modal-subtitle">
                  Lote {detailDiag.lote} · Fecha de evaluación: {detailDiag.fecha}
                </p>
              </div>
              <button
                type="button"
                className="avi-figma-modal-close"
                onClick={() => setDetailDiag(null)}
              >
                ×
              </button>
            </div>

            <div className="avi-figma-modal-grid">
              <div className="avi-figma-field">
                <label className="avi-figma-label">Enfermedad y Severidad</label>
                <div className="avi-figma-readonly-box">
                  {detailDiag.enfermedad} · Severidad {detailDiag.severidad}
                </div>
              </div>
              <div className="avi-figma-field">
                <label className="avi-figma-label">Dictamen operativo</label>
                <div className="avi-figma-valid-box">{detailDiag.dictamen}</div>
              </div>
              <div className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
                <label className="avi-figma-label">
                  Medicina prescrita y posología
                </label>
                <div className="avi-figma-readonly-box">
                  {detailDiag.medicina} — {detailDiag.dosis}
                </div>
              </div>
              <div className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
                <label className="avi-figma-label">Observaciones clínicas</label>
                <div className="avi-figma-demand-box">
                  {detailDiag.observaciones}
                </div>
              </div>
            </div>

            <div className="avi-figma-modal-footer">
              <button
                type="button"
                className="avi-figma-btn-cancel"
                onClick={() => setDetailDiag(null)}
              >
                Cerrar
              </button>
              <button
                type="button"
                className="avi-figma-btn-orange"
                onClick={() => {
                  const target = detailDiag;
                  setDetailDiag(null);
                  handleOpenEdit(target);
                }}
              >
                Editar diagnóstico
              </button>
            </div>
          </div>
        )}
      </BaseModal>
    </div>
  );
}

export default VetDiagnosePage;
