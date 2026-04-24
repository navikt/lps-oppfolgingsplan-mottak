CREATE TABLE FOLLOW_UP_PLAN_INBOX
(
    correlation_id      TEXT PRIMARY KEY,
    organization_number TEXT        NOT NULL,
    lps_orgnumber       TEXT        NOT NULL,
    raw_payload         TEXT        NOT NULL,
    received_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
