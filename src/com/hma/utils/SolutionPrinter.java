package com.hma.utils;

import com.hma.config.HMAConfig;
import com.hma.cost.CostCalculator;
import com.hma.model.HMASolution;
import java.util.ArrayList;
import java.util.List;

public class SolutionPrinter {
    public static void printSolution(HMASolution sol, HMAConfig cfg) {
        CostCalculator calc = new CostCalculator(cfg);
        calc.calcTotalCost(sol);
        
        int mobilizedCount = countMobilized(sol);
        int totalTrips = 0;
        int tempViolations = 0;
        
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("  PHƯƠNG ÁN VẬN CHUYỂN HMA TỐI ƯU — DA_GWO");
        System.out.printf("  TC = %,.0f VNĐ\n", sol.TC);
        System.out.printf("  ├── C_fixed       = %,.0f VNĐ  (%d xe × %,.0f)\n", sol.Cfixed, mobilizedCount, cfg.f);
        System.out.printf("  ├── C_operational = %,.0f VNĐ\n", sol.Coperational);
        System.out.printf("  └── C_penalty     = %,.0f VNĐ          (%s)\n", 
                          sol.Cpenalty, sol.Cpenalty == 0 ? "không chuyến nào T<120°C ✓" : "vi phạm nhiệt ✗");
        System.out.println("═══════════════════════════════════════════════════════════════\n");
        
        List<Integer> unmobilizedVehicles = new ArrayList<>();
        
        for (int k = 0; k < cfg.T; k++) {
            if (sol.zk[k] == 0) {
                unmobilizedVehicles.add(k + 1);
                continue;
            }
            
            // Count active trips for vehicle k
            int activeTripCount = 0;
            for (int m = 0; m < cfg.Mk; m++) {
                for (int i = 0; i < cfg.N; i++) {
                    if (sol.xikm[i][k][m] == 1) {
                        activeTripCount++;
                    }
                }
            }
            totalTrips += activeTripCount;
            
            System.out.printf("XE %d (zk=1) — %d chuyến\n", k + 1, activeTripCount);
            System.out.println("┌────────┬────────────┬────────────┬──────────┬──────────────┐");
            System.out.println("│Chuyến m│Công trường │Xuất phát   │Nhiệt độ  │Chi phí       │");
            System.out.println("├────────┼────────────┼────────────┼──────────┼──────────────┤");
            
            int tripIndex = 0;
            for (int m = 0; m < cfg.Mk; m++) {
                int site = -1;
                for (int i = 0; i < cfg.N; i++) {
                    if (sol.xikm[i][k][m] == 1) {
                        site = i;
                        break;
                    }
                }
                
                if (site != -1) {
                    tripIndex++;
                    double temp = calc.calcTemperature(site);
                    if (temp < 120.0) tempViolations++;
                    
                    double tripCost = 2.0 * cfg.doi[site] * cfg.coi[site];
                    
                    // Format departure time starting from 06:30
                    int startMinutes = 6 * 60 + 30 + (int) Math.round(sol.txp_km[k][m]);
                    int hh = (startMinutes / 60) % 24;
                    int mm = startMinutes % 60;
                    int duration = (int) Math.round((cfg.doi[site] / cfg.v) * 60.0);
                    
                    String departStr = String.format("%02d:%02d (%d')", hh, mm, duration);
                    String siteStr = String.format("Site %d (%.0fkm)", site + 1, cfg.doi[site]);
                    String tempStr = String.format("%.1f°C %s", temp, temp >= 120.0 ? "✓" : "✗");
                    String costStr = String.format("%,.0f VNĐ", tripCost);
                    
                    System.out.printf("│  %-6d│%-12s│%-12s│%-10s│%-14s│\n", 
                                      tripIndex, siteStr, departStr, tempStr, costStr);
                }
            }
            System.out.println("└────────┴────────────┴────────────┴──────────┴──────────────┘\n");
        }
        
        if (!unmobilizedVehicles.isEmpty()) {
            StringBuilder sb = new StringBuilder("Xe ");
            for (int idx = 0; idx < unmobilizedVehicles.size(); idx++) {
                sb.append(unmobilizedVehicles.get(idx));
                if (idx < unmobilizedVehicles.size() - 1) sb.append(", ");
            }
            sb.append(": KHÔNG HUY ĐỘNG (zk=0)\n");
            System.out.println(sb.toString());
        }
        
        // Demand check
        boolean demandFulfilled = true;
        for (int i = 0; i < cfg.N; i++) {
            int required = (int) Math.ceil(cfg.Di[i] / cfg.Q);
            int actual = 0;
            for (int k = 0; k < cfg.T; k++) {
                for (int m = 0; m < cfg.Mk; m++) {
                    actual += sol.xikm[i][k][m];
                }
            }
            if (actual < required) demandFulfilled = false;
        }
        
        double mobilizedPct = (double) mobilizedCount / cfg.T * 100.0;
        
        System.out.println("═══ TỔNG HỢP ═══");
        System.out.printf("Xe huy động: %d/%d (%.0f%%)\n", mobilizedCount, cfg.T, mobilizedPct);
        System.out.printf("Tổng chuyến: %d\n", totalTrips);
        System.out.printf("Vi phạm nhiệt: %d/%d %s\n", tempViolations, totalTrips, tempViolations == 0 ? "✓" : "✗");
        System.out.printf("Nhu cầu đáp ứng: %s\n", demandFulfilled ? "100% ✓" : "CHƯA ĐÁP ỨNG ✗");
        System.out.println("═══════════════════════════════════════════════════════════════");
    }
    
    private static int countMobilized(HMASolution sol) {
        int count = 0;
        for (int z : sol.zk) {
            if (z == 1) count++;
        }
        return count;
    }
}

