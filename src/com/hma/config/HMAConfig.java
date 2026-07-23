package com.hma.config;

public class HMAConfig {
    // Problem scale
    public int N;               // Number of construction sites
    public int T;               // Number of vehicles
    public int Mk;              // Maximum trips per vehicle per shift
    
    // Cost parameters
    public double f;            // Fixed cost per mobilized vehicle (VND/vehicle)
    public double[] coi;        // Operating cost rate per km (VND/km) for each site i
    public double alpha;        // Financial penalty rate for HMA temperature drop below 120 C (VND/ton)
    
    // Distance & Demand
    public double[] doi;        // One-way distance from plant to construction site i (km)
    public double[] Di;         // HMA demand at construction site i (ton)
    
    // Vehicle specs
    public double Q;            // Vehicle rated capacity (tons)
    public double v;            // Average design speed of vehicle (km/h)
    
    // Time parameters
    public double dtdo;         // Stopping/waiting and unloading time at the site (minutes)
    public double T_ca;         // Total shift time limit (minutes)
    
    // Temperature parameters
    public double To;           // Standard temperature of HMA when leaving plant (C)
    
    // Encoding dimension
    public int dim;             // Total dimension for continuous encoding: T + N*T*Mk + T*Mk
    
    public HMAConfig(int N, int T, int Mk, double f, double[] coi, double alpha, 
                     double[] doi, double[] Di, double Q, double v, double dtdo, 
                     double T_ca, double To) {
        this.N = N;
        this.T = T;
        this.Mk = Mk;
        this.f = f;
        this.coi = coi;
        this.alpha = alpha;
        this.doi = doi;
        this.Di = Di;
        this.Q = Q;
        this.v = v;
        this.dtdo = dtdo;
        this.T_ca = T_ca;
        this.To = To;
        
        // dim = T (zk) + N * T * Mk (xikm) + T * Mk (txp_km)
        this.dim = T + N * T * Mk + T * Mk;
    }
}
