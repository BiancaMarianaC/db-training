// ============================================================================
// recon.js — TICKET-I097
// ============================================================================
// WHAT:    Fetches /api/v1/recon/results?status=OPEN and renders the breaks.
//          Each row has a Resolve button that calls PUT /api/v1/recon/{id}/resolve.
// ============================================================================

const API_BASE = "http://localhost:8080/api/v1";
const AUTH_HEADER = "Basic " + btoa("trader:trader-pass");

document.addEventListener("DOMContentLoaded", loadBreaks);

/**
 * TODO(TICKET-I097):
 *  - fetch open breaks
 *  - render each as a row with a Resolve button
 *  - clicking Resolve calls PUT /api/v1/recon/{id}/resolve and removes the row
 *
 *  HINT: Use event delegation on the <tbody> rather than adding a listener
 *  to every button — fewer leaks, easier to maintain.
 */
async function loadBreaks() {
    const tbody = document.getElementById("recon-tbody");
    const loading = document.getElementById("recon-loading");
    const errorDiv = document.getElementById("recon-error");

    loading.classList.remove("hidden");
    errorDiv.classList.add("hidden");

    try {
        const res = await fetch(`${API_BASE}/recon/results?status=OPEN`, {
            headers: { "Authorization": AUTH_HEADER }
        });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        const items = data.content || data;
        tbody.innerHTML = items.map(rowHtml).join("");
        tbody.addEventListener("click", onResolveClick);
    } catch (e) {
        errorDiv.textContent = "Could not load breaks: " + e.message;
        errorDiv.classList.remove("hidden");
    } finally {
        loading.classList.add("hidden");
    }
}

function rowHtml(r) {
    return `
        <tr data-id="${r.id}">
            <td>${r.tradeRef || r.tradeId}</td>
            <td>${r.discrepancyType || "—"}</td>
            <td><span class="badge badge-open">${r.status}</span></td>
            <td>${r.createdAt || ""}</td>
            <td><button data-action="resolve">Resolve</button></td>
        </tr>
    `;
}

async function onResolveClick(evt) {
    const btn = evt.target;
    if (btn.dataset.action !== "resolve") return;
    const row = btn.closest("tr");
    const id = row.dataset.id;

    btn.disabled = true;
    try {
        const res = await fetch(`${API_BASE}/recon/${id}/resolve`, {
            method: "PUT",
            headers: { "Authorization": AUTH_HEADER }
        });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        row.remove();
    } catch (e) {
        alert("Resolve failed: " + e.message);
        btn.disabled = false;
    }
}
