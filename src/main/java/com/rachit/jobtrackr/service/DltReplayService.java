package com.rachit.jobtrackr.service;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;

@Service
public class DltReplayService {

    private static final Logger log = LoggerFactory.getLogger(DltReplayService.class);

    private final ConsumerFactory<String, Object> consumerFactory;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public DltReplayService(ConsumerFactory<String, Object> consumerFactory,
                            KafkaTemplate<String, Object> kafkaTemplate) {
        this.consumerFactory = consumerFactory;
        this.kafkaTemplate = kafkaTemplate;
    }

    public int replayDlt(String dltTopic, String targetTopic) {
        log.info("Starting DLT replay from {} to {}", dltTopic, targetTopic);
        int replayedCount = 0;

        try (Consumer<String, Object> consumer = consumerFactory.createConsumer("dlt-replayer", "dlt-replayer-client")) {
            // Assign explicitly to read from the beginning without messing with consumer groups
            TopicPartition partition = new TopicPartition(dltTopic, 0);
            consumer.assign(Collections.singletonList(partition));
            consumer.seekToBeginning(Collections.singletonList(partition));

            boolean keepPolling = true;
            while (keepPolling) {
                ConsumerRecords<String, Object> records = consumer.poll(Duration.ofMillis(500));
                if (records.isEmpty()) {
                    keepPolling = false;
                } else {
                    for (ConsumerRecord<String, Object> record : records) {
                        log.debug("Replaying record key={} from {}", record.key(), dltTopic);
                        kafkaTemplate.send(targetTopic, record.key(), record.value());
                        replayedCount++;
                    }
                    consumer.commitSync();
                }
            }
        } catch (Exception e) {
            log.error("Error during DLT replay", e);
            throw new RuntimeException("DLT replay failed", e);
        }

        log.info("Completed DLT replay. Replayed {} messages from {} to {}", replayedCount, dltTopic, targetTopic);
        return replayedCount;
    }
}
