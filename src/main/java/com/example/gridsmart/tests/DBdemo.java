package com.example.gridsmart.tests;

import com.example.gridsmart.DB.*;
import com.example.gridsmart.model.*;

import java.sql.SQLException;
import java.util.List;

public class DBdemo {
    public static void main(String[] args) {
        try {
            // Retrieve all energy sources
            EnergySourceDB sourceDB = new EnergySourceDB();
            List<EnergySource> sources = sourceDB.selectAll();

            System.out.println("===== Energy Sources =====");
            for (EnergySource source : sources) {
                System.out.println(source);
            }

            // Retrieve all energy consumers
            EnergyConsumerDB consumerDB = new EnergyConsumerDB();
            List<EnergyConsumer> consumers = consumerDB.selectAll();

            System.out.println("\n===== Energy Consumers =====");
            for (EnergyConsumer consumer : consumers) {
                System.out.println(consumer);
            }

            // Close database connections
            sourceDB.close();
            consumerDB.close();

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
