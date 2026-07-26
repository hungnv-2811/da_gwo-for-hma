package com.test;

import com.hma.config.HMAConfig;
import com.hma.config.SampleData;
import com.hma.fitness.HMAFitness;
import com.hma.model.HMASolution;
import com.hma.utils.SolutionPrinter;
import com.hma.constraint.RepairOperator;
import com.hma.cost.CostCalculator;

/**
 * HMAOptimizer_GWO - Tối ưu hóa vận chuyển HMA bằng Thuật toán Sói xám (GWO)
 *
 * GWO (Grey Wolf Optimizer) mô phỏng thứ bậc lãnh đạo và cơ chế săn mồi của loài sói xám.
 * Đàn sói được chia thành 4 cấp bậc:
 *   - Alpha (α): Phương án tốt nhất — dẫn đầu cuộc săn
 *   - Beta  (β): Phương án tốt thứ hai — hỗ trợ alpha
 *   - Delta (δ): Phương án tốt thứ ba — hướng dẫn phần còn lại
 *   - Omega (ω): Các con sói còn lại — đi theo α, β, δ
 *
 * Công thức cập nhật vị trí:
 *   X1 = Xα - A1 * |C1 * Xα - X|
 *   X2 = Xβ - A2 * |C2 * Xβ - X|
 *   X3 = Xδ - A3 * |C3 * Xδ - X|
 *   X_new = (X1 + X2 + X3) / 3
 *
 * Trong đó:
 *   a giảm tuyến tính từ 2 xuống 0 qua các vòng lặp (điều khiển cân bằng khám phá và khai thác)
 *   A = 2*a*r1 - a,  C = 2*r2   (r1, r2 là các số ngẫu nhiên trong [0,1])
 */
public class HMAOptimizer_GWO {

