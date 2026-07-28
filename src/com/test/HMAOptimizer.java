package com.test;

import com.hma.config.HMAConfig;
import com.hma.config.SampleData;
import com.hma.constraint.ConstraintChecker;
import com.hma.constraint.RepairOperator;
import com.hma.cost.CostCalculator;
import com.hma.fitness.HMAFitness;
import com.hma.model.HMASolution;
import com.hma.utils.ExcelExporter;
import com.hma.utils.SolutionPrinter;

/**
 * HMAOptimizer — Chương trình chính: Tối ưu hóa vận chuyển HMA bằng DA-GWO Hybrid
 *
 * Chức năng:
 *   1. Chạy thuật toán lai DA-GWO Hybrid trên bài toán vận chuyển bê tông nhựa nóng
 *   2. Decode vector tối ưu → phương án vận chuyển (xe, chuyến, thời gian, nhiệt độ)
 *   3. In phương án tối ưu chi tiết ra console
 *   4. Xuất lịch trình + đường cong hội tụ ra Excel (ketqua_hma.xlsx)
 *   5. Kiểm tra tất cả ràng buộc (6)-(11) của phương án
 *
 * Cách chạy:
 *   javac -cp "lib/*" -d out src/com/hma/**\/*.java src/com/test/*.java
 *   java -cp "out;lib/*" com.test.HMAOptimizer
 */
public class HMAOptimizer {
    public static void main(String[] args) throws Exception {
        // ═══════════════════════════════════════
        //  CẤU HÌNH BÀI TOÁN
        // ═══════════════════════════════════════
        HMAConfig cfg = SampleData.getSampleConfig();
        CostCalculator calc = new CostCalculator(cfg);
        ConstraintChecker checker = new ConstraintChecker(cfg);

        System.out.println("══════════════════════════════════════════════════════════════");
        System.out.println("   TỐI ƯU HÓA VẬN CHUYỂN BÊ TÔNG NHỰA NÓNG (HMA)");
        System.out.println("   Thuật toán: DA-GWO Hybrid (Dragonfly + Grey Wolf)");
        System.out.println("══════════════════════════════════════════════════════════════");
        System.out.println("Bài toán:");
        System.out.println("  - Số công trường (N)     = " + cfg.N);
        System.out.println("  - Số xe khả dụng (T)     = " + cfg.T);
        System.out.println("  - Chuyến tối đa/xe (Mk)  = " + cfg.Mk);
        System.out.println("  - Tải trọng xe (Q)       = " + cfg.Q + " tấn");
        System.out.println("  - Vận tốc (v)            = " + cfg.v + " km/h");
        System.out.println("  - Nhiệt độ xuất (To)     = " + cfg.To + " °C");
        System.out.println("  - Ca làm việc (T_ca)     = " + cfg.T_ca + " phút");
        System.out.println("  - Dimension              = " + cfg.dim);
        for (int i = 0; i < cfg.N; i++) {
            System.out.printf("  - Công trường %d: Di=%.1f tấn (cần %d chuyến), doi=%.1f km, coi=%,.0f VNĐ/km\n",
                    i + 1, cfg.Di[i], (int) Math.ceil(cfg.Di[i] / cfg.Q), cfg.doi[i], cfg.coi[i]);
        }

        // ═══════════════════════════════════════
        //  THIẾT LẬP THUẬT TOÁN
        // ═══════════════════════════════════════
        double[] lb = new double[cfg.dim];
        double[] ub = new double[cfg.dim];
        for (int i = 0; i < cfg.dim; i++) {
            lb[i] = 0.0;
            ub[i] = 1.0;
        }

        f_xj fobj = new HMAFitness(cfg);
        int maxIter = 300;
        int popSize = 40;

        System.out.println("\nThuật toán: PopSize=" + popSize + ", MaxIter=" + maxIter);
        System.out.println("──────────────────────────────────────────────────────────────");
        System.out.println("Đang chạy tối ưu hóa...\n");

        // ═══════════════════════════════════════
        //  CHẠY THUẬT TOÁN
        // ═══════════════════════════════════════
        long startTime = System.currentTimeMillis();
        DA_GWO hybrid = new DA_GWO(fobj, lb, ub, maxIter, popSize);
        hybrid.solution();
        long endTime = System.currentTimeMillis();

        double runTimeSeconds = (endTime - startTime) / 1000.0;

        // ═══════════════════════════════════════
        //  DECODE + REPAIR → PHƯƠNG ÁN
        // ═══════════════════════════════════════
        HMASolution sol = HMASolution.decode(hybrid.Best_pos, cfg);
        RepairOperator.repairAll(sol, cfg);
        calc.calcTotalCost(sol);

        // ═══════════════════════════════════════
        //  IN KẾT QUẢ
        // ═══════════════════════════════════════
        System.out.printf("Hoàn thành trong %.2f giây.\n\n", runTimeSeconds);

        // Đường cong hội tụ
        System.out.println("──────────────────────────────────────────────────────────────");
        System.out.println("  ĐƯỜNG CONG HỘI TỤ (Convergence Milestones)");
        System.out.println("──────────────────────────────────────────────────────────────");
        int[] milestones = {1, 25, 50, 100, 150, 200, 250, 300};
        for (int m : milestones) {
            if (m <= maxIter) {
                System.out.printf("  Vòng lặp %3d: %,15.0f VNĐ\n", m, hybrid.convergenceHistory[m]);
            }
        }

        // Phương án chi tiết
        System.out.println();
        SolutionPrinter.printSolution(sol, cfg);

        // Kiểm tra ràng buộc
        System.out.println("\n──────────────────────────────────────────────────────────────");
        System.out.println("  KIỂM TRA RÀNG BUỘC");
        System.out.println("──────────────────────────────────────────────────────────────");
        System.out.println("  (6) Toàn vẹn nhu cầu:      " + (checker.checkDemand(sol) ? "✓ ĐẠT" : "✗ VI PHẠM"));
        System.out.println("  (7) Giới hạn chuyến đi:     " + (checker.checkTripLimit(sol) ? "✓ ĐẠT" : "✗ VI PHẠM"));
        System.out.println("  (8) Thời gian tuần tự:      " + (checker.checkSequence(sol) ? "✓ ĐẠT" : "✗ VI PHẠM"));
        System.out.println("  (9) Nhiệt độ (T >= 120°C):  " + (sol.Cpenalty == 0 ? "✓ ĐẠT" : "✗ VI PHẠM (Cpenalty=" + sol.Cpenalty + ")"));
        System.out.println("  Shift limit (T_ca):         " + (checker.checkShiftLimit(sol) ? "✓ ĐẠT" : "✗ VI PHẠM"));
        System.out.println("  Tổng hợp:                   " + (checker.isFullyValid(sol) ? "✓ TẤT CẢ ĐẠT" : "✗ CÓ VI PHẠM"));
        System.out.println("══════════════════════════════════════════════════════════════\n");

        // ═══════════════════════════════════════
        //  XUẤT EXCEL
        // ═══════════════════════════════════════
        ExcelExporter.exportSolutionToExcel(sol, cfg, hybrid.convergenceHistory);
    }
}
