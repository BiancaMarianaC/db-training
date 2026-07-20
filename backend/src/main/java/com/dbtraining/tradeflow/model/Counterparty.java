package com.dbtraining.tradeflow.model;

/**
 * ============================================================================
 * Counterparty — TICKET-I022 + TICKET-I057
 * ============================================================================
 * WHAT:    The other side of a trade — broker, exchange, or another bank.
 * HOW:     Plain POJO on Day 2; converted to JPA @Entity on Day 5.
 * WHY:     The trade table has an FK to this — every trade has a counterparty.
 * OBSERVE: Build a Counterparty in main(), pass it through a Trade, print both.
 * ============================================================================
 *  TODO(TICKET-I022) [Day 2]:
 *    Fields: id (Long), name (String), leiCode (String, 20 chars), region (String).
 *    Add private constructor + Builder for clean construction.
 *    Add equals()/hashCode() on leiCode (it's globally unique).
 *
 *  TODO(TICKET-I057) [Day 5]:
 *    Convert this class to a JPA entity.
 *    - @Entity @Table(name = "counterparties")
 *    - @Id @GeneratedValue(strategy = GenerationType.IDENTITY) on id
 *    - @Column(unique = true, length = 20) on leiCode
 *    - protected no-arg constructor (JPA needs it).
 * ============================================================================
 */
public class Counterparty {

    // TODO(TICKET-I022): private fields here.

    // TODO(TICKET-I022): private constructor used by Builder.

    // TODO(TICKET-I022): getters (no setters — favour immutability).

    // TODO(TICKET-I022): static inner Builder class.

    // TODO(TICKET-I025-style): equals() + hashCode() on leiCode.

    // TODO(TICKET-I022): toString() that returns e.g. "Counterparty[GS / W22L..ZB6K528]".
}
