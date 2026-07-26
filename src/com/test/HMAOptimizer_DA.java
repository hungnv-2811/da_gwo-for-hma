package com.test;

import com.hma.config.HMAConfig;
import com.hma.config.SampleData;
import com.hma.fitness.HMAFitness;
import com.hma.model.HMASolution;
import com.hma.utils.SolutionPrinter;
import com.hma.utils.ExcelExporter;
import com.hma.constraint.RepairOperator;
import com.hma.cost.CostCalculator;
import org.apache.commons.math3.special.Gamma;

/**
 * HMAOptimizer_DA - Tối ưu hóa vận chuyển HMA bằng Thuật toán Chuồn chuồn (DA)
 *
 * DA (Dragonfly Algorithm) mô phỏng các hành vi bầy đàn tĩnh và động của loài chuồn chuồn.
 * Mỗi con chuồn chuồn cập nhật vector bước di chuyển (ΔX) dựa trên 5 hành vi bầy đàn:
 *
 *   1. Phân tách (Separation - S)  - Tránh va chạm với các con hàng xóm         [Eq. 3.1]
 *   2. Căn chỉnh (Alignment - A)   - Đồng bộ vận tốc với các con hàng xóm       [Eq. 3.2]
 *   3. Tụ hội  (Cohesion - C)     - Di chuyển về tâm của bầy đàn               [Eq. 3.3]
 *   4. Thức ăn  (Food - F)         - Thu hút về phía phương án tốt nhất (mồi)   [Eq. 3.4]
 *   5. Kẻ thù   (Enemy - E)        - Xua đuổi khỏi phương án tệ nhất (kẻ thù)   [Eq. 3.5]
 *
 * Khi không có hàng xóm xung quanh, chuyến bay Lévy (Lévy flight) được sử dụng:  [Eq. 3.8]
 *   ΔX_new = w*ΔX + rand*A + rand*C + rand*S
 *   X_new  = X + ΔX
 *
 * Bán kính lân cận r mở rộng dần từ 25% đến 75% không gian tìm kiếm.
 * Trọng số quán tính w giảm dần từ 0.9 xuống 0.4 (tăng cường khai thác ở giai đoạn sau).
 */
public class HMAOptimizer_DA {

    public static void main(String[] args) throws Exception {
        // ---------------------------------------------------------------
        // 1. CẤU HÌNH BÀI TOÁN
        // ---------------------------------------------------------------
        HMAConfig cfg = SampleData.getSampleConfig();

        System.out.println("===============================================================");
        System.out.println("   TỐI ƯU HÓA VẬN CHUYỂN HMA BẰNG THUẬT TOÁN CHUỒN CHUỒN (DA)  ");
        System.out.println("===============================================================");
        System.out.println("Quy mô bài toán:");
        System.out.println("  - Số công trường (N)     = " + cfg.N);
        System.out.println("  - Số xe khả dụng (T)     = " + cfg.T);
        System.out.println("  - Số chuyến tối đa / xe  = " + cfg.Mk);
        System.out.println("  - Số chiều không gian    = " + cfg.dim);
        System.out.println("---------------------------------------------------------------");
        System.out.println("Thuật toán: DA - Dragonfly Algorithm");
        System.out.println("  Chiến lược: 5 hành vi bầy đàn + Chuyến bay Lévy khi cô lập");
        System.out.println("  Nguồn thức ăn: Phương án tốt nhất hiện tại (mục tiêu tối thiểu hóa)");
        System.out.println("  Kẻ thù: Phương án tệ nhất trong biên (tránh xa)");
        System.out.println("  Bán kính lân cận: Mở rộng từ 25% đến 75% không gian tìm kiếm");
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
        int maxIter  = 300;  // Số vòng lặp tối đa
        int popSize  = 40;   // Kích thước quần thể (số lượng chuồn chuồn)

        System.out.println("Tham số:");
        System.out.println("  - Kích thước quần thể = " + popSize + " chuồn chuồn");
        System.out.println("  - Số vòng lặp tối đa  = " + maxIter);
        System.out.println("  - Trọng số quán tính  : 0.9 -> 0.4 (giảm tuyến tính)");
        System.out.println("  - my_c                : 0.1 -> 0   (điều khiển trọng số hành vi)");
        System.out.println("===============================================================");

        // ---------------------------------------------------------------
        // 3. CHẠY THUẬT TOÁN DA
        // ---------------------------------------------------------------
        System.out.println("\nĐang chạy tối ưu hóa DA...");

        long startTime = System.currentTimeMillis();

        DA_HMA solver = new DA_HMA(fobj, lb, ub, maxIter, popSize);
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
        double initCost   = solver.convergenceHistory[1];
        double finalCost  = solver.convergenceHistory[maxIter];
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
        System.out.println("\nPhân tích chi phí (Kết quả DA):");
        System.out.printf("  Tổng chi phí (TC)     : %,.0f VNĐ%n", sol.TC);
        System.out.printf("  Chi phí cố định       : %,.0f VNĐ%n", sol.Cfixed);
        System.out.printf("  Chi phí vận hành      : %,.0f VNĐ%n", sol.Coperational);
        System.out.printf("  Phạt chất lượng nhiệt  : %,.0f VNĐ%n", sol.Cpenalty);

        // ---------------------------------------------------------------
        // 7. XUẤT FILE EXCEL
        // ---------------------------------------------------------------
        ExcelExporter.exportSolutionToExcel(sol, cfg, solver.convergenceHistory);

        System.out.println("===============================================================");
        System.out.println("                HOÀN THÀNH TỐI ƯU HÓA DA                       ");
        System.out.println("===============================================================");
    }
}

