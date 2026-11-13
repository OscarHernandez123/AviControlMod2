import './index.css';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { FarmProvider } from './context/FarmContext';
import LoginPage from './pages/LoginPage';
import RoleSelectionPage from './pages/RoleSelectionPage';
import AdminDashboardPage from './pages/AdminDashboardPage';
import AdminBarnsPage from './pages/AdminBarnsPage';
import AdminInventoryPage from './pages/AdminInventoryPage';
import AdminFeedReceptionsPage from './pages/AdminFeedReceptionsPage';
import AdminMedReceptionsPage from './pages/AdminMedReceptionsPage';
import AdminSlaughterOrdersPage from './pages/AdminSlaughterOrdersPage';
import NutritionistDashboardPage from './pages/NutritionistDashboardPage';
import NutritionistPlansPage from './pages/NutritionistPlansPage';
import NutritionistAssignmentsPage from './pages/NutritionistAssignmentsLivePage';
import NutritionistProjectionsPage from './pages/NutritionistProjectionsPage';
import VetDashboardPage from './pages/VetDashboardPage';
import VetSlaughterOrdersPage from './pages/VetSlaughterOrdersPage';
import VetDiagnosePage from './pages/VetDiagnosePage';
import VetDiseasesPage from './pages/VetDiseasesPage';
import VetMedicinesPage from './pages/VetMedicinesPage';
import WorkerDashboardPage from './pages/WorkerDashboardPage';

function App() {
  return (
    <Router>
      <AuthProvider>
        <FarmProvider>
          <div className="avi-app-shell">
            <main className="avi-app-viewport">
              <Routes>
                <Route path="/" element={<Navigate to="/login" replace />} />
                <Route path="/login" element={<LoginPage />} />
                <Route path="/roles" element={<RoleSelectionPage />} />
                <Route path="/administrador" element={<AdminDashboardPage />} />
                <Route path="/administrador/galpones" element={<AdminBarnsPage />} />
                <Route path="/administrador/inventario" element={<AdminInventoryPage />} />
                <Route path="/administrador/recepciones-alimento" element={<AdminFeedReceptionsPage />} />
                <Route path="/administrador/recepciones-medicamento" element={<AdminMedReceptionsPage />} />
                <Route path="/administrador/ordenes-sacrificio" element={<AdminSlaughterOrdersPage />} />
                <Route path="/administrador/*" element={<Navigate to="/administrador" replace />} />
                <Route path="/admin/*" element={<Navigate to="/administrador" replace />} />
                <Route path="/nutricionista" element={<NutritionistDashboardPage />} />
                <Route path="/nutricionista/planes" element={<NutritionistPlansPage />} />
                <Route path="/nutricionista/asignaciones" element={<NutritionistAssignmentsPage />} />
                <Route path="/nutricionista/proyecciones" element={<NutritionistProjectionsPage />} />
                <Route path="/nutricionista/*" element={<Navigate to="/nutricionista" replace />} />
                <Route path="/veterinario" element={<VetDashboardPage />} />
                <Route path="/veterinario/ordenes" element={<VetSlaughterOrdersPage />} />
                <Route path="/veterinario/diagnosticar" element={<VetDiagnosePage />} />
                <Route path="/veterinario/enfermedades" element={<VetDiseasesPage />} />
                <Route path="/veterinario/medicinas" element={<VetMedicinesPage />} />
                <Route path="/veterinario/*" element={<Navigate to="/veterinario" replace />} />
                <Route path="/trabajador/*" element={<WorkerDashboardPage />} />
                <Route path="*" element={<Navigate to="/login" replace />} />
              </Routes>
            </main>
          </div>
        </FarmProvider>
      </AuthProvider>
    </Router>
  );
}

export default App;
