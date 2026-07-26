package com.test;

import com.hma.config.HMAConfig;
import com.hma.config.SampleData;
import com.hma.cost.CostCalculator;
import com.hma.fitness.HMAFitness;
import com.hma.model.HMASolution;
import com.hma.constraint.RepairOperator;
import org.apache.commons.math3.stat.inference.MannWhitneyUTest;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;

/**
 * AlgorithmComparison — So sánh hiệu năng 3 thuật toán trên bài toán HMA
 * 
 * Chương trình thực hiện:
 *   1. Chạy 3 thuật toán (DA-GWO, GWO, DA) × 30 lần chạy độc lập × 300 vòng lặp
 *   2. Tính thống kê: Best, Average, Std, Worst cho mỗi thuật toán
 *   3. Kiểm định Mann-Whitney U Test — tính p-value so sánh từng cặp
 *   4. Xuất toàn bộ kết quả ra file Excel: tonghop_hma.xlsx
 *      - Sheet "TongHop": Bảng tổng hợp Best/Avg/Std/Worst/Time + p-value
 *      - Sheet "ChiTiet_DAGWO": 30 kết quả TC của DA-GWO
 *      - Sheet "ChiTiet_GWO": 30 kết quả TC của GWO
 *      - Sheet "ChiTiet_DA": 30 kết quả TC của DA
 */
public class AlgorithmComparison {

    static final int RUNS = 30;        // Số lần chạy độc lập (chuẩn NCKH)
    static final int MAX_ITER = 300;   // Số vòng lặp mỗi lần chạy
    static final int POP_SIZE = 40;    // Kích thước quần thể
    static final String EXCEL_PATH = "D:\\DA_GWO\\tonghop_hma.xlsx";

