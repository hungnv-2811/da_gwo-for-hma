package com.hma.config;

public class SampleData {
    public static HMAConfig getSampleConfig() {
        int N = 3; // Number of sites
        int T = 5; // Number of vehicles
        int Mk = 6; // Max trips per vehicle
        
        double f = 2000000.0; // Fixed cost per vehicle (VND)
        double[] coi = {15000.0, 18000.0, 20000.0}; // Operational cost per km (VND)
        double alpha = 500000.0; // Temperature penalty per ton (VND)
        
        double[] doi = {15.0, 25.0, 35.0}; // Distances (km)
        double[] Di = {50.0, 75.0, 60.0}; // Demands (tons)
        
        double Q = 12.5; // Vehicle capacity (tons)
        double v = 40.0; // Average speed (km/h)
        double dtdo = 30.0; // Unloading/waiting time (minutes)
        double T_ca = 480.0; // Shift length (8 hours = 480 minutes)
        double To = 160.0; // Temperature at plant (C)
        
        return new HMAConfig(N, T, Mk, f, coi, alpha, doi, Di, Q, v, dtdo, T_ca, To);
    }
}
