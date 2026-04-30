package no.nav.syfo.oppfolgingsplanmottak.logging

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class FollowUpPlanSupportLoggerTest :
    DescribeSpec({
        describe("createFollowUpPlanSupportLogData") {
            it("redacts textual payload values and keeps booleans") {
                val logData =
                    createFollowUpPlanSupportLogData(
                        eventType = "follow_up_plan_received",
                        rawPayload =
                            """
                            {
                              "employeeIdentificationNumber": "12345678901",
                              "sendPlanToNav": true,
                              "needsHelpFromNav": false,
                              "lpsEmail": "lps@lps.no",
                              "needsHelpFromNavDescription": null
                            }
                            """.trimIndent(),
                        callId = "call-id",
                        planUuid = "plan-uuid",
                        consumerClientId = "client-id",
                        organizationNumber = "123456789",
                        lpsOrgnumber = "987654321",
                        parseResult = "received",
                        validationResult = "not_validated",
                    )

                logData.sanitizedPayload shouldContain """"employeeIdentificationNumber":"[REDACTED]""""
                logData.sanitizedPayload shouldContain """"sendPlanToNav":true"""
                logData.sanitizedPayload shouldContain """"needsHelpFromNav":false"""
                logData.sanitizedPayload shouldContain """"needsHelpFromNavDescription":null"""
                logData.sanitizedPayload shouldNotContain "12345678901"
                logData.sanitizedPayload shouldNotContain "lps@lps.no"
            }

            it("returns null sanitized payload for malformed json") {
                val malformedPayload = """{"lpsName":"""
                val logData =
                    createFollowUpPlanSupportLogData(
                        eventType = "follow_up_plan_parse_failed",
                        rawPayload = malformedPayload,
                        callId = "call-id",
                        planUuid = "plan-uuid",
                        consumerClientId = "client-id",
                        organizationNumber = "123456789",
                        lpsOrgnumber = "987654321",
                        parseResult = "failed",
                        validationResult = "not_validated",
                    )

                logData.payloadSizeBytes shouldBe malformedPayload.toByteArray().size
                logData.sanitizedPayload.shouldBeNull()
            }
        }
    })
