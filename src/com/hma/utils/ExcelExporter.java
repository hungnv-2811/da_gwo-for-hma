package com.hma.utils;

import com.hma.config.HMAConfig;
import com.hma.cost.CostCalculator;
import com.hma.model.HMASolution;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ExcelExporter {
    private static final String EXCEL_FILE_PATH = "D:\\DA_GWO\\ketqua_hma.xlsx";
    
    public static void exportSolutionToExcel(HMASolution sol, HMAConfig cfg, double[] convergenceHistory) throws IOException {
        Workbook workbook;
        File file = new File(EXCEL_FILE_PATH);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                workbook = new XSSFWorkbook(fis);
            }
        } else {
            workbook = new XSSFWorkbook();
        }
        
        // Tạo mới hoặc làm sạch Sheet lịch trình
        String sheetName = "LichTrinhHMA";
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet != null) {
            int index = workbook.getSheetIndex(sheet);
            workbook.removeSheetAt(index);
        }
        sheet = workbook.createSheet(sheetName);
        
        CostCalculator calc = new CostCalculator(cfg);
        calc.calcTotalCost(sol);
        
        // Thiết lập định dạng tiêu đề
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        
        // Thông tin chung
        int rowIdx = 0;
        Row r = sheet.createRow(rowIdx++);
        Cell titleCell = r.createCell(0);
        titleCell.setCellValue("PHƯƠNG ÁN VẬN CHUYỂN HMA TỐI ƯU");
        
        sheet.createRow(rowIdx++).createCell(0).setCellValue("Tổng chi phí (TC): " + String.format("%,.0f", sol.TC) + " VNĐ");
        sheet.createRow(rowIdx++).createCell(0).setCellValue("  - Chi phí cố định (Cfixed): " + String.format("%,.0f", sol.Cfixed) + " VNĐ");
        sheet.createRow(rowIdx++).createCell(0).setCellValue("  - Chi phí vận hành (Coperational): " + String.format("%,.0f", sol.Coperational) + " VNĐ");
        sheet.createRow(rowIdx++).createCell(0).setCellValue("  - Chi phí phạt nhiệt độ (Cpenalty): " + String.format("%,.0f", sol.Cpenalty) + " VNĐ");
        rowIdx++; // Dòng trống
        
        // Tiêu đề bảng
        Row headerRow = sheet.createRow(rowIdx++);
        String[] columns = {"Mã phương tiện", "Chuyến số", "Công trường đích", "Khoảng cách 1 chiều (km)", "Thời điểm xuất phát (Phút)", "Nhiệt độ khi đến (°C)", "Chi phí chuyến (VNĐ)"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Ghi dữ liệu các chuyến xe
        for (int k = 0; k < cfg.T; k++) {
            if (sol.zk[k] == 0) continue;
            int tripNum = 1;
            for (int m = 0; m < cfg.Mk; m++) {
                int site = -1;
                for (int i = 0; i < cfg.N; i++) {
                    if (sol.xikm[i][k][m] == 1) {
                        site = i;
                        break;
                    }
                }
                if (site != -1) {
                    Row dataRow = sheet.createRow(rowIdx++);
                    dataRow.createCell(0).setCellValue("Xe " + (k + 1));
                    dataRow.createCell(1).setCellValue("Chuyến " + tripNum++);
                    dataRow.createCell(2).setCellValue("Công trường " + (site + 1));
                    dataRow.createCell(3).setCellValue(cfg.doi[site]);
                    dataRow.createCell(4).setCellValue(sol.txp_km[k][m]);
                    dataRow.createCell(5).setCellValue(calc.calcTemperature(site));
                    dataRow.createCell(6).setCellValue(2.0 * cfg.doi[site] * cfg.coi[site]);
                }
            }
        }
        
        // Tự động căn chỉnh độ rộng cột
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }
        
        // Tạo mới hoặc làm sạch Sheet "HoiTu" cho phân tích đường cong hội tụ
        if (convergenceHistory != null) {
            String convSheetName = "HoiTu";
            Sheet convSheet = workbook.getSheet(convSheetName);
            if (convSheet != null) {
                int index = workbook.getSheetIndex(convSheet);
                workbook.removeSheetAt(index);
            }
            convSheet = workbook.createSheet(convSheetName);
            
            Row convHeader = convSheet.createRow(0);
            Cell cellIter = convHeader.createCell(0);
            cellIter.setCellValue("Vòng lặp (Iteration)");
            cellIter.setCellStyle(headerStyle);
            
            Cell cellCost = convHeader.createCell(1);
            cellCost.setCellValue("Chi phí tốt nhất (TC)");
            cellCost.setCellStyle(headerStyle);
            
            int convRowIdx = 1;
            for (int iter = 1; iter < convergenceHistory.length; iter++) {
                Row rConv = convSheet.createRow(convRowIdx++);
                rConv.createCell(0).setCellValue(iter);
                rConv.createCell(1).setCellValue(convergenceHistory[iter]);
            }
            convSheet.autoSizeColumn(0);
            convSheet.autoSizeColumn(1);
        }
        
        // Lưu file Excel
        try (FileOutputStream fos = new FileOutputStream(file)) {
            workbook.write(fos);
        }
        workbook.close();
        System.out.println("Đã xuất lịch trình tối ưu HMA và lịch sử hội tụ ra file: " + EXCEL_FILE_PATH);
    }
}
