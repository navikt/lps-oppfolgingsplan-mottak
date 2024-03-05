DROP INDEX fnr_index;
DROP INDEX lps_fnr_index;
DROP TABLE ALTINN_LPS;

CREATE TABLE ALTINN_LPS
(
    uuid                         UUID PRIMARY KEY,
    lps_fnr                      VARCHAR(11) NOT NULL,
    fnr                          VARCHAR(11),
    orgnummer                    VARCHAR(9)  NOT NULL,
    pdf                          BYTEA,
    xml                          TEXT        NOT NULL,
    should_send_to_nav           BOOLEAN     NOT NULL,
    should_send_to_fastlege      BOOLEAN     NOT NULL,
    sent_to_nav                  BOOLEAN     NOT NULL DEFAULT FALSE,
    sent_to_fastlege             BOOLEAN     NOT NULL DEFAULT FALSE,
    send_to_fastlege_retry_count INTEGER     NOT NULL DEFAULT 0,
    journalpost_id               VARCHAR(20),
    archive_reference            VARCHAR(40),
    created                      TIMESTAMP   NOT NULL,
    last_changed                 TIMESTAMP   NOT NULL,
    migrated                     BOOLEAN     NOT NULL DEFAULT FALSE
);

CREATE INDEX fnr_index ON ALTINN_LPS (fnr);
CREATE INDEX lps_fnr_index ON ALTINN_LPS (lps_fnr);
