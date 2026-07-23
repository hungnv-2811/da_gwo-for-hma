package com.test;

import com.hma.config.HMAConfig;
import com.hma.config.SampleData;
import com.hma.fitness.HMAFitness;
import com.hma.model.HMASolution;
import com.hma.utils.SolutionPrinter;
import com.hma.constraint.RepairOperator;

public class HMAOptimizer {
    public static void main(String[] args) throws Exception {
        HMAConfig cfg = SampleData.getSampleConfig();
        System.out.println("===============================================================");
        System.out.println("       HMA TRANSPORTATION OPTIMIZATION WITH DA-GWO HYBRID      ");
        System.out.println("===============================================================");
        System.out.println("Problem Scale: Sites (N) = " + cfg.N + ", Vehicles (T) = " + cfg.T + ", Max Trips (Mk) = " + cfg.Mk);
        System.out.println("Dimension of Search Space (dim) = " + cfg.dim);
        
        // Define bounds [0.0, 1.0] for continuous mapping of discrete decisions
        double[] lb = new double[cfg.dim];
        double[] ub = new double[cfg.dim];
        for (int i = 0; i < cfg.dim; i++) {
            lb[i] = 0.0;
            ub[i] = 1.0;
        }
        
        // Initialize objective function
        f_xj fobj = new HMAFitness(cfg);
        
        // Algorithm settings
        int maxIter = 300;
        int popSize = 40;
        
        System.out.println("Running DA-GWO Hybrid Algorithm (Pop: " + popSize + ", Iterations: " + maxIter + ")...");
        
        long startTime = System.currentTimeMillis();
        DA_GWO hybrid = new DA_GWO(fobj, lb, ub, maxIter, popSize);
        hybrid.solution();
        long endTime = System.currentTimeMillis();
        
        double bestTC = hybrid.getRes();
        double[] bestPos = hybrid.Best_pos;
        
        System.out.println("\nOptimization completed in " + ((endTime - startTime) / 1000.0) + " seconds.");
        System.out.println("Best raw fitness score (TC + penalties): " + bestTC);
        
        System.out.println("\nConvergence Milestones:");
        System.out.printf("  - Iteration 1  : %,.0f VND\n", hybrid.convergenceHistory[1]);
        System.out.printf("  - Iteration 50 : %,.0f VND\n", hybrid.convergenceHistory[50]);
        System.out.printf("  - Iteration 100: %,.0f VND\n", hybrid.convergenceHistory[100]);
        System.out.printf("  - Iteration 150: %,.0f VND\n", hybrid.convergenceHistory[150]);
        System.out.printf("  - Iteration 200: %,.0f VND\n", hybrid.convergenceHistory[200]);
        System.out.printf("  - Iteration 250: %,.0f VND\n", hybrid.convergenceHistory[250]);
        System.out.printf("  - Iteration 300: %,.0f VND\n", hybrid.convergenceHistory[300]);
        
        // Decode the best position vector
        HMASolution sol = HMASolution.decode(bestPos, cfg);
        RepairOperator.repairAll(sol, cfg); // Guarantee compliance with constraints (6), (7), (8), (11)
        
        // Print the optimized schedule
        SolutionPrinter.printSolution(sol, cfg);
        
        // Export to Excel file
        com.hma.utils.ExcelExporter.exportSolutionToExcel(sol, cfg, hybrid.convergenceHistory);
    }
}
