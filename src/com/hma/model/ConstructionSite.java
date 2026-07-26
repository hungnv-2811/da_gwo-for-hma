package com.hma.model;

public class ConstructionSite {
    public int i; // Chỉ số công trường (từ 0 đến N-1)
    public double Di; // Nhu cầu khối lượng bê tông nhựa nóng (tấn)
    public double doi; // Quãng đường di chuyển 1 chiều từ trạm trộn (km)
    public double coi; // Đơn giá chi phí vận hành phương tiện trên 1 km (VNĐ/km)
    public int requiredTrips; // Số chuyến xe cần thiết = Ceil(Di / Q)
    
    public ConstructionSite(int i, double Di, double doi, double coi, double Q) {
        this.i = i;
        this.Di = Di;
        this.doi = doi;
        this.coi = coi;
        this.requiredTrips = (int) Math.ceil(Di / Q);
    }
}
