package steps;

import context.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;
import screens.CartScreen;
import screens.ProductDetailScreen;
import screens.ProductsScreen;
import testdata.TestDataReader;
import testdata.model.Product;
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

    @When("the user adds the {string} test product to the cart")
    public void theUserAddsTheTestProductToTheCart(String productKey) {
        Product product = TestDataReader.readCsvEntry("products.csv", productKey, Product.class);
        scenarioContext.set("testProduct", product);
        ProductsScreen productsScreen = scenarioContext.get("productsScreen", ProductsScreen.class);
        ProductDetailScreen detailScreen = productsScreen.selectProduct(product.getName());
        detailScreen.addToCart();
        scenarioContext.set("productDetailScreen", detailScreen);
    }

    @When("the user opens the cart")
    public void theUserOpensTheCart() {
        ProductDetailScreen detailScreen = scenarioContext.get("productDetailScreen", ProductDetailScreen.class);
        scenarioContext.set("cartScreen", detailScreen.openCart());
    }

    @Then("the cart should contain the expected item count")
    public void theCartShouldContainTheExpectedItemCount() {
        Product product = scenarioContext.get("testProduct", Product.class);
        CartScreen cartScreen = scenarioContext.get("cartScreen", CartScreen.class);
        Assert.assertEquals(cartScreen.getItemsCount(), product.getExpectedCartText(),
                "Expected cart items label to be \"" + product.getExpectedCartText() + "\"");
    }

    @Then("the cart total should match the expected total")
    public void theCartTotalShouldMatchTheExpectedTotal() {
        Product product = scenarioContext.get("testProduct", Product.class);
        CartScreen cartScreen = scenarioContext.get("cartScreen", CartScreen.class);
        Assert.assertEquals(cartScreen.getTotalPrice(), product.getExpectedTotal(),
                "Expected cart total to be \"" + product.getExpectedTotal() + "\"");
    }
}
