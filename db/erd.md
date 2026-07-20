# TradeFlow — Entity Relationship Diagram (TICKET-I002)

> Replace this file with your team's ER diagram.

## Skeleton (replace with your real diagram)

```mermaid
erDiagram
    COUNTERPARTIES ||--o{ TRADES : "originates"
    INSTRUMENTS    ||--o{ TRADES : "references"
    TRADES         ||--o{ RECON_RESULTS : "produces"
    TRADES         ||--o{ AUDIT_LOG : "is audited by"

    COUNTERPARTIES {
        BIGINT id PK
        VARCHAR name
        CHAR(20) lei_code UK
        VARCHAR region
    }
    INSTRUMENTS {
        BIGINT id PK
        VARCHAR symbol UK
        VARCHAR name
        VARCHAR asset_class
        CHAR(3) currency
    }
    TRADES {
        BIGINT id PK
        VARCHAR trade_ref UK
        BIGINT instrument_id FK
        BIGINT counterparty_id FK
        NUMERIC quantity
        NUMERIC price
        DATE trade_date
        VARCHAR status
    }
    RECON_RESULTS {
        BIGINT id PK
        BIGINT trade_id FK
        VARCHAR status
        VARCHAR discrepancy_type
        TIMESTAMPTZ resolved_at
    }
    AUDIT_LOG {
        BIGINT id PK
        VARCHAR entity
        BIGINT entity_id
        VARCHAR action
        JSONB old_value
        JSONB new_value
        TIMESTAMPTZ "timestamp"
    }
```

## TODO(TICKET-I002)

- [ ] Replace the skeleton above with your team's accurate diagram.
- [ ] Annotate cardinalities (1:N, N:N).
- [ ] Mark optional vs mandatory fields.
- [ ] Link this from the project root `README.md`.
