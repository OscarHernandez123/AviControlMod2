import { createContext, useEffect, useReducer } from 'react';
import { farmReducer, initialFarmState } from '../reducers/farmReducer';

export const FarmContext = createContext();

const NUTRI_STORAGE_KEY = 'avicontrol_nutri_state_v2';

function initFarmState(defaultState) {
  try {
    const raw = localStorage.getItem(NUTRI_STORAGE_KEY);
    if (!raw) return defaultState;
    const parsed = JSON.parse(raw);
    if (
      parsed &&
      Array.isArray(parsed.nutriPlans) &&
      Array.isArray(parsed.assignedBarnPlans)
    ) {
      return {
        ...defaultState,
        sackWeightKg: Number(parsed.sackWeightKg) || defaultState.sackWeightKg,
        nutriPlans: parsed.nutriPlans,
        assignedBarnPlans: parsed.assignedBarnPlans,
        vetDiseases: Array.isArray(parsed.vetDiseases)
          ? parsed.vetDiseases
          : defaultState.vetDiseases,
        vetMedicines: Array.isArray(parsed.vetMedicines)
          ? parsed.vetMedicines
          : defaultState.vetMedicines,
        vetDiagnoses: Array.isArray(parsed.vetDiagnoses)
          ? parsed.vetDiagnoses
          : defaultState.vetDiagnoses,
        vetSlaughterOrders: Array.isArray(parsed.vetSlaughterOrders)
          ? parsed.vetSlaughterOrders
          : defaultState.vetSlaughterOrders,
        vetReviewRequests: Array.isArray(parsed.vetReviewRequests)
          ? parsed.vetReviewRequests
          : defaultState.vetReviewRequests,
        vetDiagnosedBarns: Array.isArray(parsed.vetDiagnosedBarns)
          ? parsed.vetDiagnosedBarns
          : defaultState.vetDiagnosedBarns
      };
    }
  } catch {
    // En caso de error de lectura en localStorage, usar el estado inicial
  }
  return defaultState;
}

export function FarmProvider({ children }) {
  const [state, dispatch] = useReducer(
    farmReducer,
    initialFarmState,
    initFarmState
  );

  useEffect(() => {
    try {
      const payload = {
        sackWeightKg: state.sackWeightKg,
        nutriPlans: state.nutriPlans,
        assignedBarnPlans: state.assignedBarnPlans,
        vetDiseases: state.vetDiseases,
        vetMedicines: state.vetMedicines,
        vetDiagnoses: state.vetDiagnoses,
        vetSlaughterOrders: state.vetSlaughterOrders,
        vetReviewRequests: state.vetReviewRequests,
        vetDiagnosedBarns: state.vetDiagnosedBarns
      };
      localStorage.setItem(NUTRI_STORAGE_KEY, JSON.stringify(payload));
    } catch {
      // Ignorar errores de cuota de almacenamiento local
    }
  }, [
    state.sackWeightKg,
    state.nutriPlans,
    state.assignedBarnPlans,
    state.vetDiseases,
    state.vetMedicines,
    state.vetDiagnoses,
    state.vetSlaughterOrders,
    state.vetReviewRequests,
    state.vetDiagnosedBarns
  ]);

  const addFeedReception = (reception) => {
    dispatch({ type: 'ADD_FEED_RECEPTION', payload: reception });
  };

  const updateFeedReception = (reception) => {
    dispatch({ type: 'UPDATE_FEED_RECEPTION', payload: reception });
  };

  const addMedReception = (reception) => {
    dispatch({ type: 'ADD_MED_RECEPTION', payload: reception });
  };

  const updateMedReception = (reception) => {
    dispatch({ type: 'UPDATE_MED_RECEPTION', payload: reception });
  };

  const validateBarnForSlaughter = (barn) => {
    dispatch({ type: 'VALIDATE_BARN_FOR_SLAUGHTER', payload: barn });
  };

  const scheduleSlaughterOrder = (orderData) => {
    dispatch({ type: 'SCHEDULE_SLAUGHTER_ORDER', payload: orderData });
  };

  const cancelSlaughterOrder = (galpon) => {
    dispatch({ type: 'CANCEL_SLAUGHTER_ORDER', payload: galpon });
  };

  const saveSlaughterResults = (results) => {
    dispatch({ type: 'SAVE_SLAUGHTER_RESULTS', payload: results });
  };

  const registerWorkerMortality = (data) => {
    dispatch({ type: 'REGISTER_WORKER_MORTALITY', payload: data });
  };

  const requestBarnIsolation = (data) => {
    dispatch({ type: 'REQUEST_BARN_ISOLATION', payload: data });
  };

  const saveNutriPlan = (payload) => {
    dispatch({ type: 'SAVE_NUTRI_PLAN', payload });
  };

  const setNutriPlanStatus = (planId, status) => {
    dispatch({ type: 'SET_NUTRI_PLAN_STATUS', payload: { planId, status } });
  };

  const assignNutriPlan = (assignment) => {
    dispatch({ type: 'ASSIGN_NUTRI_PLAN', payload: assignment });
  };

  const adjustNutriStage = (adjustment) => {
    dispatch({ type: 'ADJUST_NUTRI_STAGE', payload: adjustment });
  };

  const registerBarnFeedConsumption = (galpon, registeredConsumptionKg) => {
    dispatch({
      type: 'REGISTER_BARN_FEED_CONSUMPTION',
      payload: { galpon, registeredConsumptionKg }
    });
  };

  const setSackWeightKg = (weightKg) => {
    dispatch({ type: 'SET_SACK_WEIGHT_KG', payload: weightKg });
  };

  const resetNutriDemoData = () => {
    try {
      localStorage.removeItem(NUTRI_STORAGE_KEY);
    } catch {
      // ignore
    }
    dispatch({ type: 'RESET_NUTRI_DEMO_DATA' });
  };

  const saveVetDisease = (disease) => {
    dispatch({ type: 'SAVE_VET_DISEASE', payload: disease });
  };

  const saveVetMedicine = (medicine) => {
    dispatch({ type: 'SAVE_VET_MEDICINE', payload: medicine });
  };

  const saveVetDiagnosis = (diagnosis) => {
    dispatch({ type: 'SAVE_VET_DIAGNOSIS', payload: diagnosis });
  };

  const updateVetRequestStatus = (id, estado) => {
    dispatch({ type: 'UPDATE_VET_REQUEST_STATUS', payload: { id, estado } });
  };

  const saveVetSlaughterOrder = (order) => {
    dispatch({ type: 'SAVE_VET_SLAUGHTER_ORDER', payload: order });
  };

  const updateVetSlaughterStatus = (id, estado) => {
    dispatch({ type: 'UPDATE_VET_SLAUGHTER_STATUS', payload: { id, estado } });
  };

  return (
    <FarmContext.Provider
      value={{
        ...state,
        addFeedReception,
        updateFeedReception,
        addMedReception,
        updateMedReception,
        validateBarnForSlaughter,
        scheduleSlaughterOrder,
        cancelSlaughterOrder,
        saveSlaughterResults,
        registerWorkerMortality,
        requestBarnIsolation,
        saveNutriPlan,
        setNutriPlanStatus,
        assignNutriPlan,
        adjustNutriStage,
        registerBarnFeedConsumption,
        setSackWeightKg,
        resetNutriDemoData,
        saveVetDisease,
        saveVetMedicine,
        saveVetDiagnosis,
        updateVetRequestStatus,
        saveVetSlaughterOrder,
        updateVetSlaughterStatus
      }}
    >
      {children}
    </FarmContext.Provider>
  );
}
