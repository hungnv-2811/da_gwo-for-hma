package com.test;

import com.hma.config.HMAConfig;
import com.hma.config.SampleData;
import com.hma.cost.CostCalculator;
import com.hma.fitness.HMAFitness;
import com.hma.model.HMASolution;
import com.hma.constraint.RepairOperator;

public class AlgorithmComparison {
    public static void main(String[] args) throws Exception {
        HMAConfig cfg = SampleData.getSampleConfig();
        
        double[] lb = new double[cfg.dim];
        double[] ub = new double[cfg.dim];
        for (int i = 0; i < cfg.dim; i++) {
            lb[i] = 0.0;
            ub[i] = 1.0;
        }
        
        f_xj fobj = new HMAFitness(cfg);
        CostCalculator calc = new CostCalculator(cfg);
        
        int runs = 10;
        int maxIter = 100; // 100 iterations for quick comparative analysis
        int popSize = 30;
        
        double[] daGwoTCs = new double[runs];
        double[] gwoTCs = new double[runs];
        double[] daTCs = new double[runs];
        
        long daGwoTime = 0, gwoTime = 0, daTime = 0;
        
        System.out.println("=================================================");
        System.out.println("   ALGORITHM COMPARISON FOR HMA TRANSPORTATION   ");
        System.out.println("=================================================");
        System.out.println("Running each algorithm " + runs + " times...");
        
        // 1. Run DA-GWO Hybrid
        System.out.print("Running DA-GWO Hybrid...");
        for (int r = 0; r < runs; r++) {
            long start = System.currentTimeMillis();
            DA_GWO solver = new DA_GWO(fobj, lb, ub, maxIter, popSize);
            solver.solution();
            daGwoTime += (System.currentTimeMillis() - start);
            
            HMASolution sol = HMASolution.decode(solver.Best_pos, cfg);
            RepairOperator.repairAll(sol, cfg);
            calc.calcTotalCost(sol);
            daGwoTCs[r] = sol.TC;
            System.out.print(".");
        }
        System.out.println(" Done.");
        
        // 2. Run GWO
        System.out.print("Running GWO...");
        for (int r = 0; r < runs; r++) {
            long start = System.currentTimeMillis();
            GWO solver = new GWO(fobj, lb, ub, maxIter, popSize);
            solver.execute();
            gwoTime += (System.currentTimeMillis() - start);
            
            HMASolution sol = HMASolution.decode(solver.getBestArray(), cfg);
            RepairOperator.repairAll(sol, cfg);
            calc.calcTotalCost(sol);
            gwoTCs[r] = sol.TC;
            System.out.print(".");
        }
        System.out.println(" Done.");
        
        // 3. Run DA
        System.out.print("Running DA...");
        for (int r = 0; r < runs; r++) {
            long start = System.currentTimeMillis();
            DA solver = new DA(fobj, lb, ub, maxIter, popSize);
            solver.solution();
            daTime += (System.currentTimeMillis() - start);
            
            HMASolution sol = HMASolution.decode(solver.Best_pos, cfg);
            RepairOperator.repairAll(sol, cfg);
            calc.calcTotalCost(sol);
            daTCs[r] = sol.TC;
            System.out.print(".");
        }
        System.out.println(" Done.");
        
        // Calculate Statistics
        printStats("DA-GWO Hybrid", daGwoTCs, daGwoTime, runs);
        printStats("GWO (Grey Wolf)", gwoTCs, gwoTime, runs);
        printStats("DA (Dragonfly)", daTCs, daTime, runs);
    }
    
    private static void printStats(String name, double[] values, long totalTimeMs, int runs) {
        double min = Double.MAX_VALUE;
        double sum = 0;
        for (double v : values) {
            if (v < min) min = v;
            sum += v;
        }
        double avg = sum / runs;
        
        double variance = 0;
        for (double v : values) {
            variance += (v - avg) * (v - avg);
        }
        double std = Math.sqrt(variance / runs);
        
        System.out.println("\n-------------------------------------------------");
        System.out.println("Algorithm: " + name);
        System.out.printf("  Best Cost: %,.0f VND\n", min);
        System.out.printf("  Avg Cost:  %,.0f VND\n", avg);
        System.out.printf("  Std Dev:   %,.0f VND\n", std);
        System.out.printf("  Avg Time:  %.2f seconds\n", (totalTimeMs / 1000.0) / runs);
    }
}
