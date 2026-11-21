import { useContext } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';

const ROLES_CONFIG = [
  {
    id: 'login',
    label: 'Login',
    path: '/login',
    badgeColor: '#1b562a'
  },
  {
    id: 'portal',
    label: 'Selector de Roles',
    path: '/roles',
    badgeColor: '#0d7c75'
  },
  {
    id: 'administrador',
    label: 'Administrador',
    subtitle: 'Galpones & Inventario',
    path: '/admin',
    icon: '/assets/role-admin.svg',
    pillColor: '#d97706'
  },
  {
    id: 'nutricionista',
    label: 'Nutricionista',
    subtitle: 'Planes Nutricionales',
    path: '/nutricionista',
    icon: '/assets/role-nutri.svg',
    pillColor: '#15803d'
  },
  {
    id: 'veterinario',
    label: 'Veterinario',
    subtitle: 'Sanidad & Aislamiento',
    path: '/veterinario',
    icon: '/assets/role-vet.svg',
    pillColor: '#0d7c75'
  },
  {
    id: 'trabajador',
    label: 'Trabajador',
    subtitle: 'Mortalidad & Consulta',
    path: '/trabajador',
    icon: '/assets/role-worker.svg',
    pillColor: '#4d7c0f'
  }
];

function RoleSwitcherBar() {
  const { activeRole, setActiveRole } = useContext(AuthContext);
  const navigate = useNavigate();
  const location = useLocation();

  const handleSelect = (item) => {
    if (['administrador', 'nutricionista', 'veterinario', 'trabajador'].includes(item.id)) {
      setActiveRole(item.id);
    }
    navigate(item.path);
  };

  return (
    <header className="role-switcher-bar" data-name="Barra Selector de Roles">
      <div className="role-switcher-brand" onClick={() => navigate('/roles')}>
        <div className="role-switcher-logo">A</div>
        <div className="role-switcher-brand-text">
          <span className="role-switcher-title">AviControl M2</span>
          <span className="role-switcher-subtitle">Barra de Navegación por Rol</span>
        </div>
      </div>

      <nav className="role-switcher-pills" aria-label="Selector de inicio por rol">
        {ROLES_CONFIG.map((item) => {
          const isCurrent =
            location.pathname === item.path ||
            (location.pathname.startsWith(item.path) && item.path !== '/');

          return (
            <button
              key={item.id}
              type="button"
              onClick={() => handleSelect(item)}
              className={`role-bar-pill ${isCurrent ? 'active' : ''}`}
              style={{
                '--pill-accent': item.pillColor || item.badgeColor
              }}
            >
              {item.icon && (
                <span className="role-bar-pill-icon">
                  <img src={item.icon} alt="" width="16" height="16" />
                </span>
              )}
              <span className="role-bar-pill-label">{item.label}</span>
            </button>
          );
        })}
      </nav>

      <div className="role-switcher-status">
        <span className="role-status-dot" />
        <span>Rol activo: <strong style={{ textTransform: 'capitalize' }}>{activeRole}</strong></span>
      </div>
    </header>
  );
}

export default RoleSwitcherBar;
