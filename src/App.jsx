import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import ProtectedRoute from "./components/ProtectedRoute";
import LoginPage from "./pages/LoginPage";
import PatientsListPage from "./pages/PatientsListPage";
import UsersPage from "./pages/UsersPage";
import RolesPage from "./pages/RolesPage";

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/patients"
            element={
              <ProtectedRoute>
                <PatientsListPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/users"
            element={
              <ProtectedRoute>
                <UsersPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/roles"
            element={
              <ProtectedRoute>
                <RolesPage />
              </ProtectedRoute>
            }
          />
          <Route path="/" element={<Navigate to="/patients" replace />} />
          <Route path="*" element={<Navigate to="/patients" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
