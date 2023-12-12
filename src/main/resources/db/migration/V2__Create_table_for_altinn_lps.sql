CREATE TABLE ALTINN_LPS (
  archive_reference         VARCHAR(40)         PRIMARY KEY,
  uuid                      UUID                NOT NULL,
  lps_fnr                   VARCHAR(11)         NOT NULL,
  fnr                       VARCHAR(11),
  orgnummer                 VARCHAR(9)          NOT NULL,
  pdf                       BYTEA,
  xml                       TEXT                NOT NULL,
  should_send_to_nav        BOOLEAN             NOT NULL,
  should_send_to_gp         BOOLEAN             NOT NULL,
  sent_to_gp                BOOLEAN             NOT NULL        DEFAULT FALSE,
  send_to_gp_retry_count    INTEGER             NOT NULL        DEFAULT 0,
  originally_created        TIMESTAMP           NOT NULL,
  created                   TIMESTAMP           NOT NULL,
  last_changed              TIMESTAMP           NOT NULL
);

CREATE INDEX fnr_index ON ALTINN_LPS (fnr);
CREATE INDEX lps_fnr_index ON ALTINN_LPS (lps_fnr);
