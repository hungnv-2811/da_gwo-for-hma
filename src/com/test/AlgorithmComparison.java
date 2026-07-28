package com.test;

import com.hma.config.HMAConfig;
import com.hma.config.SampleData;
import com.hma.cost.CostCalculator;
import com.hma.fitness.HMAFitness;
import com.hma.model.HMASolution;
import com.hma.constraint.RepairOperator;
import org.apache.commons.math3.stat.inference.MannWhitneyUTest;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.*;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Arrays;

/**
 * AlgorithmComparison — So sanh hieu nang 3 thuat toan tren bai toan HMA
 *
 * Chuong trinh thuc hien:
 *   3 quy mo (Small / Medium / Large)
 *   x 3 thuat toan (DA-GWO[nhieu ratio], GWO, DA)
 *   x GWO_RATIOS = {0.1, 0.2, 0.3, 0.4, 0.5} cho DA-GWO
 *   x 30 lan chay doc lap x MAX_ITER vong lap
 *
 * Ket qua xuat:
 *   - Excel tonghop_hma.xlsx (nhieu sheets: TongHop, HoiTu, ChiTiet, TyLe)
 *   - CSV convergence_<scale>.csv de ve bieu do PNG ben ngoai
 */
public class AlgorithmComparison {

    static final int    RUNS        = 30;
    static final int    MAX_ITER    = 300;
    static final int    POP_SIZE    = 40;
    static final double[] GWO_RATIOS = {0.1, 0.2, 0.3, 0.4, 0.5};

    // --- Duong dan xuat file ---
    // Ghi ra cung thu muc voi du an (cung cap duong dan tuyet doi neu can)
    static final String BASE_DIR   = System.getProperty("user.dir") + java.io.File.separator;
    static final String EXCEL_PATH = BASE_DIR + "tonghop_hma_comparison.xlsx";

    // --- Ten quy mo ---
    static final String[] SCALE_NAMES = {"Small", "Medium", "Large"};