// -----------------------------------------------------------------------
// Lớp hỗ trợ: DA_HMA — Thực thi thuật toán DA đầy đủ với lịch sử hội tụ
// -----------------------------------------------------------------------
class DA_HMA {
    // Các trường trạng thái của DA
    double[]   lb, ub;
    double[]   r;              // Bán kính lân cận từng chiều
    double[]   Delta_max;      // Bước nhảy tối đa cho phép
    double     Food_fitness;   // Giá trị mục tiêu tốt nhất (nguồn thức ăn)
    double[]   Food_pos;       // Vị trí nguồn thức ăn
    double     Enemy_fitness;  // Giá trị mục tiêu tệ nhất (kẻ thù)
    double[]   Enemy_pos;      // Vị trí kẻ thù
    double[][] X;              // Vị trí các con chuồn chuồn
    double[]   Fitness;        // Giá trị hàm mục tiêu
    double[][] DeltaX;         // Vector bước di chuyển (vận tốc)
    int        dim;
    int        SearchAgents_no;
    int        Max_iteration;
    double     inf = 10E+50;

    // Các trường kết quả
    public double   Best_score;
    public double[] Best_pos;
    public double[] convergenceHistory;

    f_xj fobj;

    public DA_HMA(f_xj fobj, double[] lb, double[] ub, int maxIter, int popSize) {
        this.fobj              = fobj;
        this.lb                = lb;
        this.ub                = ub;
        this.Max_iteration     = maxIter;
        this.SearchAgents_no   = popSize;
        this.dim               = ub.length;

        r            = new double[dim];
        Delta_max    = new double[dim];
        Food_pos     = new double[dim];
        Enemy_pos    = new double[dim];
        X            = new double[popSize][dim];
        Fitness      = new double[popSize];
        DeltaX       = new double[popSize][dim];
        Food_fitness  = inf;
        Enemy_fitness = -inf;

        convergenceHistory = new double[maxIter + 1];
        Best_pos           = new double[dim];
        Best_score         = Double.MAX_VALUE;
    }

    // Khởi tạo ban đầu
    void init() {
        // Delta_max = (ub - lb) / 10
        for (int i = 0; i < dim; i++) {
            Delta_max[i] = (ub[i] - lb[i]) / 10.0;
        }
        // Khởi tạo vị trí ngẫu nhiên ban đầu
        for (int i = 0; i < SearchAgents_no; i++) {
            for (int j = 0; j < dim; j++) {
                X[i][j]      = lb[j] + (ub[j] - lb[j]) * Math.random();
                DeltaX[i][j] = lb[j] + (ub[j] - lb[j]) * Math.random();
            }
        }
    }

