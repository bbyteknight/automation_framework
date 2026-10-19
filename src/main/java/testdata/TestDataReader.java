package testdata;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import config.ConfigReader;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads externalised test data from src/test/resources/testdata/{env}/..., where {env} is
 * whatever ConfigReader resolves it to (dev/qa/staging) — so the same feature/step code picks up
 * different fixtures per environment with no code change. Supports JSON (keyed records), CSV, and
 * Excel (both row-based, keyed by a "key" column), keeping feature files and step definitions free
 * of literal usernames, product names, API payload values, etc.
 */
public final class TestDataReader {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private TestDataReader() {
    }

    /** Reads a JSON object file keyed by record name (e.g. {"standardUser": {...}}) and
     * deserializes the entry under {@code key} into {@code type}. */
    public static <T> T readJsonEntry(String fileName, String key, Class<T> type) {
        Map<String, Object> all = readJson(fileName);
        Object entry = all.get(key);
        if (entry == null) {
            throw new RuntimeException("❗ No test data entry \"" + key + "\" found in " + resourcePath(fileName));
        }
        return OBJECT_MAPPER.convertValue(entry, type);
    }

    private static Map<String, Object> readJson(String fileName) {
        try (InputStream is = openStream(fileName)) {
            return OBJECT_MAPPER.readValue(is, new TypeReference<Map<String, Object>>() {
            });
        } catch (IOException e) {
            throw new RuntimeException("❌ Failed to read test data file: " + resourcePath(fileName), e);
        }
    }

    /** Reads a CSV file with a header row and finds the row where the "key" column matches, then
     * maps it onto {@code type} by matching column names to field names. */
    public static <T> T readCsvEntry(String fileName, String key, Class<T> type) {
        return findByKey(readCsv(fileName), fileName, key, type);
    }

    public static List<Map<String, String>> readCsv(String fileName) {
        try (InputStream is = openStream(fileName);
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            CSVParser parser = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build()
                    .parse(reader);
            List<Map<String, String>> rows = new ArrayList<>();
            parser.forEach(record -> rows.add(new LinkedHashMap<>(record.toMap())));
            return rows;
        } catch (IOException e) {
            throw new RuntimeException("❌ Failed to read test data file: " + resourcePath(fileName), e);
        }
    }

    /** Reads a sheet with a header row and finds the row where the "key" column matches, then
     * maps it onto {@code type} by matching column names to field names. */
    public static <T> T readExcelEntry(String fileName, String sheetName, String key, Class<T> type) {
        return findByKey(readExcel(fileName, sheetName), fileName, key, type);
    }

    public static List<Map<String, String>> readExcel(String fileName, String sheetName) {
        try (InputStream is = openStream(fileName); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new RuntimeException("❗ Sheet \"" + sheetName + "\" not found in " + resourcePath(fileName));
            }
            DataFormatter formatter = new DataFormatter();
            Iterator<Row> rowIterator = sheet.iterator();
            if (!rowIterator.hasNext()) {
                return List.of();
            }
            List<String> headers = new ArrayList<>();
            for (Cell cell : rowIterator.next()) {
                headers.add(formatter.formatCellValue(cell));
            }
            List<Map<String, String>> rows = new ArrayList<>();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Map<String, String> rowData = new LinkedHashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = row.getCell(i);
                    rowData.put(headers.get(i), cell == null ? "" : formatter.formatCellValue(cell));
                }
                rows.add(rowData);
            }
            return rows;
        } catch (IOException e) {
            throw new RuntimeException("❌ Failed to read test data file: " + resourcePath(fileName), e);
        }
    }

    private static <T> T findByKey(List<Map<String, String>> rows, String fileName, String key, Class<T> type) {
        Map<String, String> row = rows.stream()
                .filter(r -> key.equals(r.get("key")))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "❗ No test data row with key \"" + key + "\" found in " + resourcePath(fileName)));
        return OBJECT_MAPPER.convertValue(row, type);
    }

    private static InputStream openStream(String fileName) {
        String path = resourcePath(fileName);
        InputStream is = TestDataReader.class.getClassLoader().getResourceAsStream(path);
        if (is == null) {
            throw new RuntimeException("❗ Test data file not found on classpath: " + path);
        }
        return is;
    }

    private static String resourcePath(String fileName) {
        return "testdata/" + ConfigReader.get("env") + "/" + fileName;
    }
}
