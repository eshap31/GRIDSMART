package com.example.gridsmart.MasterController;

public class GridSmartMain {
    public static void main(String[] args) {
        System.out.println("Starting GridSmart system...");

        // create the MasterController
        MasterController controller = new MasterController();

        controller.start();

        controller.printAllocationStatus();

        // run the system
        try
        {
            System.out.println("\nSystem running... Press Ctrl+C to terminate");

            for (int i = 0; i < 6; i++) {
                Thread.sleep(10000); // 10 seconds
                System.out.println("\n----- Status update after " + ((i+1)*10) + " seconds -----");
                controller.printAllocationStatus();
            }

            // Stop the event simulation gracefully
            controller.stopEventSimulation();

            // Print final statistics
            System.out.println("\n----- Final Statistics -----");
            controller.printStatistics();

        } catch (InterruptedException e) {
            System.out.println("Execution interrupted");
        } finally {
            // Make sure to stop the event simulation when done
            controller.stopEventSimulation();
        }

        System.out.println("GridSmart Energy Allocation System terminated");
    }
}
