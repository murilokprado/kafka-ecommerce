package br.com.alura.ecommerce;

import br.com.alura.ecommerce.database.LocalDatabase;

import java.io.Closeable;
import java.io.IOException;
import java.sql.SQLException;

public class OrdersDatabase implements Closeable {

    private final LocalDatabase database;

    public OrdersDatabase() throws SQLException {
        this.database = new LocalDatabase("orders_database");
        this.database.createIfNotExists("create table Orders(uuid varchar(200) primary key)");
    }

    @Override
    public void close() throws IOException {
        try {
            database.close();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    public boolean saveNew(Order order) throws SQLException {
        if (wasProcessed(order)) {
            return false;
        }

        database.update("insert into Orders (uuid) values (?)", order.getOrderId());

        return true;
    }

    private boolean wasProcessed(Order order) throws SQLException {
        var results = database.query("select uuid from Orders where uuid = ? limit 1", order.getOrderId());

        return results.next();
    }
}
