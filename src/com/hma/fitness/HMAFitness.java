package com.hma.fitness;

import com.hma.config.HMAConfig;
import com.hma.cost.CostCalculator;
import com.hma.model.HMASolution;
import com.hma.constraint.RepairOperator;
import com.test.f_xj;
import java.io.IOException;

public class HMAFitness extends f_xj {
    private HMAConfig cfg;
    private CostCalculator calculator;
    
    public HMAFitness(HMAConfig cfg) {
        this.cfg = cfg;
        this.calculator = new CostCalculator(cfg);
    }
    
    @Override
    public double func(double[] X) throws IOException {
        // 1. Giải mã vector liên tục X sang phương án HMASolution
        HMASolution sol = HMASolution.decode(X, cfg);
        
        // 2. Sửa chữa các ràng buộc kỹ thuật (6), (7), (8), (11)
        RepairOperator.repairAll(sol, cfg);
        
        // 3. Tính tổng chi phí mục tiêu TC (Phương trình 1)
        double cost = calculator.calcTotalCost(sol);
        
        // 4. Cộng thêm phạt nặng nếu còn vi phạm ràng buộc chưa sửa được
        double penalty = calcExtraPenalty(sol);
        
        return cost + penalty;
    }
    
    private double calcExtraPenalty(HMASolution sol) {
        double penalty = 0.0;
        double LAMBDA = 10000000.0; // Hệ số phạt cực lớn
        
        // Kiểm tra ràng buộc toàn vẹn nhu cầu (6)
        for (int i = 0; i < cfg.N; i++) {
            int required = (int) Math.ceil(cfg.Di[i] / cfg.Q);
            int actual = 0;
            for (int k = 0; k < cfg.T; k++) {
                for (int m = 0; m < cfg.Mk; m++) {
                    actual += sol.xikm[i][k][m];
                }
            }
            if (actual != required) {
                penalty += LAMBDA * Math.abs(actual - required);
            }
        }
        
        // Kiểm tra ràng buộc thời gian tuần tự (8)
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
                
                if (siteM != -1) {
                    double duration = (2.0 * cfg.doi[siteM] / cfg.v) * 60.0 + cfg.dtdo;
                    double returnTime = sol.txp_km[k][m] + duration;
                    
                    boolean nextActive = false;
                    for (int i = 0; i < cfg.N; i++) {
                        if (sol.xikm[i][k][m + 1] == 1) {
                            nextActive = true;
                            break;
                        }
                    }
                    if (nextActive && sol.txp_km[k][m + 1] < returnTime - 1e-5) {
                        penalty += LAMBDA * (returnTime - sol.txp_km[k][m + 1]);
                    }
                }
            }
        }
        
        return penalty;
    }
}
