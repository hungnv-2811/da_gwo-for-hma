package com.hma.model;

public class ConstructionSite {
    public int i; // 0-indexed site identifier
    public double Di; // Demand (tons)
    public double doi; // One-way distance (km)
    public double coi; // Operating cost per km (VND)
    public int requiredTrips; // Ceil(Di/Q)
    
    public ConstructionSite(int i, double Di, double doi, double coi, double Q) {
        this.i = i;
        this.Di = Di;
        this.doi = doi;
        this.coi = coi;
        this.requiredTrips = (int) Math.ceil(Di / Q);
    }
}
