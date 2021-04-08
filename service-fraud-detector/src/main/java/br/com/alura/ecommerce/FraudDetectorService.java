package br.com.alura.ecommerce;

import br.com.alura.ecommerce.consumer.ConsumerService;
import br.com.alura.ecommerce.consumer.ServiceRunner;
import br.com.alura.ecommerce.database.LocalDatabase;
import br.com.alura.ecommerce.dispatcher.KafkaDispatcher;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

public class FraudDetectorService implements ConsumerService<Order> {

    private final LocalDatabase database;
    private final KafkaDispatcher<Order> orderDispatcher = new KafkaDispatcher<>();

    public FraudDetectorService() throws SQLException {
        this.database = new LocalDatabase("frauds_database");
        this.database.createIfNotExists("create table Orders("
                + "uuid varchar(200) primary key,"
                + "is_fraud boolean)");
    }

    @Override
    public String getConsumerGroup() {
        return FraudDetectorService.class.getSimpleName();
    }

    @Override
    public String getTopic() {
        return "ECOMMERCE_NEW_ORDER";
    }

    public void parse(ConsumerRecord<String, Message<Order>> record)
            throws ExecutionException, InterruptedException, SQLException {
        System.out.println("-----------------------------------------");
        System.out.println("Processing new order, checking for fraud");
        System.out.println(record.key());
        System.out.println(record.value());
        System.out.println(record.partition());
        System.out.println(record.offset());

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        var message = record.value();
        var order = message.getPayload();

        if (wasProcessed(order)) {
            System.out.println("Order " + order.getOrderId() + " was already processed");

            return;
        }

        if (isFraud(order)) {
            database.update("insert into Orders (uuid, is_fraud) values (?, true)", order.getOrderId());

            // pretending that the fraud happens when the amount is >= 4500
            System.out.println("Order is a fraud!!!" + order);
            orderDispatcher.send(
                    "ECOMMERCE_ORDER_REJECTED",
                    order.getEmail(),
                    message.getId().continueWith(FraudDetectorService.class.getSimpleName()),
                    order);
        } else {
            database.update("insert into Orders (uuid, is_fraud) values (?, false)", order.getOrderId());

            orderDispatcher.send(
                    "ECOMMERCE_ORDER_APPROVED",
                    order.getEmail(),
                    message.getId().continueWith(FraudDetectorService.class.getSimpleName()),
                    order);
            System.out.println("Approved: " + order);
        }

        System.out.println("Order processed");
    }

    public static void main(String[] args) {
        new ServiceRunner<>(FraudDetectorService::new).start();
    }

    private boolean isFraud(Order order) {
        return order.getAmount().compareTo(BigDecimal.valueOf(4500)) >= 0;
    }

    private boolean wasProcessed(Order order) throws SQLException {
        var results = database.query("select uuid from Orders where uuid = ? limit 1", order.getOrderId());

        return results.next();
    }
}
