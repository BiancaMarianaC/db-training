// ============================================================================
// addTrade.js — TICKET-I095 + TICKET-I096
// ============================================================================
// WHAT:    Handles the Add-Trade form: client validation + POST.
// WHY:     Showcases why ad-hoc DOM manipulation gets ugly fast — every
//          field has its own error <span> we must show/hide manually.
//          React Hook Form (Day 9) replaces all of this.
// ============================================================================

const API_BASE = "http://localhost:8080/api/v1";
const AUTH_HEADER = "Basic " + btoa("trader:trader-pass");

document.addEventListener("DOMContentLoaded", () => {
    document.getElementById("trade-form").addEventListener("submit", onSubmit);
});

/**
 * TODO(TICKET-I095): client-side validation:
 *  - all fields required
 *  - quantity > 0, price > 0
 *  - tradeDate not in the future
 *  - show inline error spans, return false if invalid
 *
 * TODO(TICKET-I096): on valid, POST /api/v1/trades, show toast on success.
 */
async function onSubmit(evt) {
    evt.preventDefault();
    const form = evt.target;
    const data = Object.fromEntries(new FormData(form).entries());

    if (!validate(data)) return;

    try {
        const res = await fetch(`${API_BASE}/trades`, {
            method: "POST",
            headers: {
                "Authorization": AUTH_HEADER,
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                tradeRef:        data.tradeRef,
                instrumentId:    Number(data.instrumentId),
                counterpartyId:  Number(data.counterpartyId),
                quantity:        data.quantity,
                price:           data.price,
                tradeDate:       data.tradeDate
            })
        });

        if (!res.ok) {
            const body = await res.json().catch(() => ({}));
            throw new Error(body.message || `HTTP ${res.status}`);
        }

        showToast("Trade created — redirecting…");
        setTimeout(() => location.href = "trades.html", 800);
    } catch (e) {
        showToast("Error: " + e.message, true);
    }
}

function validate(data) {
    // TODO(TICKET-I095): set/clear each .field-error[data-for=...] span.
    clearErrors();
    let ok = true;

    if (Number(data.quantity) <= 0) { setError("quantity", "must be > 0"); ok = false; }
    if (Number(data.price)    <= 0) { setError("price",    "must be > 0"); ok = false; }
    if (!data.tradeDate)             { setError("tradeDate", "required");   ok = false; }
    if (data.tradeDate && new Date(data.tradeDate) > new Date()) {
        setError("tradeDate", "must not be in the future"); ok = false;
    }

    return ok;
}

function clearErrors() {
    document.querySelectorAll(".field-error").forEach(s => s.textContent = "");
}
function setError(field, msg) {
    const el = document.querySelector(`.field-error[data-for="${field}"]`);
    if (el) el.textContent = msg;
}
function showToast(msg, isError = false) {
    const t = document.getElementById("form-feedback");
    t.textContent = msg;
    t.style.borderLeftColor = isError ? "#c62828" : "#003366";
    t.classList.remove("hidden");
}