    // Vòng lặp chính của thuật toán DA
    public void solution() throws Exception {
        init();

        for (int iter = 1; iter <= Max_iteration; iter++) {

            // Cập nhật bán kính lân cận r (mở rộng từ 25% đến 75% không gian)
            for (int i = 0; i < dim; i++) {
                r[i] = (ub[i] - lb[i]) / 4.0 + ((ub[i] - lb[i]) * ((double) iter / Max_iteration) * 2.0);
            }

            // Trọng số quán tính w: 0.9 -> 0.4
            double w     = 0.9 - (double) iter * ((0.9 - 0.4) / Max_iteration);
            // Điều khiển trọng số hành vi my_c: 0.1 -> 0 (ở nửa đầu ca), sau đó giữ 0
            double my_c  = 0.1 - (double) iter * ((0.1 - 0.0) / ((double) Max_iteration / 2.0));
            if (my_c < 0) my_c = 0;

            // Trọng số ngẫu nhiên cho từng hành vi
            double s         = 2 * Math.random() * my_c;  // Trọng số Phân tách (Separation)
            double alignment = 2 * Math.random() * my_c;  // Trọng số Căn chỉnh (Alignment)
            double c         = 2 * Math.random() * my_c;  // Trọng số Tụ hội (Cohesion)
            double f         = 2 * Math.random();          // Trọng số Thu hút thức ăn (Food)
            double e         = my_c;                       // Trọng số Xua đuổi kẻ thù (Enemy)

            // ---- Đánh giá tất cả chuồn chuồn; cập nhật thức ăn và kẻ thù ----
            for (int i = 0; i < SearchAgents_no; i++) {
                Fitness[i] = fobj.func(X[i]);

                // Cập nhật nguồn thức ăn (phương án tốt nhất)
                if (Fitness[i] < Food_fitness) {
                    Food_fitness = Fitness[i];
                    System.arraycopy(X[i], 0, Food_pos, 0, dim);
                }

                // Cập nhật kẻ thù (phương án tệ nhất trong biên)
                if (Fitness[i] > Enemy_fitness) {
                    if (lt(X[i], ub) && gt(X[i], lb)) {
                        Enemy_fitness = Fitness[i];
                        System.arraycopy(X[i], 0, Enemy_pos, 0, dim);
                    }
                }
            }

            // ---- Cập nhật bước di chuyển và vị trí từng con chuồn chuồn ----
            for (int i = 0; i < SearchAgents_no; i++) {
                int neighbours_no = 0;
                double[][] Neighbours_DeltaX = new double[SearchAgents_no][dim];
                double[][] Neighbours_X      = new double[SearchAgents_no][dim];
                int index = -1;

                // Tìm các phương án lân cận trong bán kính r
                double[] zero = new double[dim];
                for (int j = 0; j < SearchAgents_no; j++) {
                    double[] dist = distance(X[i], X[j]);
                    if (lte(dist, r) && ne(dist, zero)) {
                        index++;
                        neighbours_no++;
                        Neighbours_DeltaX[index] = DeltaX[j].clone();
                        Neighbours_X[index]      = X[j].clone();
                    }
                }

                // -- Phân tách (Eq. 3.1): tránh va chạm dồn cục --
                double[] S = new double[dim];
                if (neighbours_no > 1) {
                    for (int k = 0; k < neighbours_no; k++) {
                        for (int j = 0; j < dim; j++) {
                            S[j] += (Neighbours_X[k][j] - X[i][j]);
                        }
                    }
                    for (int j = 0; j < dim; j++) S[j] = -S[j];
                }

                // -- Căn chỉnh (Eq. 3.2): đồng bộ vận tốc với hàng xóm --
                double[] A = new double[dim];
                if (neighbours_no > 1) {
                    for (int j = 0; j < dim; j++) {
                        double sum = 0;
                        for (int k = 0; k < neighbours_no; k++) sum += Neighbours_DeltaX[k][j];
                        A[j] = sum / neighbours_no;
                    }
                } else {
                    A = DeltaX[i].clone();
                }

                // -- Tụ hội (Eq. 3.3): di chuyển về tâm hàng xóm --
                double[] C_temp = new double[dim];
                double[] C      = new double[dim];
                if (neighbours_no > 1) {
                    for (int j = 0; j < dim; j++) {
                        double sum = 0;
                        for (int k = 0; k < neighbours_no; k++) sum += Neighbours_X[k][j];
                        C_temp[j] = sum / neighbours_no;
                    }
                } else {
                    C_temp = X[i].clone();
                }
                for (int j = 0; j < dim; j++) C[j] = C_temp[j] - X[i][j];

                // -- Hướng về thức ăn (Eq. 3.4): di chuyển về mồi --
                double[] F           = new double[dim];
                double[] Dist2Food   = distance(X[i], Food_pos);
                if (lte(Dist2Food, r)) {
                    for (int j = 0; j < dim; j++) F[j] = Food_pos[j] - X[i][j];
                }

                // -- Tránh xa kẻ thù (Eq. 3.5): tránh xa kẻ thù --
                double[] Enemy       = new double[dim];
                double[] Dist2Enemy  = distance(X[i], Enemy_pos);
                if (lte(Dist2Enemy, r)) {
                    for (int j = 0; j < dim; j++) Enemy[j] = Enemy_pos[j] + X[i][j];
                }

                // Xử lý vượt biên không gian
                for (int j = 0; j < dim; j++) {
                    if (X[i][j] > ub[j]) { X[i][j] = lb[j]; DeltaX[i][j] = Math.random(); }
                    if (X[i][j] < lb[j]) { X[i][j] = ub[j]; DeltaX[i][j] = Math.random(); }
                }

                // -- Cập nhật bước nhảy DeltaX và vị trí X --
                if (any_gt(Dist2Food, r)) {
                    // Không ở gần thức ăn: cập nhật theo bầy hoặc bay Lévy
                    if (neighbours_no > 1) {
                        // Cập nhật hành vi bầy đàn
                        for (int j = 0; j < dim; j++) {
                            DeltaX[i][j] = w * DeltaX[i][j]
                                         + Math.random() * A[j]
                                         + Math.random() * C[j]
                                         + Math.random() * S[j];
                            DeltaX[i][j] = Math.max(-Delta_max[j], Math.min(Delta_max[j], DeltaX[i][j]));
                            X[i][j]      = X[i][j] + DeltaX[i][j];
                        }
                    } else {
                        // Chuyến bay Lévy (Eq. 3.8) — khám phá khi bị cô lập
                        double[] levy = Levy(dim);
                        for (int j = 0; j < dim; j++) {
                            X[i][j]      = X[i][j] + levy[j] * X[i][j];
                            DeltaX[i][j] = 0;
                        }
                    }
                } else {
                    // Gần thức ăn: cập nhật đầy đủ trọng số các hành vi
                    for (int j = 0; j < dim; j++) {
                        DeltaX[i][j] = alignment * A[j] + c * C[j] + s * S[j]
                                     + f * F[j] + e * Enemy[j]
                                     + w * DeltaX[i][j];
                        DeltaX[i][j] = Math.max(-Delta_max[j], Math.min(Delta_max[j], DeltaX[i][j]));
                        X[i][j]      = X[i][j] + DeltaX[i][j];
                    }
                }

                // Đưa vị trí về nằm trong khoảng biên [lb, ub]
                for (int j = 0; j < dim; j++) {
                    X[i][j] = Math.max(lb[j], Math.min(ub[j], X[i][j]));
                }
            }

            // Ghi nhận nghiệm tốt nhất (thức ăn) cho vòng lặp này
            Best_score               = Food_fitness;
            Best_pos                 = Food_pos.clone();
            convergenceHistory[iter] = Best_score;
        }
    }