    public static void main(String[] args) throws Exception {

        // Tao thu muc neu chua co
        new java.io.File(BASE_DIR).mkdirs();

        // Cau hinh 3 quy mo
        HMAConfig[] configs = {
            SampleData.getSmallConfig(),
            SampleData.getMediumConfig(),
            SampleData.getLargeConfig()
        };

        Workbook wb = new XSSFWorkbook();
        CellStyle headerStyle = createHeaderStyle(wb);
        CellStyle numStyle    = createNumStyle(wb);
        CellStyle pctStyle    = createPctStyle(wb);

        System.out.println("══════════════════════════════════════════════════════════════════════");
        System.out.println("    SO SANH THUAT TOAN DA-GWO / GWO / DA TREN 3 QUY MO BAI TOAN HMA");
        System.out.println("══════════════════════════════════════════════════════════════════════");
        System.out.printf("  Cau hinh: %d lan chay x %d vong lap x %d agents%n", RUNS, MAX_ITER, POP_SIZE);
        System.out.printf("  GWO_RATIOS thu nghiem: %s%n", Arrays.toString(GWO_RATIOS));
        System.out.println("══════════════════════════════════════════════════════════════════════\n");

        // ================================================================
        //  VONG LAP QUA 3 QUY MO
        // ================================================================
        for (int scaleIdx = 0; scaleIdx < configs.length; scaleIdx++) {
            HMAConfig cfg   = configs[scaleIdx];
            String scaleName = SCALE_NAMES[scaleIdx];

            double[] lb = new double[cfg.dim];
            double[] ub = new double[cfg.dim];
            Arrays.fill(ub, 1.0);

            f_xj       fobj = new HMAFitness(cfg);
            CostCalculator calc = new CostCalculator(cfg);

            System.out.println("──────────────────────────────────────────────────────────────────────");
            System.out.printf("  QUY MO: %s  (N=%d, T=%d, Mk=%d, dim=%d)%n",
                    scaleName, cfg.N, cfg.T, cfg.Mk, cfg.dim);
            System.out.println("──────────────────────────────────────────────────────────────────────");

            // ─── 1. CHAY DA-GWO VOI NHIEU RATIO ───
            int nRatios = GWO_RATIOS.length;
            double[][] daGwoTCs_byRatio    = new double[nRatios][RUNS];
            long[]     daGwoTime_byRatio   = new long[nRatios];
            double[][][] daGwoConv_byRatio = new double[nRatios][RUNS][MAX_ITER + 1];

            for (int ri = 0; ri < nRatios; ri++) {
                double ratio = GWO_RATIOS[ri];
                System.out.printf("  [DA-GWO ratio=%.1f]: ", ratio);
                for (int run = 0; run < RUNS; run++) {
                    long start = System.currentTimeMillis();
                    DA_GWO solver = new DA_GWO(fobj, lb, ub, MAX_ITER, POP_SIZE, ratio);
                    solver.solution();
                    daGwoTime_byRatio[ri] += (System.currentTimeMillis() - start);

                    HMASolution sol = HMASolution.decode(solver.Best_pos, cfg);
                    RepairOperator.repairAll(sol, cfg);
                    calc.calcTotalCost(sol);
                    daGwoTCs_byRatio[ri][run]   = sol.TC;
                    daGwoConv_byRatio[ri][run]  = solver.convergenceHistory.clone();
                    System.out.print("█");
                }
                System.out.println(" Done.");
            }

            // ─── 2. CHAY GWO ───
            double[]   gwoTCs   = new double[RUNS];
            long       gwoTime  = 0;
            double[][] gwoConv  = new double[RUNS][MAX_ITER + 1];

            System.out.print("  [GWO]:                ");
            for (int run = 0; run < RUNS; run++) {
                long start = System.currentTimeMillis();
                GWO solver = new GWO(fobj, lb, ub, MAX_ITER, POP_SIZE);
                solver.execute();
                gwoTime += (System.currentTimeMillis() - start);

                HMASolution sol = HMASolution.decode(solver.getBestArray(), cfg);
                RepairOperator.repairAll(sol, cfg);
                calc.calcTotalCost(sol);
                gwoTCs[run]  = sol.TC;
                // GWO convergenceHistory co kich thuoc maxiter (index 0..maxiter-1)
                // copy sang mang co kich thuoc MAX_ITER+1 de dong nhat
                double[] ch = solver.convergenceHistory;
                for (int it = 1; it < MAX_ITER && it < ch.length; it++) {
                    gwoConv[run][it] = ch[it];
                }
                System.out.print("█");
            }
            System.out.println(" Done.");

            // ─── 3. CHAY DA ───
            double[]   daTCs   = new double[RUNS];
            long       daTime  = 0;
            double[][] daConv  = new double[RUNS][MAX_ITER + 1];

            System.out.print("  [DA]:                 ");
            for (int run = 0; run < RUNS; run++) {
                long start = System.currentTimeMillis();
                DA solver = new DA(fobj, lb, ub, MAX_ITER, POP_SIZE);
                solver.solution();
                daTime += (System.currentTimeMillis() - start);

                HMASolution sol = HMASolution.decode(solver.Best_pos, cfg);
                RepairOperator.repairAll(sol, cfg);
                calc.calcTotalCost(sol);
                daTCs[run]  = sol.TC;
                daConv[run] = solver.convergenceHistory.clone();
                System.out.print("█");
            }
            System.out.println(" Done.\n");

            // ─── 4. TINH THONG KE ───
            double[][] statsDAGWO = new double[nRatios][];
            for (int ri = 0; ri < nRatios; ri++) {
                statsDAGWO[ri] = calcStats(daGwoTCs_byRatio[ri]);
            }
            double[] statsGWO = calcStats(gwoTCs);
            double[] statsDA  = calcStats(daTCs);

            // ─── 5. KIEM DINH MANN-WHITNEY (dung ratio toi uu nhat) ───
            int bestRatioIdx = findBestRatioIdx(statsDAGWO);
            double[] bestDaGwoTCs = daGwoTCs_byRatio[bestRatioIdx];

            MannWhitneyUTest mw = new MannWhitneyUTest();
            double pDG_G = mw.mannWhitneyUTest(bestDaGwoTCs, gwoTCs);
            double pDG_D = mw.mannWhitneyUTest(bestDaGwoTCs, daTCs);
            double pG_D  = mw.mannWhitneyUTest(gwoTCs, daTCs);

            // ─── 6. IN KET QUA CONSOLE ───
            printScaleResults(scaleName, cfg, GWO_RATIOS, statsDAGWO, statsGWO, statsDA,
                    daGwoTime_byRatio, gwoTime, daTime, bestRatioIdx,
                    pDG_G, pDG_D, pG_D);

            // ─── 7. XUAT EXCEL ───
            writeScaleSheets(wb, scaleName, cfg, GWO_RATIOS,
                    statsDAGWO, statsGWO, statsDA,
                    daGwoTCs_byRatio, gwoTCs, daTCs,
                    daGwoTime_byRatio, gwoTime, daTime,
                    daGwoConv_byRatio, gwoConv, daConv,
                    bestRatioIdx, pDG_G, pDG_D, pG_D,
                    headerStyle, numStyle, pctStyle);

            // ─── 8. XUAT CSV HOI TU ───
            exportConvergenceCsv(BASE_DIR, scaleName, GWO_RATIOS,
                    daGwoConv_byRatio, gwoConv, daConv);
        }

        // Ghi file Excel
        try (FileOutputStream fos = new FileOutputStream(EXCEL_PATH)) {
            wb.write(fos);
        }
        wb.close();

        System.out.println("\n══════════════════════════════════════════════════════════════════════");
        System.out.println("  Da xuat Excel: " + EXCEL_PATH);
        System.out.println("  Da xuat CSV hoi tu: " + BASE_DIR + "convergence_<Scale>.csv");
        System.out.println("══════════════════════════════════════════════════════════════════════");
    }

