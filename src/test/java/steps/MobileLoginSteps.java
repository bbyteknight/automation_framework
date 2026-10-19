package steps;

import context.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;
import screens.HomeScreen;
import screens.LoginScreen;
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

    @When("the user submits login with username {string} and password {string}")
    public void theUserSubmitsLoginWithUsernameAndPassword(String username, String password) {
        LoginScreen loginScreen = scenarioContext.get("loginScreen", LoginScreen.class);
        scenarioContext.set("homeScreen", loginScreen.login(username, password));
    }

    @Then("the user should be logged in")
    public void theUserShouldBeLoggedIn() {
        HomeScreen homeScreen = scenarioContext.get("homeScreen", HomeScreen.class);
        homeScreen.openMenu();
        Assert.assertTrue(homeScreen.isLoggedIn(),
                "Expected the drawer menu to show \"Log Out\" after a successful login");
    }
}
