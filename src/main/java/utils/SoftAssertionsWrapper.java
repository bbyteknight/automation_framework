package utils;

import org.assertj.core.api.SoftAssertions;

/**
 * Thin, TestNG-Assert-flavoured wrapper around AssertJ's SoftAssertions, for steps that check
 * several things about the same outcome (e.g. a cart's item count AND total) and want to see
 * every mismatch in one failure instead of stopping at the first assertEquals(). Chainable:
 * collect checks, then call assertAll() once to raise (if anything failed).
 */
public class SoftAssertionsWrapper {

    private final SoftAssertions softly = new SoftAssertions();

    public SoftAssertionsWrapper assertEquals(Object actual, Object expected, String message) {
        softly.assertThat(actual).as(message).isEqualTo(expected);
        return this;
    }

    public SoftAssertionsWrapper assertTrue(boolean condition, String message) {
        softly.assertThat(condition).as(message).isTrue();
        return this;
    }

    public SoftAssertionsWrapper assertFalse(boolean condition, String message) {
        softly.assertThat(condition).as(message).isFalse();
        return this;
    }

    public SoftAssertionsWrapper assertNotNull(Object actual, String message) {
        softly.assertThat(actual).as(message).isNotNull();
        return this;
    }

    public SoftAssertionsWrapper assertContains(String actual, String expectedSubstring, String message) {
        softly.assertThat(actual).as(message).contains(expectedSubstring);
        return this;
    }

    /** Raises one assertion error summarizing every failed check collected above, if any. */
    public void assertAll() {
        softly.assertAll();
    }
}
