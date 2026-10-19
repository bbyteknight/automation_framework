package exceptions;

/**
 * Base type for every framework-specific unchecked exception. Unchecked (not checked) so it
 * never forces try/catch noise into step definitions or page/screen/client code — callers that
 * do want to react to a specific failure can still catch one of the subclasses below.
 */
public class FrameworkException extends RuntimeException {

    public FrameworkException(String message) {
        super(message);
    }

    public FrameworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
