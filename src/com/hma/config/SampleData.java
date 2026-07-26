package com.hma.config;

public class SampleData {
    public static HMAConfig getSampleConfig() {
        int N = 3; // Số lượng công trường cần phục vụ
        int T = 5; // Số lượng xe sẵn có trong đội xe
        int Mk = 6; // Số chuyến tối đa mỗi xe có thể chạy trong 1 ca
        
        double f = 2000000.0; // Đơn giá chi phí cố định (VNĐ/xe)
        double[] coi = {15000.0, 18000.0, 20000.0}; // Đơn giá chi phí vận hành (VNĐ/km)
        double alpha = 500000.0; // Hệ số phạt tài chính nhiệt độ giảm dưới 120°C (VNĐ/tấn)
        
        double[] doi = {15.0, 25.0, 35.0}; // Khoảng cách 1 chiều từ trạm trộn đến công trường (km)
        double[] Di = {50.0, 75.0, 60.0}; // Nhu cầu bê tông nhựa nóng tại công trường (tấn)
        
        double Q = 12.5; // Tải trọng định mức của phương tiện (tấn)
        double v = 40.0; // Vận tốc trung bình thiết kế của xe (km/h)
        double dtdo = 30.0; // Thời gian dừng chờ và đổ vật liệu tại hiện trường (phút)
        double T_ca = 480.0; // Giới hạn thời gian ca làm việc (8 tiếng = 480 phút)
        double To = 160.0; // Nhiệt độ tiêu chuẩn khi bê tông nhựa nóng rời trạm trộn (°C)
        
        return new HMAConfig(N, T, Mk, f, coi, alpha, doi, Di, Q, v, dtdo, T_ca, To);
    }
}
