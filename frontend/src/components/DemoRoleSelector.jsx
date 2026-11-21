import { useLocation } from 'react-router-dom';
import { demoRoles, roleForPath } from '../utils/demoRoles';

export default function DemoRoleSelector() {
  const { pathname } = useLocation();
  return (
    <label className="avi-demo-role-selector">
      <span>Rol DEMO</span>
      <select aria-label="Cambiar rol DEMO" value={roleForPath(pathname) || ''}
        onChange={(event) => window.location.assign(event.target.value ? `/${event.target.value}` : '/roles')}>
        <option value="">Seleccionar rol</option>
        {demoRoles.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
      </select>
      <a href="/roles">Ver roles</a>
    </label>
  );
}