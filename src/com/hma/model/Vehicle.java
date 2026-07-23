package com.hma.model;

public class Vehicle {
    public int k; // 0-indexed vehicle identifier
    public int zk; // 1 if mobilized, 0 otherwise
    public int Mk; // max trips
    
    public Vehicle(int k, int zk, int Mk) {
        this.k = k;
        this.zk = zk;
        this.Mk = Mk;
    }
}
