package com.hma.constraint;

import com.hma.config.HMAConfig;
import com.hma.model.HMASolution;

public class ConstraintChecker {
    private HMAConfig cfg;
    
    public ConstraintChecker(HMAConfig cfg) {
        this.cfg = cfg;
    }
    
    // Eq. (6): sum_k sum_m xikm = Ceil(Di / Q)
    public boolean checkDemand(HMASolution sol) {
        for (int i = 0; i < cfg.N; i++) {
            int required = (int) Math.ceil(cfg.Di[i] / cfg.Q);
            int actual = 0;
            for (int k = 0; k < cfg.T; k++) {
                for (int m = 0; m < cfg.Mk; m++) {
                    actual += sol.xikm[i][k][m];
                }
            }
            if (actual != required) {
                return false;
            }
        }
        return true;
    }
    
    // Eq. (7): sum_i xikm <= 1
    public boolean checkTripLimit(HMASolution sol) {
        for (int k = 0; k < cfg.T; k++) {
            for (int m = 0; m < cfg.Mk; m++) {
                int count = 0;
                for (int i = 0; i < cfg.N; i++) {
                    count += sol.xikm[i][k][m];
                }
                if (count > 1) {
                    return false;
                }
            }
        }
        return true;
    }
    
    // Eq. (8): txp_{km+1} >= txp_{km} + sum_i (2 * doi / v * 60 + dtdo) * xikm
    public boolean checkSequence(HMASolution sol) {
        for (int k = 0; k < cfg.T; k++) {
            if (sol.zk[k] == 0) continue;
            for (int m = 0; m < cfg.Mk - 1; m++) {
                int siteM = -1;
                for (int i = 0; i < cfg.N; i++) {
                    if (sol.xikm[i][k][m] == 1) {
                        siteM = i;
                        break;
                    }
                }
                
                // If trip m is active, trip m+1 must start after trip m returns
                if (siteM != -1) {
                    double duration = (2.0 * cfg.doi[siteM] / cfg.v) * 60.0 + cfg.dtdo;
                    double returnTime = sol.txp_km[k][m] + duration;
                    
                    // Check if next trip starts before current returns (only if next trip is also active)
                    boolean nextActive = false;
                    for (int i = 0; i < cfg.N; i++) {
                        if (sol.xikm[i][k][m + 1] == 1) {
                            nextActive = true;
                            break;
                        }
                    }
                    if (nextActive && sol.txp_km[k][m + 1] < returnTime - 1e-5) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    // Check if any active trip returns after T_ca
    public boolean checkShiftLimit(HMASolution sol) {
        for (int k = 0; k < cfg.T; k++) {
            if (sol.zk[k] == 0) continue;
            for (int m = 0; m < cfg.Mk; m++) {
                int site = -1;
                for (int i = 0; i < cfg.N; i++) {
                    if (sol.xikm[i][k][m] == 1) {
                        site = i;
                        break;
                    }
                }
                if (site != -1) {
                    double duration = (2.0 * cfg.doi[site] / cfg.v) * 60.0 + cfg.dtdo;
                    double returnTime = sol.txp_km[k][m] + duration;
                    if (returnTime > cfg.T_ca + 1e-5) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    public boolean isFullyValid(HMASolution sol) {
        return checkDemand(sol) && checkTripLimit(sol) && checkSequence(sol) && checkShiftLimit(sol);
    }
}
