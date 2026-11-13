import { useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';

const ROLE_BUTTONS = [
  {
    id: 'administrador',
    nodeId: '216:1639',
    title: 'Administrador',
    subtitle: 'Resumen de galpones & inventario central',
    bg: '#d97706',
    border: 'rgba(251, 191, 36, 0.4)',
    subColor: '#fef3c7',
    icon: '/assets/role-admin.svg',
    path: '/administrador'
  },
  {
    id: 'nutricionista',
    nodeId: '216:1650',
    title: 'Nutricionista',
    subtitle: 'Planes nutricionales & raciones r/p/d',
    bg: '#15803d',
    border: 'rgba(74, 222, 128, 0.4)',
    subColor: '#dcfce7',
    icon: '/assets/role-nutri.svg',
    path: '/nutricionista'
  },
  {
    id: 'veterinario',
    nodeId: '216:1660',
    title: 'Veterinario',
    subtitle: 'Aislamiento & Sacrificio Sanitario',
    bg: '#0d7c75',
    border: 'rgba(52, 211, 153, 0.4)',
    subColor: '#d1fae5',
    icon: '/assets/role-vet.svg',
    path: '/veterinario'
  },
  {
    id: 'trabajador',
    nodeId: '216:1670',
    title: 'Trabajador',
    subtitle: 'Registro de mortalidad & consulta diaria',
    bg: '#4d7c0f',
    border: 'rgba(163, 230, 53, 0.4)',
    subColor: '#ecfccb',
    icon: '/assets/role-worker.svg',
    path: '/trabajador'
  }
];

function RoleSelectionPage() {
  const { setActiveRole } = useContext(AuthContext);
  const navigate = useNavigate();

  const handleRoleClick = (roleObj) => {
    setActiveRole(roleObj.id);
    navigate(roleObj.path);
  };

  return (
    <div className="roles-portal-wrapper" data-node-id="216:1611" data-name="Navegación roles">
      <div className="roles-portal-canvas" data-node-id="216:1616">
        {/* Header Top */}
        <div className="roles-header-top" data-node-id="216:1617">
          <div className="roles-brand" data-node-id="216:1618">
            <div className="roles-brand-badge" data-node-id="216:1619">
              <span>A</span>
            </div>
            <div className="roles-brand-text" data-node-id="216:1622">
              <span className="roles-brand-name" data-node-id="216:1624">
                AviControl
              </span>
              <span className="roles-brand-sub" data-node-id="216:1626">
                Gestión Avícola
              </span>
            </div>
          </div>

          <div className="roles-permission-pill" data-node-id="216:1627">
            <span data-node-id="216:1628">● Sistema de Permisos según Rol Activo</span>
          </div>
        </div>

        {/* Main Body */}
        <div className="roles-main-body" data-node-id="216:1629">
          <div className="roles-banner-card" data-node-id="216:1631">
            <div className="roles-banner-header" data-node-id="216:1632">
              <h1 className="roles-banner-title" data-node-id="216:1634">
                Gestión avícola
              </h1>
              <p className="roles-banner-subtitle" data-node-id="216:1636">
                Selecciona tu rol para ingresar al portal
              </p>
            </div>

            <div className="roles-buttons-list" data-node-id="216:1638">
              {ROLE_BUTTONS.map((role) => (
                <button
                  key={role.id}
                  type="button"
                  className="role-oval-btn"
                  style={{
                    backgroundColor: role.bg,
                    borderColor: role.border
                  }}
                  onClick={() => handleRoleClick(role)}
                  data-node-id={role.nodeId}
                >
                  <div className="role-oval-icon-circle">
                    <div className="role-oval-icon-svg">
                      <img alt="" src={role.icon} />
                    </div>
                  </div>
                  <div className="role-oval-text">
                    <span className="role-oval-title">{role.title}</span>
                    <span className="role-oval-sub" style={{ color: role.subColor }}>
                      {role.subtitle}
                    </span>
                  </div>
                </button>
              ))}
            </div>

            <div className="roles-banner-footer" data-node-id="216:1680">
              <span data-node-id="216:1681">
                AviControl M2 · Módulo de Operaciones Avícolas
              </span>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="roles-page-footer" data-node-id="216:1716">
          <span data-node-id="216:1718">
            Universidad del Magdalena · Proyecto AviControl M2
          </span>
          <span data-node-id="216:1720">
            Basado en Especificaciones de Casos de Uso (Specs 001 - 022)
          </span>
        </div>
      </div>
    </div>
  );
}

export default RoleSelectionPage;
