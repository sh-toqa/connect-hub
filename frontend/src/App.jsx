import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './routes/ProtectedRoute';
import SignupPage from './pages/SignupPage';
import LoginPage  from './pages/LoginPage';
import './styles/global.css';

const Placeholder = ({ name }) => (
  <div style={{ padding: '2rem', textAlign: 'center' }}>
    <h2>{name}</h2>
    <p>This feature is built in its own module.</p>
  </div>
);

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          {/* Public */}
          <Route path="/login"  element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />

          {/* Protected */}
          <Route path="/" element={
            <ProtectedRoute><Placeholder name="Newsfeed" /></ProtectedRoute>
          } />
          <Route path="/profile" element={
            <ProtectedRoute><Placeholder name="Profile" /></ProtectedRoute>
          } />

          {/* Catch-all */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
} 