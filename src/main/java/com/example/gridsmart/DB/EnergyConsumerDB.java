package com.example.gridsmart.DB;

import com.example.gridsmart.model.EnergyConsumer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class EnergyConsumerDB {
    private Connection connection;
    private Statement stmt;

    public EnergyConsumerDB() throws SQLException {
        try {
            // Create a connection to the database
            this.connection = DatabaseManager.getConnection();
            // Create a statement
            this.stmt = this.connection.createStatement();
        } catch (SQLException e) {
            System.out.println("Error creating database connection: " + e.getMessage());
            throw e;
        }
    }

    // returns a list of all the EnergyConsumers in the database
    public List<EnergyConsumer> selectAll() {
        ResultSet res = null;
        List<EnergyConsumer> consumers = new ArrayList<>();

        try {
            String sqlStr = "SELECT * FROM energy_consumers";
            res = stmt.executeQuery(sqlStr);

            while (res.next()) {
                String id = res.getString("id");
                int priority = res.getInt("priority");
                double demand = res.getDouble("demand");

                // Create new energy consumer object
                EnergyConsumer consumer = new EnergyConsumer(id, priority, demand);
                consumers.add(consumer);
            }
        } catch (SQLException e) {
            System.out.println("Error selecting energy consumers: " + e.getMessage());
        } finally {
            try {
                if (res != null && !res.isClosed()) res.close();
            } catch (SQLException e) {
                System.out.println("Error closing result set: " + e.getMessage());
            }
        }

        return consumers;
    }

    // close db connection
    public void close() {
        try {
            if (stmt != null && !stmt.isClosed()) stmt.close();
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            System.out.println("Error closing database resources: " + e.getMessage());
        }
    }
}