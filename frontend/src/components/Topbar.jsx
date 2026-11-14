import { useContext, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import BaseModal from './BaseModal';

function Topbar({
  roleLabel = 'Administrador',
  avatarSrc = '/assets/admin-avatar.svg',
  leftBadge = ''
}) {
  const { user, logout } = useContext(AuthContext);
  const navigate = useNavigate();

  const [menuOpen, setMenuOpen] = useState(false);
  const [accountModalOpen, setAccountModalOpen] = useState(false);
  const dropdownRef = useRef(null);

  const displayEmail =
    user?.email ||
    (roleLabel.toLowerCase().includes('vet')
      ? 'veterinario@avicontrol.com'
      : roleLabel.toLowerCase().includes('nutri')
      ? 'nutricionista@avicontrol.com'
      : roleLabel.toLowerCase().includes('trabajador')
      ? 'trabajador@avicontrol.com'
      : 'admin@avicontrol.com');

  const displayUserName =
    user?.name ||
    (roleLabel.toLowerCase().includes('trabajador') ? 'Carlos Pérez' : roleLabel);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setMenuOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleLogout = () => {
    setMenuOpen(false);
    logout();
    navigate('/login');
  };

  const handleOpenAccount = () => {
    setMenuOpen(false);
    setAccountModalOpen(true);
  };

  return (
    <>
      <header
        className="avi-topbar"
        data-node-id="104:24"
        data-name="Top bar"
        style={leftBadge ? { justifyContent: 'space-between' } : undefined}
      >
        {leftBadge && (
          <div className="worker-zone-pill" data-node-id="238:5829">
            <span>{leftBadge}</span>
          </div>
        )}
        <div className="avi-topbar-account-wrap" ref={dropdownRef}>
          <button
            type="button"
            className={`avi-topbar-account avi-topbar-account-btn ${menuOpen ? 'active' : ''}`}
            data-node-id="104:32"
            onClick={() => setMenuOpen((prev) => !prev)}
            title="Cuenta y cierre de sesión"
          >
            <span className="avi-topbar-role" data-node-id="104:40">
              {roleLabel}
            </span>
            <div className="avi-topbar-avatar" data-node-id="104:36">
              <img alt={roleLabel} src={avatarSrc} />
            </div>
          </button>

          {menuOpen && (
            <div className="avi-account-dropdown">
              <div className="avi-account-dropdown-header">
                <div className="avi-account-dropdown-avatar">
                  <img alt={roleLabel} src={avatarSrc} />
                </div>
                <div className="avi-account-dropdown-meta">
                  <strong>{displayUserName}</strong>
                  <span>{displayEmail}</span>
                  <span className="avi-account-role-tag">{roleLabel}</span>
                </div>
              </div>

              <div className="avi-account-dropdown-divider" />

              <button
                type="button"
                className="avi-account-dropdown-item"
                onClick={handleOpenAccount}
              >
                <span>👤</span>
                <span>Detalles de la cuenta</span>
              </button>

              <div className="avi-account-dropdown-divider" />

              <button
                type="button"
                className="avi-account-dropdown-item avi-account-dropdown-logout"
                onClick={handleLogout}
              >
                <span>🚪</span>
                <span>Cerrar sesión</span>
              </button>
            </div>
          )}
        </div>
      </header>

      {/* Modal: Detalles de la cuenta */}
      <BaseModal
        isOpen={accountModalOpen}
        onClose={() => setAccountModalOpen(false)}
        maxWidth="640px"
      >
        <div className="avi-figma-modal-720">
          <div className="avi-figma-modal-header">
            <div className="avi-figma-modal-titles">
              <h2 className="avi-figma-modal-title">Información de la cuenta</h2>
              <p className="avi-figma-modal-subtitle">
                Datos del usuario autenticado y control de sesión activa
              </p>
            </div>
            <button
              type="button"
              className="avi-figma-modal-close"
              onClick={() => setAccountModalOpen(false)}
            >
              ×
            </button>
          </div>

          <div className="avi-figma-modal-grid">
            <div className="avi-figma-field">
              <label className="avi-figma-label">Nombre de usuario</label>
              <div className="avi-figma-readonly-box">{displayUserName}</div>
            </div>

            <div className="avi-figma-field">
              <label className="avi-figma-label">Rol activo</label>
              <div className="avi-figma-readonly-box">{roleLabel}</div>
            </div>

            <div className="avi-figma-field">
              <label className="avi-figma-label">Correo electrónico</label>
              <div className="avi-figma-readonly-box">{displayEmail}</div>
            </div>

            <div className="avi-figma-field">
              <label className="avi-figma-label">Estado de la sesión</label>
              <div className="avi-figma-valid-box">Sesión activa · AviControl M2</div>
            </div>
          </div>

          <div className="avi-figma-modal-footer">
            <button
              type="button"
              className="avi-figma-btn-cancel"
              onClick={() => setAccountModalOpen(false)}
            >
              Cerrar
            </button>
            <button
              type="button"
              className="avi-figma-btn-orange"
              onClick={() => {
                setAccountModalOpen(false);
                handleLogout();
              }}
            >
              Cerrar sesión
            </button>
          </div>
        </div>
      </BaseModal>
    </>
  );
}

export default Topbar;
