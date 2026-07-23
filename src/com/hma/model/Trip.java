package com.hma.model;

public class Trip implements Comparable<Trip> {
    public int k; // Vehicle index
    public int m; // Trip index for vehicle k (0 to Mk-1)
    public int siteIndex; // Dest site index i (-1 if trip is inactive)
    public double txp; // Departure time from plant (minutes)
    public double Tikm; // Temperature on arrival (C)
    public double tripCost; // Operating cost of this trip (VND)
    public double penalty; // Penalty cost of this trip (VND)
    
    public Trip(int k, int m, int siteIndex, double txp, double Tikm, double tripCost, double penalty) {
        this.k = k;
        this.m = m;
        this.siteIndex = siteIndex;
        this.txp = txp;
        this.Tikm = Tikm;
        this.tripCost = tripCost;
        this.penalty = penalty;
    }

    @Override
    public int compareTo(Trip o) {
        return Double.compare(this.txp, o.txp);
    }
}
