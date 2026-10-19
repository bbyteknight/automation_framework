package utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Shared date/time formatting — one place for the "filename-safe timestamp" format used by
 * screenshots, reports, and logs, instead of each class building its own SimpleDateFormat. */
public final class DateHelper {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private DateHelper() {
    }

    /** A filename-safe "now" timestamp, e.g. "20260101_153045". */
    public static String timestamp() {
        return LocalDateTime.now().format(TIMESTAMP_FORMAT);
    }

    /** Today's date, e.g. "2026-01-01". */
    public static String today() {
        return LocalDate.now().format(DATE_FORMAT);
    }

    public static String format(LocalDateTime dateTime, String pattern) {
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }
}
