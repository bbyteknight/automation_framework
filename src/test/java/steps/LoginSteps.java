package steps;

import context.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import pages.LoginPage;
import utils.DriverFactory;

public class LoginSteps {

    private final ScenarioContext scenarioContext;

    public LoginSteps(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Given("the user is on the login page")
    public void theUserIsOnTheLoginPage() {
        scenarioContext.set("loginPage", new LoginPage(DriverFactory.getDriver()));
    }

    @When("the user logs in with username {string} and password {string}")
    public void theUserLogsInWithUsernameAndPassword(String username, String password) {
        LoginPage loginPage = scenarioContext.get("loginPage", LoginPage.class);
        loginPage.login(username, password);
        scenarioContext.set("username", username);
    }
}
