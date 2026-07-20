/**
 * ============================================================================
 * App.jsx — TICKET-I110
 * ============================================================================
 * WHAT:    Top-level component + React Router config.
 * HOW:     <Routes> with one <Route> per page.
 * WHY:     Single source of routing truth. Easy to add new pages.
 * OBSERVE: Clicking a sidebar link doesn't reload the page — that's React
 *          Router doing client-side navigation.
 * ============================================================================
 *
 *  TODO(TICKET-I110):
 *    - Routes: /dashboard, /trades, /trades/new, /recon
 *    - Default `/` -> redirect to /dashboard
 *    - 404 fallback page
 * ============================================================================
 */
import { Navigate, Route, Routes, Link } from 'react-router-dom';
import Dashboard from './pages/Dashboard.jsx';
import Trades from './pages/Trades.jsx';
import AddTradeForm from './components/AddTradeForm.jsx';
import Recon from './pages/Recon.jsx';

export default function App() {
    return (
        <div className="layout">
            <header className="topbar">
                <span className="logo">DB · TradeFlow</span>
                <span className="user">Logged in as <strong>viewer</strong></span>
            </header>

            <div className="main">
                <nav className="sidebar">
                    {/* TODO(TICKET-I110): use NavLink for active styling. */}
                    <ul>
                        <li><Link to="/dashboard">Dashboard</Link></li>
                        <li><Link to="/trades">Trades</Link></li>
                        <li><Link to="/trades/new">+ New Trade</Link></li>
                        <li><Link to="/recon">Recon Breaks</Link></li>
                    </ul>
                </nav>

                <section className="content">
                    <Routes>
                        <Route path="/" element={<Navigate to="/dashboard" replace />} />
                        <Route path="/dashboard" element={<Dashboard />} />
                        <Route path="/trades" element={<Trades />} />
                        <Route path="/trades/new" element={<AddTradeForm />} />
                        <Route path="/recon" element={<Recon />} />
                        <Route path="*" element={<NotFound />} />
                    </Routes>
                </section>
            </div>
        </div>
    );
}

function NotFound() {
    return (
        <div>
            <h2>404 — Not Found</h2>
            <Link to="/dashboard">Back to dashboard</Link>
        </div>
    );
}