    public static void main(String[] args) throws Exception {
        // ---------------------------------------------------------------
        // 1. CẤU HÌNH BÀI TOÁN
        // ---------------------------------------------------------------
        HMAConfig cfg = SampleData.getSampleConfig();

        System.out.println("===============================================================");
        System.out.println("     TỐI ƯU HÓA VẬN CHUYỂN HMA BẰNG THUẬT TOÁN SÓI XÁM (GWO)    ");
        System.out.println("===============================================================");
        System.out.println("Quy mô bài toán:");
        System.out.println("  - Số công trường (N)     = " + cfg.N);
        System.out.println("  - Số xe khả dụng (T)     = " + cfg.T);
        System.out.println("  - Số chuyến tối đa / xe  = " + cfg.Mk);
        System.out.println("  - Số chiều không gian    = " + cfg.dim);
        System.out.println("---------------------------------------------------------------");
        System.out.println("Thuật toán: GWO - Grey Wolf Optimizer");
        System.out.println("  Chiến lược: Các con sói Alpha, Beta, Delta dẫn dắt các con sói Omega");
        System.out.println("  Khám phá: Tham số a giảm tuyến tính từ 2 xuống 0 theo vòng lặp");
        System.out.println("---------------------------------------------------------------");

        // Định nghĩa biên [0.0, 1.0] cho vector mã hóa liên tục
        double[] lb = new double[cfg.dim];
        double[] ub = new double[cfg.dim];
        for (int i = 0; i < cfg.dim; i++) {
            lb[i] = 0.0;
            ub[i] = 1.0;
        }

        // Khởi tạo hàm mục tiêu HMA
        f_xj fobj = new HMAFitness(cfg);

        // ---------------------------------------------------------------
        // 2. THAM SỐ THUẬT TOÁN
        // ---------------------------------------------------------------
        int maxIter  = 300;   // Số vòng lặp tối đa
        int popSize  = 40;    // Kích thước quần thể (số lượng sói xám)

        System.out.println("Tham số:");
        System.out.println("  - Kích thước quần thể = " + popSize + " con sói");
        System.out.println("  - Số vòng lặp tối đa  = " + maxIter);
        System.out.println("===============================================================");

        // ---------------------------------------------------------------
        // 3. CHẠY THUẬT TOÁN GWO
        // ---------------------------------------------------------------
        System.out.println("\nĐang chạy tối ưu hóa GWO...");

        long startTime = System.currentTimeMillis();

        // Lớp thực thi GWO có theo dõi lịch sử hội tụ
        GWO_HMA solver = new GWO_HMA(fobj, lb, ub, maxIter, popSize);
        solver.solution();

        long endTime = System.currentTimeMillis();

        // ---------------------------------------------------------------
        // 4. BÁO CÁO HỘI TỤ
        // ---------------------------------------------------------------
        System.out.println("\n\nHoàn thành tối ưu hóa trong " + ((endTime - startTime) / 1000.0) + " giây.");
        System.out.println("\nMốc hội tụ (Chi phí tốt nhất theo từng vòng lặp):");
        System.out.printf("  - Vòng lặp   1 : %,.0f VNĐ%n", solver.convergenceHistory[1]);
        System.out.printf("  - Vòng lặp  50 : %,.0f VNĐ%n", solver.convergenceHistory[50]);
        System.out.printf("  - Vòng lặp 100 : %,.0f VNĐ%n", solver.convergenceHistory[100]);
        System.out.printf("  - Vòng lặp 150 : %,.0f VNĐ%n", solver.convergenceHistory[150]);
        System.out.printf("  - Vòng lặp 200 : %,.0f VNĐ%n", solver.convergenceHistory[200]);
        System.out.printf("  - Vòng lặp 250 : %,.0f VNĐ%n", solver.convergenceHistory[250]);
        System.out.printf("  - Vòng lặp 300 : %,.0f VNĐ%n", solver.convergenceHistory[300]);

        // Mức độ cải thiện
        double initCost  = solver.convergenceHistory[1];
        double finalCost = solver.convergenceHistory[maxIter];
        double improvement = (initCost > 0) ? (initCost - finalCost) / initCost * 100.0 : 0;
        System.out.printf("%nMức độ cải thiện từ Vòng 1 đến %d: %.2f%%%n", maxIter, improvement);

        // ---------------------------------------------------------------
        // 5. GIẢI MÃ VÀ IN PHƯƠNG ÁN
        // ---------------------------------------------------------------
        double[] bestPos = solver.Best_pos;
        HMASolution sol  = HMASolution.decode(bestPos, cfg);
        RepairOperator.repairAll(sol, cfg);

        // In phương án chi tiết
        SolutionPrinter.printSolution(sol, cfg);

        // ---------------------------------------------------------------
        // 6. PHÂN TÍCH CHI PHÍ
        // ---------------------------------------------------------------
        CostCalculator calc = new CostCalculator(cfg);
        calc.calcTotalCost(sol);
        System.out.println("\nPhân tích chi phí (Kết quả GWO):");
        System.out.printf("  Tổng chi phí (TC)     : %,.0f VNĐ%n", sol.TC);
        System.out.printf("  Chi phí cố định       : %,.0f VNĐ%n", sol.Cfixed);
        System.out.printf("  Chi phí vận hành      : %,.0f VNĐ%n", sol.Coperational);
        System.out.printf("  Phạt chất lượng nhiệt  : %,.0f VNĐ%n", sol.Cpenalty);

        // ---------------------------------------------------------------
        // 7. XUẤT FILE EXCEL
        // ---------------------------------------------------------------
        com.hma.utils.ExcelExporter.exportSolutionToExcel(sol, cfg, solver.convergenceHistory);

        System.out.println("===============================================================");
        System.out.println("               HOÀN THÀNH TỐI ƯU HÓA GWO                       ");
        System.out.println("===============================================================");
    }
}

// -----------------------------------------------------------------------
// Lớp hỗ trợ: GWO_HMA — Thực thi thuật toán GWO đầy đủ với lịch sử hội tụ
// -----------------------------------------------------------------------
class GWO_HMA {
    // Các trường lõi của GWO
    double r1, r2;
    int N, D, maxiter;
    double[] alfa, beta, delta;
    double[] Lower, Upper;
    f_xj ff;
    double[][] XX;
    double X1, X2, X3;
    double[] fitness;
    double a;
    double A1, C1, A2, C2, A3, C3;

    // Các trường kết quả
    public double   Best_score;
    public double[] Best_pos;
    public double[] convergenceHistory;

    public GWO_HMA(f_xj fobj, double[] lb, double[] ub, int maxIter, int popSize) {
        this.ff      = fobj;
        this.Lower   = lb;
        this.Upper   = ub;
        this.maxiter = maxIter;
        this.N       = popSize;
        this.D       = ub.length;

        XX    = new double[N][D];
        alfa  = new double[D];
        beta  = new double[D];
        delta = new double[D];

        convergenceHistory = new double[maxIter + 1];
        Best_pos           = new double[D];
        Best_score         = Double.MAX_VALUE;
    }

