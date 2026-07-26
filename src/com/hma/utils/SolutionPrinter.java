package com.hma.utils;

import com.hma.config.HMAConfig;
import com.hma.cost.CostCalculator;
import com.hma.model.HMASolution;

public class SolutionPrinter {
    public static void printSolution(HMASolution sol, HMAConfig cfg) {
        CostCalculator calc = new CostCalculator(cfg);
        calc.calcTotalCost(sol);
        
        System.out.println("===============================================================");
        System.out.println("             PHƯƠNG ÁN VẬN CHUYỂN HMA TỐI ƯU                   ");
        System.out.println("===============================================================");
        System.out.printf("Tổng chi phí (TC):      %,.0f VNĐ\n", sol.TC);
        System.out.printf("  - Chi phí cố định:    %,.0f VNĐ (Số xe huy động: %d)\n", sol.Cfixed, countMobilized(sol));
        System.out.printf("  - Chi phí vận hành:   %,.0f VNĐ\n", sol.Coperational);
        System.out.printf("  - Phạt nhiệt độ:      %,.0f VNĐ\n", sol.Cpenalty);
        System.out.println("---------------------------------------------------------------");
        
        for (int k = 0; k < cfg.T; k++) {
            if (sol.zk[k] == 0) {
                System.out.printf("Xe %d: KHÔNG HUY ĐỘNG\n", k + 1);
                continue;
            }
            
            System.out.printf("Xe %d (Được huy động):\n", k + 1);
            System.out.printf("  %-10s | %-12s | %-12s | %-12s | %-15s\n", 
                              "Chuyến #", "Công trường", "Xuất phát(Phút)", "Nhiệt độ đến", "Chi phí chuyến");
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
                    System.out.printf("  %-10d | Công trường %-2d | %-12.1f | %-10.2f °C | %,.0f VNĐ\n", 
                                      activeTripCount, site + 1, sol.txp_km[k][m], temp, tripCost);
                }
            }
            if (activeTripCount == 0) {
                System.out.println("  Không có chuyến xe nào hoạt động.");
            }
            System.out.println();
        }
        
        System.out.println("Tóm tắt đáp ứng nhu cầu khối lượng:");
        for (int i = 0; i < cfg.N; i++) {
            int required = (int) Math.ceil(cfg.Di[i] / cfg.Q);
            int actual = 0;
            for (int k = 0; k < cfg.T; k++) {
                for (int m = 0; m < cfg.Mk; m++) {
                    actual += sol.xikm[i][k][m];
                }
            }
            System.out.printf("  Công trường %d: Nhu cầu %-5.1f tấn (%d chuyến) | Đã giao %-5.1f tấn (%d chuyến) - %s\n", 
                              i + 1, cfg.Di[i], required, actual * cfg.Q, actual, 
                              (actual == required) ? "ĐẠT (FULFILLED)" : "VI PHẠM (VIOLATED)");
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
