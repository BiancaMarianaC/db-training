/**
 * ============================================================================
 * apiService.js — TICKET-I100
 * ============================================================================
 * WHAT:    One module for ALL backend calls.
 * HOW:     Tiny `request()` wrapper around fetch; named exports for each
 *          endpoint.
 * WHY:     Auth header in one place, error envelope handled in one place,
 *          base URL in one place. Components stay focused on rendering.
 * OBSERVE: Open the Network tab — every API call goes through here.
 * ============================================================================
 */

const BASE = import.meta.env.VITE_API_BASE_URL || '/api/v1';

// Hard-coded TRADER credentials for Day 8. A real login flow would replace
// this in a later phase. TRADER role is required for POST/PUT/DELETE — the
// Add Trade form and Recon Resolve button both need it. VIEWER alone gets
// 403 on writes (per Phase-2.5 SecurityConfig role matchers).
const AUTH = 'Basic ' + btoa('trader:trader-pw');

/**
 * ApiError — thrown by request() on non-2xx responses.
 * Carries status and the parsed error body, so callers can show
 * meaningful messages.
 */
export class ApiError extends Error {
    constructor(status, body) {
        super(body?.message || `HTTP ${status}`);
        this.status = status;
        this.body = body;
    }
}

async function request(path, options = {}) {
    const res = await fetch(BASE + path, {
        ...options,
        headers: {
            'Content-Type': 'application/json',
            'Authorization': AUTH,
            ...(options.headers || {})
        }
    });

    if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new ApiError(res.status, body);
    }

    if (res.status === 204) return null;
    return res.json();
}

// ----- Trades --------------------------------------------------------------
export const getTrades       = (params = {}) =>
    request('/trades?' + new URLSearchParams(params).toString());

export const createTrade     = (body) =>
    request('/trades', { method: 'POST', body: JSON.stringify(body) });

export const updateStatus    = (id, status) =>
    request(`/trades/${id}/status`, { method: 'PUT', body: JSON.stringify({ status }) });

export const cancelTrade     = (id) =>
    request(`/trades/${id}`, { method: 'DELETE' });

// ----- Recon ---------------------------------------------------------------
export const runRecon        = () =>
    request('/recon/run', { method: 'POST' });

export const getReconResults = (params = {}) =>
    request('/recon/results?' + new URLSearchParams(params).toString());

export const resolveBreak    = (id) =>
    request(`/recon/${id}/resolve`, { method: 'PUT' });
