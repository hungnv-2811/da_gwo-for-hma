package com.hma.model;

public class Vehicle {
    public int k; // Chỉ số phương tiện (từ 0 đến T-1)
    public int zk; // Biến nhị phân: 1 nếu xe được huy động, 0 nếu ngược lại
    public int Mk; // Số chuyến tối đa trong ca làm việc
    
    public Vehicle(int k, int zk, int Mk) {
        this.k = k;
        this.zk = zk;
        this.Mk = Mk;
    }
}
