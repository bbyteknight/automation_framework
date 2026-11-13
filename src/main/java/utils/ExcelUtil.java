package utils;

import exceptions.FrameworkException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Create (for test fixtures) and read {@code .xlsx} exported reports via Apache POI — sheet rows,
 * individual cells, row counts, and column lookups — so steps/tests can validate an exported
 * report's content directly, including against API/DB data.
 */
public final class ExcelUtil {

    private ExcelUtil() {
    }

    /** Creates an .xlsx workbook with one sheet: a header row, then one row per entry of {@code rows}. */
    public static File createExcel(String destPath, String sheetName, List<String> headers, List<List<String>> rows) {
        File destFile = new File(destPath);
        FileHelper.ensureDirectoryExists(destFile.getAbsoluteFile().getParent());

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);

            Row headerRow = sheet.createRow(0);
            for (int col = 0; col < headers.size(); col++) {
                headerRow.createCell(col).setCellValue(headers.get(col));
            }

            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                List<String> values = rows.get(rowIndex);
                for (int col = 0; col < values.size(); col++) {
                    row.createCell(col).setCellValue(values.get(col));
                }
            }

            try (FileOutputStream outputStream = new FileOutputStream(destFile)) {
                workbook.write(outputStream);
            }
            return destFile;
        } catch (IOException e) {
            throw new FrameworkException("❌ Failed to create Excel report: " + destPath, e);
        }
    }

    /** Reads just the header row of {@code sheetName}. */
    public static List<String> headers(String path, String sheetName) {
        return headerRow(openSheet(path, sheetName));
    }

    /** Reads every data row (excluding the header) of {@code sheetName}, keyed by header name. */
    public static List<Map<String, String>> readSheet(String path, String sheetName) {
        Sheet sheet = openSheet(path, sheetName);
        List<String> headers = headerRow(sheet);
        DataFormatter formatter = new DataFormatter();

        List<Map<String, String>> rows = new ArrayList<>();
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            Map<String, String> rowData = new LinkedHashMap<>();
            for (int col = 0; col < headers.size(); col++) {
                rowData.put(headers.get(col), formatter.formatCellValue(row.getCell(col)));
            }
            rows.add(rowData);
        }
        return rows;
    }

    /** Reads one cell by its 0-based data row index (excluding the header) and column name. */
    public static String readCell(String path, String sheetName, int dataRowIndex, String columnName) {
        Sheet sheet = openSheet(path, sheetName);
        List<String> headers = headerRow(sheet);
        int columnIndex = columnIndex(headers, columnName, path, sheetName);

        Row row = sheet.getRow(dataRowIndex + 1);
        if (row == null) {
            throw new FrameworkException("❗ No data row " + dataRowIndex + " in sheet \"" + sheetName + "\" of " + path);
        }
        return new DataFormatter().formatCellValue(row.getCell(columnIndex));
    }

    /** Number of data rows in the sheet, excluding the header row. */
    public static int rowCount(String path, String sheetName) {
        return openSheet(path, sheetName).getLastRowNum();
    }

    /** Every value in the named column, top to bottom, excluding the header. */
    public static List<String> columnValues(String path, String sheetName, String columnName) {
        return readSheet(path, sheetName).stream()
                .map(row -> row.get(columnName))
                .toList();
    }

    private static Sheet openSheet(String path, String sheetName) {
        try (FileInputStream inputStream = new FileInputStream(path); Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new FrameworkException("❗ Sheet \"" + sheetName + "\" not found in " + path);
            }
            // Detached from its (now-closed) Workbook, but a sheet's already-parsed cell values
            // stay readable — same assumption org.apache.pdfbox's document info relies on.
            return sheet;
        } catch (IOException e) {
            throw new FrameworkException("❌ Failed to read Excel report: " + path, e);
        }
    }

    private static List<String> headerRow(Sheet sheet) {
        Row row = sheet.getRow(0);
        if (row == null) {
            return List.of();
        }
        DataFormatter formatter = new DataFormatter();
        List<String> headers = new ArrayList<>();
        for (Cell cell : row) {
            headers.add(formatter.formatCellValue(cell));
        }
        return headers;
    }

    private static int columnIndex(List<String> headers, String columnName, String path, String sheetName) {
        int index = headers.indexOf(columnName);
        if (index < 0) {
            throw new FrameworkException("❗ Column \"" + columnName + "\" not found in sheet \""
                    + sheetName + "\" of " + path + " — headers: " + headers);
        }
        return index;
    }
}
