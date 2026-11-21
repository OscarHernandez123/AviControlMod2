import { useLocation } from 'react-router-dom';
import { roles, roleForPath } from '../utils/roleAccess';

export default function RoleSelector() {
  const { pathname } = useLocation();
  return (
    <label className="avi-role-selector">
      <span>Rol</span>
      <select aria-label="Cambiar rol" value={roleForPath(pathname) || ''}
        onChange={(event) => window.location.assign(event.target.value ? `/${event.target.value}` : '/roles')}>
        <option value="">Seleccionar rol</option>
        {roles.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
      </select>
      <a href="/roles">Ver roles</a>
    </label>
  );
}