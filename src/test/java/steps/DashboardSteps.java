package steps;

import context.ScenarioContext;
import io.cucumber.java.en.Then;
import org.testng.Assert;
import pages.SecurePage;
import utils.DriverFactory;

public class DashboardSteps {

    private final ScenarioContext scenarioContext;

    public DashboardSteps(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Then("the user should land on the secure area")
    public void theUserShouldLandOnTheSecureArea() {
        Assert.assertTrue(scenarioContext.contains("username"), "username was not set by LoginSteps");

        SecurePage securePage = new SecurePage(DriverFactory.getDriver());
        Assert.assertTrue(securePage.isSecureAreaDisplay(), "Secure area was not displayed");
    }
}
