package com.hma.model;

import com.hma.config.HMAConfig;

public class HMASolution {
    public int[] zk;              // Biến nhị phân [T]: 1 nếu xe k được huy động
    public int[][][] xikm;        // Biến nhị phân [N][T][Mk]: 1 nếu chuyến m của xe k đến công trường i
    public double[][] txp_km;     // Biến liên tục [T][Mk]: mốc thời điểm rời trạm trộn (phút)

    // Chi tiết chi phí
    public double Cfixed;
    public double Coperational;
    public double Cpenalty;
    public double TC;
    
    public HMASolution(HMAConfig cfg) {
        this.zk = new int[cfg.T];
        this.xikm = new int[cfg.N][cfg.T][cfg.Mk];
        this.txp_km = new double[cfg.T][cfg.Mk];
    }
    
    // Giải mã vector liên tục X sang đối tượng phương án HMASolution
    public static HMASolution decode(double[] X, HMAConfig cfg) {
        HMASolution sol = new HMASolution(cfg);
        
        // 1. Giải mã zk (kích hoạt xe)
        for (int k = 0; k < cfg.T; k++) {
            sol.zk[k] = (X[k] >= 0.5) ? 1 : 0;
        }
        
        // 2. Giải mã xikm (phân công chuyến xe đến công trường)
        int xOffset = cfg.T;
        for (int k = 0; k < cfg.T; k++) {
            if (sol.zk[k] == 0) continue;
            for (int m = 0; m < cfg.Mk; m++) {
                double maxVal = -1.0;
                int bestSite = -1;
                for (int i = 0; i < cfg.N; i++) {
                    // Chỉ số cho xikm trong vector liên tục X
                    int idx = xOffset + i * cfg.T * cfg.Mk + k * cfg.Mk + m;
                    if (X[idx] > maxVal) {
                        maxVal = X[idx];
                        bestSite = i;
                    }
                }
                // Ngưỡng kích hoạt chuyến đi
                if (maxVal >= 0.3) {
                    sol.xikm[bestSite][k][m] = 1;
                }
            }
        }
        
        // 3. Giải mã txp_km (thời điểm rời trạm trộn)
        int tOffset = cfg.T + cfg.N * cfg.T * cfg.Mk;
        for (int k = 0; k < cfg.T; k++) {
            for (int m = 0; m < cfg.Mk; m++) {
                int idx = tOffset + k * cfg.Mk + m;
                // Ánh xạ [0, 1] sang miền giá trị [0, T_ca] (phút)
                sol.txp_km[k][m] = X[idx] * cfg.T_ca;
            }
        }
        
        return sol;
    }
    
    // Mã hóa ngược HMASolution thành vector biểu diễn liên tục double[]
    public double[] encode(HMAConfig cfg) {
        double[] X = new double[cfg.dim];
        
        // Mã hóa zk
        for (int k = 0; k < cfg.T; k++) {
            X[k] = (zk[k] == 1) ? 0.8 : 0.2;
        }
        
        // Mã hóa xikm
        int xOffset = cfg.T;
        for (int k = 0; k < cfg.T; k++) {
            for (int m = 0; m < cfg.Mk; m++) {
                int activeSite = -1;
                for (int i = 0; i < cfg.N; i++) {
                    if (xikm[i][k][m] == 1) {
                        activeSite = i;
                        break;
                    }
                }
                for (int i = 0; i < cfg.N; i++) {
                    int idx = xOffset + i * cfg.T * cfg.Mk + k * cfg.Mk + m;
                    if (i == activeSite) {
                        X[idx] = 0.8;
                    } else {
                        X[idx] = 0.1;
                    }
                }
            }
        }
        
        // Mã hóa txp_km
        int tOffset = cfg.T + cfg.N * cfg.T * cfg.Mk;
        for (int k = 0; k < cfg.T; k++) {
            for (int m = 0; m < cfg.Mk; m++) {
                int idx = tOffset + k * cfg.Mk + m;
                X[idx] = txp_km[k][m] / cfg.T_ca;
            }
        }
        
        return X;
    }
}
