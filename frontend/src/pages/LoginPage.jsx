import { useState, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import { useModal } from '../hooks/useModal';
import RecoverPasswordModal from '../components/auth/RecoverPasswordModal';
import ApiMessage from '../components/ApiMessage';

function LoginPage() {
  const { login } = useContext(AuthContext);
  const navigate = useNavigate();
  const recoveryModal = useModal(false);

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [remember, setRemember] = useState(true);
  const [toastMessage, setToastMessage] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    login(email || 'veterinario@avicontrol.com', password);
    navigate('/roles');
  };

  return (
    <div className="login-screen" data-node-id="216:1328" data-name="InicioSesión">
      <ApiMessage
        message={toastMessage}
        type="success"
        onClose={() => setToastMessage('')}
      />

      <div className="login-card" data-node-id="216:1329">
        {/* Panel Izquierdo: Formulario de Inicio de Sesión */}
        <div className="login-left-panel" data-node-id="216:1330">
          <div className="login-brand-header" data-node-id="216:1331">
            <div className="login-brand-badge" data-node-id="216:1332">
              <span>A</span>
            </div>
            <div className="login-brand-text" data-node-id="216:1335">
              <span className="login-brand-name" data-node-id="216:1337">
                AviControl
              </span>
              <span className="login-brand-sub" data-node-id="216:1339">
                Gestión Avícola
              </span>
            </div>
          </div>

          <div className="login-form-wrapper" data-node-id="216:1340">
            <div className="login-form-body" data-node-id="216:1341">
              <h2 className="login-welcome-title" data-node-id="216:1343">
                ¡Bienvenido de nuevo!
              </h2>
              <p className="login-welcome-subtitle" data-node-id="216:1345">
                Ingresa tus credenciales para acceder a la plataforma
              </p>

              <form className="login-form" onSubmit={handleSubmit} data-node-id="216:1346">
                <div className="login-field-group" data-node-id="216:1347">
                  <label className="login-label" data-node-id="216:1349">
                    Usuario o Correo Electrónico
                  </label>
                  <div className="login-input-wrapper" data-node-id="216:1350">
                    <div className="login-input-icon" data-node-id="216:1354">
                      <img alt="" src="/assets/login-user.svg" />
                    </div>
                    <input
                      type="text"
                      className="login-input"
                      placeholder="ej. veterinario@avicontrol.com"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      data-node-id="216:1351"
                    />
                  </div>
                </div>

                <div className="login-field-group" data-node-id="216:1356">
                  <div className="login-label-row" data-node-id="216:1357">
                    <label className="login-label" data-node-id="216:1359">
                      Contraseña
                    </label>
                    <button
                      type="button"
                      className="login-forgot-link"
                      onClick={() => recoveryModal.openModal()}
                      data-node-id="216:1360"
                    >
                      ¿Olvidaste tu contraseña?
                    </button>
                  </div>
                  <div className="login-input-wrapper" data-node-id="216:1362">
                    <div className="login-input-icon" data-node-id="216:1366">
                      <img alt="" src="/assets/login-lock.svg" />
                    </div>
                    <input
                      type="password"
                      className="login-input"
                      placeholder="••••••••••••"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      data-node-id="216:1363"
                    />
                  </div>
                </div>

                <div className="login-remember-row" data-node-id="216:1368">
                  <label className="login-remember-label" data-node-id="216:1369">
                    <input
                      type="checkbox"
                      checked={remember}
                      onChange={(e) => setRemember(e.target.checked)}
                      className="login-checkbox"
                      data-node-id="216:1370"
                    />
                    <span data-node-id="216:1372">Recordar mi sesión</span>
                  </label>
                </div>

                <button
                  type="submit"
                  className="login-submit-btn"
                  data-node-id="216:1373"
                >
                  Iniciar Sesión
                </button>
              </form>
            </div>
          </div>

          <div />
        </div>

        {/* Panel Derecho: Ilustración / Info del Sistema */}
        <div className="login-right-panel" data-node-id="216:1381">
          <div className="login-version-row" data-node-id="216:1382">
            <div className="login-version-pill" data-node-id="216:1383">
              <span data-node-id="216:1384">Versión 1.0 · Productivo</span>
            </div>
          </div>

          <div className="login-right-glow" data-node-id="216:1387" />

          <div className="login-right-center" data-node-id="216:1388">
            <div className="login-right-content" data-node-id="216:1389">
              <div className="login-hero-icon-box" data-node-id="216:1390">
                <svg
                  width="36"
                  height="36"
                  viewBox="0 0 36 36"
                  fill="none"
                  xmlns="http://www.w3.org/2000/svg"
                  aria-label="Pollo AviControl"
                  data-node-id="216:1391"
                >
                  {/* Cresta superior roja (line art con color de pollo) */}
                  <path
                    d="M14 11.5C13.5 8.2 16.2 6.5 18 8.8C19.2 6.2 22.5 6.8 22.2 10C24.2 8.8 26.2 10.6 24.8 13"
                    fill="rgba(244, 63, 94, 0.22)"
                    stroke="#FB7185"
                    strokeWidth="2.1"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                  {/* Barbilla / papada roja debajo del pico */}
                  <path
                    d="M11.5 21.2C9.2 21.8 9.0 25.0 11.5 25.0"
                    fill="rgba(244, 63, 94, 0.22)"
                    stroke="#FB7185"
                    strokeWidth="2.1"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                  {/* Pico amarillo/naranja hacia la izquierda */}
                  <path
                    d="M11.5 17.2L6.8 19.2L11.5 21.2"
                    fill="rgba(251, 191, 36, 0.25)"
                    stroke="#FBBF24"
                    strokeWidth="2.1"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                  {/* Cabeza y cuello blanco en line art */}
                  <path
                    d="M11.5 17.5C11.5 13.6 14.8 11 19 11C23.4 11 26.5 14 26.5 18.5V27.5C24.5 29 22.2 27.5 20.5 29C18.8 27.5 16.5 29 14.5 27.5C13 28.5 11.5 27.5 11.5 25.8V17.5Z"
                    fill="rgba(255, 255, 255, 0.14)"
                    stroke="#FFFFFF"
                    strokeWidth="2.1"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                  {/* Ojo */}
                  <circle cx="16.2" cy="17.2" r="1.5" fill="#FFFFFF" />
                </svg>
              </div>

              <div className="login-hero-copy" data-node-id="216:1392">
                <h3 className="login-hero-title" data-node-id="216:1394">
                  <span>Control integral para galpones y</span>
                  <span>nutrición avícola</span>
                </h3>
                <div className="login-hero-desc" data-node-id="216:1396">
                  <p>
                    Gestión en tiempo real de trazabilidad de lotes, aislamiento sanitario, raciones
                  </p>
                  <p>
                    nutricionales por etapa y cálculo automatizado de demanda de alimento.
                  </p>
                </div>
              </div>

              <div className="login-feature-cards" data-node-id="216:1397">
                <div className="login-feature-card" data-node-id="216:1398">
                  <span className="login-feature-title" data-node-id="216:1400">
                    Gestión de Galpones
                  </span>
                  <span className="login-feature-sub" data-node-id="216:1401">
                    Aislamiento &amp; Cosecha
                  </span>
                </div>
                <div className="login-feature-card" data-node-id="216:1402">
                  <span className="login-feature-title" data-node-id="216:1404">
                    Plan Nutricional
                  </span>
                  <span className="login-feature-sub" data-node-id="216:1405">
                    Raciones por Etapa
                  </span>
                </div>
              </div>
            </div>
          </div>

          <div className="login-copyright" data-node-id="216:1385">
            <span data-node-id="216:1386">
              © 2026 AviControl System. Todos los derechos reservados.
            </span>
          </div>
        </div>
      </div>

      <RecoverPasswordModal
        isOpen={recoveryModal.isOpen}
        onClose={recoveryModal.closeModal}
        onComplete={(msg) => setToastMessage(msg)}
      />
    </div>
  );
}

export default LoginPage;
