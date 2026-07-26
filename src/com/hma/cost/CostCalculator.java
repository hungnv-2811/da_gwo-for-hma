package com.hma.cost;

import com.hma.config.HMAConfig;
import com.hma.model.HMASolution;

public class CostCalculator {
    private HMAConfig cfg;
    
    public CostCalculator(HMAConfig cfg) {
        this.cfg = cfg;
    }
    
    // Phương trình (2): Cfixed = sum(f * zk) - Chi phí cố định huy động xe
    public double calcFixedCost(HMASolution sol) {
        double cFixed = 0.0;
        for (int k = 0; k < cfg.T; k++) {
            cFixed += cfg.f * sol.zk[k];
        }
        return cFixed;
    }
    
    // Phương trình (3): Coperational = sum(2 * doi * coi * xikm) - Chi phí vận hành khứ hồi
    public double calcOperationalCost(HMASolution sol) {
        double cOper = 0.0;
        for (int k = 0; k < cfg.T; k++) {
            if (sol.zk[k] == 0) continue;
            for (int i = 0; i < cfg.N; i++) {
                for (int m = 0; m < cfg.Mk; m++) {
                    cOper += 2.0 * cfg.doi[i] * cfg.coi[i] * sol.xikm[i][k][m];
                }
            }
        }
        return cOper;
    }
    
    // Phương trình (9): Tikm = To - 0.5 * (doi / (v * 60)) - Mô hình suy giảm nhiệt độ HMA tuyến tính
    public double calcTemperature(int i) {
        return cfg.To - 0.5 * (cfg.doi[i] / cfg.v) * 60;
    }
    
    // Phương trình (4) & (5): Cpenalty = sum(F(Tikm) * xikm) - Chi phí phạt chất lượng nhiệt độ HMA
    public double calcPenaltyCost(HMASolution sol) {
        double cPenalty = 0.0;
        for (int k = 0; k < cfg.T; k++) {
            if (sol.zk[k] == 0) continue;
            for (int i = 0; i < cfg.N; i++) {
                for (int m = 0; m < cfg.Mk; m++) {
                    if (sol.xikm[i][k][m] == 1) {
                        double Tikm = calcTemperature(i);
                        if (Tikm < 120.0) {
                            cPenalty += cfg.Q * cfg.alpha;
                        }
                    }
                }
            }
        }
        return cPenalty;
    }
    
    // Phương trình (1): Tổng chi phí TC = Cfixed + Coperational + Cpenalty
    public double calcTotalCost(HMASolution sol) {
        sol.Cfixed = calcFixedCost(sol);
        sol.Coperational = calcOperationalCost(sol);
        sol.Cpenalty = calcPenaltyCost(sol);
        sol.TC = sol.Cfixed + sol.Coperational + sol.Cpenalty;
        return sol.TC;
    }
}