    // ================================================================
    //  EXCEL SHEETS
    // ================================================================

    private static void writeScaleSheets(
            Workbook wb, String scaleName, HMAConfig cfg, double[] ratios,
            double[][] statsDAGWO, double[] statsGWO, double[] statsDA,
            double[][] daGwoTCs_byRatio, double[] gwoTCs, double[] daTCs,
            long[] daGwoTime_byRatio, long gwoTime, long daTime,
            double[][][] daGwoConv_byRatio, double[][] gwoConv, double[][] daConv,
            int bestRatioIdx,
            double pDG_G, double pDG_D, double pG_D,
            CellStyle headerStyle, CellStyle numStyle, CellStyle pctStyle
    ) {
        // Sheet 1: TongHop_<Scale>
        writeTongHopSheet(wb, scaleName, cfg, ratios,
                statsDAGWO, statsGWO, statsDA,
                daGwoTime_byRatio, gwoTime, daTime,
                bestRatioIdx, pDG_G, pDG_D, pG_D,
                headerStyle, numStyle);

        // Sheet 2: TyLe_<Scale> — so sanh cac ratio
        writeTyLeSheet(wb, scaleName, ratios, statsDAGWO, daGwoTime_byRatio, headerStyle, numStyle);

        // Sheet 3: HoiTu_<Scale> — du lieu hoi tu trung binh
        writeHoiTuSheet(wb, scaleName, ratios, daGwoConv_byRatio, gwoConv, daConv, headerStyle);

        // Sheet 4-6: ChiTiet moi thuat toan (dung ratio tot nhat cho DA-GWO)
        writeDetailSheet(wb, "ChiTiet_DAGWO_" + scaleName,
                daGwoTCs_byRatio[bestRatioIdx], ratios[bestRatioIdx], headerStyle, numStyle);
        writeDetailSheet(wb, "ChiTiet_GWO_"   + scaleName, gwoTCs, -1, headerStyle, numStyle);
        writeDetailSheet(wb, "ChiTiet_DA_"    + scaleName, daTCs,  -1, headerStyle, numStyle);
    }

