package steps;

import context.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;
import screens.mobile.HomeScreen;
import screens.mobile.LoginScreen;
import testdata.TestDataReader;
import testdata.model.TestUser;
import utils.DriverFactory;

public class MobileLoginSteps {

    private final ScenarioContext scenarioContext;

    public MobileLoginSteps(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Given("the user is on the home screen")
    public void theUserIsOnTheHomeScreen() {
        scenarioContext.set("homeScreen", new HomeScreen(DriverFactory.getDriver()));
    }

    @When("the user opens the login screen")
    public void theUserOpensTheLoginScreen() {
        HomeScreen homeScreen = scenarioContext.get("homeScreen", HomeScreen.class);
        scenarioContext.set("loginScreen", homeScreen.openLoginScreen());
    }

    @When("the user submits login with the {string} test account")
    public void theUserSubmitsLoginWithTheTestAccount(String accountKey) {
        TestUser user = TestDataReader.readJsonEntry("users.json", accountKey, TestUser.class);
        LoginScreen loginScreen = scenarioContext.get("loginScreen", LoginScreen.class);
        scenarioContext.set("homeScreen", loginScreen.login(user.getUsername(), user.getPassword()));
    }

    @Then("the user should be logged in")
    public void theUserShouldBeLoggedIn() {
        HomeScreen homeScreen = scenarioContext.get("homeScreen", HomeScreen.class);
        homeScreen.openMenu();
        Assert.assertTrue(homeScreen.isLoggedIn(),
                "Expected the drawer menu to show \"Log Out\" after a successful login");
    }
}
