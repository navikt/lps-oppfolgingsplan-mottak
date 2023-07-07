CREATE TABLE OPPFOLGINGSPLAN_LPS (
  uuid                     UUID               PRIMARY KEY,
  fnr                      VARCHAR(11)        NOT NULL,
  virksomhetsnummer        VARCHAR(9)         NOT NULL,
  mottaker                 VARCHAR(30)        NOT NULL,
  utfyllingsdato           TIMESTAMP          NOT NULL,
  innhold                  TEXT               NOT NULL,
  versjon                  SMALLINT           NOT NULL,
  opprettet                TIMESTAMP          NOT NULL,
  sist_endret              TIMESTAMP          NOT NULL
);
