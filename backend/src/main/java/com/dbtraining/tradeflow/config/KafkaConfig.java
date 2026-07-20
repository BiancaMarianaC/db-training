package com.dbtraining.tradeflow.config;

import org.springframework.context.annotation.Configuration;

/**
 * ============================================================================
 * KafkaConfig — TICKET-I118 (Day 9)
 * ============================================================================
 * WHAT:    Custom Kafka beans — DLT recoverer, error handler, consumer factory
 *          overrides.
 * HOW:     @Configuration class. Spring Boot auto-config covers most of it;
 *          this is where you override.
 * WHY:     Robust event pipelines need: retries, DLT for poison messages,
 *          observable error handling.
 * OBSERVE: A malformed message no longer crashes the consumer — it lands in
 *          `trade-events.DLT`.
 * ============================================================================
 *
 *  TODO(TICKET-I118):
 *    @Bean
 *    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String,Object> tpl) {
 *        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(tpl,
 *            (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition()));
 *        return new DefaultErrorHandler(recoverer, new FixedBackOff(1_000L, 3));
 *    }
 *
 *  HINT: For at-least-once semantics, ack-mode should stay MANUAL_IMMEDIATE
 *        only if your consumer acks manually after successful processing.
 * ============================================================================
 */
@Configuration
public class KafkaConfig {

    // TODO(TICKET-I118): see comments above. Uncomment after enabling Kafka in pom.
}
