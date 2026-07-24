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
        
        // Reset or create sheet
        String sheetName = "LichTrinhHMA";
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet != null) {
            int index = workbook.getSheetIndex(sheet);
            workbook.removeSheetAt(index);
        }
        sheet = workbook.createSheet(sheetName);
        
        CostCalculator calc = new CostCalculator(cfg);
        calc.calcTotalCost(sol);
        
        // Style Setup
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        
        // General Information
        int rowIdx = 0;
        Row r = sheet.createRow(rowIdx++);
        Cell titleCell = r.createCell(0);
        titleCell.setCellValue("HMA TRANSPORTATION OPTIMIZATION PLAN");
        
        sheet.createRow(rowIdx++).createCell(0).setCellValue("Total Cost (TC): " + String.format("%,.0f", sol.TC) + " VND");
        sheet.createRow(rowIdx++).createCell(0).setCellValue("  - Fixed Cost: " + String.format("%,.0f", sol.Cfixed) + " VND");
        sheet.createRow(rowIdx++).createCell(0).setCellValue("  - Operational Cost: " + String.format("%,.0f", sol.Coperational) + " VND");
        sheet.createRow(rowIdx++).createCell(0).setCellValue("  - Temperature Penalty: " + String.format("%,.0f", sol.Cpenalty) + " VND");
        rowIdx++; // Empty space
        
        // Table Headers
        Row headerRow = sheet.createRow(rowIdx++);
        String[] columns = {"Vehicle ID", "Trip #", "Destination Site", "Distance One-Way (km)", "Departure Time (Min)", "Arrival Temperature (C)", "Trip Cost (VND)"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Trip records writing
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
                    dataRow.createCell(0).setCellValue("Vehicle " + (k + 1));
                    dataRow.createCell(1).setCellValue("Trip " + tripNum++);
                    dataRow.createCell(2).setCellValue("Site " + (site + 1));
                    dataRow.createCell(3).setCellValue(cfg.doi[site]);
                    dataRow.createCell(4).setCellValue(sol.txp_km[k][m]);
                    dataRow.createCell(5).setCellValue(calc.calcTemperature(site));
                    dataRow.createCell(6).setCellValue(2.0 * cfg.doi[site] * cfg.coi[site]);
                }
            }
        }
        
        // Autofit columns
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }
        
        // Create or Reset "HoiTu" sheet for convergence curve analysis
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
            cellIter.setCellValue("Iteration");
            cellIter.setCellStyle(headerStyle);
            
            Cell cellCost = convHeader.createCell(1);
            cellCost.setCellValue("Best Cost (TC)");
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
        
        // Save Excel file
        try (FileOutputStream fos = new FileOutputStream(file)) {
            workbook.write(fos);
            System.out.println("Exported HMA optimization schedule and convergence history successfully to: " + EXCEL_FILE_PATH);
        } catch (IOException e) {
            System.err.println("[CẢNH BÁO] Không thể ghi file Excel '" + EXCEL_FILE_PATH + "': " + e.getMessage());
            System.err.println("-> Hãy đóng file Excel nếu bạn đang mở nó.");
        }
        workbook.close();
    }
}
