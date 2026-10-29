package steps;

import context.ScenarioContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;
import screens.desktop.DesktopDashboardScreen;
import screens.desktop.DesktopLoginScreen;
import utils.DriverFactory;

/**
 * Steps for the sample TestLoginApp desktop_login.feature. The app itself is
 * already launched by Hooks.setUpDesktop() (the @Before("@desktop") hook), so
 * "the Test Login App is launched" just hands the scenario its first screen
 * object — same pattern as MobileLoginSteps.
 */
public class DesktopLoginSteps {

    private final ScenarioContext scenarioContext;

    public DesktopLoginSteps(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Given("the Test Login App is launched")
    public void theTestLoginAppIsLaunched() {
        scenarioContext.set("loginScreen", new DesktopLoginScreen(DriverFactory.getDriver()));
    }

    @When("the user logs in to the desktop app with username {string} and password {string}")
    public void theUserLogsInWithUsernameAndPassword(String username, String password) {
        DesktopLoginScreen loginScreen = scenarioContext.get("loginScreen", DesktopLoginScreen.class);
        scenarioContext.set("dashboardScreen", loginScreen.login(username, password));
    }

    @When("the user logs in to the desktop app with username {string} and password {string} expecting an error")
    public void theUserLogsInExpectingAnError(String username, String password) {
        DesktopLoginScreen loginScreen = scenarioContext.get("loginScreen", DesktopLoginScreen.class);
        loginScreen.loginExpectingError(username, password);
    }

    @And("the user logs out")
    public void theUserLogsOut() {
        DesktopDashboardScreen dashboardScreen = scenarioContext.get("dashboardScreen", DesktopDashboardScreen.class);
        scenarioContext.set("loginScreen", dashboardScreen.logout());
    }

    @Then("the dashboard should be displayed")
    public void theDashboardShouldBeDisplayed() {
        DesktopDashboardScreen dashboardScreen = scenarioContext.get("dashboardScreen", DesktopDashboardScreen.class);
        Assert.assertTrue(dashboardScreen.isDisplayed(), "Expected the dashboard view to be displayed");
    }

    @Then("the welcome message should be {string}")
    public void theWelcomeMessageShouldBe(String expected) {
        DesktopDashboardScreen dashboardScreen = scenarioContext.get("dashboardScreen", DesktopDashboardScreen.class);
        Assert.assertEquals(dashboardScreen.getWelcomeMessage(), expected);
    }

    @Then("the status should be {string}")
    public void theStatusShouldBe(String expected) {
        DesktopDashboardScreen dashboardScreen = scenarioContext.get("dashboardScreen", DesktopDashboardScreen.class);
        Assert.assertEquals(dashboardScreen.getStatus(), expected);
    }

    @Then("the error message should be {string}")
    public void theErrorMessageShouldBe(String expected) {
        DesktopLoginScreen loginScreen = scenarioContext.get("loginScreen", DesktopLoginScreen.class);
        Assert.assertTrue(loginScreen.isErrorDisplayed(), "Expected an error message to be displayed");
        Assert.assertEquals(loginScreen.getErrorMessage(), expected);
    }

    @Then("the login screen should be displayed again")
    public void theLoginScreenShouldBeDisplayedAgain() {
        DesktopLoginScreen loginScreen = scenarioContext.get("loginScreen", DesktopLoginScreen.class);
        Assert.assertTrue(loginScreen.isDisplayed(), "Expected the login view to be displayed again after logout");
    }
}
