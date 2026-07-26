package com.hma.config;

public class HMAConfig {
    // Quy mô bài toán
    public int N;               // Số lượng công trường xây dựng
    public int T;               // Tổng số lượng xe vận chuyển trong đội xe
    public int Mk;              // Số chuyến tối đa của mỗi xe trong một ca
    
    // Tham số chi phí
    public double f;            // Đơn giá chi phí cố định khi huy động một xe (VNĐ/xe)
    public double[] coi;        // Đơn giá chi phí vận hành trên 1 km đối với công trường i (VNĐ/km)
    public double alpha;        // Hệ số phạt tài chính khi nhiệt độ HMA dưới 120°C (VNĐ/tấn)
    
    // Khoảng cách & Nhu cầu
    public double[] doi;        // Khoảng cách 1 chiều từ trạm trộn đến công trường i (km)
    public double[] Di;         // Nhu cầu bê tông nhựa nóng HMA tại công trường i (tấn)
    
    // Thông số xe
    public double Q;            // Tải trọng định mức chuyên chở của một xe (tấn)
    public double v;            // Vận tốc di chuyển trung bình thiết kế (km/h)
    
    // Tham số thời gian
    public double dtdo;         // Thời gian dừng chờ và đổ vật liệu tại công trường (phút)
    public double T_ca;         // Tổng thời gian giới hạn của ca làm việc (phút)
    
    // Tham số nhiệt độ
    public double To;           // Nhiệt độ tiêu chuẩn của HMA khi bắt đầu rời trạm trộn (°C)
    
    // Số chiều mã hóa
    public int dim;             // Tổng số chiều cho vector mã hóa liên tục: T + N*T*Mk + T*Mk
    
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
