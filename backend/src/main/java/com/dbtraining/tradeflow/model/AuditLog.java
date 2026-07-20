package com.dbtraining.tradeflow.model;

/**
 * ============================================================================
 * AuditLog — TICKET-I059
 * ============================================================================
 * WHAT:    Append-only history of who changed what when.
 * HOW:     JPA entity with JSONB columns for the before/after payload.
 * WHY:     Regulators (and the Ops team) need to be able to reconstruct
 *          every change. Schema agnostic via JSONB.
 * OBSERVE: A trade UPDATE writes one row with old_value+new_value JSON diffs.
 * ============================================================================
 *  TODO(TICKET-I059):
 *    Fields:
 *      Long id
 *      String entity        (e.g. "trade", "recon_result")
 *      Long entityId
 *      String action        (INSERT|UPDATE|DELETE)
 *      String oldValue      (JSON — store as JSONB on Postgres)
 *      String newValue      (JSON — store as JSONB on Postgres)
 *      Instant timestamp
 *      String userName
 *
 *  HINT: For JSONB mapping on Hibernate 6:
 *      @Column(columnDefinition = "jsonb")
 *      @JdbcTypeCode(SqlTypes.JSON)
 *      private String oldValue;
 * ============================================================================
 */
public class AuditLog {
    // TODO(TICKET-I059): fields + JPA annotations.
}