    private static void writeTongHopSheet(
            Workbook wb, String scaleName, HMAConfig cfg, double[] ratios,
            double[][] statsDAGWO, double[] statsGWO, double[] statsDA,
            long[] daGwoTime_byRatio, long gwoTime, long daTime,
            int bestRatioIdx,
            double pDG_G, double pDG_D, double pG_D,
            CellStyle headerStyle, CellStyle numStyle
    ) {
        Sheet s = wb.createSheet("TongHop_" + scaleName);

        int r = 0;
        setCellValue(s, r++, 0, "SO SANH THUAT TOAN — QUY MO: " + scaleName);
        setCellValue(s, r++, 0, String.format("N=%d, T=%d, Mk=%d, dim=%d | %d runs x %d iter x %d agents",
                cfg.N, cfg.T, cfg.Mk, cfg.dim, RUNS, MAX_ITER, POP_SIZE));
        r++;

        // Headers thong ke
        String[] hdr = {"Thuat toan", "gwoRatio", "Best TC", "Avg TC", "Std", "Worst TC", "Avg Time(s)"};
        writeHeaderRow(s, r++, hdr, headerStyle);

        // DA-GWO voi tung ratio
        int bestRatioIdx2 = bestRatioIdx; // highlight
        for (int ri = 0; ri < ratios.length; ri++) {
            boolean isBest = (ri == bestRatioIdx2);
            Row row = s.createRow(r++);
            row.createCell(0).setCellValue(isBest ? "DA-GWO [BEST]" : "DA-GWO");
            row.createCell(1).setCellValue(ratios[ri]);
            for (int ci = 0; ci < 4; ci++) {
                Cell c = row.createCell(ci + 2);
                c.setCellValue(statsDAGWO[ri][ci]);
                c.setCellStyle(numStyle);
            }
            row.createCell(6).setCellValue(
                    Math.round((daGwoTime_byRatio[ri] / 1000.0 / RUNS) * 100.0) / 100.0);
        }

        // GWO
        Row rowGWO = s.createRow(r++);
        rowGWO.createCell(0).setCellValue("GWO");
        rowGWO.createCell(1).setCellValue("N/A");
        for (int ci = 0; ci < 4; ci++) {
            Cell c = rowGWO.createCell(ci + 2);
            c.setCellValue(statsGWO[ci]);
            c.setCellStyle(numStyle);
        }
        rowGWO.createCell(6).setCellValue(
                Math.round((gwoTime / 1000.0 / RUNS) * 100.0) / 100.0);

        // DA
        Row rowDA = s.createRow(r++);
        rowDA.createCell(0).setCellValue("DA");
        rowDA.createCell(1).setCellValue("N/A");
        for (int ci = 0; ci < 4; ci++) {
            Cell c = rowDA.createCell(ci + 2);
            c.setCellValue(statsDA[ci]);
            c.setCellStyle(numStyle);
        }
        rowDA.createCell(6).setCellValue(
                Math.round((daTime / 1000.0 / RUNS) * 100.0) / 100.0);

        r++;
        // P-value
        setCellValue(s, r++, 0, "KIEM DINH MANN-WHITNEY U TEST (DA-GWO dung ratio tot nhat: " + ratios[bestRatioIdx] + ")");
        writeHeaderRow(s, r++, new String[]{"Cap so sanh", "p-value", "Ket luan (alpha=0.05)"}, headerStyle);
        writePvalRow(s, r++, "DA-GWO vs GWO", pDG_G);
        writePvalRow(s, r++, "DA-GWO vs DA",  pDG_D);
        writePvalRow(s, r,   "GWO vs DA",     pG_D);

        for (int ci = 0; ci < hdr.length; ci++) s.autoSizeColumn(ci);
    }

