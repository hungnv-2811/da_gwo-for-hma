package com.hma.constraint;

import com.hma.config.HMAConfig;
import com.hma.model.HMASolution;
import java.util.*;

public class RepairOperator {
    
    public static void repairAll(HMASolution sol, HMAConfig cfg) {
        // 1. Ensure trip limit (7) is satisfied (each trip goes to at most 1 site)
        repairTripLimit(sol, cfg);
        
        // 2. Adjust demands (6) - add/remove trips to match exactly Ceil(Di/Q)
        repairDemand(sol, cfg);
        
        // 3. Ensure sequence times (8) and non-negativity (11)
        repairSequenceAndDepartureTimes(sol, cfg);
    }
    
    // Eq. (7): sum_i xikm <= 1. 
    // Ensure that for each vehicle k and trip m, at most one site has xikm = 1
    public static void repairTripLimit(HMASolution sol, HMAConfig cfg) {
        for (int k = 0; k < cfg.T; k++) {
            for (int m = 0; m < cfg.Mk; m++) {
                int activeCount = 0;
                int bestSite = -1;
                for (int i = 0; i < cfg.N; i++) {
                    if (sol.xikm[i][k][m] == 1) {
                        activeCount++;
                        bestSite = i;
                    }
                }
                if (activeCount > 1) {
                    // Reset all and keep only the first one (or we could select randomly)
                    for (int i = 0; i < cfg.N; i++) {
                        if (i != bestSite) {
                            sol.xikm[i][k][m] = 0;
                        }
                    }
                }
            }
        }
    }
    
    // Eq. (6): sum_k sum_m xikm = Ceil(Di / Q)
    public static void repairDemand(HMASolution sol, HMAConfig cfg) {
        for (int i = 0; i < cfg.N; i++) {
            int requiredTrips = (int) Math.ceil(cfg.Di[i] / cfg.Q);
            
            // Count current actual trips going to site i
            int actualTrips = 0;
            for (int k = 0; k < cfg.T; k++) {
                for (int m = 0; m < cfg.Mk; m++) {
                    if (sol.xikm[i][k][m] == 1) {
                        actualTrips++;
                    }
                }
            }
            
            // Case 1: Too many trips -> reduce
            if (actualTrips > requiredTrips) {
                int surplus = actualTrips - requiredTrips;
                outerLoop:
                for (int k = cfg.T - 1; k >= 0; k--) {
                    for (int m = cfg.Mk - 1; m >= 0; m--) {
                        if (sol.xikm[i][k][m] == 1) {
                            sol.xikm[i][k][m] = 0;
                            surplus--;
                            if (surplus == 0) break outerLoop;
                        }
                    }
                }
            }
            // Case 2: Too few trips -> add
            else if (actualTrips < requiredTrips) {
                int deficit = requiredTrips - actualTrips;
                
                // First try to assign to active vehicles with empty slots
                for (int k = 0; k < cfg.T; k++) {
                    if (sol.zk[k] == 1) {
                        for (int m = 0; m < cfg.Mk; m++) {
                            // Check if trip m of vehicle k is completely idle
                            boolean isIdle = true;
                            for (int j = 0; j < cfg.N; j++) {
                                if (sol.xikm[j][k][m] == 1) {
                                    isIdle = false;
                                    break;
                                }
                            }
                            if (isIdle) {
                                sol.xikm[i][k][m] = 1;
                                deficit--;
                                if (deficit == 0) return;
                            }
                        }
                    }
                }
                
                // If still deficit, mobilize inactive vehicles and assign trips
                for (int k = 0; k < cfg.T; k++) {
                    if (sol.zk[k] == 0) {
                        sol.zk[k] = 1; // Mobilize
                        for (int m = 0; m < cfg.Mk; m++) {
                            sol.xikm[i][k][m] = 1;
                            deficit--;
                            if (deficit == 0) return;
                        }
                    }
                }
            }
        }
        
        // Disable vehicles that have no trips at all
        for (int k = 0; k < cfg.T; k++) {
            boolean hasTrip = false;
            for (int i = 0; i < cfg.N; i++) {
                for (int m = 0; m < cfg.Mk; m++) {
                    if (sol.xikm[i][k][m] == 1) {
                        hasTrip = true;
                        break;
                    }
                }
                if (hasTrip) break;
            }
            if (!hasTrip) {
                sol.zk[k] = 0;
            }
        }
    }
    
    // Eq. (8): txp_{km+1} >= txp_{km} + sum_i (2 * doi / v * 60 + dtdo) * xikm
    // Also ensures txp_km >= 0
    public static void repairSequenceAndDepartureTimes(HMASolution sol, HMAConfig cfg) {
        for (int k = 0; k < cfg.T; k++) {
            if (sol.zk[k] == 0) continue;
            
            // 1. Sort active trips chronologically for vehicle k based on their initial departure times
            // Create a list of active trip indices
            List<Integer> activeTripIndices = new ArrayList<>();
            for (int m = 0; m < cfg.Mk; m++) {
                boolean isActive = false;
                for (int i = 0; i < cfg.N; i++) {
                    if (sol.xikm[i][k][m] == 1) {
                        isActive = true;
                        break;
                    }
                }
                if (isActive) {
                    activeTripIndices.add(m);
                }
            }
            
            // Sort indices based on txp_km value
            final double[] times = sol.txp_km[k];
            activeTripIndices.sort(new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    return Double.compare(times[o1], times[o2]);
                }
            });
            
            // 2. Adjust times and enforce sequence constraints
            double currentTime = 0.0;
            for (int idx = 0; idx < activeTripIndices.size(); idx++) {
                int m = activeTripIndices.get(idx);
                
                // Enforce txp_km >= 0
                if (sol.txp_km[k][m] < currentTime) {
                    sol.txp_km[k][m] = currentTime;
                }
                
                // Calculate return time for this trip
                int site = -1;
                for (int i = 0; i < cfg.N; i++) {
                    if (sol.xikm[i][k][m] == 1) {
                        site = i;
                        break;
                    }
                }
                
                if (site != -1) {
                    // Trip time = 2 * doi / v (hours) * 60 (min) + dtdo (min)
                    double tripDuration = (2.0 * cfg.doi[site] / cfg.v) * 60.0 + cfg.dtdo;
                    currentTime = sol.txp_km[k][m] + tripDuration;
                    
                    // If return time exceeds the shift time, we must deactivate this and subsequent trips
                    if (currentTime > cfg.T_ca) {
                        for (int remIdx = idx; remIdx < activeTripIndices.size(); remIdx++) {
                            int remM = activeTripIndices.get(remIdx);
                            for (int i = 0; i < cfg.N; i++) {
                                sol.xikm[i][k][remM] = 0;
                            }
                        }
                        break;
                    }
                }
            }
        }
    }
}
