import { useEffect, useState } from 'react';
import Sidebar from '../components/Sidebar';
import Topbar from '../components/Topbar';
import ApiMessage from '../components/ApiMessage';
import { getBarn, getBarnAge, getBarnSummary, listBarns } from '../services/barnsApi';

function AdminBarnsPage() {
  const [page, setPage] = useState(0);
  const [listing, setListing] = useState({ elementos: [], totalElementos: 0, totalPaginas: 0 });
  const [summary, setSummary] = useState({ porEstado: {} });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selectedBarn, setSelectedBarn] = useState(null);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError('');
    Promise.all([listBarns(page, { signal: controller.signal }), getBarnSummary({ signal: controller.signal })])
      .then(([result, totals]) => { setListing(result); setSummary(totals); })
      .catch((cause) => { if (cause.name !== 'AbortError') setError(cause.message); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [page]);

  const openDetail = async (id) => {
    setError('');
    try {
      const [detail, age] = await Promise.all([getBarn(id), getBarnAge(id)]);
      setSelectedBarn({ ...detail, edadConsultada: age });
    } catch (cause) { setError(cause.message); }
  };

  const disponiblesCount = summary.porEstado.DISPONIBLE || 0;
  const aisladosCount = summary.porEstado.AISLAMIENTO || 0;

  return (
    <div className="avi-role-layout">
      <Sidebar role="administrador" />

      <div className="avi-main-frame" data-node-id="216:415" data-name="Galpones">
        <Topbar roleLabel="Administrador" avatarSrc="/assets/admin-avatar.svg" />
        <ApiMessage message={error} type="error" onClose={() => setError('')} />

        <div className="avi-dashboard-content" data-node-id="216:423">
          {/* Page heading */}
          <div className="avi-page-heading" data-node-id="216:424">
            <div className="avi-heading-copy" data-node-id="216:425">
              <h1 className="avi-page-title avi-title-lg" data-node-id="216:426">
                Galpones
              </h1>
              <p className="avi-page-subtitle" data-node-id="216:427">
                Resumen general de galpones
              </p>
            </div>
          </div>

          {/* 2 Metric cards (437x146) */}
          <div className="avi-barns-metrics-row" data-node-id="216:428">
            <div className="avi-barn-kpi-card" data-node-id="216:429">
              <div
                className="avi-barn-kpi-tile"
                style={{ backgroundColor: '#e4f4e3' }}
                data-node-id="216:430"
              >
                <img alt="" src="/assets/admin-thumbs-up.svg" className="avi-barn-kpi-icon" />
              </div>
              <div className="avi-barn-kpi-copy" data-node-id="216:432">
                <span className="avi-barn-kpi-label" data-node-id="216:433">
                  Galpones Disponibles
                </span>
                <span className="avi-barn-kpi-value" data-node-id="216:434">
                  {disponiblesCount}
                </span>
              </div>
            </div>

            <div className="avi-barn-kpi-card" data-node-id="216:435">
              <div
                className="avi-barn-kpi-tile"
                style={{ backgroundColor: '#f4e3e3' }}
                data-node-id="216:436"
              >
                <img alt="" src="/assets/admin-medicine-bottle.svg" className="avi-barn-kpi-icon" />
              </div>
              <div className="avi-barn-kpi-copy" data-node-id="216:437">
                <span className="avi-barn-kpi-label" data-node-id="216:438">
                  Galpones Aislados
                </span>
                <span className="avi-barn-kpi-value" data-node-id="216:439">
                  {aisladosCount}
                </span>
              </div>
            </div>
          </div>

          {/* Galpones Table Panel */}
          <div className="avi-recent-orders-panel avi-galpones-panel" data-node-id="216:441">
            <div className="avi-panel-header avi-panel-header-full" data-node-id="216:442">
              <div className="avi-panel-icon-28" data-node-id="216:443">
                <img alt="" src="/assets/admin-home-icon.svg" />
              </div>
              <h2 className="avi-panel-title" data-node-id="216:444">
                Galpones
              </h2>
            </div>

            <div className="avi-orders-table" data-node-id="216:447">
              <div className="avi-orders-thead" data-node-id="216:448">
                <span style={{ flex: '1.2' }}>Galpón</span>
                <span style={{ flex: '1.2' }}>Lote actual</span>
                <span style={{ flex: '1.2' }}>Estado</span>
                <span style={{ flex: '0.8' }}>Detalles</span>
              </div>

              {listing.elementos.map((barn) => (
                <div key={barn.id} className="avi-orders-trow avi-barn-trow">
                  <span className="avi-col-dark" style={{ flex: '1.2' }}>
                    {barn.nombre}
                  </span>
                  <span className="avi-col-dark" style={{ flex: '1.2' }}>
                    Ver en detalle
                  </span>
                  <div style={{ flex: '1.2', display: 'flex', alignItems: 'center' }}>
                    <span
                      className="avi-barn-status-pill"
                      style={{
                        backgroundColor: barn.estado === 'AISLAMIENTO' ? '#f4e3e3' : '#e4f4e3',
                        color: barn.estado === 'AISLAMIENTO' ? '#9c2929' : '#1b562a'
                      }}
                    >
                      <span
                        className="avi-barn-status-dot"
                        style={{ backgroundColor: barn.estado === 'AISLAMIENTO' ? '#9c2929' : '#2d9f42' }}
                      />
                      {barn.estado}
                    </span>
                  </div>
                  <div style={{ flex: '0.8', display: 'flex', alignItems: 'center', paddingLeft: '14px' }}>
                    <button
                      type="button"
                      className="avi-table-icon-btn"
                      title={`Ver detalles de ${barn.nombre}`}
                      onClick={() => openDetail(barn.id)}
                    >
                      <img alt="Detalles" src="/assets/admin-file-add.svg" />
                    </button>
                  </div>
                </div>
              ))}
              {loading && <p className="avi-page-subtitle">Cargando galpones...</p>}
              {!loading && listing.elementos.length === 0 && (
                <p className="avi-page-subtitle">{listing.mensaje || 'No hay galpones disponibles.'}</p>
              )}
            </div>
            <div className="avi-figma-modal-footer">
              <button type="button" disabled={loading || page === 0} onClick={() => setPage(page - 1)}>Anterior</button>
              <span>Página {page + 1} de {Math.max(1, listing.totalPaginas)}</span>
              <button type="button" disabled={loading || page + 1 >= listing.totalPaginas} onClick={() => setPage(page + 1)}>Siguiente</button>
            </div>
          </div>
        </div>
      </div>

      {/* Right Slide-Over Drawer: Sidebar · Detalle Galpón (Node 216:1101) */}
      {selectedBarn && (
        <div
          className="avi-drawer-overlay"
          onClick={() => setSelectedBarn(null)}
        >
          <aside
            className="avi-barn-detail-drawer"
            data-node-id="216:1101"
            data-name="Sidebar · Detalle Galpón"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="avi-drawer-header" data-node-id="216:1102">
              <div className="avi-drawer-title-group" data-node-id="216:1103">
                <h2 className="avi-drawer-title" data-node-id="216:1104">
                  Detalle del galpón
                </h2>
                <p className="avi-drawer-subtitle" data-node-id="216:1105">
                  Consulta operativa
                </p>
              </div>
              <button
                type="button"
                className="avi-drawer-close-btn"
                onClick={() => setSelectedBarn(null)}
                data-node-id="216:1106"
              >
                ×
              </button>
            </div>

            {/* Identificador Card (Green #1b562a) */}
            <div className="avi-drawer-id-card" data-node-id="216:1108">
              <div className="avi-drawer-id-top" data-node-id="216:1109">
                <span className="avi-drawer-id-label" data-node-id="216:1110">
                  IDENTIFICADOR
                </span>
                <span className="avi-drawer-id-pill" data-node-id="216:1111">
                  {selectedBarn.estado}
                </span>
              </div>
              <h3 className="avi-drawer-id-name" data-node-id="216:1113">
                {selectedBarn.nombre}
              </h3>
            </div>

            {/* Información operativa Card */}
            <div className="avi-drawer-section-card" data-node-id="216:1114">
              <div className="avi-drawer-section-header" data-node-id="216:1115">
                <span className="avi-drawer-green-dot" />
                <span className="avi-drawer-section-title" data-node-id="216:1117">
                  Información operativa
                </span>
              </div>
              <div className="avi-drawer-stats-row" data-node-id="216:1118">
                <div className="avi-drawer-stat-box" data-node-id="216:1119">
                  <span className="avi-drawer-stat-label">Aforo máximo</span>
                  <strong className="avi-drawer-stat-value">{selectedBarn.aforoMaximo}</strong>
                </div>
                <div className="avi-drawer-stat-box" data-node-id="216:1122">
                  <span className="avi-drawer-stat-label">Población actual</span>
                  <strong className="avi-drawer-stat-value">{selectedBarn.loteActual?.poblacionActual ?? 'Sin lote'}</strong>
                </div>
              </div>
            </div>

            {/* Lote alojado actualmente Card */}
            <div className="avi-drawer-section-card" data-node-id="216:1125">
              <div className="avi-drawer-section-header" data-node-id="216:1126">
                <span className="avi-drawer-green-dot" />
                <span className="avi-drawer-section-title" data-node-id="216:1128">
                  Lote alojado actualmente
                </span>
              </div>

              <div className="avi-drawer-lote-banner" data-node-id="216:1129">
                <span>{selectedBarn.loteActual?.id || 'Sin lote actual'}</span>
              </div>

              <div className="avi-drawer-age-banner" data-node-id="216:1132">
                <div className="avi-drawer-age-left" data-node-id="216:1133">
                  <span className="avi-drawer-age-caption">EDAD EN DÍAS TOTALES</span>
                  <strong className="avi-drawer-age-main">{selectedBarn.edadConsultada?.edad?.diasTotales ?? '—'}</strong>
                </div>
                <div className="avi-drawer-age-right" data-node-id="216:1136">
                  <span className="avi-drawer-age-caption">DESGLOSE</span>
                  <strong className="avi-drawer-age-sub">{selectedBarn.edadConsultada?.edad?.texto || selectedBarn.edadConsultada?.mensaje || 'Sin edad disponible'}</strong>
                </div>
              </div>

              <div className="avi-drawer-date-row" data-node-id="216:1139">
                <span className="avi-drawer-date-label">Identificador del galpón</span>
                <strong className="avi-drawer-date-value">{selectedBarn.id}</strong>
              </div>
            </div>
          </aside>
        </div>
      )}
    </div>
  );
}

export default AdminBarnsPage;
