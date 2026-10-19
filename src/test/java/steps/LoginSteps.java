package steps;

import context.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;
import pages.LoginPage;
import testdata.TestDataReader;
import testdata.model.TestUser;
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

    @When("the user logs in with the {string} test account")
    public void theUserLogsInWithTheTestAccount(String accountKey) {
        TestUser user = TestDataReader.readJsonEntry("users.json", accountKey, TestUser.class);
        theUserLogsInWithUsernameAndPassword(user.getUsername(), user.getPassword());
    }

    @When("the user logs in with username {string} and password {string}")
    public void theUserLogsInWithUsernameAndPassword(String username, String password) {
        LoginPage loginPage = scenarioContext.get("loginPage", LoginPage.class);
        loginPage.login(username, password);
        scenarioContext.set("username", username);
    }

    @Then("the user should see an error message containing {string}")
    public void theUserShouldSeeAnErrorMessageContaining(String expectedMessage) {
        LoginPage loginPage = scenarioContext.get("loginPage", LoginPage.class);
        String flashMessage = loginPage.getFlashMessage();
        Assert.assertTrue(flashMessage.contains(expectedMessage),
                "Expected flash message to contain \"" + expectedMessage + "\" but was: \"" + flashMessage + "\"");
    }
}
