package exceptions;

/** A test data file/entry (JSON, CSV, Excel) could not be found, read, or parsed. */
public class TestDataException extends FrameworkException {

    public TestDataException(String message) {
        super(message);
    }

    public TestDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