    public static void main(String[] args) throws Exception {
        HMAConfig cfg = SampleData.getSampleConfig();

        double[] lb = new double[cfg.dim];
        double[] ub = new double[cfg.dim];
        for (int i = 0; i < cfg.dim; i++) {
            lb[i] = 0.0;
            ub[i] = 1.0;
        }

        f_xj fobj = new HMAFitness(cfg);
        CostCalculator calc = new CostCalculator(cfg);

        double[] daGwoTCs = new double[RUNS];
        double[] gwoTCs   = new double[RUNS];
        double[] daTCs    = new double[RUNS];

        long daGwoTime = 0, gwoTime = 0, daTime = 0;

        System.out.println("══════════════════════════════════════════════════════════════");
        System.out.println("   SO SÁNH THUẬT TOÁN CHO BÀI TOÁN VẬN CHUYỂN HMA");
        System.out.println("══════════════════════════════════════════════════════════════");
        System.out.println("Cấu hình: " + RUNS + " lần chạy × " + MAX_ITER + " vòng lặp × " + POP_SIZE + " agents");
        System.out.println("Bài toán:  N=" + cfg.N + " công trường, T=" + cfg.T + " xe, Mk=" + cfg.Mk + " chuyến/xe");
        System.out.println("Dimension: " + cfg.dim);
        System.out.println("──────────────────────────────────────────────────────────────\n");

        // ═══════════════════════════════════════
        // 1. Chạy DA-GWO Hybrid (30 lần)
        // ═══════════════════════════════════════
        System.out.print("[1/3] DA-GWO Hybrid: ");
        for (int r = 0; r < RUNS; r++) {
            long start = System.currentTimeMillis();
            DA_GWO solver = new DA_GWO(fobj, lb, ub, MAX_ITER, POP_SIZE);
            solver.solution();
            daGwoTime += (System.currentTimeMillis() - start);

            HMASolution sol = HMASolution.decode(solver.Best_pos, cfg);
            RepairOperator.repairAll(sol, cfg);
            calc.calcTotalCost(sol);
            daGwoTCs[r] = sol.TC;
            System.out.print("█");
        }
        System.out.println(" Done.");

        // ═══════════════════════════════════════
        // 2. Chạy GWO (30 lần)
        // ═══════════════════════════════════════
        System.out.print("[2/3] GWO:           ");
        for (int r = 0; r < RUNS; r++) {
            long start = System.currentTimeMillis();
            GWO solver = new GWO(fobj, lb, ub, MAX_ITER, POP_SIZE);
            solver.execute();
            gwoTime += (System.currentTimeMillis() - start);

            HMASolution sol = HMASolution.decode(solver.getBestArray(), cfg);
            RepairOperator.repairAll(sol, cfg);
            calc.calcTotalCost(sol);
            gwoTCs[r] = sol.TC;
            System.out.print("█");
        }
        System.out.println(" Done.");

        // ═══════════════════════════════════════
        // 3. Chạy DA (30 lần)
        // ═══════════════════════════════════════
        System.out.print("[3/3] DA:            ");
        for (int r = 0; r < RUNS; r++) {
            long start = System.currentTimeMillis();
            DA solver = new DA(fobj, lb, ub, MAX_ITER, POP_SIZE);
            solver.solution();
            daTime += (System.currentTimeMillis() - start);

            HMASolution sol = HMASolution.decode(solver.Best_pos, cfg);
            RepairOperator.repairAll(sol, cfg);
            calc.calcTotalCost(sol);
            daTCs[r] = sol.TC;
            System.out.print("█");
        }
        System.out.println(" Done.\n");

        // ═══════════════════════════════════════
        // 4. Tính thống kê
        // ═══════════════════════════════════════
        double[] statsDAGWO = calcStats(daGwoTCs);
        double[] statsGWO   = calcStats(gwoTCs);
        double[] statsDA    = calcStats(daTCs);

        // ═══════════════════════════════════════
        // 5. Kiểm định Mann-Whitney U Test
        // ═══════════════════════════════════════
        MannWhitneyUTest mwTest = new MannWhitneyUTest();
        double pval_DAGWO_vs_GWO = mwTest.mannWhitneyUTest(daGwoTCs, gwoTCs);
        double pval_DAGWO_vs_DA  = mwTest.mannWhitneyUTest(daGwoTCs, daTCs);
        double pval_GWO_vs_DA    = mwTest.mannWhitneyUTest(gwoTCs, daTCs);

        // ═══════════════════════════════════════
        // 6. In kết quả ra console
        // ═══════════════════════════════════════
        System.out.println("══════════════════════════════════════════════════════════════");
        System.out.println("                     KẾT QUẢ THỐNG KÊ                        ");
        System.out.println("══════════════════════════════════════════════════════════════");
        System.out.printf("%-15s | %15s | %15s | %15s | %15s | %8s\n",
                "Thuật toán", "Best TC", "Avg TC", "Std", "Worst TC", "Time(s)");
        System.out.println("────────────────|─────────────────|─────────────────|─────────────────|─────────────────|─────────");
        printRow("DA-GWO Hybrid", statsDAGWO, daGwoTime);
        printRow("GWO",           statsGWO,   gwoTime);
        printRow("DA",            statsDA,    daTime);

        System.out.println("\n──────────────────────────────────────────────────────────────");
        System.out.println("                KIỂM ĐỊNH MANN-WHITNEY U TEST                ");
        System.out.println("──────────────────────────────────────────────────────────────");
        System.out.printf("  DA-GWO vs GWO:  p-value = %.6f  %s\n", pval_DAGWO_vs_GWO,
                pval_DAGWO_vs_GWO < 0.05 ? "(Có ý nghĩa thống kê ✓)" : "(Chưa đủ ý nghĩa ✗)");
        System.out.printf("  DA-GWO vs DA:   p-value = %.6f  %s\n", pval_DAGWO_vs_DA,
                pval_DAGWO_vs_DA < 0.05 ? "(Có ý nghĩa thống kê ✓)" : "(Chưa đủ ý nghĩa ✗)");
        System.out.printf("  GWO vs DA:      p-value = %.6f  %s\n", pval_GWO_vs_DA,
                pval_GWO_vs_DA < 0.05 ? "(Có ý nghĩa thống kê ✓)" : "(Chưa đủ ý nghĩa ✗)");
        System.out.println("══════════════════════════════════════════════════════════════\n");

        // ═══════════════════════════════════════
        // 7. Xuất toàn bộ ra Excel
        // ═══════════════════════════════════════
        exportToExcel(statsDAGWO, statsGWO, statsDA,
                      daGwoTCs, gwoTCs, daTCs,
                      daGwoTime, gwoTime, daTime,
                      pval_DAGWO_vs_GWO, pval_DAGWO_vs_DA, pval_GWO_vs_DA);
        System.out.println("Đã xuất kết quả ra file: " + EXCEL_PATH);
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPER METHODS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Tính thống kê: [0]=Best, [1]=Avg, [2]=Std, [3]=Worst
     */
    private static double[] calcStats(double[] values) {
        double min = Double.MAX_VALUE, max = Double.MIN_VALUE, sum = 0;
        for (double v : values) {
            if (v < min) min = v;
            if (v > max) max = v;
            sum += v;
        }
        double avg = sum / values.length;
        double variance = 0;
        for (double v : values) {
            variance += (v - avg) * (v - avg);
        }
        double std = Math.sqrt(variance / values.length);
        return new double[]{min, avg, std, max};
    }

    private static void printRow(String name, double[] stats, long totalTimeMs) {
        System.out.printf("%-15s | %,15.0f | %,15.0f | %,15.0f | %,15.0f | %8.2f\n",
                name, stats[0], stats[1], stats[2], stats[3],
                (totalTimeMs / 1000.0) / RUNS);
    }

    /**
     * Xuất toàn bộ kết quả ra file Excel tonghop_hma.xlsx
     */
    private static void exportToExcel(
            double[] statsDAGWO, double[] statsGWO, double[] statsDA,
            double[] daGwoTCs, double[] gwoTCs, double[] daTCs,
            long daGwoTime, long gwoTime, long daTime,
            double pval_DAGWO_vs_GWO, double pval_DAGWO_vs_DA, double pval_GWO_vs_DA
    ) throws Exception {
        Workbook wb = new XSSFWorkbook();

        // ── Style setup ──
        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());

        CellStyle headerStyle = wb.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle numStyle = wb.createCellStyle();
        DataFormat df = wb.createDataFormat();
        numStyle.setDataFormat(df.getFormat("#,##0"));

        // ════════════════════════════════════════
        //  Sheet 1: TongHop (Summary)
        // ════════════════════════════════════════
        Sheet s1 = wb.createSheet("TongHop");

        // Title
        Row titleRow = s1.createRow(0);
        titleRow.createCell(0).setCellValue("SO SÁNH THUẬT TOÁN CHO BÀI TOÁN VẬN CHUYỂN HMA");

        Row configRow = s1.createRow(1);
        configRow.createCell(0).setCellValue("Cấu hình: " + RUNS + " lần chạy × " + MAX_ITER + " vòng lặp × " + POP_SIZE + " agents");

        // Headers
        Row h = s1.createRow(3);
        String[] headers = {"Thuật toán", "Best TC (VNĐ)", "Average TC (VNĐ)", "Std Dev (VNĐ)", "Worst TC (VNĐ)", "Avg Time (s)"};
        for (int i = 0; i < headers.length; i++) {
            Cell c = h.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }

        // Data rows
        writeStatsRow(s1, 4, "DA-GWO Hybrid", statsDAGWO, daGwoTime, numStyle);
        writeStatsRow(s1, 5, "GWO",           statsGWO,   gwoTime,   numStyle);
        writeStatsRow(s1, 6, "DA",            statsDA,    daTime,    numStyle);

        // p-value section
        Row pTitle = s1.createRow(8);
        Cell ptCell = pTitle.createCell(0);
        ptCell.setCellValue("KIỂM ĐỊNH MANN-WHITNEY U TEST (p-value)");

        Row ph = s1.createRow(9);
        String[] pHeaders = {"Cặp so sánh", "p-value", "Kết luận (α=0.05)"};
        for (int i = 0; i < pHeaders.length; i++) {
            Cell c = ph.createCell(i);
            c.setCellValue(pHeaders[i]);
            c.setCellStyle(headerStyle);
        }

        writePvalueRow(s1, 10, "DA-GWO vs GWO", pval_DAGWO_vs_GWO);
        writePvalueRow(s1, 11, "DA-GWO vs DA",  pval_DAGWO_vs_DA);
        writePvalueRow(s1, 12, "GWO vs DA",     pval_GWO_vs_DA);

        for (int i = 0; i < headers.length; i++) s1.autoSizeColumn(i);

        // ════════════════════════════════════════
        //  Sheet 2-4: Chi tiết 30 kết quả mỗi thuật toán
        // ════════════════════════════════════════
        writeDetailSheet(wb, "ChiTiet_DAGWO", daGwoTCs, headerStyle, numStyle);
        writeDetailSheet(wb, "ChiTiet_GWO",   gwoTCs,   headerStyle, numStyle);
        writeDetailSheet(wb, "ChiTiet_DA",    daTCs,    headerStyle, numStyle);

        // Save
        try (FileOutputStream fos = new FileOutputStream(EXCEL_PATH)) {
            wb.write(fos);
        }
        wb.close();
    }

