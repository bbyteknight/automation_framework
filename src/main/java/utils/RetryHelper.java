package utils;

import exceptions.RetryExhaustedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

/**
 * Retries an infrastructure-level operation (a flaky network call, a driver/session creation
 * against a remote grid, a cloud API upload) a fixed number of times before giving up — not a
 * substitute for WaitUtils' explicit UI waits, which already handle element-timing flakiness.
 *
 * Uses LockSupport.parkNanos for the backoff delay rather than Thread.sleep(), which checkstyle
 * bans project-wide (see checkstyle.xml) specifically to stop fixed sleeps standing in for a
 * proper UI wait; a backoff between retries of a flaky infra call is a different concern.
 */
public final class RetryHelper {

    private static final Logger logger = LogManager.getLogger(RetryHelper.class);

    private RetryHelper() {
    }

    /** Retries on any exception. */
    public static <T> T retry(Supplier<T> action, int maxAttempts, Duration delayBetweenAttempts) {
        return retry(action, maxAttempts, delayBetweenAttempts, Exception.class);
    }

    public static void retry(Runnable action, int maxAttempts, Duration delayBetweenAttempts) {
        retry(() -> {
            action.run();
            return null;
        }, maxAttempts, delayBetweenAttempts);
    }

    /** Retries only when the failure is an instance of one of {@code retryableExceptions} —
     * anything else is rethrown immediately on the first attempt. */
    @SafeVarargs
    public static <T> T retry(Supplier<T> action, int maxAttempts, Duration delayBetweenAttempts,
                               Class<? extends Exception>... retryableExceptions) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1, was " + maxAttempts);
        }

        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return action.get();
            } catch (Exception e) {
                if (!isRetryable(e, retryableExceptions)) {
                    throw asRuntimeException(e);
                }
                lastFailure = asRuntimeException(e);
                logger.warn("⚠️ Attempt {}/{} failed: {}", attempt, maxAttempts, e.getMessage());
                if (attempt < maxAttempts && !delayBetweenAttempts.isZero()) {
                    LockSupport.parkNanos(delayBetweenAttempts.toNanos());
                }
            }
        }
        throw new RetryExhaustedException(
                "❗ Action failed after " + maxAttempts + " attempts", lastFailure);
    }

    private static boolean isRetryable(Exception e, Class<? extends Exception>[] retryableExceptions) {
        for (Class<? extends Exception> type : retryableExceptions) {
            if (type.isInstance(e)) {
                return true;
            }
        }
        return false;
    }

    private static RuntimeException asRuntimeException(Exception e) {
        return e instanceof RuntimeException runtimeException ? runtimeException : new RuntimeException(e);
    }
}
