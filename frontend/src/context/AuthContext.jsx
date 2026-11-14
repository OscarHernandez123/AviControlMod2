import { createContext, useReducer } from 'react';
import { authReducer, initialAuthState } from '../reducers/authReducer';

export const AuthContext = createContext();

export function AuthProvider({ children }) {
  const [state, dispatch] = useReducer(authReducer, initialAuthState);

  const login = (email, password, role = 'administrador') => {
    let detectedRole = role;
    const lower = (email || '').toLowerCase();
    if (lower.includes('veterinario') || lower.includes('vet')) {
      detectedRole = 'veterinario';
    } else if (lower.includes('nutricionista') || lower.includes('nutri')) {
      detectedRole = 'nutricionista';
    } else if (lower.includes('trabajador') || lower.includes('operario')) {
      detectedRole = 'trabajador';
    }

    dispatch({
      type: 'LOGIN_SUCCESS',
      payload: {
        role: detectedRole,
        user: {
          name:
            detectedRole === 'veterinario'
              ? 'Veterinario'
              : detectedRole === 'nutricionista'
              ? 'Usuario (Nutricionista)'
              : detectedRole === 'trabajador'
              ? 'Carlos Pérez'
              : 'Administrador',
          email: email || 'veterinario@avicontrol.com',
          role: detectedRole
        }
      }
    });

    return detectedRole;
  };

  const logout = () => {
    dispatch({ type: 'LOGOUT' });
  };

  const setActiveRole = (role) => {
    dispatch({ type: 'SET_ACTIVE_ROLE', payload: role });
  };

  const setRecoveryEmail = (email) => {
    dispatch({ type: 'SET_RECOVERY_EMAIL', payload: email });
  };

  return (
    <AuthContext.Provider
      value={{
        ...state,
        login,
        logout,
        setActiveRole,
        setRecoveryEmail
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}
