package br.com.alura.ecommerce;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.io.Closeable;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Pattern;

class KafkaService<T> implements Closeable {

    private final KafkaConsumer<String, T> consumer;
    private final ConsumerFunction parse;

    private KafkaService(String groupId, ConsumerFunction parse, Class<T> type, Map<String, String> properties) {
        this.parse = parse;
        this.consumer = new KafkaConsumer<>(getProperties(type, groupId, properties));
    }

    KafkaService(String groupId, String topic, ConsumerFunction parse, Class<T> type, Map<String, String> properties) {
        this(groupId, parse, type, properties);

        consumer.subscribe(Collections.singletonList(topic));
    }

    public KafkaService(String groupId,
                        Pattern topic,
                        ConsumerFunction parse,
                        Class<T> type,
                        Map<String, String> properties) {
        this(groupId, parse, type, properties);

        consumer.subscribe(topic);
    }

    @Override
    public void close() {
        consumer.close();
    }

    void run() {
        while (true) {
            recordListening(parse);
        }
    }

    private Properties getProperties(Class<T> type, String groupId, Map<String, String> overrideProperties) {
        var properties = new Properties();

        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, GsonDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.setProperty(ConsumerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString());
        properties.setProperty(GsonDeserializer.TYPE_CONFIG, type.getName());

        properties.putAll(overrideProperties);

        return properties;
    }

    private void recordListening(ConsumerFunction parse) {
        var records = consumer.poll(Duration.ofMillis(100));

        if (!records.isEmpty()) {
            System.out.println("Encontrei " + records.count() + " registros");

            records.forEach(parse::consume);
        }
    }
}