    // Khởi tạo: vị trí ngẫu nhiên, xác định các vị trí dẫn đầu Alpha/Beta/Delta
    void init() throws Exception {
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < D; j++) {
                XX[i][j] = Lower[j] + (Upper[j] - Lower[j]) * Math.random();
            }
        }
        XX = sort_and_index(XX);
        for (int j = 0; j < D; j++) alfa[j]  = XX[0][j];
        for (int j = 0; j < D; j++) beta[j]  = XX[1][j];
        for (int j = 0; j < D; j++) delta[j] = XX[2][j];
    }

    // Vòng lặp tối ưu hóa chính
    public void solution() throws Exception {
        init();

        for (int iter = 1; iter <= maxiter; iter++) {
            // a giảm tuyến tính từ 2 xuống 0 (điều khiển cân bằng giữa khám phá và khai thác)
            a = 2.0 - (double) iter * (2.0 / (double) maxiter);

            // Cập nhật vị trí của tất cả các con sói (các con sói Omega đi theo α, β, δ)
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < D; j++) {
                    // Tương tác với Alpha
                    r1 = Math.random(); r2 = Math.random();
                    A1 = 2.0 * a * r1 - a;
                    C1 = 2.0 * r2;
                    X1 = alfa[j] - A1 * Math.abs(C1 * alfa[j] - XX[i][j]);
                    if (X1 < Lower[j] || X1 > Upper[j]) X1 = Lower[j] + (Upper[j] - Lower[j]) * Math.random();

                    // Tương tác với Beta
                    r1 = Math.random(); r2 = Math.random();
                    A2 = 2.0 * a * r1 - a;
                    C2 = 2.0 * r2;
                    X2 = beta[j] - A2 * Math.abs(C2 * beta[j] - XX[i][j]);
                    if (X2 < Lower[j] || X2 > Upper[j]) X2 = Lower[j] + (Upper[j] - Lower[j]) * Math.random();

                    // Tương tác với Delta
                    r1 = Math.random(); r2 = Math.random();
                    A3 = 2.0 * a * r1 - a;
                    C3 = 2.0 * r2;
                    X3 = delta[j] - A3 * Math.abs(C3 * delta[j] - XX[i][j]);
                    if (X3 < Lower[j] || X3 > Upper[j]) X3 = Lower[j] + (Upper[j] - Lower[j]) * Math.random();

                    // Vị trí mới = trung bình ảnh hưởng của cả 3 con sói dẫn đầu
                    XX[i][j] = (X1 + X2 + X3) / 3.0;
                }
            }

            // Áp dụng giới hạn biên + sắp xếp tìm các con sói dẫn đầu mới
            XX = simplebounds(XX);
            XX = sort_and_index(XX);

            // Cập nhật vai trò Alpha, Beta, Delta
            for (int j = 0; j < D; j++) alfa[j]  = XX[0][j];
            for (int j = 0; j < D; j++) beta[j]  = XX[1][j];
            for (int j = 0; j < D; j++) delta[j] = XX[2][j];

            // Ghi nhận chi phí tốt nhất ở vòng lặp này
            Best_score             = ff.func(XX[0]);
            Best_pos               = XX[0].clone();
            convergenceHistory[iter] = Best_score;
        }
    }

    // Sắp xếp quần thể theo giá trị hàm mục tiêu (tăng dần = tốt nhất xếp đầu)
    double[][] sort_and_index(double[][] pop) throws Exception {
        int n = pop.length;
        double[] vals = new double[n];
        for (int i = 0; i < n; i++) vals[i] = ff.func(pop[i]);

        int[] idx = new int[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        for (int i = 0; i < n - 1; i++) {
            for (int j = i + 1; j < n; j++) {
                if (vals[j] < vals[i]) {
                    double tmp = vals[i]; vals[i] = vals[j]; vals[j] = tmp;
                    int   ti  = idx[i];  idx[i]  = idx[j];  idx[j]  = ti;
                }
            }
        }
        double[][] sorted = new double[n][D];
        for (int i = 0; i < n; i++) sorted[i] = pop[idx[i]].clone();
        return sorted;
    }

    // Giữ các vị trí nằm trong khoảng biên [Lower, Upper]
    double[][] simplebounds(double[][] s) {
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < D; j++) {
                if (s[i][j] < Lower[j]) s[i][j] = Lower[j] + (Upper[j] - Lower[j]) * Math.random();
                if (s[i][j] > Upper[j]) s[i][j] = Lower[j] + (Upper[j] - Lower[j]) * Math.random();
            }
        }
        return s;
    }
}
