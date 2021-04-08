package br.com.alura.ecommerce.database;

import java.sql.*;

public class LocalDatabase {

    private final Connection connection;

    public LocalDatabase(String name) throws SQLException {
        this.connection = DriverManager.getConnection(String.format("jdbc:sqlite:target/%s.db", name));
    }

    public void close() throws SQLException {
        connection.close();
    }

    // generic method, avoid injection
    public void createIfNotExists(String sql) {
        try {
            connection.createStatement().execute(sql);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public ResultSet query(String query, String... params) throws SQLException {
        return prepare(query, params).executeQuery();
    }

    public boolean update(String statement, String... params) throws SQLException {
        return prepare(statement, params).execute();
    }

    private PreparedStatement prepare(String query, String[] params) throws SQLException {
        var preparedStatement = connection.prepareStatement(query);

        for (int i = 0; i < params.length; i++) {
            preparedStatement.setString(i + 1, params[i]);
        }

        return preparedStatement;
    }
}