    private static void writeStatsRow(Sheet s, int rowIdx, String name, double[] stats, long timeMs, CellStyle numStyle) {
        Row r = s.createRow(rowIdx);
        r.createCell(0).setCellValue(name);
        for (int i = 0; i < 4; i++) {
            Cell c = r.createCell(i + 1);
            c.setCellValue(stats[i]);
            c.setCellStyle(numStyle);
        }
        r.createCell(5).setCellValue(Math.round((timeMs / 1000.0 / RUNS) * 100.0) / 100.0);
    }

    private static void writePvalueRow(Sheet s, int rowIdx, String pair, double pval) {
        Row r = s.createRow(rowIdx);
        r.createCell(0).setCellValue(pair);
        r.createCell(1).setCellValue(pval);
        r.createCell(2).setCellValue(pval < 0.05 ? "Có ý nghĩa thống kê (p < 0.05)" : "Chưa đủ ý nghĩa thống kê");
    }

    private static void writeDetailSheet(Workbook wb, String sheetName, double[] values,
                                         CellStyle headerStyle, CellStyle numStyle) {
        Sheet s = wb.createSheet(sheetName);
        Row h = s.createRow(0);
        Cell c1 = h.createCell(0); c1.setCellValue("Lần chạy"); c1.setCellStyle(headerStyle);
        Cell c2 = h.createCell(1); c2.setCellValue("TC (VNĐ)"); c2.setCellStyle(headerStyle);

        for (int i = 0; i < values.length; i++) {
            Row r = s.createRow(i + 1);
            r.createCell(0).setCellValue(i + 1);
            Cell vc = r.createCell(1);
            vc.setCellValue(values[i]);
            vc.setCellStyle(numStyle);
        }
        s.autoSizeColumn(0);
        s.autoSizeColumn(1);
    }
}
