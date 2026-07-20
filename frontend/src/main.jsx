/**
 * ============================================================================
 * main.jsx — React entry point (TICKET-I099 + TICKET-I110)
 * ============================================================================
 * WHAT:    Mounts <App /> into #root, wraps it in BrowserRouter.
 * HOW:     React 18 `createRoot` + StrictMode.
 * WHY:     One file, one job — bootstrap.
 * ============================================================================
 */
import React from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App.jsx';
import './styles/global.css';

const root = createRoot(document.getElementById('root'));

root.render(
    <React.StrictMode>
        <BrowserRouter>
            <App />
        </BrowserRouter>
    </React.StrictMode>
);
