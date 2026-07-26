package com.hma.constraint;

import com.hma.config.HMAConfig;
import com.hma.model.HMASolution;

public class ConstraintChecker {
    private HMAConfig cfg;
    
    public ConstraintChecker(HMAConfig cfg) {
        this.cfg = cfg;
    }
    
    // Phương trình (6): Ràng buộc toàn vẹn nhu cầu khối lượng từng công trường: sum_k sum_m xikm = Ceil(Di / Q)
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
    
    // Phương trình (7): Ràng buộc giới hạn chuyến đi: sum_i xikm <= 1
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
    
    // Phương trình (8): Ràng buộc thời gian tuần tự giữa các chuyến xe: txp_{km+1} >= txp_{km} + sum_i (2 * doi / v * 60 + dtdo) * xikm
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
                
                // Nếu chuyến m đang hoạt động, chuyến m+1 phải xuất phát sau khi chuyến m hoàn thành quay về
                if (siteM != -1) {
                    double duration = (2.0 * cfg.doi[siteM] / cfg.v) * 60.0 + cfg.dtdo;
                    double returnTime = sol.txp_km[k][m] + duration;
                    
                    // Kiểm tra chuyến tiếp theo có xuất phát trước khi chuyến hiện tại quay về hay không
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
    
    // Kiểm tra xe có quay về sau khi kết thúc ca làm việc (T_ca) hay không
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
    
    // Kiểm tra tất cả các ràng buộc hợp lệ
    public boolean isFullyValid(HMASolution sol) {
        return checkDemand(sol) && checkTripLimit(sol) && checkSequence(sol) && checkShiftLimit(sol);
    }
}
