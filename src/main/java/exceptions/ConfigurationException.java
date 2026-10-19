package exceptions;

/** A required config value (property, env var, credential) is missing or invalid. */
public class ConfigurationException extends FrameworkException {

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