    private static void writeTyLeSheet(
            Workbook wb, String scaleName, double[] ratios,
            double[][] statsDAGWO, long[] daGwoTime_byRatio,
            CellStyle headerStyle, CellStyle numStyle
    ) {
        Sheet s = wb.createSheet("TyLe_" + scaleName);
        int r = 0;
        setCellValue(s, r++, 0, "SO SANH CAC TY LE GWO_RATIO — QUY MO: " + scaleName);
        r++;
        writeHeaderRow(s, r++,
                new String[]{"gwoRatio", "% GWO", "% DA", "Best TC", "Avg TC", "Std", "Worst TC", "Avg Time(s)"},
                headerStyle);

        for (int ri = 0; ri < ratios.length; ri++) {
            Row row = s.createRow(r++);
            row.createCell(0).setCellValue(ratios[ri]);
            row.createCell(1).setCellValue(String.format("%.0f%%", ratios[ri] * 100));
            row.createCell(2).setCellValue(String.format("%.0f%%", (1 - ratios[ri]) * 100));
            for (int ci = 0; ci < 4; ci++) {
                Cell c = row.createCell(ci + 3);
                c.setCellValue(statsDAGWO[ri][ci]);
                c.setCellStyle(numStyle);
            }
            row.createCell(7).setCellValue(
                    Math.round((daGwoTime_byRatio[ri] / 1000.0 / RUNS) * 100.0) / 100.0);
        }
        for (int ci = 0; ci < 8; ci++) s.autoSizeColumn(ci);
    }

    private static void writeHoiTuSheet(
            Workbook wb, String scaleName, double[] ratios,
            double[][][] daGwoConv_byRatio, double[][] gwoConv, double[][] daConv,
            CellStyle headerStyle
    ) {
        XSSFSheet s = (XSSFSheet) wb.createSheet("HoiTu_" + scaleName);

        // Tinh gia tri hoi tu trung binh theo iteration
        int nRatios = ratios.length;

        // Header row
        Row hdr = s.createRow(0);
        Cell c0 = hdr.createCell(0); c0.setCellValue("Iteration"); c0.setCellStyle(headerStyle);
        for (int ri = 0; ri < nRatios; ri++) {
            Cell c = hdr.createCell(ri + 1);
            c.setCellValue("DAGWO_r=" + ratios[ri]);
            c.setCellStyle(headerStyle);
        }
        Cell cGWO = hdr.createCell(nRatios + 1); cGWO.setCellValue("GWO"); cGWO.setCellStyle(headerStyle);
        Cell cDA  = hdr.createCell(nRatios + 2); cDA.setCellValue("DA");   cDA.setCellStyle(headerStyle);

        // Data: trung binh qua RUNS lan chay
        int step = Math.max(1, MAX_ITER / 100); // chi ghi toi da 100 diem de Excel nhe
        int rowIdx = 1;
        for (int it = 1; it <= MAX_ITER; it += step) {
            Row row = s.createRow(rowIdx++);
            row.createCell(0).setCellValue(it);

            for (int ri = 0; ri < nRatios; ri++) {
                double avg = 0;
                int count = 0;
                for (int run = 0; run < RUNS; run++) {
                    if (it < daGwoConv_byRatio[ri][run].length
                            && daGwoConv_byRatio[ri][run][it] > 0) {
                        avg += daGwoConv_byRatio[ri][run][it];
                        count++;
                    }
                }
                row.createCell(ri + 1).setCellValue(count > 0 ? avg / count : 0);
            }

            // GWO
            double avgGWO = 0; int cntG = 0;
            for (int run = 0; run < RUNS; run++) {
                if (it < gwoConv[run].length && gwoConv[run][it] > 0) {
                    avgGWO += gwoConv[run][it]; cntG++;
                }
            }
            row.createCell(nRatios + 1).setCellValue(cntG > 0 ? avgGWO / cntG : 0);

            // DA
            double avgDA = 0; int cntD = 0;
            for (int run = 0; run < RUNS; run++) {
                if (it < daConv[run].length && daConv[run][it] > 0) {
                    avgDA += daConv[run][it]; cntD++;
                }
            }
            row.createCell(nRatios + 2).setCellValue(cntD > 0 ? avgDA / cntD : 0);
        }

        // ─── Ve bieu do duong hoi tu truc tiep vao sheet ───
        addConvergenceChart(s, scaleName, ratios, 1, rowIdx - 1);
    }

