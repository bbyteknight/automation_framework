package context;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds state for the duration of a single Cucumber scenario.
 * PicoContainer creates one instance of this class per scenario and injects
 * it into every step/hook class constructor that needs it, so data shared
 * here never leaks into other scenarios and no static/ThreadLocal state is needed.
 */
public class ScenarioContext {

    private final Map<String, Object> data = new HashMap<>();

    public void set(String key, Object value) {
        data.put(key, value);
    }

    public Object get(String key) {
        return data.get(key);
    }

    public <T> T get(String key, Class<T> type) {
        return type.cast(data.get(key));
    }

    public boolean contains(String key) {
        return data.containsKey(key);
    }
}
