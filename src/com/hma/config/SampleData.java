package com.hma.config;

public class SampleData {

    /**
     * Cau hinh nho (Small): N=3 cong truong, T=5 xe, Mk=6 chuyen/xe
     * dim = T + N*T*Mk + T*Mk = 5 + 3*5*6 + 5*6 = 5 + 90 + 30 = 125
     */
    public static HMAConfig getSmallConfig() {
        int N = 3;
        int T = 5;
        int Mk = 6;

        double f = 2000000.0;
        double[] coi = {15000.0, 18000.0, 20000.0};
        double alpha = 500000.0;

        double[] doi = {15.0, 25.0, 35.0};
        double[] Di  = {50.0, 75.0, 60.0};

        double Q    = 12.5;
        double v    = 40.0;
        double dtdo = 30.0;
        double T_ca = 480.0;
        double To   = 160.0;

        return new HMAConfig(N, T, Mk, f, coi, alpha, doi, Di, Q, v, dtdo, T_ca, To);
    }

    /**
     * Cau hinh vua (Medium): N=5 cong truong, T=8 xe, Mk=8 chuyen/xe
     * dim = T + N*T*Mk + T*Mk = 8 + 5*8*8 + 8*8 = 8 + 320 + 64 = 392
     */
    public static HMAConfig getMediumConfig() {
        int N = 5;
        int T = 8;
        int Mk = 8;

        double f = 2000000.0;
        double[] coi = {15000.0, 17000.0, 19000.0, 21000.0, 23000.0};
        double alpha = 500000.0;

        double[] doi = {10.0, 18.0, 25.0, 32.0, 40.0};
        double[] Di  = {40.0, 60.0, 80.0, 55.0, 70.0};

        double Q    = 12.5;
        double v    = 40.0;
        double dtdo = 30.0;
        double T_ca = 480.0;
        double To   = 160.0;

        return new HMAConfig(N, T, Mk, f, coi, alpha, doi, Di, Q, v, dtdo, T_ca, To);
    }

    /**
     * Cau hinh lon (Large): N=8 cong truong, T=12 xe, Mk=10 chuyen/xe
     * dim = T + N*T*Mk + T*Mk = 12 + 8*12*10 + 12*10 = 12 + 960 + 120 = 1092
     */
    public static HMAConfig getLargeConfig() {
        int N = 8;
        int T = 12;
        int Mk = 10;

        double f = 2000000.0;
        double[] coi = {14000.0, 15000.0, 16000.0, 17000.0,
                        18000.0, 19000.0, 20000.0, 22000.0};
        double alpha = 500000.0;

        double[] doi = {8.0, 12.0, 18.0, 22.0, 28.0, 33.0, 38.0, 45.0};
        double[] Di  = {35.0, 50.0, 65.0, 70.0, 55.0, 80.0, 45.0, 60.0};

        double Q    = 12.5;
        double v    = 40.0;
        double dtdo = 30.0;
        double T_ca = 480.0;
        double To   = 160.0;

        return new HMAConfig(N, T, Mk, f, coi, alpha, doi, Di, Q, v, dtdo, T_ca, To);
    }

    /**
     * Giu nguyen cho tuong thich nguoc — goi lai getSmallConfig().
     */
    public static HMAConfig getSampleConfig() {
        return getSmallConfig();
    }
}
