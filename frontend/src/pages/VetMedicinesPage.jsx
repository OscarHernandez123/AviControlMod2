import { useContext, useState } from 'react';
import { FarmContext } from '../context/FarmContext';
import Sidebar from '../components/Sidebar';
import Topbar from '../components/Topbar';
import StatCard from '../components/StatCard';
import BaseModal from '../components/BaseModal';
import ApiMessage from '../components/ApiMessage';

function VetMedicinesPage() {
  const { vetMedicines, saveVetMedicine } = useContext(FarmContext);

  const [searchTerm, setSearchTerm] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('Todas');
  const [routeFilter, setRouteFilter] = useState('Todas');
  const [toastMsg, setToastMsg] = useState('');

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [detailMed, setDetailMed] = useState(null);

  const [form, setForm] = useState({
    id: '',
    nombre: '',
    categoria: 'Antibiótico',
    principioActivo: '',
    via: 'Agua de bebida',
    dosisRecomendada: '',
    tiempoRetiroDias: 5,
    indicaciones: ''
  });

  const filteredMedicines = vetMedicines.filter((row) => {
    const q = searchTerm.trim().toLowerCase();
    const matchesSearch =
      !q ||
      row.id.toLowerCase().includes(q) ||
      row.nombre.toLowerCase().includes(q) ||
      row.principioActivo.toLowerCase().includes(q) ||
      row.indicaciones.toLowerCase().includes(q);
    const matchesCategory =
      categoryFilter === 'Todas' || row.categoria === categoryFilter;
    const matchesRoute = routeFilter === 'Todas' || row.via === routeFilter;
    return matchesSearch && matchesCategory && matchesRoute;
  });

  const withWithdrawalCount = vetMedicines.filter(
    (m) => Number(m.tiempoRetiroDias) > 0
  ).length;

  const handleOpenNew = () => {
    const nextNum = String(vetMedicines.length + 1).padStart(3, '0');
    setForm({
      id: `#MED-${nextNum}`,
      nombre: '',
      categoria: 'Antibiótico',
      principioActivo: '',
      via: 'Agua de bebida',
      dosisRecomendada: '',
      tiempoRetiroDias: 5,
      indicaciones: ''
    });
    setIsModalOpen(true);
  };

  const handleOpenEdit = (med) => {
    setForm({ ...med });
    setIsModalOpen(true);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    saveVetMedicine({
      ...form,
      tiempoRetiroDias: Math.max(0, parseInt(form.tiempoRetiroDias, 10) || 0)
    });
    setIsModalOpen(false);
    setToastMsg(
      `Medicina "${form.nombre}" (${form.id}) guardada en el vademécum veterinario.`
    );
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
                Vademécum farmacológico, posología recomendada y control de tiempos de retiro previo a sacrificio
              </p>
            </div>

            <button
              type="button"
              className="avi-btn-green-cta"
              onClick={handleOpenNew}
            >
              <span>＋  Registrar medicina</span>
            </button>
          </div>

          {/* 2 Tarjetas KPI */}
          <div className="avi-metrics-row">
            <StatCard
              label="Medicinas registradas"
              value={`${vetMedicines.length} Fármacos`}
              status="Vademécum veterinario activo"
              statusColor="#2d743b"
              icon="/assets/vet-syringe.svg"
              tileBg="#e6f7e4"
              width="437px"
            />
            <StatCard
              label="Con tiempo de retiro"
              value={`${withWithdrawalCount} Medicinas`}
              status="Control previo a cosecha"
              statusColor="#9e4a0f"
              icon="/assets/vet-stethoscope.svg"
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
              placeholder="Buscar por nombre comercial, principio activo o indicación..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />

            <select
              className="avi-receptions-select"
              value={categoryFilter}
              onChange={(e) => setCategoryFilter(e.target.value)}
            >
              <option value="Todas">Todas las categorías</option>
              <option value="Antibiótico">Antibiótico</option>
              <option value="Vacuna">Vacuna</option>
              <option value="Antiparasitario">Antiparasitario</option>
              <option value="Vitamina / Electrolito">Vitamina / Electrolito</option>
            </select>

            <select
              className="avi-receptions-select"
              value={routeFilter}
              onChange={(e) => setRouteFilter(e.target.value)}
            >
              <option value="Todas">Todas las vías</option>
              <option value="Agua de bebida">Agua de bebida</option>
              <option value="Ocular / Agua">Ocular / Agua</option>
              <option value="Alimento">Alimento</option>
              <option value="Inyectable">Inyectable</option>
            </select>

            <button
              type="button"
              className="avi-receptions-filter-btn"
              onClick={() => {
                setSearchTerm('');
                setCategoryFilter('Todas');
                setRouteFilter('Todas');
              }}
            >
              Filtrar
            </button>
          </form>

          {/* Tabla de Medicinas */}
          <div className="avi-recent-orders-panel">
            <div className="avi-panel-header avi-panel-header-full">
              <div className="avi-panel-icon-28">
                <img alt="" src="/assets/vet-syringe.svg" />
              </div>
              <h2 className="avi-panel-title">
                Catálogo de medicinas, dosis y tiempos de retiro
              </h2>
            </div>

            <div className="avi-orders-table">
              <div className="avi-orders-thead">
                <span style={{ flex: '0.8' }}>Código</span>
                <span style={{ flex: '1.6' }}>Medicamento</span>
                <span style={{ flex: '1.1' }}>Categoría</span>
                <span style={{ flex: '1.1' }}>Vía</span>
                <span style={{ flex: '1.8' }}>Posología recomendada</span>
                <span style={{ flex: '1' }}>Retiro</span>
                <span style={{ flex: '1.2', textAlign: 'right' }}>Acciones</span>
              </div>

              {filteredMedicines.map((row) => {
                const hasRetiro = Number(row.tiempoRetiroDias) > 0;
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
                      style={{ flex: '1.6', fontWeight: 600 }}
                    >
                      {row.nombre}
                    </span>
                    <span className="avi-col-dark" style={{ flex: '1.1' }}>
                      {row.categoria}
                    </span>
                    <span className="avi-col-muted" style={{ flex: '1.1' }}>
                      {row.via}
                    </span>
                    <span className="avi-col-muted" style={{ flex: '1.8' }}>
                      {row.dosisRecomendada}
                    </span>
                    <div style={{ flex: '1', display: 'flex', alignItems: 'center' }}>
                      <div
                        className="avi-crit-badge"
                        style={{
                          backgroundColor: hasRetiro ? '#ffe8ba' : '#d6f0d1'
                        }}
                      >
                        <div className="avi-crit-dot">
                          <img
                            alt=""
                            src={
                              hasRetiro
                                ? '/assets/dot-orange.svg'
                                : '/assets/vet-dot-aceptada.svg'
                            }
                          />
                        </div>
                        <span
                          style={{
                            color: hasRetiro ? '#bf5400' : '#1f5929'
                          }}
                        >
                          {hasRetiro
                            ? `${row.tiempoRetiroDias} días`
                            : '0 días'}
                        </span>
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
                        onClick={() => setDetailMed(row)}
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

      {/* Modal: Registrar / Editar Medicina */}
      <BaseModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        maxWidth="720px"
      >
        <form className="avi-figma-modal-720" onSubmit={handleSubmit}>
          <div className="avi-figma-modal-header">
            <div className="avi-figma-modal-titles">
              <h2 className="avi-figma-modal-title">
                Registrar medicina veterinaria ({form.id})
              </h2>
              <p className="avi-figma-modal-subtitle">
                Configura principio activo, dosis estándar y días de retiro previo a sacrificio
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
              <label className="avi-figma-label">Nombre del medicamento</label>
              <input
                type="text"
                className="avi-figma-input"
                placeholder="Ej. Tilmicosina 25% Solución Oral"
                value={form.nombre}
                onChange={(e) => setForm({ ...form, nombre: e.target.value })}
                required
              />
            </div>

            <div className="avi-figma-field">
              <label className="avi-figma-label">Categoría farmacológica</label>
              <div className="avi-figma-select-wrap">
                <select
                  className="avi-figma-input avi-figma-select"
                  value={form.categoria}
                  onChange={(e) =>
                    setForm({ ...form, categoria: e.target.value })
                  }
                >
                  <option value="Antibiótico">Antibiótico</option>
                  <option value="Vacuna">Vacuna</option>
                  <option value="Antiparasitario">Antiparasitario</option>
                  <option value="Vitamina / Electrolito">
                    Vitamina / Electrolito
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
              <label className="avi-figma-label">
                Principio activo y concentración
              </label>
              <input
                type="text"
                className="avi-figma-input"
                placeholder="Ej. Fosfato de tilmicosina 250 mg/ml"
                value={form.principioActivo}
                onChange={(e) =>
                  setForm({ ...form, principioActivo: e.target.value })
                }
                required
              />
            </div>

            <div className="avi-figma-field">
              <label className="avi-figma-label">
                Vía y tiempo de retiro (días)
              </label>
              <div style={{ display: 'grid', gridTemplateColumns: '1.3fr 0.7fr', gap: '8px' }}>
                <div className="avi-figma-select-wrap">
                  <select
                    className="avi-figma-input avi-figma-select"
                    value={form.via}
                    onChange={(e) => setForm({ ...form, via: e.target.value })}
                  >
                    <option value="Agua de bebida">Agua de bebida</option>
                    <option value="Ocular / Agua">Ocular / Agua</option>
                    <option value="Alimento">Alimento</option>
                    <option value="Inyectable">Inyectable</option>
                  </select>
                  <img
                    alt=""
                    src="/assets/admin-select-chevron.svg"
                    className="avi-figma-select-chevron"
                  />
                </div>
                <input
                  type="number"
                  min="0"
                  max="60"
                  className="avi-figma-input"
                  value={form.tiempoRetiroDias}
                  onChange={(e) =>
                    setForm({
                      ...form,
                      tiempoRetiroDias: parseInt(e.target.value, 10) || 0
                    })
                  }
                  title="Días de retiro antes de sacrificio"
                  required
                />
              </div>
            </div>

            <div className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
              <label className="avi-figma-label">
                Dosis y esquema terapéutico recomendado
              </label>
              <input
                type="text"
                className="avi-figma-input"
                placeholder="Ej. 15–20 mg/kg PV en agua de bebida durante 3 a 5 días continuos"
                value={form.dosisRecomendada}
                onChange={(e) =>
                  setForm({ ...form, dosisRecomendada: e.target.value })
                }
                required
              />
            </div>

            <div className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
              <label className="avi-figma-label">Indicaciones terapéuticas</label>
              <input
                type="text"
                className="avi-figma-input"
                placeholder="Ej. Control de enfermedad respiratoria crónica y micoplasmosis aviar..."
                value={form.indicaciones}
                onChange={(e) =>
                  setForm({ ...form, indicaciones: e.target.value })
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
              Guardar medicina
            </button>
          </div>
        </form>
      </BaseModal>

      {/* Modal: Ficha Farmacológica */}
      <BaseModal
        isOpen={Boolean(detailMed)}
        onClose={() => setDetailMed(null)}
        maxWidth="680px"
      >
        {detailMed && (
          <div className="avi-figma-modal-720">
            <div className="avi-figma-modal-header">
              <div className="avi-figma-modal-titles">
                <h2 className="avi-figma-modal-title">
                  {detailMed.nombre} ({detailMed.id})
                </h2>
                <p className="avi-figma-modal-subtitle">
                  {detailMed.categoria} · Principio activo: {detailMed.principioActivo}
                </p>
              </div>
              <button
                type="button"
                className="avi-figma-modal-close"
                onClick={() => setDetailMed(null)}
              >
                ×
              </button>
            </div>

            <div className="avi-figma-modal-grid">
              <div className="avi-figma-field">
                <label className="avi-figma-label">Vía de administración</label>
                <div className="avi-figma-readonly-box">{detailMed.via}</div>
              </div>
              <div className="avi-figma-field">
                <label className="avi-figma-label">
                  Tiempo de retiro obligatorio
                </label>
                <div className="avi-figma-valid-box">
                  {Number(detailMed.tiempoRetiroDias) > 0
                    ? `${detailMed.tiempoRetiroDias} días antes del sacrificio`
                    : 'Sin tiempo de retiro (0 días)'}
                </div>
              </div>
              <div className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
                <label className="avi-figma-label">Posología recomendada</label>
                <div className="avi-figma-readonly-box">
                  {detailMed.dosisRecomendada}
                </div>
              </div>
              <div className="avi-figma-field" style={{ gridColumn: '1 / -1' }}>
                <label className="avi-figma-label">Indicaciones terapéuticas</label>
                <div className="avi-figma-demand-box">
                  {detailMed.indicaciones}
                </div>
              </div>
            </div>

            <div className="avi-figma-modal-footer">
              <button
                type="button"
                className="avi-figma-btn-cancel"
                onClick={() => setDetailMed(null)}
              >
                Cerrar
              </button>
              <button
                type="button"
                className="avi-figma-btn-orange"
                onClick={() => {
                  const target = detailMed;
                  setDetailMed(null);
                  handleOpenEdit(target);
                }}
              >
                Editar medicina
              </button>
            </div>
          </div>
        )}
      </BaseModal>
    </div>
  );
}

export default VetMedicinesPage;
