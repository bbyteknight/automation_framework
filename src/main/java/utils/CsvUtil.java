package utils;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import exceptions.FrameworkException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Create (for test fixtures) and read {@code .csv} exported reports via OpenCSV — rows, individual
 * cells, row counts, and column lookups — so steps/tests can validate an exported report's content
 * directly, including against API/DB data.
 */
public final class CsvUtil {

    private CsvUtil() {
    }

    /** Creates a CSV with a header row, then one row per entry of {@code rows}. */
    public static File createCsv(String destPath, List<String> headers, List<List<String>> rows) {
        File destFile = new File(destPath);
        FileHelper.ensureDirectoryExists(destFile.getAbsoluteFile().getParent());

        try (CSVWriter writer = new CSVWriter(new FileWriter(destFile))) {
            writer.writeNext(headers.toArray(new String[0]));
            for (List<String> row : rows) {
                writer.writeNext(row.toArray(new String[0]));
            }
            return destFile;
        } catch (IOException e) {
            throw new FrameworkException("❌ Failed to create CSV report: " + destPath, e);
        }
    }

    /** Reads just the header row. */
    public static List<String> headers(String path) {
        try (CSVReader reader = new CSVReader(new FileReader(path))) {
            String[] header = reader.readNext();
            return header == null ? List.of() : Arrays.asList(header);
        } catch (IOException | CsvValidationException e) {
            throw new FrameworkException("❌ Failed to read CSV report headers: " + path, e);
        }
    }

    /** Reads every data row (excluding the header), keyed by header name. */
    public static List<Map<String, String>> readRows(String path) {
        try (CSVReaderHeaderAware reader = new CSVReaderHeaderAware(new FileReader(path))) {
            List<Map<String, String>> rows = new ArrayList<>();
            Map<String, String> row;
            while ((row = reader.readMap()) != null) {
                rows.add(row);
            }
            return rows;
        } catch (IOException | CsvValidationException e) {
            throw new FrameworkException("❌ Failed to read CSV report: " + path, e);
        }
    }

    /** Reads one cell by its 0-based data row index (excluding the header) and column name. */
    public static String readCell(String path, int dataRowIndex, String columnName) {
        List<Map<String, String>> rows = readRows(path);
        if (dataRowIndex < 0 || dataRowIndex >= rows.size()) {
            throw new FrameworkException("❗ No data row " + dataRowIndex + " in " + path + " (" + rows.size() + " row(s))");
        }
        Map<String, String> row = rows.get(dataRowIndex);
        if (!row.containsKey(columnName)) {
            throw new FrameworkException("❗ Column \"" + columnName + "\" not found in " + path + " — headers: " + row.keySet());
        }
        return row.get(columnName);
    }

    /** Number of data rows, excluding the header row. */
    public static int rowCount(String path) {
        return readRows(path).size();
    }

    /** Every value in the named column, top to bottom, excluding the header. */
    public static List<String> columnValues(String path, String columnName) {
        return readRows(path).stream()
                .map(row -> row.get(columnName))
                .toList();
    }
}
