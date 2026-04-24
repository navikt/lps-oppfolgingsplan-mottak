CREATE TABLE FOLLOW_UP_PLAN_INBOX
(
    correlation_id      TEXT PRIMARY KEY,
    organization_number VARCHAR(9)  NOT NULL,
    lps_orgnumber       VARCHAR(9)  NOT NULL,
    employee_identification_number TEXT,
    raw_payload         TEXT        NOT NULL,
    received_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
