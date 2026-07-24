package com.hma.utils;

import com.hma.config.HMAConfig;
import com.hma.cost.CostCalculator;
import com.hma.model.HMASolution;

public class SolutionPrinter {
    public static void printSolution(HMASolution sol, HMAConfig cfg) {
        CostCalculator calc = new CostCalculator(cfg);
        calc.calcTotalCost(sol);
        
        System.out.println("===============================================================");
        System.out.println("             HMA TRANSPORTATION OPTIMIZATION PLAN              ");
        System.out.println("===============================================================");
        System.out.printf("Total Cost (TC):        %,.0f VND\n", sol.TC);
        System.out.printf("  - Fixed Cost:         %,.0f VND (Mobilized vehicles: %d)\n", sol.Cfixed, countMobilized(sol));
        System.out.printf("  - Operational Cost:   %,.0f VND\n", sol.Coperational);
        System.out.printf("  - Temperature Penalty:%,.0f VND\n", sol.Cpenalty);
        System.out.println("---------------------------------------------------------------");
        
        for (int k = 0; k < cfg.T; k++) {
            if (sol.zk[k] == 0) {
                System.out.printf("Vehicle %d: NOT MOBILIZED\n", k + 1);
                continue;
            }
            
            System.out.printf("Vehicle %d (Mobilized):\n", k + 1);
            System.out.printf("  %-10s | %-12s | %-12s | %-12s | %-15s\n", 
                              "Trip #", "Site", "Depart (Min)", "Arrival T", "Trip Cost");
            System.out.println("  -------------------------------------------------------------");
            
            int activeTripCount = 0;
            for (int m = 0; m < cfg.Mk; m++) {
                int site = -1;
                for (int i = 0; i < cfg.N; i++) {
                    if (sol.xikm[i][k][m] == 1) {
                        site = i;
                        break;
                    }
                }
                
                if (site != -1) {
                    activeTripCount++;
                    double temp = calc.calcTemperature(site);
                    double tripCost = 2.0 * cfg.doi[site] * cfg.coi[site];
                    System.out.printf("  %-10d | Site %-9d | %-12.1f | %-10.2f C | %,.0f VND\n", 
                                      activeTripCount, site + 1, sol.txp_km[k][m], temp, tripCost);
                }
            }
            if (activeTripCount == 0) {
                System.out.println("  No active trips.");
            }
            System.out.println();
        }
        
        System.out.println("Demand Fulfillment Summary:");
        for (int i = 0; i < cfg.N; i++) {
            int required = (int) Math.ceil(cfg.Di[i] / cfg.Q);
            int actual = 0;
            for (int k = 0; k < cfg.T; k++) {
                for (int m = 0; m < cfg.Mk; m++) {
                    actual += sol.xikm[i][k][m];
                }
            }
            System.out.printf("  Site %d: Demanded %-5.1f tons (%d trips) | Delivered %-5.1f tons (%d trips) - %s\n", 
                              i + 1, cfg.Di[i], required, actual * cfg.Q, actual, 
                              (actual == required) ? "FULFILLED" : "VIOLATED");
        }
        System.out.println("===============================================================");
    }
    
    private static int countMobilized(HMASolution sol) {
        int count = 0;
        for (int z : sol.zk) {
            if (z == 1) count++;
        }
        return count;
    }
}
