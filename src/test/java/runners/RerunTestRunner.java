package runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
        features = "@target/rerun.txt",
        glue = {"steps", "hooks"},
        plugin = {
                "pretty",
                "html:target/cucumber-reports/rerun-report.html",
                "json:target/cucumber-reports/run2.json"
        },
        monochrome = true
)
public class RerunTestRunner extends AbstractTestNGCucumberTests {
}
