import { NavLink } from 'react-router-dom';

const MENU_BY_ROLE = {
  administrador: [
    { label: 'Inicio', path: '/administrador', icon: '/assets/nav-home.svg', size: '18px' },
    { label: 'Galpones', path: '/administrador/galpones', icon: '/assets/nav-galpones.svg', size: '18px' },
    { label: 'Inventario', path: '/administrador/inventario', icon: '/assets/nav-inventario.svg', size: '18px' },
    { label: 'Recepciones Alimento', path: '/administrador/recepciones-alimento', icon: '/assets/nav-alimento.svg', size: '16px' },
    { label: 'Recepciones Medicamento', path: '/administrador/recepciones-medicamento', icon: '/assets/nav-alimento.svg', size: '16px', wrap: true },
    { label: 'Ordenar Sacrificio', path: '/administrador/ordenes-sacrificio', icon: '/assets/nav-alimento.svg', size: '16px' }
  ],
  nutricionista: [
    { label: 'Inicio', path: '/nutricionista', icon: '/assets/nav-home.svg', size: '18px' },
    { label: 'Planes nutricionales', path: '/nutricionista/planes', icon: '/assets/nav-alimento.svg', size: '17px' },
    { label: 'Asignaciones a galpones', path: '/nutricionista/asignaciones', icon: '/assets/nav-galpones.svg', size: '16px', wrap: true },
    { label: 'Proyecciones y balance', path: '/nutricionista/proyecciones', icon: '/assets/nav-inventario.svg', size: '16px', wrap: true }
  ],
  veterinario: [
    { label: 'Inicio', path: '/veterinario', icon: '/assets/nav-home.svg', size: '18px' },
    { label: 'Órdenes de sacrificio', path: '/veterinario/ordenes', icon: '/assets/nav-alimento.svg', size: '16px' },
    { label: 'Diagnosticar', path: '/veterinario/diagnosticar', icon: '/assets/nav-galpones.svg', size: '17px' },
    { label: 'Registrar enfermedad', path: '/veterinario/enfermedades', icon: '/assets/nav-inventario.svg', size: '16px' },
    { label: 'Registrar medicina', path: '/veterinario/medicinas', icon: '/assets/nav-alimento.svg', size: '16px' }
  ],
  trabajador: [
    { label: 'Inicio', path: '/trabajador', icon: '/assets/nav-home.svg', size: '18px' }
  ]
};

function Sidebar({ role = 'administrador' }) {
  const items = MENU_BY_ROLE[role] || MENU_BY_ROLE.administrador;

  return (
    <aside className="avi-sidebar" data-node-id="130:256" data-name={`Sidebar-${role}`}>
      <div className="avi-sidebar-brand" data-node-id="130:257">
        <div className="avi-sidebar-logo-wrap" data-node-id="130:258">
          <img alt="Granja Avícola" className="avi-sidebar-logo-img" src="/assets/logo-granja.svg" />
        </div>
        <p className="avi-sidebar-brand-title" data-node-id="130:265">
          Granja Avícola
        </p>
        <p className="avi-sidebar-brand-tagline" data-node-id="130:266">
          Producción que alimenta
        </p>
      </div>

      <nav className="avi-sidebar-nav" data-node-id="130:267">
        {items.map((item, idx) => (
          <NavLink
            key={item.path}
            to={item.path}
            end={idx === 0}
            className={({ isActive }) =>
              `avi-sidebar-link ${isActive ? 'active' : ''}`
            }
          >
            <div className="avi-sidebar-icon-box">
              <img alt="" src={item.icon} className="avi-sidebar-icon" />
            </div>
            <span
              className="avi-sidebar-label"
              style={{
                fontSize: item.size,
                whiteSpace: item.wrap ? 'normal' : 'nowrap',
                width: item.wrap ? '171px' : 'auto'
              }}
            >
              {item.label}
            </span>
          </NavLink>
        ))}
      </nav>

      <div className="avi-sidebar-spacer" />

      <div className="avi-sidebar-footer" data-node-id="130:297">
        <div className="avi-sidebar-divider" />
        <div className="avi-sidebar-sustainability">
          <div className="avi-sidebar-leaf">
            <img alt="" src="/assets/sidebar-leaf.svg" />
          </div>
          <div className="avi-sidebar-footer-copy">
            <p>Aves sanas</p>
            <p>un mejor mañana</p>
          </div>
        </div>
      </div>
    </aside>
  );
}

export default Sidebar;
