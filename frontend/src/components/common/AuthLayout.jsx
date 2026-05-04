import { Outlet } from 'react-router-dom';
import Navbar from './Navbar';

/**
 * Shared layout for all authenticated pages.
 * Renders the navbar at the top and the current page below via <Outlet />.
 */
export default function AuthLayout() {
  return (
    <div className="app-shell">
      <Navbar />
      <main className="page-content">
        <Outlet />
      </main>
    </div>
  );
}