    // ---- Bước ngẫu nhiên chuyến bay Lévy (Eq. 3.10) ----
    double[] Levy(int d) {
        double beta  = 3.0 / 2.0;
        double sigma = Math.pow(
            Gamma.gamma(1.0 + beta) * Math.sin(Math.PI * beta / 2.0)
            / (Gamma.gamma((1.0 + beta) / 2.0) * beta * Math.pow(2.0, (beta - 1.0) / 2.0)),
            1.0 / beta
        );
        double[] step = new double[d];
        for (int i = 0; i < d; i++) {
            double u = Math.random() * sigma;
            double v = Math.random();
            step[i]  = 0.01 * u / Math.pow(Math.abs(v), 1.0 / beta);
        }
        return step;
    }

    // ---- Các hàm hỗ trợ so sánh toán học ----
    boolean gt(double[] x, double[] y) {
        for (int i = 0; i < x.length; i++) if (x[i] <= y[i]) return false;
        return true;
    }
    boolean lt(double[] x, double[] y) {
        for (int i = 0; i < x.length; i++) if (x[i] >= y[i]) return false;
        return true;
    }
    boolean lte(double[] x, double[] y) {
        for (int i = 0; i < x.length; i++) if (x[i] > y[i]) return false;
        return true;
    }
    boolean ne(double[] x, double[] y) {
        for (int i = 0; i < x.length; i++) if (x[i] == y[i]) return false;
        return true;
    }
    boolean any_gt(double[] x, double[] y) {
        for (int i = 0; i < x.length; i++) if (x[i] > y[i]) return true;
        return false;
    }
    double[] distance(double[] a, double[] b) {
        double[] d = new double[a.length];
        for (int i = 0; i < a.length; i++) d[i] = Math.sqrt((a[i] - b[i]) * (a[i] - b[i]));
        return d;
    }
}
