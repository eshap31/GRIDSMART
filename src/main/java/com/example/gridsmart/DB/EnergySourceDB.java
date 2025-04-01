package com.example.gridsmart.DB;

import com.example.gridsmart.model.EnergySource;
import com.example.gridsmart.model.SourceType;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class EnergySourceDB {
    private Connection connection;
    private Statement stmt;

    public EnergySourceDB() throws SQLException {
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

    // returns a list of all the EnergySources in the database
    public List<EnergySource> selectAll() {
        ResultSet res = null;
        List<EnergySource> sources = new ArrayList<>();

        try {
            String sqlStr = "SELECT * FROM energy_sources";
            res = stmt.executeQuery(sqlStr);

            while (res.next()) {
                String id = res.getString("id");
                double capacity = res.getDouble("capacity");
                String sourceTypeStr = res.getString("source_type");

                // Convert string to enum
                SourceType sourceType = SourceType.valueOf(sourceTypeStr);

                // Create new energy source object
                EnergySource source = new EnergySource(id, capacity, sourceType);
                sources.add(source);
            }
        } catch (SQLException e) {
            System.out.println("Error selecting energy sources: " + e.getMessage());
        } finally {
            try {
                if (res != null && !res.isClosed()) res.close();
            } catch (SQLException e) {
                System.out.println("Error closing result set: " + e.getMessage());
            }
        }

        return sources;
    }

    // close connection
    public void close() {
        try {
            if (stmt != null && !stmt.isClosed()) stmt.close();
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            System.out.println("Error closing database resources: " + e.getMessage());
        }
    }
}