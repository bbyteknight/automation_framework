package steps;

import context.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;
import screens.CartScreen;
import screens.ProductDetailScreen;
import screens.ProductsScreen;
import utils.DriverFactory;

public class MobileShoppingSteps {

    private final ScenarioContext scenarioContext;

    public MobileShoppingSteps(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Given("the user is on the products screen")
    public void theUserIsOnTheProductsScreen() {
        scenarioContext.set("productsScreen", new ProductsScreen(DriverFactory.getDriver()));
    }

    @When("the user adds {string} to the cart")
    public void theUserAddsProductToTheCart(String productName) {
        ProductsScreen productsScreen = scenarioContext.get("productsScreen", ProductsScreen.class);
        ProductDetailScreen detailScreen = productsScreen.selectProduct(productName);
        detailScreen.addToCart();
        scenarioContext.set("productDetailScreen", detailScreen);
    }

    @When("the user opens the cart")
    public void theUserOpensTheCart() {
        ProductDetailScreen detailScreen = scenarioContext.get("productDetailScreen", ProductDetailScreen.class);
        scenarioContext.set("cartScreen", detailScreen.openCart());
    }

    @Then("the cart should contain {string}")
    public void theCartShouldContain(String expectedItemsText) {
        CartScreen cartScreen = scenarioContext.get("cartScreen", CartScreen.class);
        Assert.assertEquals(cartScreen.getItemsCount(), expectedItemsText,
                "Expected cart items label to be \"" + expectedItemsText + "\"");
    }

    @Then("the cart total should be {string}")
    public void theCartTotalShouldBe(String expectedTotal) {
        CartScreen cartScreen = scenarioContext.get("cartScreen", CartScreen.class);
        Assert.assertEquals(cartScreen.getTotalPrice(), expectedTotal,
                "Expected cart total to be \"" + expectedTotal + "\"");
    }
}
