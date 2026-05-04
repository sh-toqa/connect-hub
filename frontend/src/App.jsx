import { BrowserRouter, Routes, Route, Navigate, Outlet } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import AuthLayout    from './components/common/AuthLayout';
import LoginPage     from './pages/LoginPage';
import SignupPage    from './pages/SignupPage';
import ProfilePage   from './pages/ProfilePage';
import NewsfeedPage  from './pages/NewsfeedPage';
import FriendsPage   from './pages/FriendsPage';
import NotFoundPage  from './pages/NotFoundPage';
import './styles/global.css'
import './styles/profile.css';
import './App.css';

function PrivateRoutes() {
  const { user } = useAuth();
  return user
    ? <AuthLayout />
    : <Navigate to="/login" replace />;
}

function PublicRoutes() {
  const { user } = useAuth();
  return user
    ? <Navigate to="/feed" replace />
    : <Outlet />;
}

function AppRoutes() {
  return (
    <Routes>
      <Route element={<PublicRoutes />}>
        <Route path="/login"  element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
      </Route>

      <Route element={<PrivateRoutes />}>
        <Route path="/feed"    element={<NewsfeedPage />} />
        <Route path="/profile" element={<ProfilePage />} />
        <Route path="/friends" element={<FriendsPage />} />
      </Route>

      <Route path="/"  element={<Navigate to="/feed" replace />} />
      <Route path="*"  element={<NotFoundPage />} />
    </Routes>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  );
}