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
import org.apache.kafka.common.errors.WakeupException
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertFailsWith

class SendtSykmeldingAivenConsumerTest :
    FunSpec({
        test("avsluttet ready-løkke lukker consumer uten wakeup") {
            val kafkaConsumer = mockk<Consumer<String, String>>(relaxed = true)
            val consumer = SendtSykmeldingAivenConsumer(kafkaConsumer, mockk(relaxed = true))

            consumer.listen(ApplicationState(ready = false))

            verify(exactly = 0) { kafkaConsumer.poll(any<Duration>()) }
            verify(exactly = 0) { kafkaConsumer.wakeup() }
            verify(exactly = 1) { kafkaConsumer.close(any<CloseOptions>()) }
        }

        listOf("poll", "commit").forEach { blockedOperation ->
            test("cancellation vekker blokkert $blockedOperation og lukker consumer") {
                val operationStarted = CountDownLatch(1)
                val releaseOperation = CountDownLatch(1)
                val wakeupCalled = CountDownLatch(1)
                val kafkaConsumer = mockk<Consumer<String, String>>(relaxed = true)
                val sykmeldingService = mockk<SendtSykmeldingService>(relaxed = true)
                val records =
                    ConsumerRecords(
                        mapOf(
                            TopicPartition(SENDT_SYKMELDING_TOPIC, 0) to
                                listOf(ConsumerRecord<String, String>(SENDT_SYKMELDING_TOPIC, 0, 0, "id", null)),
                        ),
                    )

                fun blockUntilWakeup(): Nothing {
                    operationStarted.countDown()
                    check(releaseOperation.await(5, TimeUnit.SECONDS))
                    throw WakeupException()
                }
                every { kafkaConsumer.wakeup() } answers {
                    wakeupCalled.countDown()
                    releaseOperation.countDown()
                }
                every { kafkaConsumer.poll(any<Duration>()) } answers {
                    if (blockedOperation == "poll") blockUntilWakeup() else records
                }
                every { kafkaConsumer.commitSync() } answers { blockUntilWakeup() }
                val consumer = SendtSykmeldingAivenConsumer(kafkaConsumer, sykmeldingService)

                runBlocking {
                    val listenerJob = launch(Dispatchers.Default) { consumer.listen(ApplicationState(ready = true)) }
                    try {
                        operationStarted.await(1, TimeUnit.SECONDS) shouldBe true
                        listenerJob.cancel()
                        wakeupCalled.await(1, TimeUnit.SECONDS) shouldBe true
                        withTimeout(1_000) { listenerJob.join() }
                    } finally {
                        listenerJob.cancel()
                        releaseOperation.countDown()
                        listenerJob.join()
                    }
                }

                verify(exactly = 1) { kafkaConsumer.wakeup() }
                verify(exactly = 1) { kafkaConsumer.close(any<CloseOptions>()) }
                verify(exactly = if (blockedOperation == "poll") 0 else 1) {
                    sykmeldingService.deleteSykmeldingsperioder(any())
                }
                verify(exactly = if (blockedOperation == "poll") 0 else 1) { kafkaConsumer.commitSync() }
            }
        }

        test("uventet wakeup uten cancellation propageres og lukker consumer") {
            val kafkaConsumer = mockk<Consumer<String, String>>(relaxed = true)
            every { kafkaConsumer.poll(any<Duration>()) } throws WakeupException()
            val consumer = SendtSykmeldingAivenConsumer(kafkaConsumer, mockk(relaxed = true))

            assertFailsWith<WakeupException> { consumer.listen(ApplicationState(ready = true)) }

            verify(exactly = 1) { kafkaConsumer.close(any<CloseOptions>()) }
        }

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
