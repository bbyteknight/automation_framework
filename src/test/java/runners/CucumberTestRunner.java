package runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
        features = "src/test/resources/features",
        glue = {"steps", "hooks"},
        plugin = {
                "pretty",
                "html:target/cucumber-reports/report.html",
                "json:target/cucumber-reports/run1.json",
                "rerun:target/rerun.txt",
                "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"
        },
        monochrome = true,
        // Fallback only — pom.xml's systemPropertyVariables always sets the
        // cucumber.filter.tags JVM property (from the active Maven profile, or
        // "@web" by default), which overrides this attribute on every run.
        // See README.md for the full CLI/profile reference.
        tags = "@web"
)
public class CucumberTestRunner extends AbstractTestNGCucumberTests {
}