    /**
     * Ve bieu do duong hoi tu (Line Chart) nhung vao XSSFSheet da co du lieu.
     *
     * @param sheet     Sheet HoiTu_<Scale> (du lieu tu hang dataFirst tro di)
     * @param scaleName Ten quy mo hien thi tren tieu de bieu do
     * @param ratios    Mang GWO_RATIOS (xac dinh so chuoi DA-GWO)
     * @param dataFirst Hang du lieu dau tien (0-indexed, thuong = 1 vi hang 0 la header)
     * @param dataLast  Hang du lieu cuoi  (0-indexed, inclusive)
     */
    private static void addConvergenceChart(
            XSSFSheet sheet, String scaleName, double[] ratios,
            int dataFirst, int dataLast) {

        int nRatios = ratios.length;
        // Dat bieu do phia duoi bang du lieu:
        // hang bat dau = dataLast + 3, chieu cao = 20 dong, chieu rong = (nRatios+5) cot
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(
                0, 0, 0, 0,
                0,              dataLast + 3,
                nRatios + 5,    dataLast + 23
        );

        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText("Duong hoi tu — " + scaleName);
        chart.setTitleOverlay(false);

        XDDFChartLegend legend = chart.getOrAddLegend();
        legend.setPosition(LegendPosition.BOTTOM);

        // Truc hoanh: Vong lap
        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        bottomAxis.setTitle("Vong lap (Iteration)");

        // Truc tung: Fitness trung binh
        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setTitle("Gia tri ham muc tieu TB");
        leftAxis.setCrosses(AxisCrosses.AUTO_ZERO);

        XDDFLineChartData lineData = (XDDFLineChartData)
                chart.createData(ChartTypes.LINE, bottomAxis, leftAxis);
        lineData.setVaryColors(false);

        // Truc X lay tu cot 0 (Iteration)
        XDDFNumericalDataSource<Double> xs = XDDFDataSourcesFactory.fromNumericCellRange(
                sheet, new CellRangeAddress(dataFirst, dataLast, 0, 0));

        // ─ Chuoi DA-GWO: moi ratio mot chuoi (cot 1 den nRatios) ─
        for (int ri = 0; ri < nRatios; ri++) {
            XDDFNumericalDataSource<Double> ys = XDDFDataSourcesFactory.fromNumericCellRange(
                    sheet, new CellRangeAddress(dataFirst, dataLast, ri + 1, ri + 1));
            XDDFLineChartData.Series ser = (XDDFLineChartData.Series) lineData.addSeries(xs, ys);
            ser.setTitle("DA-GWO r=" + ratios[ri], null);
            ser.setSmooth(false);
            ser.setMarkerStyle(MarkerStyle.NONE);
        }

        // ─ Chuoi GWO (cot nRatios+1) ─
        XDDFNumericalDataSource<Double> ysGWO = XDDFDataSourcesFactory.fromNumericCellRange(
                sheet, new CellRangeAddress(dataFirst, dataLast, nRatios + 1, nRatios + 1));
        XDDFLineChartData.Series serGWO = (XDDFLineChartData.Series) lineData.addSeries(xs, ysGWO);
        serGWO.setTitle("GWO", null);
        serGWO.setSmooth(false);
        serGWO.setMarkerStyle(MarkerStyle.NONE);

        // ─ Chuoi DA (cot nRatios+2) ─
        XDDFNumericalDataSource<Double> ysDA = XDDFDataSourcesFactory.fromNumericCellRange(
                sheet, new CellRangeAddress(dataFirst, dataLast, nRatios + 2, nRatios + 2));
        XDDFLineChartData.Series serDA = (XDDFLineChartData.Series) lineData.addSeries(xs, ysDA);
        serDA.setTitle("DA", null);
        serDA.setSmooth(false);
        serDA.setMarkerStyle(MarkerStyle.NONE);

        chart.plot(lineData);
    }

