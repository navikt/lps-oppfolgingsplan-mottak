package no.nav.syfo.sykmelding

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.sykmelding.service.SendtSykmeldingService
import org.apache.kafka.clients.consumer.CloseOptions
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class SendtSykmeldingAivenConsumerTest :
    FunSpec({
        test("cancellation etter poll hopper over records og lukker consumer selv om applikasjonen er ready") {
            val pollStarted = CountDownLatch(1)
            val releasePoll = CountDownLatch(1)
            val records =
                ConsumerRecords(
                    mapOf(
                        TopicPartition(SENDT_SYKMELDING_TOPIC, 0) to
                            listOf(
                                ConsumerRecord<String, String>(
                                    SENDT_SYKMELDING_TOPIC,
                                    0,
                                    0,
                                    "sykmelding-id",
                                    null,
                                ),
                            ),
                    ),
                )
            val kafkaConsumer = mockk<Consumer<String, String>>(relaxed = true)
            every { kafkaConsumer.poll(any<Duration>()) } answers {
                pollStarted.countDown()
                releasePoll.await(1, TimeUnit.SECONDS)
                records
            }
            val sykmeldingService = mockk<SendtSykmeldingService>(relaxed = true)
            val consumer =
                SendtSykmeldingAivenConsumer(
                    kafkaListener = kafkaConsumer,
                    sykmeldingService = sykmeldingService,
                )
            val applicationState = ApplicationState(ready = true)

            runBlocking {
                val listenerJob =
                    launch(Dispatchers.Default) {
                        consumer.listen(applicationState)
                    }
                pollStarted.await(1, TimeUnit.SECONDS) shouldBe true
                listenerJob.cancel()
                releasePoll.countDown()

                withTimeout(1_000) {
                    listenerJob.cancelAndJoin()
                }
            }

            verify(exactly = 0) { sykmeldingService.deleteSykmeldingsperioder(any()) }
            verify(exactly = 0) {
                sykmeldingService.persistSykmeldingsperioder(any(), any(), any(), any())
            }
            verify(exactly = 0) { kafkaConsumer.commitSync() }
            verify(exactly = 1) {
                kafkaConsumer.close(
                    match<CloseOptions> { it.timeout().orElseThrow() == Duration.ofSeconds(1) },
                )
            }
        }
    })
