package com.test;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ExcelUtils {
    
    private static final String EXCEL_FILE_PATH = "D:\\DA_GWO\\tonghop.xlsx";

    private static Cell getOrCreateCell(Sheet sheet, int rowIndex, int colIndex) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            row = sheet.createRow(rowIndex);
        }
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            cell = row.createCell(colIndex);
        }
        return cell;
    }

    private static Workbook getOrCreateWorkbook(File file) throws IOException {
        if (file.exists()) {
            FileInputStream inputStream = new FileInputStream(file);
            try {
                return new XSSFWorkbook(inputStream);
            } finally {
                inputStream.close();
            }
        } else {
            return new XSSFWorkbook();
        }
    }

    private static Sheet getOrCreateSheet(Workbook workbook, String sheetName) {
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            sheet = workbook.createSheet(sheetName);
        }
        return sheet;
    }

    public static void fillAvgAndStdToExcel(int N, int max_iteration, int times, double avg[], double std[], int startColumn) throws IOException {
        File file = new File(EXCEL_FILE_PATH);
        Workbook workbook = getOrCreateWorkbook(file);
        Sheet sheet = getOrCreateSheet(workbook, "Sheet1");

        Cell cell_N = getOrCreateCell(sheet, 2, 1);
        Cell cell_Iter = getOrCreateCell(sheet, 3, 1);
        Cell cell_Times = getOrCreateCell(sheet, 4, 1);
        cell_N.setCellValue("N = " + N);
        cell_Iter.setCellValue("Max iteration = " + max_iteration);
        cell_Times.setCellValue(times + " times");

        int rowIndex = 9;
        for (int i = 0; i < 23; i++) {
            Cell avg_cell = getOrCreateCell(sheet, rowIndex + i, 1 + startColumn);
            Cell std_cell = getOrCreateCell(sheet, rowIndex + i, 2 + startColumn);
            avg_cell.setCellValue(avg[i]);
            std_cell.setCellValue(std[i]);
        }

        FileOutputStream out = new FileOutputStream(file);
        workbook.write(out);
        out.close();
        workbook.close();
        System.out.println("Write to excel done!");
    }

    public static void fillBestScoreToExcel(double [][] bestScore, int numOfF, int times, int startRow) throws IOException {
        File file = new File(EXCEL_FILE_PATH);
        Workbook workbook = getOrCreateWorkbook(file);
        Sheet sheet = getOrCreateSheet(workbook, "Sheet2");

        int rowIndex = 5 + startRow;

        for (int i = 0; i < numOfF; i++) {
            for (int j = 0; j < times; j++) {
                Cell cell = getOrCreateCell(sheet, rowIndex + i, j + 2);
                cell.setCellValue(bestScore[i][j]);
            }
        }

        FileOutputStream out = new FileOutputStream(file);
        workbook.write(out);
        out.close();
        workbook.close();
        System.out.println("Write to excel done!");
    }

    public static void fillPValueToExcel(double [] pvalue_DAGWO_DA, double [] pvalue_DAGWO_GWO, double [] pvalue_GWO_DA, int numOfF) throws IOException {
        File file = new File(EXCEL_FILE_PATH);
        Workbook workbook = getOrCreateWorkbook(file);
        Sheet sheet = getOrCreateSheet(workbook, "Sheet1");

        int rowIndex = 38;

        for (int i = 0; i < numOfF; i++) {
            Cell cell1 = getOrCreateCell(sheet, rowIndex + i, 1);
            cell1.setCellValue(pvalue_DAGWO_DA[i]);

            Cell cell2 = getOrCreateCell(sheet, rowIndex + i, 4);
            cell2.setCellValue(pvalue_DAGWO_GWO[i]);

            Cell cell3 = getOrCreateCell(sheet, rowIndex + i, 7);
            cell3.setCellValue(pvalue_GWO_DA[i]);
        }

        FileOutputStream out = new FileOutputStream(file);
        workbook.write(out);
        out.close();
        workbook.close();
        System.out.println("Write to excel done!");
    }

    public static void fillForDrawFunctionToExcel(double [] Fmin, double [] Favg, double [] X_1, double [] X_2,
                                                  int startRowFmin, int startRowFavg, int startRowX1, int startRowX2,
                                                  int orderOfF, int maxIter) throws IOException {
        File file = new File(EXCEL_FILE_PATH);
        Workbook workbook = getOrCreateWorkbook(file);
        Sheet sheet = getOrCreateSheet(workbook, "Sheet3");

        int rowIndex = startRowFmin + orderOfF;

        for (int i = 0; i < maxIter; i++) {
            Cell cell = getOrCreateCell(sheet, rowIndex, 2 + i);
            cell.setCellValue(Fmin[i]);
        }

        FileOutputStream out = new FileOutputStream(file);
        workbook.write(out);
        out.close();
        workbook.close();
        System.out.println("Write to excel done!");
    }
}
