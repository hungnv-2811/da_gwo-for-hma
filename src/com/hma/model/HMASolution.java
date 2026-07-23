package com.hma.model;

import com.hma.config.HMAConfig;

public class HMASolution {
    public int[] zk;              // [T] - vehicle k mobilized
    public int[][][] xikm;        // [N][T][Mk] - trip m of vehicle k to site i
    public double[][] txp_km;     // [T][Mk] - departure time from plant (minutes)

    // Cost details
    public double Cfixed;
    public double Coperational;
    public double Cpenalty;
    public double TC;
    
    public HMASolution(HMAConfig cfg) {
        this.zk = new int[cfg.T];
        this.xikm = new int[cfg.N][cfg.T][cfg.Mk];
        this.txp_km = new double[cfg.T][cfg.Mk];
    }
    
    // Decode continuous vector X to HMASolution
    public static HMASolution decode(double[] X, HMAConfig cfg) {
        HMASolution sol = new HMASolution(cfg);
        
        // 1. Decode zk (vehicle activation)
        for (int k = 0; k < cfg.T; k++) {
            sol.zk[k] = (X[k] >= 0.5) ? 1 : 0;
        }
        
        // 2. Decode xikm (assignment of trips to sites)
        int xOffset = cfg.T;
        for (int k = 0; k < cfg.T; k++) {
            if (sol.zk[k] == 0) continue;
            for (int m = 0; m < cfg.Mk; m++) {
                double maxVal = -1.0;
                int bestSite = -1;
                for (int i = 0; i < cfg.N; i++) {
                    // index for xikm in continuous vector X
                    int idx = xOffset + i * cfg.T * cfg.Mk + k * cfg.Mk + m;
                    if (X[idx] > maxVal) {
                        maxVal = X[idx];
                        bestSite = i;
                    }
                }
                // threshold for trip activation
                if (maxVal >= 0.3) {
                    sol.xikm[bestSite][k][m] = 1;
                }
            }
        }
        
        // 3. Decode txp_km (departure times)
        int tOffset = cfg.T + cfg.N * cfg.T * cfg.Mk;
        for (int k = 0; k < cfg.T; k++) {
            for (int m = 0; m < cfg.Mk; m++) {
                int idx = tOffset + k * cfg.Mk + m;
                // mapping [0, 1] to [0, T_ca]
                sol.txp_km[k][m] = X[idx] * cfg.T_ca;
            }
        }
        
        return sol;
    }
    
    // Encode HMASolution back to double[] continuous representation
    public double[] encode(HMAConfig cfg) {
        double[] X = new double[cfg.dim];
        
        // Encode zk
        for (int k = 0; k < cfg.T; k++) {
            X[k] = (zk[k] == 1) ? 0.8 : 0.2;
        }
        
        // Encode xikm
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
        
        // Encode txp_km
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
