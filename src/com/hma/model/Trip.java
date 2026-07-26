package com.hma.model;

public class Trip implements Comparable<Trip> {
    public int k; // Chỉ số phương tiện vận chuyển
    public int m; // Chỉ số chuyến của xe k (từ 0 đến Mk-1)
    public int siteIndex; // Chỉ số công trường đích i (-1 nếu chuyến không kích hoạt)
    public double txp; // Mốc thời điểm rời trạm trộn (phút)
    public double Tikm; // Nhiệt độ thực tế khi đến công trường (°C)
    public double tripCost; // Chi phí vận hành chuyến xe (VNĐ)
    public double penalty; // Chi phí phạt chất lượng do giảm nhiệt độ (VNĐ)
    
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
