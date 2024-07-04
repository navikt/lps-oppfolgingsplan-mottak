CREATE TABLE SYKMELDINGSPERIODE
(
    uuid                           UUID PRIMARY KEY,
    sykmelding_id                  TEXT        NOT NULL,
    organization_number            VARCHAR(9)  NOT NULL,
    employee_identification_number VARCHAR(11) NOT NULL,
    fom                            TIMESTAMP   NOT NULL,
    tom                            TIMESTAMP   NOT NULL,
    created_at                     TIMESTAMP   NOT NULL
);

CREATE INDEX organization_nr_index ON SYKMELDINGSPERIODE (organization_number);
CREATE INDEX employee_nr_index ON SYKMELDINGSPERIODE (employee_identification_number);
