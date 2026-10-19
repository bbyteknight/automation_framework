package exceptions;

/** RetryHelper gave up after exhausting every attempt; the cause is the last attempt's failure. */
public class RetryExhaustedException extends FrameworkException {

    public RetryExhaustedException(String message, Throwable cause) {
        super(message, cause);
    }
}
