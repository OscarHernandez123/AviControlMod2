import { useContext, useState } from 'react';
import { FarmContext } from '../context/FarmContext';
import Sidebar from '../components/Sidebar';
import Topbar from '../components/Topbar';
import StatCard from '../components/StatCard';
import BaseModal from '../components/BaseModal';
import ApiMessage from '../components/ApiMessage';

function VetDiseasesPage() {
  const { vetDiseases, saveVetDisease } = useContext(FarmContext);

  const [searchTerm, setSearchTerm] = useState('');
  const [typeFilter, setTypeFilter] = useState('Todas');
  const [riskFilter, setRiskFilter] = useState('Todos');
  const [toastMsg, setToastMsg] = useState('');

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [detailDisease, setDetailDisease] = useState(null);

  const [form, setForm] = useState({
    id: '',
    nombre: '',
    tipo: 'Viral',
    agente: '',
    riesgo: 'Alto',
    aislamientoObligatorio: true,
    sintomas: '',
    protocolo: ''
  });

  const filteredDiseases = vetDiseases.filter((row) => {
    const q = searchTerm.trim().toLowerCase();
    const matchesSearch =
      !q ||
      row.id.toLowerCase().includes(q) ||
      row.nombre.toLowerCase().includes(q) ||
      row.agente.toLowerCase().includes(q) ||
      row.sintomas.toLowerCase().includes(q);
    const matchesType = typeFilter === 'Todas' || row.tipo === typeFilter;
    const matchesRisk = riskFilter === 'Todos' || row.riesgo === riskFilter;
    return matchesSearch && matchesType && matchesRisk;
  });

  const highRiskCount = vetDiseases.filter(
    (d) => d.riesgo === 'Alto' || d.aislamientoObligatorio
  ).length;

  const handleOpenNew = () => {
    const nextNum = String(vetDiseases.length + 1).padStart(3, '0');
    setForm({
      id: `#ENF-${nextNum}`,
      nombre: '',
      tipo: 'Viral',
      agente: '',
      riesgo: 'Alto',
      aislamientoObligatorio: true,
      sintomas: '',
      protocolo: ''
    });
    setIsModalOpen(true);
  };

  const handleOpenEdit = (disease) => {
    setForm({ ...disease });
    setIsModalOpen(true);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    saveVetDisease(form);
    setIsModalOpen(false);
    setToastMsg(
      `Enfermedad "${form.nombre}" (${form.id}) guardada en el catálogo sanitario.`
    );
  };

  const getRiskBadge = (riesgo) => {
    if (riesgo === 'Alto') {
      return { bg: '#ffe0e3', color: '#ed2b33', dot: '/assets/dot-red.svg' };
    }
    if (riesgo === 'Medio') {
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
                Catálogo y registro de patologías aviares, sintomatología clínica y protocolos de aislamiento
              </p>
            </div>

            <button
              type="button"
              className="avi-btn-green-cta"
              onClick={handleOpenNew}
            >
              <span>＋  Registrar enfermedad</span>
            </button>
          </div>

          {/* 2 Tarjetas KPI */}
          <div className="avi-metrics-row">
            <StatCard
              label="Enfermedades registradas"
              value={`${vetDiseases.length} Patologías`}
              status="Catálogo epidemiológico activo"
              statusColor="#2d743b"
              icon="/assets/vet-stethoscope.svg"
              tileBg="#e6f7e4"
              width="437px"
            />
            <StatCard
              label="Patologías de cuarentena"
              value={`${highRiskCount} Alto riesgo`}
              status="Exigen aislamiento obligatorio"
              statusColor="#9e4a0f"
              icon="/assets/vet-bell.svg"
              tileBg="#f4eee3"
              width="437px"
            />
          </div>

          {/* Barra de búsqueda y filtro */}
          <form
            className="avi-receptions-filter-bar"
            onSubmit={(e) => e.preventDefault()}
          >
            <input
              type="text"
              className="avi-receptions-search-input"
              placeholder="Buscar por enfermedad, agente causal o síntomas..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />

            <select
              className="avi-receptions-select"
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value)}
            >
              <option value="Todas">Todos los tipos</option>
              <option value="Viral">Viral</option>
              <option value="Bacteriana">Bacteriana</option>
              <option value="Parasitaria">Parasitaria</option>
              <option value="Metabólica">Metabólica</option>
            </select>

            <select
              className="avi-receptions-select"
              value={riskFilter}
              onChange={(e) => setRiskFilter(e.target.value)}
            >
              <option value="Todos">Todos los niveles de riesgo</option>
              <option value="Alto">Riesgo Alto</option>
              <option value="Medio">Riesgo Medio</option>
              <option value="Bajo">Riesgo Bajo</option>
            </select>

            <button
              type="button"
              className="avi-receptions-filter-btn"
              onClick={() => {
                setSearchTerm('');
                setTypeFilter('Todas');
                setRiskFilter('Todos');
              }}
            >
              Filtrar
            </button>
          </form>

          {/* Tabla de Enfermedades */}
          <div className="avi-recent-orders-panel">
            <div className="avi-panel-header avi-panel-header-full">
              <div className="avi-panel-icon-28">
                <img alt="" src="/assets/admin-orders-icon.svg" />
              </div>
              <h2 className="avi-panel-title">
                Catálogo de enfermedades y criterios diagnósticos
              </h2>
            </div>

            <div className="avi-orders-table">
              <div className="avi-orders-thead">
                <span style={{ flex: '0.8' }}>Código</span>
                <span style={{ flex: '1.5' }}>Nombre patología</span>
                <span style={{ flex: '1' }}>Clasificación</span>
                <span style={{ flex: '1.6' }}>Agente causal</span>
                <span style={{ flex: '1' }}>Riesgo</span>
                <span style={{ flex: '1' }}>Aislamiento</span>
                <span style={{ flex: '1.2', textAlign: 'right' }}>Acciones</span>
              </div>

              {filteredDiseases.map((row) => {
                const rk = getRiskBadge(row.riesgo);
                return (
                  <div key={row.id} className="avi-orders-trow">
                    <span
                      className="avi-col-dark"
                      style={{ flex: '0.8', fontWeight: 700 }}
                    >
                      {row.id}
                    </span>
                    <span
                      className="avi-col-dark"
                      style={{ flex: '1.5', fontWeight: 600 }}
                    >
                      {row.nombre}
                    </span>
                    <span className="avi-col-dark" style={{ flex: '1' }}>
                      {row.tipo}
                    </span>
                    <span className="avi-col-muted" style={{ flex: '1.6' }}>
                      {row.agente}
                    </span>
                    <div style={{ flex: '1', display: 'flex', alignItems: 'center' }}>
                      <div
                        className="avi-crit-badge"
                        style={{ backgroundColor: rk.bg }}
                      >
                        <div className="avi-crit-dot">
                          <img alt="" src={rk.dot} />
                        </div>
                        <span style={{ color: rk.color }}>{row.riesgo}</span>
                      </div>
                    </div>
                    <span
                      className="avi-col-dark"
                      style={{
                        flex: '1',
                        fontWeight: 600,
                        color: row.aislamientoObligatorio ? '#b45309' : '#2d743b'
                      }}
                    >
                      {row.aislamientoObligatorio ? 'Obligatorio' : 'Preventivo'}
                    </span>
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
                        onClick={() => setDetailDisease(row)}
                      >
                        Ver ficha
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

      {/* Modal: Registrar / Editar Enfermedad */}
      <BaseModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        maxWidth="720px"
      >
        <form className="avi-figma-modal-720" onSubmit={handleSubmit}>
          <div className="avi-figma-modal-header">
            <div className="avi-figma-modal-titles">
              <h2 className="avi-figma-modal-title">
                Registrar enfermedad aviar ({form.id})
              </h2>
              <p className="avi-figma-modal-subtitle">
                Estandariza patologías para los diagnósticos clínicos y protocolos de bioseguridad
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
              <label className="avi-figma-label">Nombre de la patología</label>
              <input
                type="text"
                className="avi-figma-input"
                placeholder="Ej. Gumboro (Infección de la Bolsa de Fabricio)"
                value={form.nombre}
                onChange={(e) => setForm({ ...form, nombre: e.target.value })}
                required
              />
            </div>

            <div className="avi-figma-field">
              <label className="avi-figma-label">Clasificación etiológica</label>
              <div className="avi-figma-select-wrap">
                <select
                  className="avi-figma-input avi-figma-select"
                  value={form.tipo}
                  onChange={(e) => setForm({ ...form, tipo: e.target.value })}
                >
                  <option value="Viral">Viral</option>
                  <option value="Bacteriana">Bacteriana</option>
                  <option value="Parasitaria">Parasitaria</option>
                  <option value="Metabólica">Metabólica</option>
                </select>
                <img
                  alt=""
                  src="/assets/admin-select-chevron.svg"
                  className="avi-figma-select-chevron"
                />
              </div>
            </div>

            <div className="avi-figma-field">
              <label className="avi-figma-label">Agente causal</label>
              <input
                type="text"
                className="avi-figma-input"
                placeholder="Ej. Birnavirus aviar / Salmonella spp."
                value={form.agente}
                onChange={(e) => setForm({ ...form, agente: e.target.value })}
                required
              />
            </div>

            <div className="avi-figma-field">
              <label className="avi-figma-label">
                Nivel de riesgo y aislamiento
              </label>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
                <div className="avi-figma-select-wrap">
                  <select
                    className="avi-figma-input avi-figma-select"
                    value={form.riesgo}
                    onChange={(e) => setForm({ ...form, riesgo: e.target.value })}
                  >
                    <option value="Alto">Riesgo Alto</option>
                    <option value="Medio">Riesgo Medio</option>
                    <option value="Bajo">Riesgo Bajo</option>
                  </select>
                  <img
                    alt=""
                    src="/assets/admin-select-chevron.svg"
                    className="avi-figma-select-chevron"
                  />
                </div>
                <div className="avi-figma-select-wrap">
                  <select
                    className="avi-figma-input avi-figma-select"
                    value={form.aislamientoObligatorio ? 'si' : 'no'}
                    onChange={(e) =>
                      setForm({
                        ...form,
                        aislamientoObligatorio: e.target.value === 'si'
                      })
                    }
                  >
                    <option value="si">Aislamiento Obligatorio</option>
                    <option value="no">Manejo Preventivo</option>
                  </select>
                  <img
                    alt=""
                    src="/assets/admin-select-chevron.svg"
                    className="avi-figma-select-chevron"
                  />
                </div>
              </div>
            </div>

            <div className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
              <label className="avi-figma-label">
                Signos clínicos y lesiones características
              </label>
              <input
                type="text"
                className="avi-figma-input"
                placeholder="Ej. Depresión aguda, diarrea blanquecina, postración..."
                value={form.sintomas}
                onChange={(e) => setForm({ ...form, sintomas: e.target.value })}
                required
              />
            </div>

            <div className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
              <label className="avi-figma-label">
                Protocolo sanitario y de bioseguridad recomendado
              </label>
              <input
                type="text"
                className="avi-figma-input"
                placeholder="Ej. Cuarentena de galpón, desinfección de equipos..."
                value={form.protocolo}
                onChange={(e) => setForm({ ...form, protocolo: e.target.value })}
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
              Guardar enfermedad
            </button>
          </div>
        </form>
      </BaseModal>

      {/* Modal: Ficha Clínica de Enfermedad */}
      <BaseModal
        isOpen={Boolean(detailDisease)}
        onClose={() => setDetailDisease(null)}
        maxWidth="680px"
      >
        {detailDisease && (
          <div className="avi-figma-modal-720">
            <div className="avi-figma-modal-header">
              <div className="avi-figma-modal-titles">
                <h2 className="avi-figma-modal-title">
                  {detailDisease.nombre} ({detailDisease.id})
                </h2>
                <p className="avi-figma-modal-subtitle">
                  Clasificación {detailDisease.tipo} · Agente: {detailDisease.agente}
                </p>
              </div>
              <button
                type="button"
                className="avi-figma-modal-close"
                onClick={() => setDetailDisease(null)}
              >
                ×
              </button>
            </div>

            <div className="avi-figma-modal-grid">
              <div className="avi-figma-field">
                <label className="avi-figma-label">Nivel de riesgo</label>
                <div className="avi-figma-readonly-box">
                  Riesgo {detailDisease.riesgo}
                </div>
              </div>
              <div className="avi-figma-field">
                <label className="avi-figma-label">Medida de contención</label>
                <div className="avi-figma-valid-box">
                  {detailDisease.aislamientoObligatorio
                    ? 'Aislamiento obligatorio de galpón'
                    : 'Manejo preventivo en lote'}
                </div>
              </div>
              <div className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
                <label className="avi-figma-label">Sintomatología principal</label>
                <div className="avi-figma-readonly-box">
                  {detailDisease.sintomas}
                </div>
              </div>
              <div className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
                <label className="avi-figma-label">Protocolo recomendado</label>
                <div className="avi-figma-demand-box">
                  {detailDisease.protocolo}
                </div>
              </div>
            </div>

            <div className="avi-figma-modal-footer">
              <button
                type="button"
                className="avi-figma-btn-cancel"
                onClick={() => setDetailDisease(null)}
              >
                Cerrar
              </button>
              <button
                type="button"
                className="avi-figma-btn-orange"
                onClick={() => {
                  const target = detailDisease;
                  setDetailDisease(null);
                  handleOpenEdit(target);
                }}
              >
                Editar enfermedad
              </button>
            </div>
          </div>
        )}
      </BaseModal>
    </div>
  );
}

export default VetDiseasesPage;
