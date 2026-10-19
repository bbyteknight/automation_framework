package config;

import exceptions.ConfigurationException;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

public class ConfigReader {

    private static final Properties properties = new Properties();
    private static final String DEFAULT_ENV = "dev";

    // Local-only, optional (.env is gitignored — see .env.example and SECRETS.md).
    // Simulates OS env vars for local dev; real CI/pipeline env vars take precedence.
    private static final Dotenv dotenv = Dotenv.configure()
            .ignoreIfMissing()
            .ignoreIfMalformed()
            .load();

    private static final Map<String, String> DEFAULTS = Map.of(
            "env", DEFAULT_ENV,
            "platform", "web",
            "browser", "chrome",
            "browsers", "chrome",
            "headless", "false",
            "executionMode", "local",
            "timeout", "10",
            "implicitTimeout", "10",
            "explicitTimeout", "15",
            "pageLoadTimeout", "30"
    );

    public static void loadProperties() {
        String env = resolveEnv();
        String resourcePath = "config/" + env + ".properties";

        try (InputStream is = ConfigReader.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new ConfigurationException("❗ Config file not found on classpath: " + resourcePath);
            }
            properties.load(is);
            System.out.println("✅ Loaded configuration for environment [" + env + "] from " + resourcePath);
        } catch (IOException e) {
            throw new ConfigurationException("❌ Failed to load " + resourcePath + ": " + e.getMessage(), e);
        }
    }

    /**
     * Resolves a config value using this priority order:
     * 1. JVM system property, e.g. -Dbrowser=firefox (highest — CLI/pipeline override)
     * 2. OS environment variable, e.g. BROWSER=firefox (pipeline/CI override)
     * 3. Local .env file, e.g. BROWSER=firefox (gitignored, dev-only substitute for #2)
     * 4. Environment properties file (dev/qa/staging.properties) loaded via loadProperties()
     * 5. Built-in default (see DEFAULTS) — last resort before failing
     */
    public static String get(String key) {
        return get(key, DEFAULTS.get(key));
    }

    /**
     * Same priority order as {@link #get(String)}, but falls back to the given
     * default instead of (or in addition to) the built-in DEFAULTS when no value
     * is found via system property, env variable, .env file, or config file.
     */
    public static String get(String key, String defaultValue) {
        String systemProperty = System.getProperty(key);
        if (systemProperty != null && !systemProperty.isBlank()) {
            return systemProperty;
        }

        String envVarName = toEnvVarName(key);

        String envVariable = System.getenv(envVarName);
        if (envVariable != null && !envVariable.isBlank()) {
            return envVariable;
        }

        String dotenvValue = dotenv.get(envVarName);
        if (dotenvValue != null && !dotenvValue.isBlank()) {
            return dotenvValue;
        }

        String value = properties.getProperty(key);
        if (value != null) {
            return value;
        }

        if (defaultValue != null) {
            return defaultValue;
        }

        throw new ConfigurationException("❗ Property not found via system property, env variable, .env file, config file, or default: " + key);
    }

    private static String resolveEnv() {
        String env = System.getProperty("env");
        if (env == null || env.isBlank()) {
            env = System.getenv("ENV");
        }
        if (env == null || env.isBlank()) {
            env = dotenv.get("ENV");
        }
        if (env == null || env.isBlank()) {
            env = DEFAULT_ENV;
        }
        return env.toLowerCase();
    }

    private static String toEnvVarName(String key) {
        return key.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
    }
}