    private static void writeDetailSheet(
            Workbook wb, String sheetName, double[] tcs,
            double ratio, CellStyle headerStyle, CellStyle numStyle
    ) {
        Sheet s = wb.createSheet(sheetName);
        Row hdr = s.createRow(0);
        Cell c1 = hdr.createCell(0); c1.setCellValue("Lan chay");   c1.setCellStyle(headerStyle);
        Cell c2 = hdr.createCell(1); c2.setCellValue("TC (VND)");   c2.setCellStyle(headerStyle);
        if (ratio >= 0) {
            Cell c3 = hdr.createCell(2); c3.setCellValue("gwoRatio=" + ratio); c3.setCellStyle(headerStyle);
        }

        for (int i = 0; i < tcs.length; i++) {
            Row row = s.createRow(i + 1);
            row.createCell(0).setCellValue(i + 1);
            Cell vc = row.createCell(1);
            vc.setCellValue(tcs[i]);
            vc.setCellStyle(numStyle);
        }
        s.autoSizeColumn(0); s.autoSizeColumn(1);
    }

    // ================================================================
    //  EXPORT CSV HOI TU (de ve PNG bang Python/Excel ben ngoai)
    // ================================================================

    private static void exportConvergenceCsv(
            String baseDir, String scaleName, double[] ratios,
            double[][][] daGwoConv_byRatio, double[][] gwoConv, double[][] daConv
    ) throws Exception {
        String path = baseDir + "convergence_" + scaleName + ".csv";
        try (PrintWriter pw = new PrintWriter(path, "UTF-8")) {
            // Header
            pw.print("iteration");
            for (double r : ratios) pw.print(",DAGWO_r" + r);
            pw.print(",GWO,DA\n");

            // Data (moi iteration)
            int nRatios = ratios.length;
            for (int it = 1; it <= MAX_ITER; it++) {
                pw.print(it);
                for (int ri = 0; ri < nRatios; ri++) {
                    double avg = 0; int cnt = 0;
                    for (int run = 0; run < RUNS; run++) {
                        if (it < daGwoConv_byRatio[ri][run].length
                                && daGwoConv_byRatio[ri][run][it] > 0) {
                            avg += daGwoConv_byRatio[ri][run][it]; cnt++;
                        }
                    }
                    pw.print("," + (cnt > 0 ? avg / cnt : 0));
                }
                // GWO
                double avgGWO = 0; int cntG = 0;
                for (int run = 0; run < RUNS; run++) {
                    if (it < gwoConv[run].length && gwoConv[run][it] > 0) {
                        avgGWO += gwoConv[run][it]; cntG++;
                    }
                }
                pw.print("," + (cntG > 0 ? avgGWO / cntG : 0));

                // DA
                double avgDA = 0; int cntD = 0;
                for (int run = 0; run < RUNS; run++) {
                    if (it < daConv[run].length && daConv[run][it] > 0) {
                        avgDA += daConv[run][it]; cntD++;
                    }
                }
                pw.print("," + (cntD > 0 ? avgDA / cntD : 0));
                pw.print("\n");
            }
        }
        System.out.println("  [CSV] Xuat hoi tu: " + path);
    }

    // ================================================================
    //  HELPER — THONG KE & IN
    // ================================================================

    /** Tinh [Best, Avg, Std, Worst] */
    private static double[] calcStats(double[] values) {
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE, sum = 0;
        for (double v : values) {
            if (v < min) min = v;
            if (v > max) max = v;
            sum += v;
        }
        double avg = sum / values.length;
        double var = 0;
        for (double v : values) var += (v - avg) * (v - avg);
        return new double[]{min, avg, Math.sqrt(var / values.length), max};
    }

