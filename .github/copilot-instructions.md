# lps-oppfolgingsplan-mottak

- `./gradlew build` runs build, tests and lint; `./gradlew test` runs tests.
- Local startup: `mise docker-up`, then `mise start`; this starts the local
  database, auth server, Texas and Kafka dependencies.
- Maskinporten's `consumer` organisation is the employer; `supplier` is the
  delegated LPS organisation. Without delegation, the employer is also the
  LPS organisation. Do not replace these claims with request-body values.
- Preserve validation of the employment relationship and active sykmelding
  sent to that employer. The `TEST_FNR_LIST` bypass is limited to dev.
- `needsHelpFromNav` requires `sendPlanToNav` and a description; absence of
  employee participation requires an explanation. Keep these linked rules
  in `FollowUpPlanValidator` when changing the submission contract.
- The plan is stored before delivery. Nav and GP sending statuses are tracked
  separately; do not equate receipt with successful delivery to both.
- Follow-up plans and generated PDFs contain health information; do not put
  their content or employee identifiers in ordinary logs.
