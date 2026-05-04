import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <div className="placeholder-page">
      <h2>404 — Page not found</h2>
      <p>The page you're looking for doesn't exist.</p>
      <Link to="/" className="btn-primary" style={{ marginTop: 16, display: 'inline-block' }}>
        Go home
      </Link>
    </div>
  );
}
