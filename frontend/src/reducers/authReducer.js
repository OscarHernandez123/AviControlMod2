export const initialAuthState = {
  isAuthenticated: false,
  user: {
    name: 'Administrador',
    email: 'admin@avicontrol.com',
    role: 'administrador'
  },
  activeRole: 'administrador',
  rememberSession: true,
  recoveryEmail: 'usuario@avicontrol.com'
};

export function authReducer(state, action) {
  switch (action.type) {
    case 'LOGIN_SUCCESS':
      return {
        ...state,
        isAuthenticated: true,
        user: action.payload.user || state.user,
        activeRole: action.payload.role || state.activeRole
      };
    case 'LOGOUT':
      return {
        ...state,
        isAuthenticated: false
      };
    case 'SET_ACTIVE_ROLE': {
      const roleNames = {
        administrador: { name: 'Administrador', email: 'admin@avicontrol.com', role: 'administrador' },
        nutricionista: { name: 'Usuario (Nutricionista)', email: 'nutricionista@avicontrol.com', role: 'nutricionista' },
        veterinario: { name: 'Veterinario', email: 'veterinario@avicontrol.com', role: 'veterinario' },
        trabajador: { name: 'Carlos Pérez', email: 'trabajador@avicontrol.com', role: 'trabajador' }
      };
      return {
        ...state,
        activeRole: action.payload,
        user: roleNames[action.payload] || state.user
      };
    }
    case 'SET_RECOVERY_EMAIL':
      return {
        ...state,
        recoveryEmail: action.payload
      };
    default:
      return state;
  }
}
