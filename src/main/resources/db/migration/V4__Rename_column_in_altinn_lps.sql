ALTER TABLE ALTINN_LPS
  RENAME COLUMN should_send_to_gp TO should_send_to_fastlege;

ALTER TABLE ALTINN_LPS
  RENAME COLUMN sent_to_gp TO sent_to_fastlege;

ALTER TABLE ALTINN_LPS
  RENAME COLUMN send_to_gp_retry_count TO send_to_fastlege_retry_count;
