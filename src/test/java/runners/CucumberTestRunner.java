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
        // CI default: skip work-in-progress scenarios. Override with
        // -Dcucumber.filter.tags="..." (see GHERKIN_GUIDE.md) to run a different subset.
        tags = "not @wip"
)
public class CucumberTestRunner extends AbstractTestNGCucumberTests {
}
