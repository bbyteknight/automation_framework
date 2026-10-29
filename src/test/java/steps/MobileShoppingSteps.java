package steps;

import context.ScenarioContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import screens.mobile.CartScreen;
import screens.mobile.ProductDetailScreen;
import screens.mobile.ProductsScreen;
import testdata.TestDataReader;
import testdata.model.Product;
import utils.DriverFactory;
import utils.SoftAssertionsWrapper;

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

    // Soft-asserted together: if the cart's item count AND total are both wrong, the failure
    // reports both mismatches instead of stopping at whichever check runs first.
    @Then("the cart should match the expected item count and total")
    public void theCartShouldMatchTheExpectedItemCountAndTotal() {
        Product product = scenarioContext.get("testProduct", Product.class);
        CartScreen cartScreen = scenarioContext.get("cartScreen", CartScreen.class);
        new SoftAssertionsWrapper()
                .assertEquals(cartScreen.getItemsCount(), product.getExpectedCartText(),
                        "Cart items label should be \"" + product.getExpectedCartText() + "\"")
                .assertEquals(cartScreen.getTotalPrice(), product.getExpectedTotal(),
                        "Cart total should be \"" + product.getExpectedTotal() + "\"")
                .assertAll();
    }
}