    /** Tim ratio co Best TC nho nhat */
    private static int findBestRatioIdx(double[][] statsArray) {
        int best = 0;
        for (int i = 1; i < statsArray.length; i++) {
            if (statsArray[i][0] < statsArray[best][0]) best = i; // so sanh Best TC
        }
        return best;
    }

    private static void printScaleResults(
            String scaleName, HMAConfig cfg,
            double[] ratios, double[][] statsDAGWO, double[] statsGWO, double[] statsDA,
            long[] daGwoTime_byRatio, long gwoTime, long daTime,
            int bestRatioIdx, double pDG_G, double pDG_D, double pG_D
    ) {
        System.out.printf("%n  ═══ KET QUA THONG KE: %s ═══%n", scaleName);
        System.out.printf("  %-22s | %15s | %15s | %15s | %15s | %10s%n",
                "Thuat toan", "Best TC", "Avg TC", "Std", "Worst TC", "Avg Time(s)");
        System.out.println("  ──────────────────────|─────────────────|─────────────────|─────────────────|─────────────────|───────────");

        for (int ri = 0; ri < ratios.length; ri++) {
            String name = String.format("DA-GWO[r=%.1f]%s", ratios[ri],
                    ri == bestRatioIdx ? " *" : "  ");
            double[] st = statsDAGWO[ri];
            System.out.printf("  %-22s | %,15.0f | %,15.0f | %,15.0f | %,15.0f | %10.2f%n",
                    name, st[0], st[1], st[2], st[3],
                    (daGwoTime_byRatio[ri] / 1000.0) / RUNS);
        }
        System.out.printf("  %-22s | %,15.0f | %,15.0f | %,15.0f | %,15.0f | %10.2f%n",
                "GWO", statsGWO[0], statsGWO[1], statsGWO[2], statsGWO[3],
                (gwoTime / 1000.0) / RUNS);
        System.out.printf("  %-22s | %,15.0f | %,15.0f | %,15.0f | %,15.0f | %10.2f%n",
                "DA", statsDA[0], statsDA[1], statsDA[2], statsDA[3],
                (daTime / 1000.0) / RUNS);

        System.out.printf("%n  KIEM DINH MANN-WHITNEY (DA-GWO dung ratio=%.1f):%n", ratios[bestRatioIdx]);
        printPval("DA-GWO vs GWO", pDG_G);
        printPval("DA-GWO vs DA",  pDG_D);
        printPval("GWO vs DA",     pG_D);
        System.out.println();
    }

    private static void printPval(String pair, double p) {
        System.out.printf("    %-18s: p=%.6f  %s%n", pair, p,
                p < 0.05 ? "(Co y nghia thong ke *)" : "(Chua du y nghia)");
    }

    // ================================================================
    //  HELPER — EXCEL CELLS
    // ================================================================

    private static void writeHeaderRow(Sheet s, int rowIdx, String[] headers, CellStyle style) {
        Row row = s.createRow(rowIdx);
        for (int i = 0; i < headers.length; i++) {
            Cell c = row.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(style);
        }
    }

    private static void writePvalRow(Sheet s, int rowIdx, String pair, double pval) {
        Row row = s.createRow(rowIdx);
        row.createCell(0).setCellValue(pair);
        row.createCell(1).setCellValue(pval);
        row.createCell(2).setCellValue(pval < 0.05
                ? "Co y nghia thong ke (p < 0.05)"
                : "Chua du y nghia thong ke");
    }

    private static void setCellValue(Sheet s, int row, int col, String value) {
        Row r = s.getRow(row);
        if (r == null) r = s.createRow(row);
        r.createCell(col).setCellValue(value);
    }

    private static CellStyle createHeaderStyle(Workbook wb) {
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());

        CellStyle style = wb.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private static CellStyle createNumStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        DataFormat df = wb.createDataFormat();
        style.setDataFormat(df.getFormat("#,##0"));
        return style;
    }

    private static CellStyle createPctStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        DataFormat df = wb.createDataFormat();
        style.setDataFormat(df.getFormat("0.00%"));
        return style;
    }
}
