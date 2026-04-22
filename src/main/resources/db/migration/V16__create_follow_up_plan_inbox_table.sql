CREATE TABLE FOLLOW_UP_PLAN_INBOX
(
    correlation_id      TEXT PRIMARY KEY,
    organization_number TEXT        NOT NULL,
    lps_orgnumber       TEXT        NOT NULL,
    raw_payload         TEXT        NOT NULL,
    status              TEXT        NOT NULL,
    status_message      TEXT,
    received_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    validated_at        TIMESTAMPTZ,
    processed_at        TIMESTAMPTZ,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (status IN ('RECEIVED', 'VALIDATED', 'REJECTED', 'PROCESSED', 'VALIDATED_TECHNICAL_FAILURE'))
);

CREATE INDEX follow_up_plan_inbox_status_received_at_idx ON FOLLOW_UP_PLAN_INBOX (status, received_at);
CREATE INDEX follow_up_plan_inbox_organization_number_idx ON FOLLOW_UP_PLAN_INBOX (organization_number);
CREATE INDEX follow_up_plan_inbox_processed_at_idx ON FOLLOW_UP_PLAN_INBOX (processed_at);
