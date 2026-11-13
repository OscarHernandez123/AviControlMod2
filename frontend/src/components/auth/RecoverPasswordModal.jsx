import { useState } from 'react';
import BaseModal from '../BaseModal';

function RecoverPasswordModal({ isOpen, onClose, onComplete }) {
  const [step, setStep] = useState(1); // 1: 216:1407, 2: 216:1427, 3: 216:1450
  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [errorMsg, setErrorMsg] = useState('');

  const handleClose = () => {
    setStep(1);
    setEmail('');
    setCode('');
    setNewPassword('');
    setConfirmPassword('');
    setErrorMsg('');
    onClose();
  };

  const handleSendCode = (e) => {
    e.preventDefault();
    setErrorMsg('');
    setStep(2);
  };

  const handleVerifyCode = (e) => {
    e.preventDefault();
    if (!/^\d{6}$/.test(code)) {
      setErrorMsg('El código debe tener exactamente 6 dígitos numéricos.');
      return;
    }
    setErrorMsg('');
    setStep(3);
  };

  const handleSavePassword = (e) => {
    e.preventDefault();
    if (newPassword.length < 8) {
      setErrorMsg('La contraseña debe tener al menos 8 caracteres.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setErrorMsg('Las contraseñas no coinciden. Verifica que ambas sean iguales.');
      return;
    }
    setErrorMsg('');
    if (onComplete) {
      onComplete('Contraseña actualizada exitosamente.');
    }
    handleClose();
  };

  return (
    <BaseModal isOpen={isOpen} onClose={handleClose} maxWidth="440px">
      {step === 1 && (
        <div className="auth-modal-card" data-node-id="216:1407" data-name="Componente Modal Standalone (440px)">
          <div className="auth-modal-header" data-node-id="216:1408">
            <h3 className="auth-modal-title" data-node-id="216:1410">
              Recuperar Contraseña
            </h3>
            <p className="auth-modal-subtitle" style={{ color: '#0d7c75' }} data-node-id="216:1411">
              Te enviaremos un código de restablecimiento
            </p>
          </div>

          <form className="auth-modal-form" onSubmit={handleSendCode} data-node-id="216:1412">
            <div className="auth-modal-description" data-node-id="216:1414">
              <p>Ingresa tu correo electrónico registrado y te enviaremos las instrucciones</p>
              <p>para restablecer tu contraseña.</p>
            </div>

            <div className="auth-modal-field" data-node-id="216:1415">
              <label className="auth-modal-label" data-node-id="216:1417">
                Correo Electrónico Registrado
              </label>
              <input
                type="email"
                className="auth-modal-input"
                placeholder="ej. usuario@avicontrol.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                data-node-id="216:1418"
              />
            </div>

            <div className="auth-modal-footer" data-node-id="216:1421">
              <button
                type="button"
                className="auth-modal-btn-cancel"
                onClick={handleClose}
                data-node-id="216:1422"
              >
                Cancelar
              </button>
              <button
                type="submit"
                className="auth-modal-btn-submit"
                data-node-id="216:1424"
              >
                Enviar Código
              </button>
            </div>
          </form>
        </div>
      )}

      {step === 2 && (
        <div className="auth-modal-card" data-node-id="216:1427" data-name="Componente Modal Standalone (440px)">
          <div className="auth-modal-header" data-node-id="216:1428">
            <h3 className="auth-modal-title" data-node-id="216:1430">
              Código Enviado
            </h3>
            <p className="auth-modal-subtitle" style={{ color: '#059669' }} data-node-id="216:1431">
              Revisa tu bandeja de entrada
            </p>
          </div>

          <form className="auth-modal-form" onSubmit={handleVerifyCode} data-node-id="216:1432">
            <div className="auth-modal-alert-success" data-node-id="216:1433">
              <p className="auth-modal-alert-title" data-node-id="216:1435">
                ✅ Correo de verificación enviado
              </p>
              <p className="auth-modal-alert-body" data-node-id="216:1437">
                Hemos enviado un enlace y código de 6 dígitos a{' '}
                <strong>{email || 'usuario@avicontrol.com'}</strong>.
              </p>
            </div>

            <div className="auth-modal-field" data-node-id="216:1438">
              <label className="auth-modal-label" data-node-id="216:1440">
                Ingresa el Código de 6 dígitos
              </label>
              <input
                type="text"
                inputMode="numeric"
                maxLength={6}
                className="auth-modal-input auth-modal-code-input"
                placeholder="123456"
                value={code}
                onChange={(e) => {
                  const onlyDigits = e.target.value.replace(/\D/g, '').slice(0, 6);
                  setCode(onlyDigits);
                  if (errorMsg) setErrorMsg('');
                }}
                required
                data-node-id="216:1441"
              />
              {errorMsg && <p className="auth-modal-error-text">{errorMsg}</p>}
            </div>

            <div className="auth-modal-footer" data-node-id="216:1444">
              <button
                type="button"
                className="auth-modal-btn-cancel"
                onClick={() => {
                  setCode('');
                  setErrorMsg('');
                }}
                data-node-id="216:1445"
              >
                Reenviar
              </button>
              <button
                type="submit"
                className="auth-modal-btn-submit"
                data-node-id="216:1447"
              >
                Verificar Código
              </button>
            </div>
          </form>
        </div>
      )}

      {step === 3 && (
        <div className="auth-modal-card" data-node-id="216:1450" data-name="Componente Modal Standalone (440px)">
          <div className="auth-modal-header" data-node-id="216:1451">
            <h3 className="auth-modal-title" data-node-id="216:1453">
              Nueva Contraseña
            </h3>
            <p className="auth-modal-subtitle" style={{ color: '#2e7d32' }} data-node-id="216:1454">
              Ingresa tu nueva clave de acceso
            </p>
          </div>

          <form className="auth-modal-form" onSubmit={handleSavePassword} data-node-id="216:1455">
            <div className="auth-modal-field" data-node-id="216:1456">
              <label className="auth-modal-label" data-node-id="216:1458">
                Nueva Contraseña
              </label>
              <input
                type="password"
                className="auth-modal-input"
                placeholder="••••••••••••"
                value={newPassword}
                onChange={(e) => {
                  setNewPassword(e.target.value);
                  if (errorMsg) setErrorMsg('');
                }}
                required
                data-node-id="216:1459"
              />
            </div>

            <div className="auth-modal-field" data-node-id="216:1462">
              <label className="auth-modal-label" data-node-id="216:1464">
                Confirmar Nueva Contraseña
              </label>
              <input
                type="password"
                className="auth-modal-input"
                placeholder="••••••••••••"
                value={confirmPassword}
                onChange={(e) => {
                  setConfirmPassword(e.target.value);
                  if (errorMsg) setErrorMsg('');
                }}
                required
                data-node-id="216:1465"
              />
              {confirmPassword.length > 0 && newPassword !== confirmPassword ? (
                <p className="auth-modal-error-text">Las contraseñas no coinciden.</p>
              ) : (
                errorMsg && <p className="auth-modal-error-text">{errorMsg}</p>
              )}
            </div>

            <div className="auth-modal-req-box" data-node-id="216:1468">
              <p className="auth-modal-req-title" data-node-id="216:1470">
                Requisitos de contraseña:
              </p>
              <ul className="auth-modal-req-list" data-node-id="216:1471">
                <li>Mínimo 8 caracteres</li>
                <li>Al menos una letra mayúscula y un número</li>
              </ul>
            </div>

            <div className="auth-modal-footer" data-node-id="216:1478">
              <button
                type="button"
                className="auth-modal-btn-cancel"
                onClick={handleClose}
                data-node-id="216:1479"
              >
                Cancelar
              </button>
              <button
                type="submit"
                className="auth-modal-btn-submit"
                data-node-id="216:1481"
              >
                Guardar Contraseña
              </button>
            </div>
          </form>
        </div>
      )}
    </BaseModal>
  );
}

export default RecoverPasswordModal;
