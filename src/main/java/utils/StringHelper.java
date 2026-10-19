package utils;

/** Shared string helpers with no third-party dependency — small enough that pulling in a whole
 * string-utils library wasn't worth it. */
public final class StringHelper {

    private StringHelper() {
    }

    public static boolean isNullOrBlank(String value) {
        return value == null || value.isBlank();
    }

    /** Turns a free-text value into a filesystem-safe fragment, e.g. for screenshot/report filenames. */
    public static String sanitizeForFilename(String value) {
        return value == null ? "" : value.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    public static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    /** Redacts everything but the last {@code visibleChars}, e.g. for logging a token/secret
     * safely: "mask("Bearer abc123xyz", 4)" -> "********xyz". Never logs the real length either,
     * beyond a fixed mask width, so the secret's size can't be inferred from the log. */
    public static String mask(String secret, int visibleChars) {
        if (secret == null || secret.isEmpty()) {
            return secret;
        }
        String suffix = secret.length() > visibleChars ? secret.substring(secret.length() - visibleChars) : "";
        return "********" + suffix;
    }
}
